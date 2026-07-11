/*
 * Copyright (C) 2005 Torren (torren@users.sourceforge.net)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


/*
 * Derived from NFCChat, a GPL chat client/server.
 * Original code can be found @ http://nfcchat.sourceforge.net
 * Our thanks to the original authors.
 */

package mekwars.server.MWChatServer;

import java.net.ServerSocket;
import java.rmi.AccessException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import megamek.logging.MMLogger;
import mekwars.server.MWChatServer.auth.Auth;
import mekwars.server.MWChatServer.auth.IAuthenticator;
import mekwars.server.MWChatServer.auth.IRoomAuthenticator;
import mekwars.server.MWChatServer.auth.NullRoomAuthenticator;
import mekwars.server.MWChatServer.auth.PasswdAuthenticator;
import mekwars.server.MWChatServer.commands.ICommands;

public class MWChatServer implements ICommands {
    private static final MMLogger LOGGER = MMLogger.create(MWChatServer.class);

    protected static Properties _properties;
    protected boolean _asciiRoomNames;

    protected TimedUserList _killedUsers;
    protected IAuthenticator _authenticator;
    protected IRoomAuthenticator _roomAuthenticator;

    protected Map<String, MWChatClient> _users = Collections.synchronizedMap(new HashMap<>());
    protected HashMap<String, RoomServer> _rooms = new HashMap<>();

    protected String _motd;

    protected ServerSocket _serverSocket;

    protected int _cumulativeLogins = 0;
    protected int _port = 0;
    protected int _kickBanSeconds = 60 * 60 * 24;

    public MWChatServer(String IPAddress, int port) throws Exception {
        Properties commandProps = new Properties();
        commandProps.setProperty("signon.class", "server.MWChatServer.commands.SignOn");
        commandProps.setProperty("signon.access", "0");
        commandProps.setProperty("comm.class", "server.MWChatServer.commands.Command");
        initCommandProcessor(commandProps);

        Properties messagesProps = new Properties();
        messagesProps.setProperty("access_denied", "Accessdenied.");
        messagesProps.setProperty("sql_error", "SQLError:{0}");
        messagesProps.setProperty("already_on", "Youarealreadysignedon");
        messagesProps.setProperty("unknown_command", "Sorry,Ididnotrecognizethatcommand.");
        messagesProps.setProperty("noroom", "Thereisnoroomcalled{0}");
        messagesProps.setProperty("reserved_name", "{0}isareservedname");
        messagesProps.setProperty("nosuchcommand", "Nosuchcommand:{0}");
        messagesProps.setProperty("nosuchuser", "Nosuchuser:{0}");
        messagesProps.setProperty("nosuchroom", "Nosuchroom:{0}");
        initTranslator(messagesProps);

        this._port = port;

        //construct authenticators
        _authenticator = new PasswdAuthenticator(this, true, false);
        _roomAuthenticator = new NullRoomAuthenticator();

        this._asciiRoomNames = true;

        /*
         * Binding to localhost by default was causing problems for ClanHomeWorlds.
         * Proper behaviour is to pass a null InetAddress to the server socket in
         * order to bind the socket to all available IPs/devices. If we check for a
         * real null and a "-1", we can know to bind on either the first run (when
         * no config is present) or when the value is missing from serverconfig.txt
         *
         * @urgru 12.4.05
         */
        if (IPAddress == null || IPAddress.equals("-1")) {
            _serverSocket = new java.net.ServerSocket(_port, -1, null);
        } else {
            _serverSocket = new java.net.ServerSocket(_port, -1, java.net.InetAddress.getByName(IPAddress));
        }

        // this is called but never initlized.
        _killedUsers = new TimedUserList(60 * 60 * 2);
    }

    public void initCommandProcessor(java.util.Properties p) {
        CommandProcessorRemote.init(p);
    }

    public void initTranslator(java.util.Properties p) {
        Translator.init(p);
    }

    public static java.util.Properties getProperties() {
        return _properties;
    }

    public int getKickBanSeconds() {
        return _kickBanSeconds;
    }

