/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
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

package mekwars.dedicatedhost;

import java.awt.Dimension;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;

import megamek.common.enums.GamePhase;
import megamek.common.event.GameCFREvent;
import megamek.common.event.PostGameResolution;
import megamek.common.game.Game;
import megamek.common.options.IOption;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.Server;
import mekwars.common.AdvancedTerrain;
import mekwars.common.CampaignData;
import mekwars.common.Equipment;
import mekwars.common.GameInterface;
import mekwars.common.GameWrapper;
import mekwars.common.Influences;
import mekwars.common.MMGame;
import mekwars.common.Planet;
import mekwars.common.PlanetEnvironment;
import mekwars.common.campaign.Buildings;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.GameHost;
import mekwars.common.campaign.clientutils.IClientUser;
import mekwars.common.campaign.clientutils.SerializeEntity;
import mekwars.common.campaign.clientutils.protocol.CConnector;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.commands.CommPCmd;
import mekwars.common.commands.Command;
import mekwars.common.commands.IProtCommand;
import mekwars.common.commands.PingPlayerCommand;
import mekwars.common.commands.PongPCmd;
import mekwars.common.gui.CMainFrame;
import mekwars.common.threads.ClientThread;
import mekwars.common.threads.RepairManagmentThread;
import mekwars.common.threads.SalvageManagmentThread;
import mekwars.common.util.UnitUtils;
import mekwars.dedicatedhost.protocol.DataFetchClient;


// This is the client used for connecting to the master server.
// @Author: Helge Richter (McWizard@gmx.de)

public final class MWDedHost extends GameHost implements IClient {
    public static final String CLIENT_VERSION = "0.8.0.0"; // change this with
    private static final MMLogger LOGGER = MMLogger.create(MWDedHost.class);
    // all client
    // changes @Torren
    /**
     * @author Torren place holder until I can think of something better to say.
     */
    public Properties serverConfigs = new Properties();
    DataFetchClient dataFetcher;
    TimeOutThread TO;
    Collection<CUser> Users;
    Vector<IOption> GameOptions = new Vector<IOption>(1, 1);
    boolean SignOff = false;
    String password = "";
    String myDedOwners = "";
    int myPort = -1;
    int gameCount = 0; // number of games played on a ded
    long lastResetCheck = System.currentTimeMillis(); // how quick a reset check
    // can be done on a ded.
    int dedRestartAt = 50; // number of games played on a ded before auto
    // restart.
    int savedGamesMaxDays = 30; // max number of days a save game can be before
    // its deleted.
    long TimeOut = 120;
    long LastPing = 0;
    int Status = 0;
    Dimension MapSize;
    Dimension BoardSize;

    /**
     * Maps the task prefixes as HS, PL, SP etc. to a command under package cmd. key: String, value: cmd.Command
     */
    HashMap<String, Command> commands = new HashMap<String, Command>();

    String LastQuery = ""; // receiver of last mail
    private GamePhase currentPhase = GamePhase.DEPLOYMENT;
    private int turn = 0;
    private String cacheDir;


