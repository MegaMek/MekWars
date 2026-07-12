/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package mekwars.server;

//The MegaMek.NET Master Server Application
//@Author: Helge Richter (McWizard@gmx.de)

// import org.mekwars.libpk.logging.PKLogManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.MMGame;
import mekwars.common.commands.Command;
import mekwars.common.commands.ServerCommand;
import mekwars.server.MWChatServer.MWChatClient;
import mekwars.server.MWChatServer.MWChatServer;
import mekwars.server.MWChatServer.auth.AccessRole;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.DefaultServerOptions;
import mekwars.server.campaign.SPlayer;
import mekwars.server.util.IpCountry;
import mekwars.server.util.TrackerThread;

public class MWServ {

    // Static logging engine, and static version info.
    public static final String SERVER_VERSION = "0.8.0.0";
    private static final MMLogger LOGGER = MMLogger.create(MWServ.class);
    private final Hashtable<String, MMGame> games = new Hashtable<>();
    private final Hashtable<String, MWClientInfo> users = new Hashtable<>();
    private final Hashtable<InetAddress, Vector<MWClientInfo>> ips = new Hashtable<>();
    private final Hashtable<InetAddress, Long> bannedIPs = new Hashtable<>();
    private final Hashtable<String, String> bannedAccounts = new Hashtable<>();
    private final Hashtable<String, Long> ISPLog = new Hashtable<>();
    private final Hashtable<MWClientInfo, InetAddress> ipHelp = new Hashtable<>();
    private final Properties config = new Properties();
    private final Hashtable<InetAddress, String> IPLog = new Hashtable<>();
    private final CampaignMain campaign;
    private final Command.Table myCommands = new Command.Table();
    private final Vector<String> ignoreList = new Vector<>(1, 1);
    private final Vector<String> factionLeaderIgnoreList = new Vector<>(1, 1);
    private ServerWrapper myCommunicator;
    private Hashtable<String, String> mails = new Hashtable<>();
    private IpCountry ipToCountry = null;
    private TrackerThread trackerThread;

    // private boolean debug = true;

    /*
     * List of Abbreviations for the protocol used by the client only:
     * - NG = New Game (NG|<IP>|<Port>|<MaxPlayers>|<Version>|<Comment>)
     * - CG = Close Game (CG)
     * - RU = Refresh Users (RU)
     * - GB = Goodbye (client exit) (GB)
     * - SO = Sign-On (SO|<Version>|<UserName>)
     *
     * Used by Both:
     * - CH = Chat Server news:(CH|<text>)
     *
     * - client Chat: (CH|<UserName>|<Color>|<Text>)
     *
     * Used only by the Server:
     * - GS = Games (GS|<MMGame.toString()>|<MMGame.toString()|...)
     *
     * UsersCommand = Users
     * (UsersCommand|<MWClientInfo.toString()>|<MWClientInfo.toString()>|..)
     * - UR = Update Request (UR|<Text to Show>)
     *
     * UserGoneCommand = User Gone
     * (UserGoneCommand|<MWClientInfo.toString>|[Gone])
     * Gone is used when the client didn't just change his name
     *
     * NU = New User (NU|<MWClientInfo.toString>|[NEW])
     * NEW is used the same way as GONE in UserGoneCommand
     *
     * ER = Error (Not yet used) (ER|<ErrorLevel>|<description>)
     * CR = Campaign Result -> Just give to CampaignMain
     */

    MWServ(String[] argv) {
        LOGGER.info("Server Start up");

        LOGGER.info(String.format("----- MekWars Server V %s is starting up... -----", SERVER_VERSION));
        LOGGER.info("Loading configuration...");
        loadConfig();
        LOGGER.info("Configuration loaded.");

        if (MathUtility.parseBoolean(getConfigParam("RESOLVE_COUNTRY"), false)) {
            ipToCountry = new IpCountry("./data/iplist.txt", "./data/countrynames.txt");
        }

        LOGGER.info("Loading mail file...");
        mails = checkAndCreateConfig("./data/mails.txt");
        LOGGER.info("Mail file loaded.");
        LOGGER.info("Creating new campaign environment...");
        campaign = new CampaignMain(this);
        LOGGER.info("Environment created.");

        // Touch log files
        LOGGER.info("Initializing log subsystem. Touching log files.");
        LOGGER.info("Main channel log touched.");
        LOGGER.info("Game log touched.");
        LOGGER.info("Command log touched.");
        LOGGER.info("Private messages (PM) log touched.");
        LOGGER.info("Black Market (BM) log touched.");
        LOGGER.info("Server info log touched.");
        LOGGER.warn("Server warnings log touched.");
        LOGGER.error("Server errors log touched.");
        LOGGER.info("Moderators log touched.");
        LOGGER.info("Tick report log touched.");

        // start the TrackerThread if using tracker
        this.startTracker();

        //start server
        LOGGER.info("Entering main loop cycle. Starting the server...");
        startServer(argv);
    }

