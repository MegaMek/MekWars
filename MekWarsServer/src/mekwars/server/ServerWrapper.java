/*
 * Copyright (C) 2004 MekWars
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
 * ClientTest.java
 *
 * Created on June 30, 2002, 5:26 PM
 */

package mekwars.server;

import java.rmi.AccessException;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.CConnector;
import mekwars.common.util.StringUtils;
import mekwars.server.MWChatServer.MWChatClient;
import mekwars.server.MWChatServer.MWChatServer;
import mekwars.server.MWChatServer.auth.Auth;
import mekwars.server.MWChatServer.auth.IAuthenticator;
import mekwars.server.MWChatServer.commands.ICommands;


public class ServerWrapper extends MWChatServer {
    private final static MMLogger LOGGER = MMLogger.create(ServerWrapper.class);
    private final MWServ myServer;

    public ServerWrapper(MWServ server) throws Exception {
        super(server.getConfigParam("SERVER_IP"), MathUtility.parseInt(server.getConfigParam("SERVER_PORT"), 2350));
        this.myServer = server;
    }

    public static ServerWrapper createServer(MWServ server) throws Exception {
        return new ServerWrapper(server);
    }

    public void start() {
        LOGGER.info("Starting");
        this.acceptConnections();
    }

    public MWServ getMWServ() {
        return this.myServer;
    }

    public void processCommand(String username, String command) {
        if (username == null) {
            return; //happens when access is denied
        }

        this.myServer.clientRecieve(command, username);
    }

    //this is a hack...
    //there should be comm objects
    public void broadcastComm(String command) {
        LOGGER.debug(STR."Sending Broadcast Message: \{command}");
        synchronized (_users) {
            for (MWChatClient cc : _users.values()) {
                this.sendServerMessage(command, MWChatServer.clientKey(cc));
            }
        }
    }

    public void sendServerMessage(String msg, String name) {
        MWChatClient client = this.getClient(MWChatServer.clientKey(name));
        if (client != null) {
            try {
                client.sendRaw("/comm" + ICommands.DELIMITER + CConnector.encode(msg));
            } catch (Exception e) {
                LOGGER.error(e, "");
            }
        }
    }

    public java.net.InetAddress getIP(String username) {

        try {
            MWChatClient c = this.getClient(username);
            if (c == null) {
                LOGGER.info("WARNING: Tried to get the IP from " + username + ", who is not here.");

                /*
                 * We don't want to log out player who we can't find - logout uses getIP
                 * itself, which causes an infinite loop and kills the server. If there
                 * was a valid reason to be doing this, we can find some alternative, or
                 * simply store the IP in SPlayer @ connection time as a string and use it
                 * instead of fetching it from the client thread.
                 *
                 * We know that this code was causing then nobodies after a /c check, so getting
                 * rid o fit may be good anyway ... but be wary of problems with offline players
                 * for the next few releases.
                 *
                 * @urgru 2.18.06
                 */
                //this.myServer.clientLogout(username);

                try {
                    return java.net.InetAddress.getLocalHost();
                } catch (Exception ex) {
                    LOGGER.error(ex, "");
                    return null;
                }
            }
            return java.net.InetAddress.getByName(c.getHost());
        } catch (Exception e) {
            LOGGER.error(e, "");
            try {
                return java.net.InetAddress.getLocalHost();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * Sign on to the server
     *
     * @param client   the MWChatClient
     * @param password
     *
     * @throws AccessException is the login failed
     *                         <p>
     *                         This needs to be coupled a little more (or less) with the signon command object
     */
    @Override
    public boolean signOn(MWChatClient client, String password) throws Exception {

        LOGGER.info(client.getUserId() + " is attempting a signon: ");
        String userId = client.getUserId();
        validateUserId(userId);

        Auth auth = null;
        auth = _authenticator.authenticate(client, password);

        client.setUserId(auth.getUserId());
        synchronized (_users) {
            if (userExists(clientKey(client))) {
                if (auth.getAccess() >= IAuthenticator.REGISTERED ||
                          (auth.getAccess() < IAuthenticator.REGISTERED &&
                                 client.getUserId().startsWith("[Dedicated]"))) {
                    //kill the old instance
                    signOff(client.getServer().getClient(clientKey(client)));
                } else {
                    //this should trigger the assignment of a nobody
                    throw new Exception(ACCESS_DENIED);
                }
            }
            int access = auth.getAccess();
            client.setAccessLevel(access);
            _users.put(clientKey(client), client);
            LOGGER.info(client.getUserId() +
                                   " is authenticated.  Access = " +
                                   access +
                                   (client.getTunneling() ? " (tunneling)" : ""));
            _cumulativeLogins++;
        }

        client.ackSignOn(auth.getUserId());
        return this.myServer.clientLogin(client.getUserId());
    }

    /**
     * Determines if a user id is valid.  Ensures that the name is made up of alphanumerical characters and contains no
     * spaces.
     */
    @Override
    protected void validateUserId(String user) throws Exception {
        if (getName().toLowerCase().equals(user.toLowerCase())) {throw new Exception(user + " is a reserved name");}

        if (StringUtils.hasBadChars(user).trim().length() > 0) {
            throw new Exception(INVALID_CHARACTER);
        }

    }

    @Override
    public void signOff(MWChatClient client) {
        super.signOff(client);
        try {
            this.myServer.clientLogout(client.getUserId());
        } catch (Exception e) {
            LOGGER.error(e, "");
        }
    }
}