    /*protected Dispatcher createDispatcher() {
        return new Dispatcher();
    }

    public Dispatcher getDispatcher() {
        return _dispatcher;
    }*/

    public int getRoomAccessLevel(MWChatClient client, RoomServer rs) {
        return _roomAuthenticator.getAccessLevel(client, rs);
    }

    public MWChatClient getRoomNextOp(RoomServer rs) {
        return _roomAuthenticator.getNextOp(rs);
    }

    /**
     * Sign on to the server
     *
     * @param client   the MWChatClient
     * @param password
     *
     * @throws AccessException is the login failed
     */
    public boolean signOn(MWChatClient client, String password) throws Exception {
        String userId = client.getUserId();
        validateUserId(userId);
        Auth auth = _authenticator.authenticate(client, password);
        int access = auth.getAccess();
        LOGGER.info(client.getUserId() + " signon from "
                               + client.getHost() + ".  Access = " + access
                               + (client.getTunneling() ? " (tunneling)" : ""));

        if (access == IAuthenticator.NONE || _killedUsers.contains(client.getKey())) {
            throw new Exception(ACCESS_DENIED);
        }

        client.setUserId(auth.getUserId());
        synchronized (_users) {
            LOGGER.info("signOn: " + client.getUserId());
            // if signed on locally, let new take precedence
            MWChatClient oldC = _users.get(clientKey(client));
            if (oldC != null) {
                oldC.killed(client.getUserId(), "Terminated by signing on elsewhere");
                LOGGER.error("Terminated by signing on elsewhere");
                signOff(oldC);
            }

            client.setAccessLevel(access);
            _users.put(clientKey(client), client);
            _cumulativeLogins++;
            try {
                this.joinRoom(client, "Main Chat", "");
            } catch (Exception ex) {
                LOGGER.error("Unable to join room");
                LOGGER.error(ex, "");
            }
        }

        client.ackSignOn(auth.getUserId());

        return true;
    }