    public void loadConfig() {
        try {
            config.load(new FileInputStream("./data/serverconfig.txt"));
        } catch (Exception e) {
            config.setProperty("INFO_MESSAGE", "For MekWars project info, visit https://github.com/megamek/MekWars");
            config.setProperty("RESOLVE_COUNTRY", "true");
            config.setProperty("CAMPAIGN_CONFIG", "./data/campaignconfig.txt");
            config.setProperty("DATA_PORT", "4867");
            config.setProperty("SERVERIP", "-1");// this binds to all local IPs
            // in MWChatServer.java
            try {
                config.store(new FileOutputStream("./data/serverconfig.txt"), "Server config File");
            } catch (Exception e1) {
                LOGGER.error(e1, "config file could not be read or written, defaults will be used.");
            }
        }

        loadBanPlayers();
        loadBanIP();
        loadISPs();
    }

    public String getConfigParam(String propertyName) {
        String res = config.getProperty(propertyName);

        if (res != null) {
            return res;
        }

        return "-1";
    }

    public Hashtable<String, String> checkAndCreateConfig(String filename) {
        try {
            File configFile = new File(filename);
            FileInputStream fis = new FileInputStream(configFile);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
            dis.readLine();
            dis.close();
            fis.close();
        } catch (Exception ex) {
            try {
                LOGGER.info("Creating new File");
                if (filename.equals("./data/mails.txt")) {
                    FileOutputStream out = new FileOutputStream(filename);
                    PrintStream printStream = new PrintStream(out);
                    printStream.println("<EOF>");
                    printStream.close();
                    out.close();
                }
            } catch (Exception e) {
                LOGGER.error(e, String.format("No file named %s was found and cannot create one!", filename));
                System.exit(1);
            }
        }

        if (filename.equals("./data/mails.txt")) {
            return getMails();
        }

        return new Hashtable<>();
    }

    public void startTracker() {
        if (MathUtility.parseBoolean(getCampaign().getConfig("UseTracker"), false)) {

            if (this.trackerThread != null) {
                this.trackerThread.interrupt();
            }

            LOGGER.info("Attempting to create TrackerThread in MWServ.");
            TrackerThread trackT = new TrackerThread(this);
            trackT.start();
            this.trackerThread = trackT;
        }
    }

    // this will just loop and take in info...
    public void startServer(String[] args) {
        if (args == null) {
            LOGGER.info("Server started without parameters");
        }
        try {
            myCommunicator = ServerWrapper.createServer(this);
            myCommunicator.start();
        } catch (Exception e) {
            LOGGER.error(e, "== PROBLEM STARTING SERVER WRAPPER ==");
        }
    }

    public void loadBanPlayers() {
        // Loading the banned players file.
        try {
            LOGGER.info("Loading Ban Players");
            File banFile = new File("./data/accountbans.txt");

            // make the file if its missing
            if (!banFile.exists()) {
                banFile.createNewFile();
            }

            FileInputStream fileInputStream = new FileInputStream(banFile);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

            while (bufferedReader.ready()) {
                String input = bufferedReader.readLine();
                StringTokenizer stringTokenizer = new StringTokenizer(input, "=");
                String toBan = stringTokenizer.nextToken().trim();
                String howLong = stringTokenizer.nextToken().trim();

                bannedAccounts.put(toBan.toLowerCase(), howLong);
                LOGGER.info("Added {} to the banlist (for {})", toBan, howLong);
            }

            bufferedReader.close();
            fileInputStream.close();
        } catch (Exception ex) {
            LOGGER.error("Problems reading ban file at startup!");
        }
    }

    public void loadBanIP() {
        // Load the Permanently Banned IP'S
        try {
            File banFile = new File("./data/ipbans.txt");

            // make the file, if its missing
            if (!banFile.exists()) {
                banFile.createNewFile();
            }

            FileInputStream fileInputStream = new FileInputStream(banFile);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            while (bufferedReader.ready()) {
                String line = bufferedReader.readLine();
                StringTokenizer stringTokenizer = new StringTokenizer(line, "=");
                String ip = stringTokenizer.nextToken();
                Long time = Long.valueOf(stringTokenizer.nextToken());
                // get rid of any actual system names and just grab the IP.
                if (ip.contains("/")) {
                    ip = ip.substring(ip.indexOf("/") + 1);
                }

                InetAddress inetAddress = java.net.InetAddress.getByName(ip);

                if (inetAddress != null) {
                    bannedIPs.put(inetAddress, time);
                    LOGGER.info("Added {} to the list of banned IP's", line);
                } else {
                    LOGGER.warn("Importing IP bans; offending line: {}", line);
                }
            }
            bufferedReader.close();
            fileInputStream.close();
        } catch (Exception ex) {
            LOGGER.error(ex, "Problems with loading IP banlist:");
        }
    }