    public MWDedHost(DedConfig config) {

        ProtCommands = new TreeMap<String, IProtCommand>();

        Config = config;

        Connector = new CConnector(this);

        Users = Collections.synchronizedList(new Vector<CUser>(1, 1));

        createProtCommands();
        dataFetcher = new DataFetchClient(Integer.parseInt(Config.getParam("DATAPORT")),
              Integer.parseInt(Config.getParam("SOCKETTIMEOUTDELAY")));
        dataFetcher.setData(Config.getParam("SERVERIP"), getCacheDir());
        dataFetcher.closeDataConnection();

        // Remove any MM option files that deds may have.
        File localGameOptions = new File("./mmconf");
        try {
            if (localGameOptions.exists()) {
                localGameOptions = new File("./mmconf/gameoptions.xml");
                if (localGameOptions.exists()) {
                    localGameOptions.delete();
                }
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }

        // set New timestamp
        // this.dataFetcher.setLastTimestamp(new
        // Date(System.currentTimeMillis()));
        // this.dataFetcher.store();

        getServerConfigData();

        myUsername = getConfigParam("NAME");

        // if this is dedicated host, we mark its name with "[Dedicated]" stamp
        if (!myUsername.startsWith("[Dedicated]")) {
            Config.setParam("NAME", "[Dedicated] " + Config.getParam("NAME"));
            myUsername = Config.getParam("NAME");
        }

        dedRestartAt = Integer.parseInt(getConfigParam("DEDAUTORESTART"));
        savedGamesMaxDays = Integer.parseInt(getConfigParam("MAXSAVEDGAMEDAYS"));
        myDedOwners = getConfigParam("DEDICATEDOWNERNAME");
        myPort = Integer.parseInt(getConfigParam("PORT"));

        /*
         * Start the pruge thread when the client starts, not when the host
         * starts. This prevents the creation of multiple threads when the host
         * is restarted, or after disconnections.
         */
        LOGGER.info("Starting pAS");
        PurgeAutoSaves pAS = new PurgeAutoSaves();
        new Thread(pAS).start();

        /*
         * Load IP and Port to connect to from the config. In older code the
         * signon dialog was shown at this point. The dialog has been moved, and
         * is now displayed -before- the client attempts to fetch vital data,
         * like the map.
         */
        String chatServerIP = "";
        int chatServerPort = -1;
        try {
            chatServerIP = Config.getParam("SERVERIP");
            chatServerPort = Config.getIntParam("SERVERPORT");
        } catch (Exception e) {
            LOGGER.error(e, "");
            System.exit(1);
        }

        int retryCount = 0;
        while ((Status == STATUS_DISCONNECTED) && (retryCount++ < 20)) {
            connectToServer(chatServerIP, chatServerPort);
            if (Status == STATUS_DISCONNECTED) {
                LOGGER.info("Couldn't connect to server. Retrying in 90 seconds.");
                try {
                    Thread.sleep(90000);
                } catch (Exception exe) {
                    LOGGER.error(exe, "");
                    System.exit(2);
                }
            }
        }

        // start checking for timeouts
        TimeOut = Long.parseLong(Config.getParam("TIMEOUT"));
        LastPing = System.currentTimeMillis() / 1000;
        TO = new TimeOutThread(this);
        TO.run();
    }

    protected void createProtCommands() {
        addProtCommand(new CommPCmd(this));
        addProtCommand(new PingPlayerCommand(this));
        addProtCommand(new PongPCmd(this));
        addProtCommand(new AckSignonPCmd(this));
    }

    protected void addProtCommand(IProtCommand command) {
        ProtCommands.put(command.getName(), command);
    }

    // Main-Method
    public static void main(String[] args) {

        DedConfig config;

        createLoggers();

        /*
         * put StdErr and StdOut into ./logs/megameklog.txt, because MegaMek
         * uses StdOut and StdErr, but the part of MegaMek that sets that up
         * does not get called when we launch MegaMek in MekWars Redirect output
         * to logfiles, unless turned off. Moved megameklog.txt to the logs
         * folder -- Torren
         */
        String logFileName = "./logs/megameklog.txt";

        try {
            PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFileName), 64));
            System.setOut(ps);
            System.setErr(ps);
        } catch (Exception ex) {
            LOGGER.error(ex, "");
            LOGGER.error("Unable to redirect MegaMek output to " + logFileName);
        }

        LOGGER.info("Starting MekWars client Version: " + CLIENT_VERSION);
        try {
            config = new DedConfig(true);

            /*
             * clear any cache'd unit files. these will be rebuilt later in the
             * start process. clearing @ each start ensures that updates take
             * hold properly.
             */
            File cache = new File("./data/mechfiles/units.cache");
            if (cache.exists()) {
                cache.delete();
            }

            /*
             * Config files have been loaded, and command line args have been
             * parsed. Construct the actual client.
             *
             * NOTE: client constrtuctor attempts to pull the oplist, campaign
             * config and other non-interactive data over the DATAPORT before
             * client.start() attempts to connect to the chat server on the
             * SERVERPORT.
             */
            new MWDedHost(config);

        } catch (Exception ex) {
            LOGGER.error(ex, "");
            LOGGER.error("Couldn't create client Object");
            System.exit(1);
        }
    }

    private static void createLoggers() {
        PKLogManager logger = PKLogManager.getInstance();
        logger.addLog("infolog");
        logger.addLog("errlog");
        logger.addLog("debuglog");
    }

    /*
     * Actual GUI-mode parseData. Before we started streaming data over the chat
     * part, this was called directly. Now we buffer all incoming non-data chat
     * and spit it out at once when the GUI draws. Once the GUI is up, this is
     * called by a simple pass through from doParseDataInput(), above.
     *
     * Ded's call the helper directly to bypass the buffer.
     */
    private void doParseDataHelper(String input) {
        try {

            // 0-length input is spurious call from MWDedHost constructor.
            if (input.length() == 0) {
                return;
            }

            StringTokenizer ST = null;
            String task = null;

            // debug info
            LOGGER.info(input);

            // Create a String Tokenizer to parse the elements of the input
            ST = new StringTokenizer(input, COMMAND_DELIMITER);
            task = ST.nextToken();

            if (!commands.containsKey(task)) {
                try {
                    Class<?> cmdClass = Class.forName(getClass().getPackage().getName() + ".cmd." + task);
                    Constructor<?> c = cmdClass.getConstructor(new Class[]
                                                                     { MWDedHost.class });
                    Command cmd = (Command) c.newInstance(new Object[]
                                                                { this });
                    commands.put(task, cmd);
                } catch (Exception e) {
                    LOGGER.error(e, "");
                }
            }
            if (commands.containsKey(task)) {
                commands.get(task).execute(input);
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }
    }

    protected Vector<String> splitString(String string, String splitter) {
        Vector<String> vector = new Vector<String>(1, 1);
        String[] splitted = string.split(splitter);
        for (String element : splitted) {
            vector.add(element.trim());
        }

        /*
         * Remove empty entries from the set. Strip ",," and "" from the vector.
         * Helps with ignore and keyword lists.
         */
        Iterator<String> i = vector.iterator();
        while (i.hasNext()) {
            String currString = i.next();
            if (currString.trim().length() == 0) {
                i.remove();
            }
        }

        return vector;
    }

    public synchronized void clearUserCampaignData() {
        for (CUser currUser : Users) {
            currUser.clearCampaignData();
        }
    }

    public void resetGame() { // reset hosted game
        if (myServer != null) {
            myServer.resetGame();
            ((Game) myServer.getGame()).purgeGameListeners();
            ((Game) myServer.getGame()).addGameListener(this);
        }
    }

    public boolean loadGame(String filename) {// load saved game
        if ((myServer != null) && (filename != null) && !filename.equals("")) {
            boolean loaded = myServer.loadGame(new File("./savegames/", filename));
            ((Game) myServer.getGame()).addGameListener(this);
            return loaded;
        }

        // else (null server/filename)
        if (myServer == null) {
            LOGGER.info("MyServer == NULL!");
        }
        if (filename == null) {
            LOGGER.info("Filename == NULL!");
        } else if (filename.equals("")) {
            LOGGER.info("Filename == \"\"!");
        }

        return false;
    }

    public boolean loadGameWithFullPath(String filename) {// load saved game
        if ((myServer != null) && (filename != null) && !filename.equals("")) {
            boolean loaded = myServer.loadGame(new File(filename));
            ((Game) myServer.getGame()).addGameListener(this);
            return loaded;

        }

        // else (null server/filename)
        if (myServer == null) {
            LOGGER.info("MyServer == NULL!");
        }
        if (filename == null) {
            LOGGER.info("Filename == NULL!");
        } else if (filename.equals("")) {
            LOGGER.info("Filename == \"\"!");
        }

        return false;
    }

    public Dimension getBoardSize() {
        return BoardSize;
    }

    public void loadServerMegaMekGameOptions() {
        try {
            dataFetcher.getServerMegaMekGameOptions();
        } catch (Exception ex) {
            LOGGER.error("Error loading Server MegaMekGameOptions files");
            LOGGER.error(ex, "");
        }
    }

    /**
     * Changes the duty to a new status.
     *
     * @param newStatus
     */
    public void changeStatus(int newStatus) {
        Status = newStatus;
    }

    public void clearSavedGames() {

        long daysInSeconds = ((long) savedGamesMaxDays) * 24 * 60 * 60 * 1000;

        File saveFiles = new File("./savegames/");
        if (!saveFiles.exists()) {
            return;
        }
        File[] fileList = saveFiles.listFiles();
        for (File savedFile : fileList) {
            long lastTime = savedFile.lastModified();
            if (savedFile.exists() && savedFile.isFile() && (lastTime < (System.currentTimeMillis() - daysInSeconds))) {
                try {
                    LOGGER.info("Purging File: " +
                                      savedFile.getName() +
                                      " Time: " +
                                      lastTime +
                                      " purge Time: " +
                                      (System.currentTimeMillis() - daysInSeconds));
                    savedFile.delete();
                } catch (Exception ex) {
                    LOGGER.error("Error trying to delete these files!");
                    LOGGER.error(ex, "");
                }
            }
        }
    }

    public String getParanoidAutoSave() {

        File tempFile = new File("./savegames/");
        FilenameFilter filter = new AutoSaveFilter();
        File[] fileList = tempFile.listFiles(filter);
        long time = 0;
        String saveFile = "autosave.sav";
        for (File newFile : fileList) {
            if (newFile.lastModified() > time) {
                time = newFile.lastModified();
                saveFile = newFile.getName();
            }
        }
        return saveFile;
    }

    public Server getMyServer() {
        return myServer;
    }

    public void systemMessage(String message) {
        // TODO Auto-generated method stub

    }

    public void errorMessage(String message) {
        // TODO Auto-generated method stub

    }

    public void processIncoming(String incoming) {
        IProtCommand pcommand = null;

        // MWLogger.infoLog("INCOMING: " + incoming);
        if (incoming.startsWith(PROTOCOL_PREFIX)) {
            incoming = incoming.substring(PROTOCOL_PREFIX.length());
            StringTokenizer ST = new StringTokenizer(incoming, PROTOCOL_DELIMITER);
            String s = ST.nextToken();
            pcommand = getProtCommand(s);
            if ((pcommand != null) && pcommand.check(s)) {
                if (!pcommand.execute(incoming)) {
                    LOGGER.info("COMMAND ERROR: wrong protocol command executed or execution failed.");
                    LOGGER.info("COMMAND RECEIVED: " + incoming);
                }
                return;
            }
            if (pcommand == null) {
                LOGGER.info("COMMAND ERROR: unknown protocol command from server.");
                LOGGER.info("COMMAND RECEIVED: " + incoming);
                return;
            }
        } else {
            LOGGER.info("COMMAND ERROR: received protocol command without protocol prefix.");
            LOGGER.info("COMMAND RECEIVED: " + incoming);
            return;
        }
    }

    IProtCommand getProtCommand(String command) {
        return ProtCommands.get(command);
    }

    public void connectionLost() {

        Status = STATUS_DISCONNECTED;
        if (SignOff) {
            return;
        }

        errorMessage("Connection lost.");
        if (isDedicated()) {

            // no point in having a server open w/o connection to campaign
            // server
            stopHost();

            // wait at least 90 seconds before trying to connect again
            try {
                Thread.sleep(90000);
            } catch (Exception ex) {
                LOGGER.error(ex, "");
            }

            // keep retrying every two minutes after the first 90 sec downtime.
            while (Status == STATUS_DISCONNECTED) {
                connectToServer(Config.getParam("SERVERIP"), Config.getIntParam("SERVERPORT"));
                if (Status == STATUS_DISCONNECTED) {
                    LOGGER.info("Couldn't reconnect to server. Retrying in 120 seconds.");
                    try {
                        Thread.sleep(90000);
                    } catch (Exception exe) {
                        LOGGER.error(exe, "");
                    }
                }
            }
        } else {
            Users.clear();
        }
    }

    public void connectionEstablished() {

        LastPing = System.currentTimeMillis() / 1000;
        LOGGER.error("Connected. Signing on.");

        String VersionSubID = new java.rmi.dgc.VMID().toString();
        StringTokenizer ST = new StringTokenizer(VersionSubID, ":");

        /*
         * If password is blank, send a filler password instead of an empty
         * token. This prevents the no-password "whitescreen" error. HACKY.
         *
         * It would be probably be better to actually fix the server SignOn so
         * an empty password creates a nobody, but this does the trick ...
         */
        String passToSend = getConfigParam("NAMEPASSWORD");
        if ((passToSend == null) || (passToSend.length() == 0)) {
            passToSend = "1337";
        }

        Connector.send(PROTOCOL_PREFIX +
                             "signon\t" +
                             getConfigParam("NAME") +
                             "\t" +
                             passToSend +
                             "\t" +
                             getProtocolVersion() +
                             "\t" +
                             Config.getParam("COLOR") +
                             "\t" +
                             CLIENT_VERSION +
                             "\t" +
                             ST.nextToken());
        Status = STATUS_LOGGEDOUT;
    }

    public String getProtocolVersion() {
        return "4";
    }

    public void startHost(boolean dedicated, boolean deploy, boolean loadSavegame) {

        // reread the config to allow the user to change setting during runtime
        String ip = "127.0.0.1";
        if (!getConfigParam("IP:").equals("")) {// IP Setting set, override IP
            // detection.
            try {
                ip = getConfigParam("IP:");
                InetAddress IA = InetAddress.getByName(ip); // Resolve Dyndns
                // Entries
                ip = IA.getHostAddress();
            } catch (Exception ex) {
                return;
            }
        }

        String MMVersion = getserverConfigs("AllowedMegaMekVersion");
        if (!MMVersion.equals("-1") && !MMVersion.equalsIgnoreCase(megamek.SuiteConstants.VERSION.toString())) {
            LOGGER.error("You are using an invalid version of MegaMek. Please use version " + MMVersion);
            stopHost();
            updateDed();
            return;
        }

        if (servers.get(myUsername) != null) {
            if (isDedicated()) {
                LOGGER.error("Attempted to start a second host while host was already running.");
            } else {
                String toUser = "CH|CLIENT: You already have a host open.";
                doParseDataInput(toUser);
            }
            return;
        }

        // int port = Integer.parseInt(getConfigParam("PORT:"));
        int MaxPlayers = Integer.parseInt(getConfigParam("MAXPLAYERS:"));
        String comment = getConfigParam("COMMENT:");
        String gpassword = getConfigParam("GAMEPASSWORD:");

        if (gpassword == null) {
            gpassword = "";
        }
        try {
            myServer = new Server(gpassword, myPort, new GameManager());
        } catch (Exception ex) {
            try {
                if (myServer == null) {
                    LOGGER.error("Error opening dedicated server. Result = null host.");
                    LOGGER.error(ex, "");
                } else {
                    LOGGER.error("Error opening dedicated server. Will attempt a .die().");
                    LOGGER.error(ex, "");
                    myServer.die();
                    myServer = null;
                }
            } catch (Exception e) {
                LOGGER.error("Further error while trying to clean up failed host attempt.");
                LOGGER.error(e, "");
            }
            return;
        }

        ((Game) myServer.getGame()).addGameListener(this);
        // Send the new game info to the Server
        serverSend("NG|" +
                         new MMGame(myUsername,
                               ip,
                               myPort,
                               MaxPlayers,
                               megamek.SuiteConstants.VERSION.toString(),
                               comment).toString());
        clearSavedGames();
        purgeOldLogs();
        ClientPreferences cs = PreferenceManager.getClientPreferences();
        cs.setStampFilenames(Boolean.parseBoolean(getserverConfigs("MMTimeStampLogFile")));
    }

    public boolean isDedicated() {
        return true;
    }

    /*
     * NOTE: this list is ancient. sometimes useful. often out of date.
     *
     * List of Abreviations for the protocol used by the client only: NG = New
     * Game (NG|<IP>|<Port>|<MaxPlayers>|<Version>|<Comment>) CG = Close Game
     * (CG) GB = Goodbye (client exit) (GB) SO = Sign-On
     * (SO|<Version>|<UserName>)
     *
     * Used by Both: CH = Chat Server news:(CH|<text>) client Chat:
     * (CH|<UserName>|<Color>|<Text>)
     *
     * Used only by the Server: ServerListCommand|NG = Games
     * (GS|<MMGame.toString()>|<MMGame.toString()|...) ServerListCommand|CG = close game ServerListCommand|JG
     * = add a player to game list ServerListCommand|LG = remove a player from game list ServerListCommand|SHS
     * = Set Host Status (SHS|<GameID>|<Status>) UsersCommand = Users
     * (UsersCommand|<MMClientInfo.toString()>|<MMClientInfo.toString()>|..) UserGoneCommand = User
     * Gone (UserGoneCommand|<MMClientInfo.toString>|[Gone]) Gone is used when the client
     * didn't just change his name NU = New User
     * (NU|<MMClientInfo.toString>|[NEW]) NEW is used the same way as GONE in UserGoneCommand
     * ER = Error (Not yet used) (ER|<ErrorLevel>|<description>) NN = New name
     * (My name Change was successful) CT = Campaign Task Offset (CT|Offset) CS
     * = Campaign Status (CS|Status) GO = Game Options
     * (GO|OPTION1NAME|OPTION1VALUE|OPTION2NAME...) PE = SPlanet Environment
     * (Used to initialize the MM map generator) HS = SHouse Status TI = Tick
     * Info (TI|TIMETILLNEXT) SP = Show PopupWindow SM = Show Miscellaneous
     * (Puts text into Misc Tab)
     */
    public synchronized void doParseDataInput(String input) {

        // non-null main frame, unbuffer or just pass through
        if (decodeBuffer.size() > 0) {
            Iterator<String> i = decodeBuffer.iterator();
            while (i.hasNext()) {
                String currS = i.next();
                doParseDataHelper(currS);
                i.remove();
            }
        } else {
            doParseDataHelper(input);
        }
    }

    public synchronized void parseDedDataInput(String data) {

        // Debug info
        // MWLogger.infoLog(data);

        StringTokenizer st, own;
        String name, owner, command;
        int port;

        /*
         * New users, report requests and data should be sent to standard
         * processor. PM's are checked below, and all other commands are tossed
         * (e.g. - CH).
         *
         * Note that ded's bypass the doParseDeda() buffering process (never
         * have a main frame, so no null check or buffer needed) and call
         * doParseDataHelper() directly.
         */
        if (data.startsWith("UsersCommand|") ||
                  data.startsWith("NU|") ||
                  data.startsWith("UserGoneCommand|") ||
                  data.startsWith("RGTS|") ||
                  data.startsWith("DSD|") ||
                  data.startsWith("USD|")) {
            doParseDataHelper(data);// bypass the buffering process -
            // ded's never have a main fraime
            return;
        }

        // only parse PM's for commands
        if (!data.startsWith("PM|")) {
            return;
        }

        data = data.substring(3);// strip "PM|"
        st = new StringTokenizer(data, "|");
        own = new StringTokenizer(myDedOwners, "$");

        name = st.nextToken().trim();
        if (!st.hasMoreTokens()) {
            return;
        } // it's not real chat message
        if (name.equals(myUsername)) {
            return;
        } // server can't send commands to itself
        command = st.nextToken().trim();

        /*
         * Commands that can be executed by ANY user.
         */
        if (command.equals("checkrestartcount")) {// check the restart amount.
            checkForRestart();
            return;
        } else if (command.equals("displaymegameklog")) { // display
            // megameklog.txt
            LOGGER.info("display megameklog command received from " + name);
            try {
                File logFile = new File("./logs/megameklog.txt");
                FileInputStream fis = new FileInputStream(logFile);
                BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
                sendChat(PROTOCOL_PREFIX + "c sendtomisc#" + name + "#MegaMek Log from " + myUsername);
                int counter = 0;
                while (dis.ready()) {
                    sendChat(PROTOCOL_PREFIX + "c sendtomisc#" + name + "#" + dis.readLine());
                    // problems with huge logs getting shoved down players
                    // throats so a 100ms delay should allow
                    // the message queue to breath.
                    if ((counter++ % 100) == 0) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ex) {
                            // Do nothing
                        }
                    }
                }
                fis.close();
                dis.close();

            } catch (Exception ex) {
                // do nothing?
            }
            sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the display megamek logs command on " + myUsername);
            return;
        } else if (command.equals("displaydederrorlog")) { // display
            // error.0
            LOGGER.info("display ded error command received from " + name);
            try {
                File logFile = new File("./logs/errlog.0");
                FileInputStream fis = new FileInputStream(logFile);
                BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
                sendChat(PROTOCOL_PREFIX + "c sendtomisc#" + name + "#Error Log from " + myUsername);
                int counter = 0;
                while (dis.ready()) {
                    sendChat(PROTOCOL_PREFIX + "c sendtomisc#" + name + "#" + dis.readLine());
                    // problems with huge logs getting shoved down players
                    // throats so a 100ms delay should allow
                    // the message queue to breath.
                    if ((counter++ % 100) == 0) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ex) {
                            // Do nothing
                        }
                    }
                }
                fis.close();
                dis.close();

            } catch (Exception ex) {
                // do nothing?
            }
            sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the display ded error log command on " + myUsername);
            return;
        } else if (command.equals("displaydedlog")) { // display
            // log.0
            LOGGER.info("display ded log command received from " + name);
            try {
                File logFile = new File("./logs/infolog.0");
                FileInputStream fis = new FileInputStream(logFile);
                BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
                sendChat(PROTOCOL_PREFIX + "c sendtomisc#" + name + "#Ded Log from " + myUsername);
                int counter = 0;
                while (dis.ready()) {
                    sendChat(PROTOCOL_PREFIX + "c sendtomisc#" + name + "#" + dis.readLine());
                    // problems with huge logs getting shoved down players
                    // throats so a 100ms delay should allow
                    // the message queue to breath.
                    if ((counter++ % 100) == 0) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ex) {
                            // Do nothing
                        }
                    }
                }
                fis.close();
                dis.close();

            } catch (Exception ex) {
                // do nothing?
            }
            sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the display ded log command on " + myUsername);
            return;
        }

        /*
         * Commands that can only be executed by owners, mods, or in the absence
         * of an owner list.
         */
        while (myDedOwners.equals("") || own.hasMoreTokens()) {

            if (own.hasMoreTokens()) {
                owner = own.nextToken();
            } else {
                owner = "";
            }

            if (myDedOwners.equals("") || name.equals(owner) || (getUser(name).getUserlevel() >= 100)) {
                // if no owners set, anyone can send commands

                if (command.equals("restart")) { // Restart the dedicated
                    // server

                    LOGGER.info("Restart command received from " + name);
                    stopHost();// kill the host

                    // Remove any MM option files that deds may have.
                    File localGameOptions = new File("./mmconf");
                    try {
                        if (localGameOptions.exists()) {
                            localGameOptions = new File("./mmconf/gameoptions.xml");
                            if (localGameOptions.exists()) {
                                localGameOptions.delete();
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.error(ex, "");
                    }

                    // sleep for a few seconds before restarting
                    try {
                        Thread.sleep(5000);
                    } catch (Exception ex) {
                        LOGGER.error(ex, "");
                    }
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the restart command on " + myUsername);
                    restartDed();
                    return;

                } else if (command.equals("reset")) { // server reset (like
                    // /reset in MM)

                    LOGGER.info("Reset command received from " + name);
                    if (myServer != null) {
                        resetGame();
                    }
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the reset command on " + myUsername);
                    return;

                } else if (command.equals("die")) { // shut the dedicated down

                    goodbye();
                    System.exit(0);

                } else if (command.equals("start")) { // start hosting a MM
                    // game

                    LOGGER.info("Start command received from " + name);
                    if (myServer == null) {
                        startHost(true, false, false);
                    }
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the start command on " + myUsername);
                    return;

                } else if (command.equals("stop")) { // stop MM host, but w/o
                    // killing ded's
                    // connection

                    // stop the host
                    LOGGER.info("Stop command received from " + name);
                    if (myServer != null) {
                        stopHost();
                    }

                    // sleep, then wait around for a start command ...
                    try {
                        Thread.sleep(5000);
                    } catch (Exception ex) {
                        LOGGER.error(ex, "");
                    }
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the stop command on " + myUsername);
                    return;

                } else if (command.equals("owners")) { // return a list of
                    // owners

                    LOGGER.info("Owners command received from " + name);
                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", My owners: " + myDedOwners.replace('$', ' '));
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the owners command on " + myUsername);
                    return;

                } else if (command.startsWith("owner ")) { // add new owner(s)

                    LOGGER.info("Owner command received from " + name);
                    if (!myDedOwners.equals("")) {
                        myDedOwners = myDedOwners + "$";
                    }

                    myDedOwners = myDedOwners + command.substring(("owner ").length()).trim();
                    getConfig().setParam("DEDICATEDOWNERNAME", myDedOwners);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX +
                                   "c mm# " +
                                   name +
                                   " used the owner " +
                                   myDedOwners +
                                   " command on " +
                                   myUsername);
                    return;

                } else if (command.equals("clearowners")) { // clear owners, and
                    // send feedback.

                    LOGGER.info("Clearowners command received from " + name);
                    myDedOwners = "";
                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", My owners: " + myDedOwners);
                    getConfig().setParam("DEDICATEDOWNERNAME", myDedOwners);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the clear owners command on " + myUsername);
                    return;

                } else if (command.equals("port")) {// return the server's port

                    LOGGER.info("Port command received from " + name);
                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", My port: " + myPort);
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the port command on " + myUsername);
                    return;

                } else if (command.startsWith("port ")) {// new server port

                    LOGGER.info("Port (set) command received from " + name);
                    try {
                        port = Integer.parseInt(command.substring(("port ").length()).trim());
                    } catch (Exception ex) {
                        LOGGER.info("Command error: " + command + ": non-numeral port.");
                        return;
                    }

                    if ((port > 0) && (port < 65536)) {
                        myPort = port;
                    }// check for legal port range
                    else {
                        LOGGER.info("Command error: " + command + ": port out of valid range.");
                    }
                    String portString = Integer.toString(myPort);
                    getConfig().setParam("PORT", portString);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX +
                                   "c mm# " +
                                   name +
                                   " changed the port for " +
                                   myUsername +
                                   " to " +
                                   myPort);
                    return;

                } else if (command.equals("savegamepurge")) {// server days
                    // to purge

                    LOGGER.info("Save game purge command received from " + name);
                    sendChat(PROTOCOL_PREFIX +
                                   "mail " +
                                   name +
                                   ", I purge saved games that are " +
                                   savedGamesMaxDays +
                                   " days old, or older.");
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the save game purge command on " + myUsername);
                    return;

                } else if (command.startsWith("savegamepurge ")) { // set
                    // number of
                    // days to
                    // delete is
                    // purge is
                    // called

                    int mySavedGamesMaxDays = 7;
                    LOGGER.info("Savegamepurge command received from " + name);
                    try {
                        mySavedGamesMaxDays = Integer.parseInt(command.substring(("savegamepurge ").length()).trim());
                    } catch (Exception ex) {
                        LOGGER.info("Command error: " + command + ": invalid number.");
                        return;
                    }

                    String purgeString = Integer.toString(mySavedGamesMaxDays);
                    getConfig().setParam("MAXSAVEDGAMEDAYS", purgeString);
                    getConfig().saveConfig();
                    setConfig();
                    savedGamesMaxDays = mySavedGamesMaxDays;
                    sendChat(PROTOCOL_PREFIX +
                                   "c mm# " +
                                   name +
                                   " changed the save game purge for " +
                                   myUsername +
                                   " to " +
                                   mySavedGamesMaxDays +
                                   " days.");
                    return;

                } else if (command.equals("displaysavedgames")) { // display
                    // saved
                    // games

                    LOGGER.info("displaysavedgames command received from " + name);
                    File[] fileList;
                    String list = "<br><b>Saved files on " + myUsername + "</b><br>";
                    String dateTimeFormat = "MM/dd/yyyy HH:mm:ss";
                    SimpleDateFormat sDF = new SimpleDateFormat(dateTimeFormat);
                    try {
                        File tempFile = new File("./savegames/");
                        fileList = tempFile.listFiles();
                        for (File dateFile : fileList) {
                            Date date = new Date(dateFile.lastModified());
                            String dateTime = sDF.format(date);
                            list += "<a href=\"MEKMAIL" +
                                          myUsername +
                                          "*loadgamewithfullpath " +
                                          dateFile +
                                          "\">Load " +
                                          dateFile +
                                          "</a> " +
                                          dateTime +
                                          "<br>";
                        }
                    } catch (Exception ex) {
                        // do something?
                    }

                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", " + list);
                    sendChat(PROTOCOL_PREFIX +
                                   "c mm# " +
                                   name +
                                   " used the display saved games command on " +
                                   myUsername);
                    return;

                } else if (command.equals("update")) { // update the dedicated
                    // host using
                    // MWAutoUpdate

                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the update command on " + myUsername);
                    LOGGER.info("Update command received from " + name);
                    stopHost();
                    updateDed();
                    return;

                } else if (command.equals("ping")) { // ping dedicated

                    LOGGER.info("Ping command received from " + name);
                    String version = MWDedHost.CLIENT_VERSION;
                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", I'm active with version " + version + ".");
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the ping command on " + myUsername);
                    return;

                }
                if (command.equals("loadgame") || command.startsWith("loadgame ")) { // load
                    // game
                    // from
                    // file

                    LOGGER.info("Loadgame command received from " + name);
                    String filename = "";
                    if (command.startsWith("loadgame ")) {
                        filename = command.substring(("loadgame ").length()).trim();
                    }
                    if (command.equals("loadgame") || filename.equals("")) {
                        filename = "autosave.sav";
                    }
                    if (myServer != null) {
                        if (!loadGame(filename)) {
                            sendChat(PROTOCOL_PREFIX + "mail " + name + ", Unable to load saved game.");
                        } else {
                            sendChat(PROTOCOL_PREFIX + "mail " + name + ", Saved game loaded.");
                        }
                    }
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " loaded game " + filename + " on " + myUsername);
                    return;

                } else if (command.startsWith("loadgamewithfullpath ")) { // load
                    // game
                    // from
                    // file,
                    // using
                    // full
                    // path

                    LOGGER.info("Loadgamewithfullpath command received from " + name);
                    String filename = "";
                    if (command.startsWith("loadgamewithfullpath ")) {
                        filename = command.substring(("loadgamewithfullpath ").length()).trim();
                    }
                    if (command.equals("loadgamewithfullpath") || filename.equals("")) {
                        filename = "autosave.sav";
                    }
                    if (myServer != null) {
                        if (!loadGameWithFullPath(filename)) {
                            sendChat(PROTOCOL_PREFIX + "mail " + name + ", Unable to load saved game.");
                        } else {
                            sendChat(PROTOCOL_PREFIX + "mail " + name + ", Saved game loaded.");
                        }
                    }
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " loaded game " + filename + " on " + myUsername);
                    return;

                } else if (command.equals("loadautosave")) { // load the most
                    // recent auto
                    // save file

                    LOGGER.info("Loadautosave command received from " + name);
                    String filename = "autosave.sav";
                    if (myServer != null) {
                        filename = getParanoidAutoSave();
                        if (!loadGame(filename)) {
                            sendChat(PROTOCOL_PREFIX + "mail " + name + ", Unable to load saved game.");
                        } else {
                            sendChat(PROTOCOL_PREFIX + "mail " + name + ", " + filename + " loaded.");
                        }
                    }
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " loaded " + filename + " game on " + myUsername);
                    return;

                } else if (command.startsWith("name ")) { // new command
                    // prefix

                    LOGGER.info("Name command received from " + name);
                    String myComName = command.substring(("name ").length()).trim();
                    getConfig().setParam("NAME", myComName);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX +
                                   "c mm# " +
                                   name +
                                   " used the set name command to change the name to " +
                                   myComName +
                                   " command on " +
                                   myUsername);
                    Config.setParam("NAME", "[Dedicated] " + myComName);
                    myUsername = Config.getParam("NAME");
                    return;

                } else if (command.startsWith("comment ")) { // new command
                    // prefix

                    LOGGER.info("Prefix command received from " + name);
                    String myComComment = command.substring(("comment ").length()).trim();
                    getConfig().setParam("COMMENT", myComComment);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX +
                                   "c mm# " +
                                   name +
                                   " has set the comment to " +
                                   myComComment +
                                   " on " +
                                   myUsername);
                    return;

                } else if (command.startsWith("players ")) { // new command
                    // prefix

                    LOGGER.info("Prefix command received from " + name);
                    try {
                        String numPlayers = command.substring(("players ").length()).trim();
                        getConfig().setParam("MAXPLAYERS", numPlayers);
                        getConfig().saveConfig();
                        setConfig();
                        sendChat(PROTOCOL_PREFIX +
                                       "c mm# " +
                                       name +
                                       " has set the max number of players to " +
                                       numPlayers +
                                       " on " +
                                       myUsername);
                        return;
                    } catch (Exception ex) {
                        LOGGER.error(ex, "");
                        LOGGER.error("Unable to convert number of players to int");
                        return;
                    }

                } else if (command.equals("restartcount")) { // server port

                    LOGGER.info("Restartcount command received from " + name);
                    sendChat(PROTOCOL_PREFIX +
                                   "mail " +
                                   name +
                                   ", My restart count is set to " +
                                   dedRestartAt +
                                   " my current game count is " +
                                   gameCount);
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the restartcount command on " + myUsername);
                    return;

                } else if (command.startsWith("restartcount ")) {// new
                    // server
                    // port

                    LOGGER.info("restartcount change command received from " + name);
                    try {
                        dedRestartAt = Integer.parseInt(command.substring(("restartcount ").length()).trim());
                    } catch (Exception ex) {
                        LOGGER.info("Command error: " + command + ": bad counter.");
                        return;
                    }
                    String restartString = Integer.toString(dedRestartAt);
                    getConfig().setParam("DEDAUTORESTART", restartString);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX +
                                   "c mm# " +
                                   name +
                                   " changed the restart count for " +
                                   myUsername +
                                   " to " +
                                   dedRestartAt);
                    return;

                } else if (command.equals("getupdateurl")) {// find out what url
                    // the ded is set to
                    // update with

                    LOGGER.info("GetUpdateUrl command received from " + name);
                    String updateURL = getConfigParam("UPDATEURL");
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the getUpdateURL command on " + myUsername);
                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", My update URL is " + updateURL + ".");
                    return;

                } else if (command.startsWith("setupdateurl ")) {

                    LOGGER.info("setUpdateURL command received from " + name);
                    String myUpdateURL = command.substring(("setupdateurl ").length()).trim();
                    getConfig().setParam("UPDATEURL", myUpdateURL);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX +
                                   "c mm# " +
                                   name +
                                   " used the set update url command to change the the update url to " +
                                   myUpdateURL +
                                   " on " +
                                   myUsername);
                    return;

                }

                LOGGER.info("Command error: " + command + ": unknown command.");
                return;
            }
        }

        sendChat(PROTOCOL_PREFIX +
                       "c mm# " +
                       name +
                       " tried to use the " +
                       command +
                       " on " +
                       myUsername +
                       ", but does not have ownership.");
        sendChat(PROTOCOL_PREFIX + "mail " + name + ", You do not have management rights for this host!");
        LOGGER.info("Command error: " + command + ": access denied for " + name + ".");
    }

    public void setLastPing(long lastping) {
        LastPing = lastping;
    }

    /**
     * @return the {@link CPlayer} representing the locally logged-in player.
     */
    @Override
    public CPlayer getPlayer() {
        return null;
    }

    /**
     * Builds a formatted string describing an amount of money or "flu" (an in-campaign currency/resource), depending on
     * campaign settings.
     *
     * @param b  implementation-specific flag (e.g. whether to abbreviate)
     * @param b1 implementation-specific flag (e.g. whether to show a sign)
     * @param i  the amount to format
     *
     * @return the formatted money/flu message
     */
    @Override
    public String moneyOrFluMessage(boolean b, boolean b1, int i) {
        return "";
    }

    /**
     * Overload of {@link #moneyOrFluMessage(boolean, boolean, int)} with an additional formatting flag.
     */
    @Override
    public String moneyOrFluMessage(boolean b, boolean b1, int i, boolean b2) {
        return "";
    }

    /**
     * Looks up a cached server-side configuration value by key.
     *
     * @param rpShortName the config key to look up
     *
     * @return the cached value for that key
     */
    @Override
    public String getServerConfigs(String rpShortName) {
        return "";
    }

    /**
     * Sets/caches a server-side configuration value locally.
     *
     * @param rpShortName the config key to set
     * @param rpValue     the value to store
     */
    @Override
    public void setServerConfigs(String rpShortName, String rpValue) {

    }

    /**
     * @return the {@link CampaignData} holding the static rules/reference data for the current campaign.
     */
    @Override
    public CampaignData getData() {
        return null;
    }

    /**
     * Loads the list of banned ammo types from disk/config into memory.
     */
    @Override
    public void loadBannedAmmo() {

    }

    /**
     * Checks whether a given (target-)system type is currently banned by this server's rules.
     *
     * @param type the system/equipment type identifier to check
     *
     * @return true if that type is banned
     */
    @Override
    public boolean getTargetSystemBanStatus(int type) {
        return false;
    }

    /**
     * @return the client's main GUI window ({@link CMainFrame}). May not be meaningful for headless implementations.
     */
    @Override
    public CMainFrame getMainFrame() {
        return null;
    }

    /**
     * Loads the set of chat/protocol commands supported by the connected server.
     */
    @Override
    public void loadServerCommands() {

    }

    /**
     * Requests/loads the current black market settings from the server.
     */
    @Override
    public void getBlackMarketSettings() {

    }

    /**
     * @return the map of internal equipment name to {@link Equipment} available on the black market.
     */
    @Override
    public Map<String, Equipment> getBlackMarketEquipmentList() {
        return Map.of();
    }

    /**
     * Reloads the client's campaign data from disk/server.
     */
    @Override
    public void reloadData() {

    }

    public void getServerConfigData() {
        try {
            dataFetcher.getServerConfigData(this);
        } catch (Exception ex) {
        }
    }

    /**
     * Sends a single server configuration key/value pair up to the server.
     *
     * @param config the config key
     * @param text   the value to set
     */
    @Override
    public void putServerConfigs(String config, String text) {

    }

    /**
     * Refreshes locally cached data from the server/campaign state.
     */
    @Override
    public void refreshData() {

    }

    /**
     * Appends a line to the default chat display.
     */
    @Override
    public void addToChat(String s) {

    }

    /**
     * Appends a line to a specific chat channel's display.
     *
     * @param s       the text to append
     * @param channel the channel identifier to append to
     */
    @Override
    public void addToChat(String s, int channel) {

    }

    public DedConfig getConfig() {
        return (DedConfig) (Config);
    }

    /**
     * Advances any time-based client-side processing by the given tick amount (e.g. countdown timers).
     *
     * @param time the elapsed time, in an implementation-defined unit (commonly milliseconds or seconds)
     */
    @Override
    public void processTick(int time) {

    }

    /**
     * @return the board edge the player's forces should start deployment from.
     */
    @Override
    public int getPlayerStartingEdge() {
        return 0;
    }

    /**
     * Sets the board edge the player's forces should start deployment from.
     *
     * @param edge the edge identifier (implementation/MegaMek-defined constant)
     */
    @Override
    public void setPlayerStartingEdge(int edge) {

    }

    public String getConfigParam(String p) {
        String tparam = "";

        if (p.endsWith(":")) {
            p = p.substring(0, p.lastIndexOf(":"));
        }
        if (p.equals("NAME") && !(myUsername.equals(""))) {
            return myUsername;
        }
        if (p.equals("NAMEPASSWORD") && !password.equals("")) {
            return password;
        }

        tparam = Config.getParam(p);
        if (tparam == null) {
            tparam = "";
        }

        if (tparam.equals("") && p.equals("NAME") && isDedicated()) {
            LOGGER.info("Error: no dedicated name set.");
            System.exit(1);
        }
        return (tparam);
    }

    public void setUsername(String s) {
        myUsername = s.trim();
    }

    /**
     * Refreshes cached operations/faction ("Op") data, optionally forcing a full reload.
     *
     * @param b true to force a full refresh rather than an incremental one
     */
    @Override
    public void updateOpData(boolean b) {

    }

    public int getMyStatus() {
        return Status;
    }

    public synchronized Collection<CUser> getUsers() {
        return Users;
    }

    /**
     * @return a map of operation ("Op") short name to its associated data array.
     */
    @Override
    public TreeMap<String, String[]> getAllOps() {
        return null;
    }

    /**
     * @return true if the client is currently blocked waiting on a server response (see {@link #setWaiting(boolean)}).
     */
    @Override
    public boolean isWaiting() {
        return false;
    }

    /**
     * Sets whether the client is blocked waiting on a server response. Callers elsewhere in the codebase (e.g.
     * {@code BuildTableViewer.run()}) busy-wait in a sleep loop checking {@link #isWaiting()} until this is cleared.
     *
     * @param b true to enter the waiting state, false to clear it
     */
    @Override
    public void setWaiting(boolean b) {

    }

    /**
     * Return the directory, where all cache files can go into. The dirname depends on the server you connect.
     */
    public String getCacheDir() {
        // if (cacheDir == null) {
        // first access. Check if need to create directory.
        cacheDir = "data/servers/" + Config.getParam("SERVERIP") + "." + Config.getParam("SERVERPORT");
        File dir = new File(cacheDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // }
        return cacheDir;
    }

    /**
     * Loads server-defined "trait" files used by the campaign rules.
     */
    @Override
    public void loadServerTraitFiles() {

    }

    /**
     * Loads/initializes the embedded MegaMek game client used to actually play battles.
     */
    @Override
    public void loadMegaMekClient() {

    }

    public void setConfig() {
        Config = new DedConfig(false);
    }

    /**
     * @return the permission/access level of the currently logged-in user (see {@link IClientUser#getUserLevel()}).
     */
    @Override
    public int getUserLevel() {
        return 0;
    }

    /**
     * @return the {@link RepairManagmentThread} that processes background repair-queue work for this client.
     */
    @Override
    public RepairManagmentThread getRMT() {
        return null;
    }

    /**
     * Looks up the campaign-adjusted cost of a piece of ammunition by its internal MegaMek name.
     *
     * @param internalName the ammo's internal MegaMek identifier
     *
     * @return the computed cost
     */
    @Override
    public double getAmmoCost(String internalName) {
        return 0;
    }

    /**
     * @return the {@link SalvageManagmentThread} that processes background salvage-queue work for this client.
     */
    @Override
    public SalvageManagmentThread getSMT() {
        return null;
    }

    /**
     * @return the {@link CCampaign} representing the overall campaign state.
     */
    @Override
    public CCampaign getCampaign() {
        return null;
    }

    public void setPassword(String s) {
        password = s;
    }

    /**
     * Sets the chat ignore-list scope to "house" (see {@link #IGNORE_HOUSE}).
     */
    @Override
    public void setIgnoreHouse() {

    }

    /**
     * Sets the chat ignore-list scope to "private" (see {@link #IGNORE_PRIVATE}).
     */
    @Override
    public void setIgnorePrivate() {

    }

    /**
     * Sets the chat ignore-list scope to "public" (see {@link #IGNORE_PUBLIC}).
     */
    @Override
    public void setIgnorePublic() {

    }

    /**
     * (Re)loads the set of chat keyword filters this client watches for (see {@link #hasKeyWords(String)}).
     */
    @Override
    public void setKeyWords() {

    }

    /**
     * Switches the GUI's look-and-feel.
     *
     * @param b implementation-defined flag selecting which look-and-feel to use (e.g. native vs. cross-platform)
     */
    @Override
    public void setLookAndFeel(boolean b) {

    }

    /**
     * Displays a modal/non-modal informational window with the given text.
     */
    @Override
    public void showInfoWindow(String s) {

    }

    /**
     * @return the MegaMek {@link Game} currently associated with this client (rules/board/entity state).
     */
    @Override
    public Game getGame() {
        return null;
    }

    /**
     * Sets the advanced terrain configuration used for the current/next map.
     */
    @Override
    public void setAdvancedTerrain(AdvancedTerrain aTerrain) {

    }

    /**
     * Requests a GUI refresh of the given panel/section (one of the {@code REFRESH_*} constants).
     *
     * @param refreshHqPanel the refresh code identifying what to redraw
     */
    @Override
    public void refreshGUI(int refreshHqPanel) {

    }

    /**
     * Checks whether a given user is on this client's ignore list for the given scope.
     *
     * @param name        the username to check
     * @param ignoreHouse the ignore scope (one of the {@code IGNORE_*} constants, despite the parameter name always
     *                    referring to "house")
     *
     * @return true if messages from that user in that scope should be ignored
     */
    @Override
    public boolean isIgnored(String name, int ignoreHouse) {
        return false;
    }

    public String getShortTime() {
        mytime = new Date();
        StringTokenizer s = new StringTokenizer(mytime.toString());
        s.nextElement();
        s.nextElement();
        s.nextElement();
        String t = (String) s.nextElement();
        s = new StringTokenizer(t, ":");
        String result = "[" + s.nextElement() + ":" + s.nextElement() + "] ";
        return result;
    }

    /**
     * Plays a named sound effect, if sound is enabled and not muted.
     */
    @Override
    public void doPlaySound(String soundName) {

    }

    public Vector<IOption> getGameOptions() {
        return GameOptions;
    }

    /**
     * Sets the current planetary environment and map dimensions/medium for the game about to be played.
     *
     * @param planetEnvironment the environmental conditions (weather, light, etc.) to apply
     * @param dimension         the board size
     * @param mapMedium         the map medium/terrain type identifier
     */
    @Override
    public void setEnvironment(PlanetEnvironment planetEnvironment, Dimension dimension, int mapMedium) {

    }

    /**
     * Starts (or reconnects) the embedded MegaMek game client under the given player name.
     *
     * @param curName the player name to connect as
     * @param b       implementation-specific flag (e.g. whether this is a reconnect)
     */
    @Override
    public void startClient(String curName, boolean b) {

    }

    // Stop & send the close game event to the Server
    public void stopHost() {

        serverSend("CG");// send close game to server
        try {
            if (myServer != null) {
                myServer.die();
            }
        } catch (Exception ex) {
            LOGGER.error("Megamek Error:");
            LOGGER.error(ex, "");
        }
        myServer = null;
    }

    public boolean isServerRunning() {
        return myServer != null;
    }

    public void goodbye() {
        SignOff = true;
        if (Status != STATUS_DISCONNECTED) {
            // serverSend("GB");
            Connector.send(PROTOCOL_PREFIX + "signoff");
            dataFetcher.closeDataConnection();
            Connector.closeConnection();
        }

    }

    /**
     * @return the list of active {@link ClientThread}s connecting this client to in-progress MegaMek games.
     */
    @Override
    public List<ClientThread> getMMClients() {
        return List.of();
    }

    /**
     * @return true if the local player is the leader/host of the current game/lobby.
     */
    @Override
    public boolean isLeader() {
        return false;
    }

    /**
     * Shows the dialog for spending/viewing reward points.
     */
    @Override
    public void rewardPointsDialog() {

    }

    /**
     * Shows the dialog for spending/viewing influence points.
     */
    @Override
    public void influencePointsDialog() {

    }

    /**
     * Mutes or unmutes all client sound effects.
     */
    @Override
    public void setSoundMuted(boolean state) {

    }

    // IClient interface
    public void connectToServer() {
        connectToServer(Config.getParam("SERVERIP"), Config.getIntParam("SERVERPORT"));
    }

    public void connectToServer(String ip, int port) {
        if ((myUsername == null) || myUsername.equals("")) {
            errorMessage("Username not set.");
            return;
        }
        // connect to specific ip and port
        // System exits from connector on failure.
        Connector.connect(ip, port);
    }

    public String getStatus() {
        if (Status == STATUS_DISCONNECTED) {
            return ("Not connected");
        }
        if (Status == STATUS_LOGGEDOUT) {
            return ("Logged out");
        }
        return ("");
    }

    /**
     * Processes a line of input typed by the user into the GUI (chat box or command line), dispatching it as a command
     * or plain chat as appropriate.
     */
    @Override
    public void processGUIInput(String s) {

    }

    public Dimension getMapSize() {
        return MapSize;
    }

    /**
     * @return the {@link PlanetEnvironment} currently in effect for the game being set up/played.
     */
    @Override
    public PlanetEnvironment getCurrentEnvironment() {
        return null;
    }

    @Override
    public Buildings getBuildingTemplate() {
        return buildingTemplate;
    }

    public void setBuildingTemplate(Buildings buildingTemplate) {
        this.buildingTemplate = buildingTemplate;
    }

    /**
     * @return the identifier of the current map medium/terrain type.
     */
    @Override
    public int getMapMedium() {
        return 0;
    }

    /**
     * @return the {@link AdvancedTerrain} configuration currently in effect.
     */
    @Override
    public AdvancedTerrain getCurrentAdvancedTerrain() {
        return null;
    }

    /**
     * @return true if bot-controlled forces are enabled for this client's games.
     */
    @Override
    public boolean isUsingBots() {
        return false;
    }

    /**
     * Enables or disables bot-controlled forces for this client's games.
     */
    @Override
    public void setUsingBots(boolean b) {

    }

    /**
     * Appends a line to a chat channel, tagging it with the originating server name (used in multi-server displays).
     *
     * @param s           the text to append
     * @param channelMail the channel identifier to append to
     * @param server      the name of the server the message originated from
     */
    @Override
    public void addToChat(String s, int channelMail, String server) {

    }

    /**
     * Checks whether a string contains any of this client's configured chat keyword filters (see
     * {@link #setKeyWords()}).
     */
    @Override
    public boolean hasKeyWords(String string) {
        return false;
    }

    /**
     * Applies any pending client-side updates (e.g. after downloading a new version's data).
     */
    @Override
    public void updateClient() {

    }

    public void retrieveOpData(String type, String data) {

        StringTokenizer st = new StringTokenizer(data, "#");

        String opName = st.nextToken();

        File opFile = new File("./data/operations/" + type);

        if (!opFile.exists()) {
            opFile.mkdirs();
        }

        opFile = new File("./data/operations/" + type + "/" + opName + ".txt");
        try {
            FileOutputStream out = new FileOutputStream(opFile);
            PrintStream p = new PrintStream(out);
            while (st.hasMoreTokens()) {
                p.println(st.nextToken().replaceAll("\\(pound\\)", "#"));
            }
            p.close();
            out.close();
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }

    }

    public void updateParam(StringTokenizer ST) {
        try {
            getConfig().setParam(ST.nextToken(), ST.nextToken());
            getConfig().saveConfig();
            setConfig();
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }
    }

    /**
     * Applies incoming server operation-flag settings, parsed from the given tokenizer.
     */
    @Override
    public void setServerOpFlags(StringTokenizer st) {

    }

    /**
     * Updates the cached black-market parts listing for a given campaign year.
     *
     * @param s            implementation-defined update payload
     * @param campaignYear the in-campaign year the update applies to
     */
    @Override
    public void updatePartsBlackMarket(String s, int campaignYear) {

    }

    /**
     * Updates the locally cached player parts inventory from an incoming update payload.
     */
    @Override
    public void updatePlayerPartsCache(String s) {

    }

    public void retrieveMul(String data) {

        StringTokenizer st = new StringTokenizer(data, "#");

        String mulName = st.nextToken();

        File mulFile = new File("./data/armies/");

        if (!mulFile.exists()) {
            mulFile.mkdirs();
        }

        mulFile = new File("./data/armies/" + mulName);
        try {
            FileOutputStream out = new FileOutputStream(mulFile);
            PrintStream p = new PrintStream(out);
            while (st.hasMoreTokens()) {
                p.println(st.nextToken().replaceAll("\\(pound\\)", "#"));
            }
            p.close();
            out.close();
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }

    }

    /**
     * Applies an incoming "create new house" (faction) protocol command, parsed from the given tokenizer.
     */
    @Override
    public void createNewHouse(StringTokenizer st) {

    }

    /**
     * Computes a checksum for a filename/string, used to validate file transfers/caches.
     *
     * @param s the input to checksum
     *
     * @return the computed checksum string
     *
     * @throws Exception if the checksum cannot be computed (e.g. algorithm unavailable, I/O error)
     */
    @Override
    public String createFilenameChecksum(String s) throws Exception {
        return "";
    }

    public String getLastQuery() {
        return LastQuery;
    }

    public void setLastQuery(String name) {
        LastQuery = name;
    }

    /**
     * Finds usernames that partially match the given text, for chat auto-completion.
     *
     * @param text the partial username typed so far
     *
     * @return the list of matching usernames
     */
    @Override
    public ArrayList<String> getPartialUser(String text) {
        return null;
    }

    /**
     * @return the map of house/faction ID to {@link Influences} changes accumulated since the last GUI refresh.
     */
    @Override
    public Map<Integer, Influences> getChangesSinceLastRefresh() {
        return Map.of();
    }

    /**
     * @return the minimum ownership percentage/threshold required to be considered an owner of the given planet.
     */
    @Override
    public int getMinPlanetOwnerShip(Planet planet) {
        return 0;
    }

    /**
     * Computes the technician labor cost (in hours or points, implementation-defined) to repair/build the given entity
     * at the given tech skill level.
     *
     * @param entity    the unit being worked on
     * @param techGreen the technician's skill level (name suggests a "green"/rookie skill constant, but the actual
     *                  meaning is defined by the implementation)
     *
     * @return the computed labor cost
     */
    @Override
    public int getTechLaborCosts(Entity entity, int techGreen) {
        return 0;
    }

    /**
     * Computes the total monetary cost to fully repair the given entity.
     */
    @Override
    public double getTotalRepairCosts(Entity entity) {
        return 0;
    }

    /**
     * @return true if bot-controlled forces are configured to be placed on the same team as their controlling player,
     *       rather than as a separate opposing side.
     */
    @Override
    public boolean isBotsOnSameTeam() {
        return false;
    }

    /**
     * Sets whether bot-controlled forces are placed on the same team as their controlling player. Takes a boxed
     * {@link Boolean} rather than a primitive {@code boolean}.
     */
    @Override
    public void setBotsOnSameTeam(Boolean aBoolean) {

    }

    public void closingGame(String hostName) {

        // update battles tab for all players, via server
        LOGGER.info("Leaving " + hostName);
        serverSend("LG|" + hostName);

        System.gc();
    }

    public Properties getServerConfigs() {
        return serverConfigs;
    }

    private void updateDed() {
        try {
            if (myServer != null) {
                myServer.die();
            }
            goodbye();
            Runtime runtime = Runtime.getRuntime();
            String[] call =
                  { "java", "-jar", "MekWarsAutoUpdate.jar", "DEDICATED", getConfigParam("DEDUPDATECOMMANDFILE") };
            runtime.exec(call);
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }
        System.exit(0);// restart the ded
    }

    @Override
    public void gameClientFeedbackRequest(GameCFREvent arg0) {
        // TODO Auto-generated method stub

    }

    public boolean isUsingAdvanceRepairs() {
        return Boolean.parseBoolean(getserverConfigs("UseAdvanceRepair")) ||
                     Boolean.parseBoolean(getserverConfigs("UseSimpleRepair"));
    }

    protected void sendServerGameUpdate() {
        // Report the mech stat

        // Only send data for units currently on the board.
        // any units removed from play will have already sent thier final
        // update.
        Iterator<Entity> en = ((Game) myServer.getGame()).getEntities();
        while (en.hasNext()) {
            Entity ent = en.next();
            if (ent.getOwner().getName().startsWith("War Bot")
                      || (!(ent instanceof MechWarrior)
                                && !UnitUtils.hasArmorDamage(ent)
                                && !UnitUtils.hasISDamage(ent)
                                && !UnitUtils.hasCriticalDamage(ent)
                                && !UnitUtils.hasLowAmmo(ent) && !UnitUtils
                                                                        .hasEmptyAmmo(ent))) {
                continue;
            }
            if ((ent instanceof Mech) && (ent.getInternal(Mech.LOC_CT) <= 0)) {
                serverSend("IPU|"
                                 + SerializeEntity.serializeEntity(ent, true, true,
                      isUsingAdvanceRepairs()));
            } else {
                serverSend("IPU|"
                                 + SerializeEntity.serializeEntity(ent, true, false,
                      isUsingAdvanceRepairs()));
            }
        }
    }

    public String getserverConfigs(String key) {
        if (serverConfigs.getProperty(key) == null) {
            return "-1";
        }
        return serverConfigs.getProperty(key).trim();
    }

    protected void sendGameReport() {
        if (myServer == null) {
            return;
        }

        //GameReporter.prepareReport(myGame, usingAdvancedRepairs, buildingTemplate)

        StringBuilder result = prepareReport(new GameWrapper(((Game) myServer.getGame())),
              isUsingAdvanceRepairs(),
              buildingTemplate);
        serverSend("CR|" + result.toString());

/*        StringBuilder result = new StringBuilder();
        String name = "";
        // Parse the real playername from the Modified In game one..
        String winnerName = "";
        if (((Game)myServer.getGame()).getVictoryTeam() != Player.TEAM_NONE) {

            int numberOfWinners = 0;
            // Multiple Winners
            Enumeration<Player> en = ((Game)myServer.getGame()).getPlayers();
            while (en.hasMoreElements()) {
                Player p = en.nextElement();
                if (p.getTeam() == ((Game)myServer.getGame()).getVictoryTeam()) {
                    StringTokenizer st = new StringTokenizer(p.getName().trim(), "~");
                    name = "";
                    while (st.hasMoreElements()) {
                        name = st.nextToken().trim();
                    }
                    // some of the players set themselves as a team of 1.
                    // This keeps that from happening.
                    if (numberOfWinners > 0) {
                        winnerName += "*";
                    }
                    numberOfWinners++;

                    winnerName += name;
                }
            }
            if (winnerName.endsWith("*")) {
                winnerName = winnerName.substring(0, winnerName.length() - 1);
            }
            winnerName += "#";
        }

        // Only one winner
        else {
            if (((Game)myServer.getGame()).getVictoryPlayerId() == Player.PLAYER_NONE) {
                winnerName = "DRAW#";
            } else {
                winnerName = ((Game)myServer.getGame()).getPlayer(((Game)myServer.getGame()).getVictoryPlayerId()).getName();
                StringTokenizer st = new StringTokenizer(winnerName, "~");
                name = "";
                while (st.hasMoreElements()) {
                    name = st.nextToken().trim();
                }
                winnerName = name + "#";
            }
        }

        result.append(winnerName);

        // Report the mech stat
        Enumeration<Entity> en = ((Game)myServer.getGame()).getDevastatedEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false, isUsingAdvanceRepairs()));
            result.append("#");
        }
        en = ((Game)myServer.getGame()).getGraveyardEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false, isUsingAdvanceRepairs()));
            result.append("#");

        }
        en = ((Game)myServer.getGame()).getEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false, isUsingAdvanceRepairs()));
            result.append("#");
        }
        en = ((Game)myServer.getGame()).getRetreatedEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false, isUsingAdvanceRepairs()));
            result.append("#");
        }

        if (getBuildingTemplate() != null) {
            result.append("BL*" + getBuildingsLeft());
        }
        MWLogger.infoLog("CR|" + result);

        // send the autoreport
        serverSend("CR|" + result.toString());*/

        // we may assume that a server which reports a game is no longer
        // "Running"
        serverSend("SHS|" + myUsername + "|Open");

        // myServer.resetGame();

        if (isDedicated()) {
            checkForRestart();
        }
    }

    public synchronized CUser getUser(String name) {

        for (CUser currUser : Users) {
            if (currUser.getName().equalsIgnoreCase(name)) {
                return currUser;
            }
        }
        CUser dummyUser = new CUser();
        return dummyUser;
    }

    public static StringBuilder prepareReport(GameInterface myGame, boolean usingAdvancedRepairs,
          Buildings buildingTemplate) {
        StringBuilder result = new StringBuilder();
        String name = "";
        // Parse the real playername from the Modified In game one..
        String winnerName = "";
        if (myGame.hasWinner()) {

            int numberOfWinners = 0;
            // Multiple Winners
            List<String> winners = myGame.getWinners();

            //TODO: Winners is sometimes coming up empty.  Let's see why
            LOGGER.error("Finding winners:");
            LOGGER.error(winners.toString());

            for (String winner : winners) {
                StringTokenizer st = new StringTokenizer(winner, "~");
                name = "";
                while (st.hasMoreElements()) {
                    name = st.nextToken().trim();
                }
                // some of the players set themselves as a team of 1.
                // This keeps that from happening.
                if (numberOfWinners > 0) {
                    winnerName += "*";
                }
                numberOfWinners++;

                winnerName += name;
            }
            if (winnerName.endsWith("*")) {
                winnerName = winnerName.substring(0, winnerName.length() - 1);
            }
            winnerName += "#";
        } else {
            winnerName = "DRAW#";
        }

        result.append(winnerName);

        // Report the mech stat
        Enumeration<Entity> en = myGame.getDevastatedEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false, usingAdvancedRepairs));
            result.append("#");
        }
        en = myGame.getGraveyardEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false, usingAdvancedRepairs));
            result.append("#");

        }
        Iterator<Entity> en2 = myGame.getEntities();
        while (en2.hasNext()) {
            Entity ent = en2.next();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false, usingAdvancedRepairs));
            result.append("#");
        }
        en = myGame.getRetreatedEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false, usingAdvancedRepairs));
            result.append("#");
        }

        if (buildingTemplate != null) {
            result.append("BL*" + buildingTemplate);
        }
        LOGGER.info("CR|" + result);
        return result;
    }

    // this adds 1 to the number of games played and if it matched the restart
    // amount it restarts the ded.
    public void checkForRestart() {
        gameCount++;

        // only check for restart once every 30 seconds.
        if (System.currentTimeMillis() - 30000 < lastResetCheck) {
            return;
        }

        if (gameCount >= dedRestartAt) {
            LOGGER.info("System has reached " + gameCount + " games played and is restarting");
            try {
                Thread.sleep(5000);
            }// give people time to vacate
            catch (Exception ex) {
                LOGGER.error(ex, "");
            }
            try {
                stopHost();
                Thread.sleep(5000);
            }// give people time to vacate
            catch (Exception ex) {
                LOGGER.error(ex, "");
            }
            restartDed();
        }

        lastResetCheck = System.currentTimeMillis();
    }

    private void restartDed() {
        try {
            String memory = Config.getParam("DEDMEMORY");
            Runtime runTime = Runtime.getRuntime();
            String[] call =
                  { "java", "-Xmx" + memory + "m", "-jar", "MekWarsDed.jar" };
            runTime.exec(call);
            System.exit(0);

        } catch (Exception ex) {
            LOGGER.error("Unable to find MekWarsDed.jar");
        }
    }

    @Override
    public void gameVictory(PostGameResolution e) {

    }

    /*
     * INNER CLASSES
     */
    static class AutoSaveFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.startsWith("autosave"));
        }
    }

    private static class PurgeAutoSaves implements Runnable {

        public PurgeAutoSaves() {
            super();
        }

        public void run() {
            long twoHours = 2 * 60 * 60 * 1000;
            try {
                while (true) {
                    File saveFiles = new File("./savegames");
                    if (!saveFiles.exists()) {
                        return;
                    }
                    FilenameFilter filter = new AutoSaveFilter();
                    File[] fileList = saveFiles.listFiles(filter);
                    for (File savedFile : fileList) {
                        long lastTime = savedFile.lastModified();
                        if (savedFile.exists() &&
                                  savedFile.isFile() &&
                                  (lastTime < (System.currentTimeMillis() - twoHours))) {
                            try {
                                LOGGER.info("Purging File: " +
                                                  savedFile.getName() +
                                                  " Time: " +
                                                  lastTime +
                                                  " purge Time: " +
                                                  (System.currentTimeMillis() - twoHours));
                                savedFile.delete();
                            } catch (Exception ex) {
                                LOGGER.error("Error trying to delete these files!");
                                LOGGER.error(ex, "");
                            }
                        }
                    }
                    Thread.sleep(twoHours);
                }
            } catch (Exception ex) {
                return;
            }
        }
    }// end PurgeAutoSaves

    protected class TimeOutThread extends Thread {

        MWDedHost mwdedhost;

        public TimeOutThread(MWDedHost client) {
            mwdedhost = client;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(mwdedhost.TimeOut * 100);
                } catch (Exception ex) {
                    LOGGER.error(ex, "");
                }
                if (mwdedhost.Status != MWDedHost.STATUS_DISCONNECTED) {
                    long timeout = (System.currentTimeMillis() / 1000) - LastPing;
                    if (timeout > mwdedhost.TimeOut) {
                        systemMessage("Ping timeout (" + timeout + " s)");
                        Connector.closeConnection();
                    }
                } else {
                    LastPing = System.currentTimeMillis() / 1000;
                }
            }
        }
    }
}