    /**
     * Determines if a user id is valid. Ensures that the name is made up of alphanumerical characters and contains no
     * spaces.
     */
    protected void validateUserId(String user) throws Exception {
        if (getName().toLowerCase().equals(user.toLowerCase())) {throw new Exception(user + " is a reserved name");}

        if (user.toLowerCase().startsWith("war bot")) {throw new Exception(ACCESS_DENIED);}

        char[] chars = user.toLowerCase().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (!(Character.isLetterOrDigit(chars[i]) || ch == '_' || ch == '-'
                        || ch == '\\' || ch == '^' || ch == '`' || ch == '|'
                        || ch == '[' || ch == '{' || ch == ']' || ch == '}'
                        || ch == '(' || ch == ')' || ch == '\'')) {
                throw new Exception(INVALID_CHARACTER);
            }
        }
    }

    /**
     * returns a key for referencing the users hashmap. mainly to avoid case-sensitivity problems.
     */
    public static String clientKey(MWChatClient client) {
        return client.getKey();
    }

    /**
     * Sign off of the system. This is called by the MWChatClient when the socket unexpectedly closes, or when the user
     * quits.
     */
    public void signOff(MWChatClient client) {
        if (client.getAccessLevel() == IAuthenticator.NONE) {

            // not authenticated means not logged in.
            // just kill the client and return
            if (clientKey(client) != null) {
                try {
                    _users.remove(clientKey(client));
                } catch (Exception ex) {
                    LOGGER.error(ex, "");
                }
            }

            client.die();
            return;
        }

        Object o = null;
        synchronized (_users) {
            o = _users.remove(clientKey(client));
        }

        if (o != null) {
            // inform all the rooms that this user is gone
            synchronized (_rooms) {

                for (String key : _rooms.keySet()) {
                    RoomServer room = _rooms.get(key);
                    room.part(client, true);
                    if (room.isEmpty()) {
                        LOGGER.info("Removing empty room: " + key);
                        _rooms.remove(key);
                    }
                }
            }
        }

        client.die();
    }

    public void joinRoom(MWChatClient client, String roomName, String password) throws Exception {
        String key = roomKey(roomName);
        RoomServer room = _rooms.get(key);
        synchronized (_rooms) {
            if (room == null) {
                if (!_roomAuthenticator.isCreateAllowed(client, roomName,
                      password)) {
                    throw new Exception(ICommands.ROOM_ACCESS_DENIED);
                }
                room = _rooms.get(roomKey(roomName));
                if (room == null) {
                    if (_asciiRoomNames) {
                        for (int i = 0; i < roomName.length(); i++) {
                            int c = roomName.charAt(i);
                            // don't include space or DEL
                            if (c <= 32 && c >= 128) {
                                LOGGER.info(client.getUserId() + " room creation rejected: " + roomName);
                                throw new Exception(ICommands.INVALID_CHARACTER);
                            }
                        }
                    }
                    LOGGER.info(client.getUserId()
                                           + " created new room: " + roomName);
                    room = createRoomServer(roomName, password);
                    _rooms.put(roomKey(room), room);
                }
            }
        }
        room.join(client, password);
    }

    public String getName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName() + ":" + _port;
        } catch (java.net.UnknownHostException e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * returns a key for referencing the rooms hashmap. mainly to avoid case-sensitivity problems.
     */
    public static String roomKey(String room) {
        return room.toLowerCase();
    }

    public RoomServer createRoomServer(String roomName, String password) {
        return new RoomServer(roomName, password, this);
    }

    /**
     * returns a key for referencing the rooms hashmap. mainly to avoid case-sensitivity problems.
     */
    public static String roomKey(RoomServer room) {
        return roomKey(room.getName());
    }

    /**
     * Kick a user off the system.
     */
    public void kill(String victim, MWChatClient killer, String message) {
        if (killer.getAccessLevel() < IAuthenticator.MODERATOR) {
            killer.error(ACCESS_DENIED, KILL + " " + victim);
            return;
        }

        String killedKey = null;
        MWChatClient c = getClient(victim);
        if (c == null) {
            killer.error(NO_SUCH_USER, victim);
            // add user to _killedUsers even if he's not logged on
            String victimId = _authenticator.getUserId(victim);
            if (victimId != null) {
                killedKey = MWChatClient.getKey(victimId);
            }
        } else {
            LOGGER.info(victim + " kicked off by " + killer._userId);
            killedKey = c.getKey();
            c.killed(killer.getUserId(), message);
            signOff(c);
            killer.ackKill(victim);
        }
        if (killedKey != null) {
            // killer.generalMessage(Translator.getMessage("kill_queued",
            // killedKey, String.valueOf(_killBanMinutes)));
            _killedUsers.add(killedKey);
        }
    }

    /**
     * Get a MWChatClient by name
     */
    public MWChatClient getClient(String target) {
        try {
            return _users.get(clientKey(target));
        } catch (Exception ex) {
            LOGGER.error(ex, "");
            return null;
        }
    }

    /**
     * returns a key for referencing the users hashmap. mainly to avoid case-sensitivity problems.
     */
    public static String clientKey(String client) {

        // Sometimes bad strings are set up. Not much to do about it except
        // return the null and hope for the best --Torren.
        if (client == null) {return null;}

        try {
            return client.toLowerCase();
        } catch (Exception ex) {
            LOGGER.error(ex, "");
            return null;
        }
    }

    /**
     * Kick a user off the system by the system itself.
     */
    public void kill(String victim, String message) {
        String killedKey = null;
        MWChatClient c = getClient(victim);
        if (c == null) {
            // add user to _killedUsers even if he's not logged on
            String victimId = _authenticator.getUserId(victim);
            if (victimId != null) {
                killedKey = MWChatClient.getKey(victimId);
            }
        } else {
            LOGGER.info(victim + " kicked off by server.");
            killedKey = c.getKey();
            signOff(c);
        }
        if (killedKey != null) {
            // killer.generalMessage(Translator.getMessage("kill_queued",
            // killedKey, String.valueOf(_killBanMinutes)));
            _killedUsers.add(killedKey);
        }
    }

    /**
     * Tells the server to begin accepting connections.
     */
    protected void acceptConnections() {

        mekwars.server.MWChatServer.MWChatServer.PingThread pingKeepAlive = new mekwars.server.MWChatServer.MWChatServer.PingThread(
              this);
        pingKeepAlive.start();

        LOGGER.info("Accepting socket connections on port " + _port);
        while (true) {
            try {
                java.net.Socket s = _serverSocket.accept();
                //s.setTcpNoDelay(true);// couldn't hurt
                //s.setKeepAlive(false);
                //s.setSoTimeout(15000);// 15 second timeout
                //s.setSoLinger(false, 0);
                // MWChatClient client =
                createMWChatClient(s);
            } catch (java.io.IOException e) {
                LOGGER.error(e, "");
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    LOGGER.error(ex, "");
                }
            } catch (Exception ex) {
                LOGGER.error(ex, "");
                try {
                    Thread.sleep(1000);
                } catch (Exception exs) {
                    LOGGER.error(exs, "");
                }
            }
        }
    }

    protected MWChatClient createMWChatClient(java.net.Socket s) throws java.io.IOException {
        return new MWChatClient(this, s);
    }

    public boolean userExists(String username) {
        //MWLogger.infoLog("userExists: " + username);
        return _users.containsKey(username.toLowerCase());
    }

    protected void sendServerPing(String server) {
        StringBuilder sb = new StringBuilder();
        sb.append(PING);
        sb.append(DELIMITER);
        sb.append(server);
        sb.append(DELIMITER);
        sb.append(System.currentTimeMillis());

        /*
         * Had issues where people where disconnecting while the ping wasbeing
         * set which caused a Concurrent Mod Error. Put in the try to protect
         * eveyone, and added a lock to _users. - Torren
         */
        try {
            synchronized (_users) {
                LOGGER.info("sendServerPing: " + server);
                java.util.Iterator<mekwars.server.MWChatServer.MWChatClient> clients = _users.values().iterator();
                while (clients.hasNext()) {
                    MWChatClient client = clients.next();
                    client.sendRaw(sb.toString());
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error while sending server ping!");
            LOGGER.error(ex, "");
        }
    }

    /**
     * Loop though all clients and make sure that the latest command or the latest pong was within the last 200 seconds.
     * If not, add to a removal pile.
     * <p>
     * After all clients are checked, sign off everyone on toRemove list.
     */
    protected void checkForPongs() {

        synchronized (_users) {

            java.util.ArrayList<mekwars.server.MWChatServer.MWChatClient> clientToRemove = new java.util.ArrayList<mekwars.server.MWChatServer.MWChatClient>();
            java.util.Iterator<mekwars.server.MWChatServer.MWChatClient> clients = _users.values().iterator();

            while (clients.hasNext()) {
                MWChatClient client = clients.next();
                if (client._connectionHandler._lastReceived + 120000 < System.currentTimeMillis()) {
                    clientToRemove.add(client);
                }
            }

            for (MWChatClient client : clientToRemove) {
                LOGGER.info("RemovalThread sign off: " + client.getUserId());
                this.signOff(client);
            }

        }
    }

    /**
     * PingThread was created because NFC 1.1-RC stopped using the heartbeat ping to the clients. MWClients still need
     * this heartbeat. This is the best thing I could think of.
     *
     * @author Torren (Jason Tighe) 11.7.05
     */
    private static final class PingThread extends Thread {

        private long waittime = 45000;// 45 second wait

        private mekwars.server.MWChatServer.MWChatServer server;

        public PingThread(mekwars.server.MWChatServer.MWChatServer server) {
            super("PingThread");
            LOGGER.info("Starting PingThread");
            this.server = server;
        }

        @Override
        public synchronized void run() {
            try {
                while (true) {

                    // send pings
                    this.wait(waittime);
                    server.sendServerPing("server");

                    // check for pongs
                    // this.wait(waittime);
                    server.checkForPongs();

                }
            } catch (Exception ex) {
                LOGGER.error("Error while trying to sleep PingThread");
                LOGGER.error(ex, "");
            }

        }
    }// end PingThread class

}// end MWChatServer class