    public void loadISPs() {
        // Loading the ISP file.
        try {
            LOGGER.info("Loading ISPs");
            File ispFile = new File("./data/isps.txt");

            // make the file if its missing
            if (!ispFile.exists()) {
                ispFile.createNewFile();
            }

            FileInputStream fileInputStream = new FileInputStream(ispFile);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

            while (bufferedReader.ready()) {
                String input = bufferedReader.readLine();
                StringTokenizer stringTokenizer = new StringTokenizer(input, "=");
                String isp = stringTokenizer.nextToken().trim();
                Long address = MathUtility.parseLong(stringTokenizer.nextToken().trim(), 0L);

                ISPLog.put(isp.toLowerCase(), address);
                LOGGER.info("Added {} to the ISP List", isp);
            }

            bufferedReader.close();
            fileInputStream.close();
        } catch (Exception ex) {
            LOGGER.error(ex, "Problems reading ISP file at startup!");
        }
    }

    public Hashtable<String, String> getMails() {
        Hashtable<String, String> result = new Hashtable<>();

        try {
            File configFile = new File("./data/mails.txt");
            FileInputStream fileInputStream = new FileInputStream(configFile);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            String tmp = bufferedReader.readLine();

            while (!(tmp == null)) {
                StringTokenizer stringTokenizer = new StringTokenizer(tmp, "|");

                if (stringTokenizer.hasMoreElements()) {
                    String name = (String) stringTokenizer.nextElement();

                    if (stringTokenizer.hasMoreElements()) {
                        String mail = (String) stringTokenizer.nextElement();
                        if (result.get(name) != null) {
                            result.put(name, String.format("%s<br>%s", mails.get(name), mail));
                        } else {
                            result.put(name, mail);
                        }
                    }
                }
                tmp = bufferedReader.readLine();
            }

            bufferedReader.close();
            fileInputStream.close();
        } catch (Exception ex) {
            LOGGER.error(ex, "Problems reading mail file:");
        }

        return result;
    }

    public CampaignMain getCampaign() {
        return campaign;
    }

    public static void main(String[] argv) {
        new MWServ(argv);
    }

    public static void stop() {
        System.exit(0);
    }

    /*** Once a user(lPID) logs in, this function is kicked off ****/
    public boolean clientLogin(String name) {
        String originalName = name;
        name = name.toLowerCase();
        InetAddress userIP = getIP(name);
        MWChatClient client = myCommunicator.getClient(name);


        LOGGER.info(String.format("Connection from %s (%s)", getIP(name), name));
        // Double account check
        // Don't worry about dedicated or nobodies.
        if (!originalName.startsWith("[Dedicated]") && !originalName.startsWith("Nobody")) {
            if (IPLog.get(userIP) != null) {
                String logname = IPLog.get(userIP);

                if (!name.equals(logname)) {
                    String nametmp = name;

                    SPlayer player = getCampaign().getPlayer(name);
                    if (player != null) {
                        nametmp += String.format(" %s", getCampaign().getPlayer(name).getMyHouse().getAbbreviation());
                    }

                    SPlayer playerWithLogName = getCampaign().getPlayer(logname);

                    if (playerWithLogName != null) {
                        logname += String.format(" %s",
                              getCampaign().getPlayer(logname).getMyHouse().getAbbreviation());
                    }

                    if ((player != null) && (playerWithLogName != null)) {
                        if (player.getGroupAllowance() != 0 &&
                                  player.getGroupAllowance() != playerWithLogName.getGroupAllowance()) {
                            getCampaign().doSendModMail("NOTE:",
                                  String.format("Double Accounting: %s Group: %s and %s Group: %s IP: %s",
                                        nametmp,
                                        player.getGroupAllowance(),
                                        logname,
                                        playerWithLogName.getGroupAllowance(),
                                        userIP));
                        }
                    } else {
                        getCampaign().doSendModMail("NOTE:",
                              String.format("Double Accounting: %s and %s IP: %s", nametmp, logname, userIP));
                    }
                }
            }
            IPLog.put(userIP, name);
        }

        // Banned IP-check
        if (bannedIPs.get(userIP) != null) {
            Long until = bannedIPs.get(userIP);
            if (until > System.currentTimeMillis() || until == 0) {
                if (until != 0) {
                    clientSend(String.format("CH|You are banned. You may not join this server until %s",
                                new Date(until).toString()),
                          name);
                    getCampaign().doSendModMail("NOTE:",
                          String.format("%s (IP: %s) tried to gain access to the server", name, userIP));
                }

                myCommunicator.kill(name, "");
                return false;
            }

            // else
            bannedIPs.remove(userIP);
            bansUpdate();
        }
        // Banned Accounts name Check
        if (bannedAccounts.get(name.toLowerCase()) != null) {
            long until = MathUtility.parseLong(bannedAccounts.get(name.toLowerCase()), 0);

            if (until > System.currentTimeMillis() || until == 0) {
                if (until != 0) {
                    clientSend(String.format("CH|You are banned. You may not join this server until %s",
                                new Date(until)),
                          name);
                    getCampaign().doSendModMail("NOTE:",
                          String.format("%s (IP: %s) tried to gain access to the server", name, userIP));
                }

                myCommunicator.kill(name, "");
                return false;
            }

            // else
            bannedAccounts.remove(name);
            bansUpdate();
        }
        // Banned Accounts name Check
        if (ISPLog.containsKey(client.getClientVersion())) {
            Long until = ISPLog.get(client.getClientVersion());
            if (until > System.currentTimeMillis() || until == 0) {
                if (until != 0) {
                    clientSend(String.format("CH|You have been banned. You may not join this server until %s",
                                new Date(until).toString()),
                          name);
                    getCampaign().doSendModMail("NOTE:",
                          String.format("%s (IP: %s) tried to gain access to the server", name, userIP));
                }

                myCommunicator.kill(name, "");
                return false;
            }

            // else
            ISPLog.remove(name);
            bansUpdate();
        }

        AccessRole status = AccessRole.REGISTERED;
        MWChatClient chatClient = myCommunicator.getClient(MWChatServer.clientKey(name));

        if (chatClient != null) {
            status = chatClient.getAccessLevel();
        }

        boolean invis = false;

        if (getCampaign().getPlayer(name) != null) {
            invis = getCampaign().getPlayer(name).isInvisible();
        }

        MWClientInfo newUser = new MWClientInfo(originalName, getIP(name), System.currentTimeMillis(), status, invis);
        LOGGER.info("{} logged in from {} at {}",
              originalName,
              getIP(name).toString(),
              new Date(System.currentTimeMillis()).toString());

        // Double IP Check
        if (!originalName.startsWith("[Dedicated]")) {
            if (ips.get(userIP) != null) {
                java.util.Vector<MWClientInfo> allthose = ips.get(userIP);
                StringBuilder result = new StringBuilder("Warning: " + originalName + " has the same IP as ");
                boolean allowed = true;
                int groupid = 0;
                for (int i = 0; i < allthose.size(); i++) {
                    MWClientInfo user = (MWClientInfo) allthose.elementAt(i);

                    if (originalName.equalsIgnoreCase(user.getName())) {
                        continue;
                    }

                    // Check if they're allowed to do so.
                    SPlayer p = getCampaign().getPlayer(user.getName());
                    if (p != null) {
                        // Is this the first player to check? Then set the Group
                        if (i == 0) {
                            groupid = p.getGroupAllowance();
                        } else if (p.getGroupAllowance() != groupid || groupid == 0) {
                            allowed = false;
                        }
                    } else {
                        // One non-campaign player involved. Illegal!
                        allowed = false;
                    }

                    result.append(user.getName());
                    if (allthose.size() > i + 1) {
                        result.append(" and ");
                    }
                }
                if (!allowed) {
                    getCampaign().doSendModMail("NOTE:", result.toString());
                }
                allthose.add(newUser);
            } else {
                java.util.Vector<MWClientInfo> allthose = new java.util.Vector<MWClientInfo>(1, 1);
                allthose.add(newUser);
                ips.put(userIP, allthose);
            }
            ipHelp.put(newUser, userIP);
        }

        String clientVersion = "any " + SERVER_VERSION.substring(0, SERVER_VERSION.lastIndexOf(".")) + ".x";
        clientSend("CH|Welcome to " +
                         getCampaign().getConfig("ServerName") +
                         " (Server Version: " +
                         SERVER_VERSION +
                         ", Compatible Clients: " +
                         clientVersion +
                         ")", name);
        clientSend("CH|" + getConfigParam("INFOMESSAGE"), name);

        // send MMGame info for currently open hosts. future updates
        // incremental.
        for (MMGame currGame : games.values()) {
            if (currGame == null) {
                continue;
            }
            clientSend("ServerListCommand|NG|" + currGame.toString(), name);
            clientSend("ServerListCommand|SHS|" + currGame.getHostName() + "|" + currGame.getStatus(), name);
        }

        // send all online users to the client. future updates incremental.
        java.util.Enumeration<MWClientInfo> u = users.elements();
        String toSend = "UsersCommand";
        while (u.hasMoreElements()) {
            toSend = toSend.concat("|" + u.nextElement().toString());
        }
        clientSend(toSend, name);

        if (Boolean.parseBoolean(getConfigParam("RESOLVECOUNTRY"))) {
            // Get the Country of the User (getIP returns "/127.0.0.1" so remove
            // the trailing /
            newUser.setCountry(getCountryString(getIP(name).toString().substring(1)));
        }
        users.put(name, newUser);

        /*
         * Sending mail before the players GUi has finished constructing can
         * hang clients. Wait for a client to send a "/getsavedmail" command,
         * instead.
         */
        // send him any saved mail
        // checkAndSendMail(name);
        return true;
    }

    public java.net.InetAddress getIP(String name) {
        return myCommunicator.getIP(name);
    }

    public void clientSend(String msg, String name) {
        // Don't send empty messages
        if (msg != null && !msg.trim().equals("CH|")) {
            myCommunicator.sendServerMessage(msg, name);
        }
    }

    /**
     * Method to update the two ban files (to be used everytime bans are updated, to keep the file in sync with the
     * in-mem status)
     */
    public void bansUpdate() {
        try {
            // Updating ban file for account names
            java.io.FileOutputStream out = new java.io.FileOutputStream("./data/accountbans.txt");
            java.io.PrintStream p = new java.io.PrintStream(out);
            for (java.util.Enumeration<String> e = bannedAccounts.keys(); e.hasMoreElements(); ) {
                Object q = e.nextElement();
                p.println(q + "=" + bannedAccounts.get(q));
            }
            p.close();
            out.close();
            // Updating ban file for IP addresses
            out = new java.io.FileOutputStream("./data/ipbans.txt");
            p = new java.io.PrintStream(out);
            for (java.util.Enumeration<java.net.InetAddress> e = bannedIPs.keys(); e.hasMoreElements(); ) {
                Object q = e.nextElement();
                p.println(q + "=" + bannedIPs.get(q));
            }
            p.close();
            out.close();
            // Updating ISP List
            out = new java.io.FileOutputStream("./data/isps.txt");
            p = new java.io.PrintStream(out);
            for (String key : ISPLog.keySet()) {
                p.println(key + "=" + ISPLog.get(key));
            }
            p.close();
            out.close();
        } catch (Exception e) {
            LOGGER.error("Problem updating ban files:");
            LOGGER.error(e);
        }
    }

    public String getCountryString(String ip) {

        if (ipToCountry == null) {
            return "";
        }
        if (ip.startsWith("/")) {
            ip = ip.substring(1);
        }
        String result = ipToCountry.seachIpCountry(ip);
        if (result.equals("LOC")) {
            result = "The LAN";
        }
        return result;
        /*
         * String country = getLandForIP(ip); if (country.equals("DE")) return
         * "Germany";
         *
         * // || country.length() > 7 if (country.equals("UsersCommand")) return
         * "the USA"; if (country.equals("CA")) return "Canada"; else return
         * country;
         */
    }

    public AccessRole getUserLevel(String username) {
        MWChatClient chatClient = myCommunicator.getClient(MWChatServer.clientKey(username));

        if (chatClient != null) {
            return chatClient.getAccessLevel();
        }

        return AccessRole.NONE;
    }

    public void clientLogout(String name) {

        // name can be null if the user was never logged in
        if (name == null) {
            return;
        }

        // get the user to log out. may be null if banned.
        MWClientInfo user = getUser(name);
        if (user == null) {
            return;
        }

        java.net.InetAddress hisip = ipHelp.get(user);
        if (hisip != null) {
            java.util.Vector<MWClientInfo> all = ips.get(hisip);
            if (all != null) {
                if (all.size() == 1) {
                    ips.remove(hisip);
                } else {
                    while (all.remove(user)) {
                    }
                }
            }
            ipHelp.remove(user);
        }

        campaign.getOpsManager().doDisconnectCheckOnPlayer(name);
        campaign.doLogoutPlayer(name);

        if (hisip != null) {
            sendRemoveUserToAll(name, true, hisip.toString());
        } else {
            sendRemoveUserToAll(name, true);
        }
        LOGGER.info("client " + name + "logged out.");
        users.remove(name.toLowerCase());

        // remove his host, if he has a game open
        if (games.get(name) != null) {
            MMGame game = games.get(name);
            games.remove(name);
            doCloseGame(game);
        }

        /*
         * Attempt to remove his name from all MMGames. If successfully removed,
         * doRemoveUserFromGame will send an update to all users.
         */
        for (MMGame currGame : games.values()) {
            doRemoveUserFromGame(name, currGame);
        }
    }

    public MWClientInfo getUser(String name) {
        if (name == null || users.get(name.toLowerCase()) == null) {
            return new MWClientInfo();
        }
        // else
        return users.get(name.toLowerCase());
    }

    public void sendRemoveUserToAll(String name, boolean userGone, String ip) {
        if (userGone && ip != null) {
            LOGGER.info(name + " left the room (IP:" + ip + ").");
        } else if (userGone) {
            LOGGER.info(name + " left the room");
        }
        myCommunicator.broadcastComm("UserGoneCommand|" + getUser(name) + (userGone ? "|GONE" : ""));
    }

    public void sendRemoveUserToAll(String name, boolean userGone) {
        if (userGone) {
            LOGGER.info(name + " left the room.");
        }
        myCommunicator.broadcastComm("UserGoneCommand|" + getUser(name) + (userGone ? "|GONE" : ""));
    }

    private void doCloseGame(MMGame game) {
        if (game != null) {
            myCommunicator.broadcastComm("ServerListCommand|CG|" + game.getHostName());
        }
    }

    public void doRemoveUserFromGame(String name, MMGame mygame) {
        if (mygame.getCurrentPlayers().remove(name)) {
            myCommunicator.broadcastComm("ServerListCommand|LG|" + mygame.getHostName() + "|" + name);
        }
    }

    public void broadcastRaw(String msg) {
        myCommunicator.broadcastComm(msg);
    }

    /*** when the user sends information to the server this function kicks off ***/
    public void clientRecieve(String lineIn, String name) {
        java.util.StringTokenizer st = new java.util.StringTokenizer(lineIn, "|");
        try {
            String task = (String) st.nextElement();
            ServerCommand c;
            if ((c = (ServerCommand) myCommands.get(task.toUpperCase())) != null) {
                c.reset();
                c.setUsername(name);
                c.parseArguments(lineIn.substring(lineIn.indexOf("|") + 1));
                return;
            }

            /*
             * Commands related to hosting.
             */
            if (task.equals("NG")) { // new game in hosts list.
                // NG|<MMGame.toString()>

                MMGame newGame = new MMGame(st.nextToken());

                // replace localhost and 127.0.0.1
                if (newGame.getIp().equals("127.0.0.1") || newGame.getIp().equals("localhost")) {
                    newGame.setIp(myCommunicator.getClient(name).getHost());
                }

                games.put(name, newGame);
                myCommunicator.broadcastComm("ServerListCommand|NG|" + newGame.toString());

            } else if (task.equals("CG")) { // close game command, received from
                // player
                MMGame game = games.remove(name);
                doCloseGame(game);
            } else if (task.equals("LG")) { // player leaving game
                MMGame toUpdate = games.get(st.nextToken());
                if (toUpdate != null) {
                    doRemoveUserFromGame(name, toUpdate);
                }
            } else if (task.equals("JG")) {// join a specified host. JG|HostName

                String hostName = st.nextToken();
                MMGame toUpdate = games.get(hostName);
                if (toUpdate != null) {
                    if (toUpdate.getCurrentPlayers().add(name)) {
                        myCommunicator.broadcastComm("ServerListCommand|JG|" + toUpdate.getHostName() + "|" + name);
                    }
                }

                if (hostName.startsWith("[Dedicated]")) {
                    clientSend("PM|SERVER|You joined " + hostName + ". Please remember where you parked.", name);
                }

            } else if (task.equals("SHS")) { //@salient - i found this elsewhere -> Set Host Status (SHS|<GameID>|<Status>) UsersCommand = Users

                MMGame toUpdate = games.get(st.nextToken());
                if (toUpdate.getHostName().startsWith("[Dedicated]") || name.equals(toUpdate.getHostName())) {
                    toUpdate.setStatus(st.nextToken());
                    myCommunicator.broadcastComm("ServerListCommand|SHS|" +
                                                       toUpdate.getHostName() +
                                                       "|" +
                                                       toUpdate.getStatus());
                }

            } else if (task.equals("CH")) {
                // CHAT
                String text = st.nextToken();

                // allow | in text
                while (st.hasMoreElements()) {
                    text += "|" + st.nextToken();
                }

                // Ensure no links come from the players.. at least no harmful
                // ones)
                if (text.indexOf("href") != -1 && text.indexOf("MEKWARS") != -1 && !isAdmin(name)) {
                    clientSend("CH|Message deleted (external hyperlinks not allowed).", name);
                    return;
                }
                if (!text.startsWith("/") && text.length() > 450 && !isModerator(name)) {
                    clientSend("CH|Message blocked by the spam protector (450 char limit).", name);
                    return;
                }
                if (text.startsWith("/")) {
                    text = text.substring(1);
                    // StringTokenizer commandTokenizer = new
                    // StringTokenizer(text);
                    // String taskcommand = commandTokenizer.nextToken();

                    if (text.toLowerCase().startsWith("c ")) {
                        campaign.fromUser(text, name);
                    } else {
                        // Replace the first space with a # to mimic the /c
                        // commands
                        // Just prepend c+space so that it can be chopped off in
                        // the
                        // fromUser method --Torren
                        text = "c " + text.trim().replaceFirst(" ", "#");
                        campaign.fromUser(text, name);
                    }

                } else {
                    MWClientInfo client = getUser(name);
                    if (client != null) {
                        if (!ignoreList.contains(client.getName()) &&
                                  !factionLeaderIgnoreList.contains(client.getName())) {
                            sendChat(name + "|" + text);
                        } else {
                            clientSend("CH|You've been set to ignore mode and cannot participate in chat.", name);
                        }
                    } else {
                        sendChat(name + "|" + text);
                    }

                }
            } else if (task.equals("CR")) {
                String result = st.nextToken();
                LOGGER.info("Starting report process by " + name);
                LOGGER.info(name + " reported: " + lineIn);
                getCampaign().doProcessAutomaticReport(result, name);
            } else if (task.equals("IPU")) {// InProgressUpdate
                String result = st.nextToken();
                getCampaign().addInProgressUpdate(result, name);
            } else {
                clientSend("CH|Unknown command. Please make sure your client is up to date.", name);
                LOGGER.warn("Got a strange command, " + task + ", from " + name);
            }
        } catch (Exception ex) {
            // The GB doesn't arrive at the server because of the client
            // disconnecting
            if (!lineIn.equals("GB")) {
                // Most propably an out of date client. Send him the request to
                // update
                clientSend(
                      "CH|Your client sent a false packet or caused a server error. You probably entered an illegal server command.",
                      name);
                LOGGER.error("False packet/illegal command (from " + name + "):");
                LOGGER.error(ex);
            }
        }
    }

    public boolean isAdmin(String username) {
        MWChatClient myCommunicatorClient = myCommunicator.getClient(MWChatServer.clientKey(
              username));
        if (username.startsWith("[Dedicated] ")) {
            return true;
        }
        if (myCommunicatorClient != null) {
            return myCommunicatorClient.getAccessLevel().isGreaterOrEqual((AccessRole.ADMIN));
        }
        return false;

    }

    public boolean isModerator(String username) {
        MWChatClient myCommunicatorClient = myCommunicator.getClient(MWChatServer.clientKey(username));
        if (username.startsWith("[Dedicated] ")) {
            return true;
        }
        if (myCommunicatorClient != null) {
            return myCommunicatorClient.getAccessLevel().isGreaterOrEqual(AccessRole.MODERATOR);
        }
        return false;
    }

    public void sendChat(String s) {
        myCommunicator.broadcastComm("CH|" + s);
        LOGGER.info(s);
    }

    public void statusMessage() {
        LOGGER.info("Open Games: " + games.size());
        LOGGER.info("Open Games: " + games.size());
    }

    public void retrieveISPS(Long time, String name) {

        java.io.File tempFile = new java.io.File("./data/Providers/");

        if (!tempFile.exists()) {
            return;
        }

        java.io.FilenameFilter filter = new mekwars.server.ISPFilter();
        java.io.File[] fileList = tempFile.listFiles(filter);
        for (java.io.File newFile : fileList) {

            try {
                java.io.FileInputStream in = new java.io.FileInputStream(newFile);
                java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(in));

                while (dis.ready()) {
                    String player = dis.readLine();
                    if (player.equalsIgnoreCase(name)) {
                        String provderName = newFile.getName().substring(0, newFile.getName().lastIndexOf(".prv"));
                        LOGGER.error("Provider: " + provderName);
                        ISPLog.put(provderName, time);
                        in.close();
                        dis.close();
                        break;
                    }
                }

                dis.close();
                in.close();

            } catch (Exception ex) {
                // Do something?
            }
        }// end For
    }

    public void sendNewUserToAll(String name, boolean newUser) {
        myCommunicator.broadcastComm("NU|" + getUser(name) + (newUser ? "|NEW" : ""));
    }

    // Check for new Mail
    public void checkAndSendMail(String name) {
        if (mails.get(name.toLowerCase()) != null) {
            clientSend("PM|SERVER|You have stored mail.[<a href=\"MEKWARS/c requestservermail\">Read</a>]", name);
        }
    }

    /**
     * Method which returns the number of users who are not Dedicated hosts or Nobodies.
     */
    public int userCount(boolean includeDeds) {

        if (includeDeds) {
            return users.size();
        }

        // else, no deds. filter the list.
        synchronized (users) {
            int toReturn = users.size();
            for (String client : users.keySet()) {
                if (client.toLowerCase().indexOf("[dedicated]") >= 0 || client.toLowerCase().startsWith("nobody")) {
                    toReturn--;// decrease count
                }
            }
            return toReturn;
        }

    }

    public java.util.Hashtable<String, MWClientInfo> getUsers() {
        return users;
    }

    public void doStoreMail(String s, String name) {
        // LOGGER.info("Debug: " + s);
        java.util.StringTokenizer st = new java.util.StringTokenizer(s, ",");
        String target = "";
        String text = "";
        if (st.hasMoreElements()) {
            target = ((String) (st.nextElement())).trim();
            if (st.hasMoreElements()) {
                text = "from " + name + ": ";
                String mailtext = "";
                while (st.hasMoreElements()) {
                    mailtext = mailtext + st.nextElement();
                }
                text = text + mailtext;
                if (getUser(target).getName().equalsIgnoreCase(target)) {
                    if (getUser(target).isInvis() && getUser(target).getLevel().isGreater(getUser(name).getLevel())) {
                        clientSend("CH|AM:Saved mail to " + target + ".", name);
                    }
                    clientSend("PM|" + name + "|" + mailtext, target);
                } else {
                    doStoreMailToHashtable(name, target, text);
                    clientSend("CH|AM:Saved mail to " + target + ".", name);
                }

                if (campaign.getPlayer(name) != null) {
                    if (campaign.getPlayer(target) != null) {
                        LOGGER.info(name +
                                          "[" +
                                          campaign.getPlayer(name).getMyHouse().getAbbreviation() +
                                          "] -> " +
                                          target +
                                          "[" +
                                          campaign.getPlayer(target).getMyHouse().getAbbreviation() +
                                          "]: " +
                                          mailtext);
                    } else {
                        LOGGER.info(name +
                                          "[" +
                                          campaign.getPlayer(name).getMyHouse().getAbbreviation() +
                                          "] -> " +
                                          target +
                                          ": " +
                                          mailtext);
                    }
                } else {
                    LOGGER.info(name + " -> " + target + ": " + mailtext);
                }

            }
        }
    }

    public void doStoreMailToHashtable(String from, String name, String text) {
        if (name == null || text == null) {
            return;
        }

        if (text.trim().startsWith("CH|")) {
            text = text.substring(3); // get rid of chat market
        }
        if (text.trim().startsWith("FSM|")) {
            text = text.substring(4); // get rid of the full system message mark
        }

        // Add Timestamp
        String[] ids = java.util.TimeZone.getAvailableIDs(0);
        // if no ids were returned, something is wrong. get out.
        if (ids.length != 0) {
            java.util.SimpleTimeZone pdt = new java.util.SimpleTimeZone(0, ids[0]);
            java.util.Calendar now = new java.util.GregorianCalendar(pdt);
            java.util.Date currentTime = new java.util.Date();
            now.setTime(currentTime);
            text = (now.get(java.util.Calendar.MONTH) + 1) + "/" + now.get(java.util.Calendar.DATE) + " " + now.get(
                  java.util.Calendar.HOUR) + ":" + now.get(java.util.Calendar.MINUTE) + " " + text;
        }
        if (mails.get(name.toLowerCase()) == null) {
            mails.put(name.toLowerCase(), text);
        } else {
            mails.put(name.toLowerCase(), (String) (mails.get(name.toLowerCase())) + "<br>" + text);
        }
        doWriteMailFile();
    }

    public void doWriteMailFile() {
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream("./data/mails.txt");
            java.io.PrintStream p = new java.io.PrintStream(out);
            java.util.Enumeration<String> e = mails.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                p.println(key + "|" + (String) mails.get(key));
            }
            p.close();
            out.close();
        } catch (Exception ex) {
            LOGGER.error("Problems writing mail file:");
            LOGGER.error(ex);
        }
    }

    public void setConfigParam(String config, String text) {
        String res = this.config.getProperty(config);
        if (res != null) {
            this.config.setProperty(config, text);
        }
    }

    public void fromCampaignMod(String txt, String username) {
        /*
         * Enumeration e = users.elements(); boolean found = false; while
         * (e.hasMoreElements()) { MWClientInfo usr = (MWClientInfo)
         * e.nextElement(); if (usr.getName().equals(username)) {
         * clientSend(txt, username); found = true; } }
         */
        try {
            MWClientInfo usr = users.get(username.toLowerCase());
            if (usr != null) {
                clientSend(txt, username);
            } else if (txt.startsWith("CH|") || txt.startsWith("FSM|")) {
                doStoreMailToHashtable(null, username, txt);
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }

    public java.util.Vector<String> getIgnoreList() {
        return ignoreList;
    }

    public java.util.Vector<String> getFactionLeaderIgnoreList() {
        return factionLeaderIgnoreList;
    }

    public java.util.Hashtable<String, String> getServerMail() {
        return mails;
    }

    public void killClient(String toKick, String kicker) {
        myCommunicator.kill(toKick,
              myCommunicator.getClient(MWChatServer.clientKey(myCommunicator.getClient(kicker))),
              "");
    }

    public MWChatClient getClient(String name) {
        return myCommunicator.getClient(name);
    }

    public java.util.Hashtable<MWClientInfo, java.net.InetAddress> getIPHelp() {
        return ipHelp;
    }

    public java.util.Hashtable<java.net.InetAddress, Long> getBanIps() {
        return bannedIPs;
    }

    public java.util.Hashtable<String, String> getBanAccounts() {
        return bannedAccounts;
    }

    public void saveConfigs() {
        DefaultServerOptions dso = new DefaultServerOptions();
        dso.createConfig();
    }
}

