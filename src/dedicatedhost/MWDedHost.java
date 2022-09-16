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

package dedicatedhost;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import org.mekwars.libpk.logging.PKLogManager;

import common.GameInterface;
import common.GameWrapper;
import common.MMGame;
import common.campaign.Buildings;
import common.campaign.clientutils.GameHost;
import common.campaign.clientutils.IGameHost;
import common.campaign.clientutils.SerializeEntity;
import common.campaign.clientutils.protocol.CConnector;
import common.campaign.clientutils.protocol.IClient;
import common.campaign.clientutils.protocol.commands.AckSignonPCmd;
import common.campaign.clientutils.protocol.commands.CommPCmd;
import common.campaign.clientutils.protocol.commands.IProtCommand;
import common.campaign.clientutils.protocol.commands.PingPCmd;
import common.campaign.clientutils.protocol.commands.PongPCmd;
import common.util.MWLogger;
import common.util.UnitUtils;
import dedicatedhost.cmd.Command;
import dedicatedhost.protocol.DataFetchClient;
import megamek.MegaMek;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IGame.Phase;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.event.GameCFREvent;
import megamek.common.options.IOption;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.server.Server;


// This is the Client used for connecting to the master server.
// @Author: Helge Richter (McWizard@gmx.de)

public final class MWDedHost extends GameHost implements IClient, IGameHost {

    DataFetchClient dataFetcher;

    public static final String CLIENT_VERSION = "0.8.0.0"; // change this with
    // all client
    // changes @Torren

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

    private Phase currentPhase = IGame.Phase.PHASE_DEPLOYMENT;
    private int turn = 0;

    Dimension MapSize;
    Dimension BoardSize;

    /**
     * Maps the task prefixes as HS, PL, SP etc. to a command under package cmd.
     * key: String, value: cmd.Command
     */
    HashMap<String, Command> commands = new HashMap<String, Command>();

    String LastQuery = ""; // receiver of last mail
    private String cacheDir;

    /**
     * @author Torren place holder until I can think of something better to say.
     */
    public Properties serverConfigs = new Properties();

    private static MWLogger logger;
    

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
            MWLogger.errLog(ex);
            MWLogger.errLog("Unable to redirect MegaMek output to " + logFileName);
        }

        MWLogger.infoLog("Starting MekWars Client Version: " + CLIENT_VERSION);
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
             * NOTE: Client constrtuctor attempts to pull the oplist, campaign
             * config and other non-interactive data over the DATAPORT before
             * client.start() attempts to connect to the chat server on the
             * SERVERPORT.
             */
            new MWDedHost(config);

        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Couldn't create Client Object");
            System.exit(1);
        }
    }

    public MWDedHost(DedConfig config) {

    	ProtCommands = new TreeMap<String, IProtCommand>();
    	
        Config = config;

        Connector = new CConnector(this);

        Users = Collections.synchronizedList(new Vector<CUser>(1, 1));

        createProtCommands();
        dataFetcher = new DataFetchClient(Integer.parseInt(Config.getParam("DATAPORT")), Integer.parseInt(Config.getParam("SOCKETTIMEOUTDELAY")));
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
            MWLogger.errLog(ex);
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
        MWLogger.infoLog("Starting pAS");
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
            MWLogger.errLog(e);
            System.exit(1);
        }

        int retryCount = 0;
        while ((Status == STATUS_DISCONNECTED) && (retryCount++ < 20)) {
            connectToServer(chatServerIP, chatServerPort);
            if (Status == STATUS_DISCONNECTED) {
                MWLogger.infoLog("Couldn't connect to server. Retrying in 90 seconds.");
                try {
                    Thread.sleep(90000);
                } catch (Exception exe) {
                    MWLogger.errLog(exe);
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

    /*
     * NOTE: this list is ancient. sometimes useful. often out of date.
     * 
     * List of Abreviations for the protocol used by the client only: NG = New
     * Game (NG|<IP>|<Port>|<MaxPlayers>|<Version>|<Comment>) CG = Close Game
     * (CG) GB = Goodbye (Client exit) (GB) SO = Sign-On
     * (SO|<Version>|<UserName>)
     * 
     * Used by Both: CH = Chat Server news:(CH|<text>) Client Chat:
     * (CH|<UserName>|<Color>|<Text>)
     * 
     * Used only by the Server: SL|NG = Games
     * (GS|<MMGame.toString()>|<MMGame.toString()|...) SL|CG = close game SL|JG
     * = add a player to game list SL|LG = remove a player from game list SL|SHS
     * = Set Host Status (SHS|<GameID>|<Status>) US = Users
     * (US|<MMClientInfo.toString()>|<MMClientInfo.toString()>|..) UG = User
     * Gone (UG|<MMClientInfo.toString>|[Gone]) Gone is used when the client
     * didn't just change his name NU = New User
     * (NU|<MMClientInfo.toString>|[NEW]) NEW is used the same way as GONE in UG
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
            MWLogger.infoLog(input);

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
                    MWLogger.errLog(e);
                }
            }
            if (commands.containsKey(task)) {
                commands.get(task).execute(input);
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
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
        if (data.startsWith("US|") || data.startsWith("NU|") || data.startsWith("UG|") || data.startsWith("RGTS|") || data.startsWith("DSD|") || data.startsWith("USD|")) {
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
            MWLogger.infoLog("display megameklog command received from " + name);
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
            MWLogger.infoLog("display ded error command received from " + name);
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
            MWLogger.infoLog("display ded log command received from " + name);
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

                    MWLogger.infoLog("Restart command received from " + name);
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
                        MWLogger.errLog(ex);
                    }

                    // sleep for a few seconds before restarting
                    try {
                        Thread.sleep(5000);
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                    }
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the restart command on " + myUsername);
                    restartDed();
                    return;

                } else if (command.equals("reset")) { // server reset (like
                    // /reset in MM)

                    MWLogger.infoLog("Reset command received from " + name);
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

                    MWLogger.infoLog("Start command received from " + name);
                    if (myServer == null) {
                        startHost(true, false, false);
                    }
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the start command on " + myUsername);
                    return;

                } else if (command.equals("stop")) { // stop MM host, but w/o
                    // killing ded's
                    // connection

                    // stop the host
                    MWLogger.infoLog("Stop command received from " + name);
                    if (myServer != null) {
                        stopHost();
                    }

                    // sleep, then wait around for a start command ...
                    try {
                        Thread.sleep(5000);
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                    }
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the stop command on " + myUsername);
                    return;

                } else if (command.equals("owners")) { // return a list of
                    // owners

                    MWLogger.infoLog("Owners command received from " + name);
                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", My owners: " + myDedOwners.replace('$', ' '));
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the owners command on " + myUsername);
                    return;

                } else if (command.startsWith("owner ")) { // add new owner(s)

                    MWLogger.infoLog("Owner command received from " + name);
                    if (!myDedOwners.equals("")) {
                        myDedOwners = myDedOwners + "$";
                    }

                    myDedOwners = myDedOwners + command.substring(("owner ").length()).trim();
                    getConfig().setParam("DEDICATEDOWNERNAME", myDedOwners);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the owner " + myDedOwners + " command on " + myUsername);
                    return;

                } else if (command.equals("clearowners")) { // clear owners, and
                    // send feedback.

                    MWLogger.infoLog("Clearowners command received from " + name);
                    myDedOwners = "";
                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", My owners: " + myDedOwners);
                    getConfig().setParam("DEDICATEDOWNERNAME", myDedOwners);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the clear owners command on " + myUsername);
                    return;

                } else if (command.equals("port")) {// return the server's port

                    MWLogger.infoLog("Port command received from " + name);
                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", My port: " + myPort);
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the port command on " + myUsername);
                    return;

                } else if (command.startsWith("port ")) {// new server port

                    MWLogger.infoLog("Port (set) command received from " + name);
                    try {
                        port = Integer.parseInt(command.substring(("port ").length()).trim());
                    } catch (Exception ex) {
                        MWLogger.infoLog("Command error: " + command + ": non-numeral port.");
                        return;
                    }

                    if ((port > 0) && (port < 65536)) {
                        myPort = port;
                    }// check for legal port range
                    else {
                        MWLogger.infoLog("Command error: " + command + ": port out of valid range.");
                    }
                    String portString = Integer.toString(myPort);
                    getConfig().setParam("PORT", portString);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " changed the port for " + myUsername + " to " + myPort);
                    return;

                } else if (command.equals("savegamepurge")) {// server days
                    // to purge

                    MWLogger.infoLog("Save game purge command received from " + name);
                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", I purge saved games that are " + savedGamesMaxDays + " days old, or older.");
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the save game purge command on " + myUsername);
                    return;

                } else if (command.startsWith("savegamepurge ")) { // set
                    // number of
                    // days to
                    // delete is
                    // purge is
                    // called

                    int mySavedGamesMaxDays = 7;
                    MWLogger.infoLog("Savegamepurge command received from " + name);
                    try {
                        mySavedGamesMaxDays = Integer.parseInt(command.substring(("savegamepurge ").length()).trim());
                    } catch (Exception ex) {
                        MWLogger.infoLog("Command error: " + command + ": invalid number.");
                        return;
                    }

                    String purgeString = Integer.toString(mySavedGamesMaxDays);
                    getConfig().setParam("MAXSAVEDGAMEDAYS", purgeString);
                    getConfig().saveConfig();
                    setConfig();
                    savedGamesMaxDays = mySavedGamesMaxDays;
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " changed the save game purge for " + myUsername + " to " + mySavedGamesMaxDays + " days.");
                    return;

                } else if (command.equals("displaysavedgames")) { // display
                    // saved
                    // games

                    MWLogger.infoLog("displaysavedgames command received from " + name);
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
                            list += "<a href=\"MEKMAIL" + myUsername + "*loadgamewithfullpath " + dateFile + "\">Load " + dateFile + "</a> " + dateTime + "<br>";
                        }
                    } catch (Exception ex) {
                        // do something?
                    }

                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", " + list);
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the display saved games command on " + myUsername);
                    return;

                } else if (command.equals("update")) { // update the dedicated
                    // host using
                    // MWAutoUpdate

                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the update command on " + myUsername);
                    MWLogger.infoLog("Update command received from " + name);
                    stopHost();
                    updateDed();
                    return;

                } else if (command.equals("ping")) { // ping dedicated

                    MWLogger.infoLog("Ping command received from " + name);
                    String version = MWDedHost.CLIENT_VERSION;
                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", I'm active with version " + version + ".");
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the ping command on " + myUsername);
                    return;

                }
                if (command.equals("loadgame") || command.startsWith("loadgame ")) { // load
                    // game
                    // from
                    // file

                    MWLogger.infoLog("Loadgame command received from " + name);
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

                    MWLogger.infoLog("Loadgamewithfullpath command received from " + name);
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

                    MWLogger.infoLog("Loadautosave command received from " + name);
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

                    MWLogger.infoLog("Name command received from " + name);
                    String myComName = command.substring(("name ").length()).trim();
                    getConfig().setParam("NAME", myComName);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the set name command to change the name to " + myComName + " command on " + myUsername);
                    Config.setParam("NAME", "[Dedicated] " + myComName);
                    myUsername = Config.getParam("NAME");
                    return;

                } else if (command.startsWith("comment ")) { // new command
                    // prefix

                    MWLogger.infoLog("Prefix command received from " + name);
                    String myComComment = command.substring(("comment ").length()).trim();
                    getConfig().setParam("COMMENT", myComComment);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " has set the comment to " + myComComment + " on " + myUsername);
                    return;

                } else if (command.startsWith("players ")) { // new command
                    // prefix

                    MWLogger.infoLog("Prefix command received from " + name);
                    try {
                        String numPlayers = command.substring(("players ").length()).trim();
                        getConfig().setParam("MAXPLAYERS", numPlayers);
                        getConfig().saveConfig();
                        setConfig();
                        sendChat(PROTOCOL_PREFIX + "c mm# " + name + " has set the max number of players to " + numPlayers + " on " + myUsername);
                        return;
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                        MWLogger.errLog("Unable to convert number of players to int");
                        return;
                    }

                } else if (command.equals("restartcount")) { // server port

                    MWLogger.infoLog("Restartcount command received from " + name);
                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", My restart count is set to " + dedRestartAt + " my current game count is " + gameCount);
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the restartcount command on " + myUsername);
                    return;

                } else if (command.startsWith("restartcount ")) {// new
                    // server
                    // port

                    MWLogger.infoLog("restartcount change command received from " + name);
                    try {
                        dedRestartAt = Integer.parseInt(command.substring(("restartcount ").length()).trim());
                    } catch (Exception ex) {
                        MWLogger.infoLog("Command error: " + command + ": bad counter.");
                        return;
                    }
                    String restartString = Integer.toString(dedRestartAt);
                    getConfig().setParam("DEDAUTORESTART", restartString);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " changed the restart count for " + myUsername + " to " + dedRestartAt);
                    return;

                } else if (command.equals("getupdateurl")) {// find out what url
                    // the ded is set to
                    // update with

                    MWLogger.infoLog("GetUpdateUrl command received from " + name);
                    String updateURL = getConfigParam("UPDATEURL");
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the getUpdateURL command on " + myUsername);
                    sendChat(PROTOCOL_PREFIX + "mail " + name + ", My update URL is " + updateURL + ".");
                    return;

                } else if (command.startsWith("setupdateurl ")) {

                    MWLogger.infoLog("setUpdateURL command received from " + name);
                    String myUpdateURL = command.substring(("setupdateurl ").length()).trim();
                    getConfig().setParam("UPDATEURL", myUpdateURL);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(PROTOCOL_PREFIX + "c mm# " + name + " used the set update url command to change the the update url to " + myUpdateURL + " on " + myUsername);
                    return;

                }

                MWLogger.infoLog("Command error: " + command + ": unknown command.");
                return;
            }
        }

        sendChat(PROTOCOL_PREFIX + "c mm# " + name + " tried to use the " + command + " on " + myUsername + ", but does not have ownership.");
        sendChat(PROTOCOL_PREFIX + "mail " + name + ", You do not have management rights for this host!");
        MWLogger.infoLog("Command error: " + command + ": access denied for " + name + ".");
    }

    protected void createProtCommands() {
        addProtCommand(new CommPCmd(this));
        addProtCommand(new PingPCmd(this));
        addProtCommand(new PongPCmd(this));
        addProtCommand(new AckSignonPCmd(this));
    }

    protected void addProtCommand(IProtCommand command) {
        ProtCommands.put(command.getName(), command);
    }

    IProtCommand getProtCommand(String command) {
        return ProtCommands.get(command);
    }

    public String getLastQuery() {
        return LastQuery;
    }

    public void setLastQuery(String name) {
        LastQuery = name;
    }

    public int getMyStatus() {
        return Status;
    }

    public void setLastPing(long lastping) {
        LastPing = lastping;
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

    public synchronized CUser getUser(String name) {

        for (CUser currUser : Users) {
            if (currUser.getName().equalsIgnoreCase(name)) {
                return currUser;
            }
        }
        CUser dummyUser = new CUser();
        return dummyUser;
    }

    public synchronized void clearUserCampaignData() {
        for (CUser currUser : Users) {
            currUser.clearCampaignData();
        }
    }

    public synchronized Collection<CUser> getUsers() {
        return Users;
    }

    public String getProtocolVersion() {
        return "4";
    }

    public void setUsername(String s) {
        myUsername = s.trim();
    }

    public void setPassword(String s) {
        password = s;
    }

    public DedConfig getConfig() {
        return (DedConfig) (Config);
    }

    public void setConfig() {
        Config = new DedConfig(false);
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
            MWLogger.infoLog("Error: no dedicated name set.");
            System.exit(1);
        }
        return (tparam);
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
                    MWLogger.infoLog("COMMAND ERROR: wrong protocol command executed or execution failed.");
                    MWLogger.infoLog("COMMAND RECEIVED: " + incoming);
                }
                return;
            }
            if (pcommand == null) {
                MWLogger.infoLog("COMMAND ERROR: unknown protocol command from server.");
                MWLogger.infoLog("COMMAND RECEIVED: " + incoming);
                return;
            }
        } else {
            MWLogger.infoLog("COMMAND ERROR: received protocol command without protocol prefix.");
            MWLogger.infoLog("COMMAND RECEIVED: " + incoming);
            return;
        }
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
                MWLogger.errLog(ex);
            }

            // keep retrying every two minutes after the first 90 sec downtime.
            while (Status == STATUS_DISCONNECTED) {
                connectToServer(Config.getParam("SERVERIP"), Config.getIntParam("SERVERPORT"));
                if (Status == STATUS_DISCONNECTED) {
                    MWLogger.infoLog("Couldn't reconnect to server. Retrying in 120 seconds.");
                    try {
                        Thread.sleep(90000);
                    } catch (Exception exe) {
                        MWLogger.errLog(exe);
                    }
                }
            }
        } else {
            Users.clear();
        }
    }

    public void connectionEstablished() {

        LastPing = System.currentTimeMillis() / 1000;
        MWLogger.errLog("Connected. Signing on.");

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

        Connector.send(PROTOCOL_PREFIX + "signon\t" + getConfigParam("NAME") + "\t" + passToSend + "\t" + getProtocolVersion() + "\t" + Config.getParam("COLOR") + "\t" + CLIENT_VERSION + "\t" + ST.nextToken());
        Status = STATUS_LOGGEDOUT;
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

    public void goodbye() {
        SignOff = true;
        if (Status != STATUS_DISCONNECTED) {
            // serverSend("GB");
            Connector.send(PROTOCOL_PREFIX + "signoff");
            dataFetcher.closeDataConnection();
            Connector.closeConnection();
        }

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
        if (!MMVersion.equals("-1") && !MMVersion.equalsIgnoreCase(MegaMek.VERSION)) {
            MWLogger.errLog("You are using an invalid version of MegaMek. Please use version " + MMVersion);
            stopHost();
            updateDed();
            return;
        }

        if (servers.get(myUsername) != null) {
            if (isDedicated()) {
                MWLogger.errLog("Attempted to start a second host while host was already running.");
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
            myServer = new Server(gpassword, myPort);
        } catch (Exception ex) {
            try {
                if (myServer == null) {
                    MWLogger.errLog("Error opening dedicated server. Result = null host.");
                    MWLogger.errLog(ex);
                } else {
                    MWLogger.errLog("Error opening dedicated server. Will attempt a .die().");
                    MWLogger.errLog(ex);
                    myServer.die();
                    myServer = null;
                }
            } catch (Exception e) {
                MWLogger.errLog("Further error while trying to clean up failed host attempt.");
                MWLogger.errLog(e);
            }
            return;
        }

        myServer.getGame().addGameListener(this);
        // Send the new game info to the Server
        serverSend("NG|" + new MMGame(myUsername, ip, myPort, MaxPlayers, MegaMek.VERSION + " " + MegaMek.TIMESTAMP, comment).toString());
        clearSavedGames();
        purgeOldLogs();
        IClientPreferences cs = PreferenceManager.getClientPreferences();
        cs.setStampFilenames(Boolean.parseBoolean(getserverConfigs("MMTimeStampLogFile")));
    }

    // Stop & send the close game event to the Server
    public void stopHost() {

        serverSend("CG");// send close game to server
        try {
            if (myServer != null) {
                myServer.die();
            }
        } catch (Exception ex) {
            MWLogger.errLog("Megamek Error:");
            MWLogger.errLog(ex);
        }
        myServer = null;
    }

    public void resetGame() { // reset hosted game
        if (myServer != null) {
            myServer.resetGame();
            myServer.getGame().purgeGameListeners();
            myServer.getGame().addGameListener(this);
        }
    }

    public boolean loadGame(String filename) {// load saved game
        if ((myServer != null) && (filename != null) && !filename.equals("")) {
            boolean loaded = myServer.loadGame(new File("./savegames/", filename));
            myServer.getGame().addGameListener(this);
            return loaded;
        }

        // else (null server/filename)
        if (myServer == null) {
            MWLogger.infoLog("MyServer == NULL!");
        }
        if (filename == null) {
            MWLogger.infoLog("Filename == NULL!");
        } else if (filename.equals("")) {
            MWLogger.infoLog("Filename == \"\"!");
        }

        return false;
    }

    public boolean loadGameWithFullPath(String filename) {// load saved game
        if ((myServer != null) && (filename != null) && !filename.equals("")) {
            boolean loaded = myServer.loadGame(new File(filename));
            myServer.getGame().addGameListener(this);
            return loaded;

        }

        // else (null server/filename)
        if (myServer == null) {
            MWLogger.infoLog("MyServer == NULL!");
        }
        if (filename == null) {
            MWLogger.infoLog("Filename == NULL!");
        } else if (filename.equals("")) {
            MWLogger.infoLog("Filename == \"\"!");
        }

        return false;
    }

    public boolean isServerRunning() {
        return myServer != null;
    }

    public void closingGame(String hostName) {

        // update battles tab for all players, via server
        MWLogger.infoLog("Leaving " + hostName);
        serverSend("LG|" + hostName);

        System.gc();
    }

    public Vector<IOption> getGameOptions() {
        return GameOptions;
    }

    public Dimension getMapSize() {
        return MapSize;
    }

    public Dimension getBoardSize() {
        return BoardSize;
    }

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
                    MWLogger.errLog(ex);
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

    public void loadServerMegaMekGameOptions() {
        try {
            dataFetcher.getServerMegaMekGameOptions();
        } catch (Exception ex) {
            MWLogger.errLog("Error loading Server MegaMekGameOptions files");
            MWLogger.errLog(ex);
        }
    }

    /**
     * Return the directory, where all cache files can go into. The dirname
     * depends on the server you connect.
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
     * Changes the duty to a new status.
     * 
     * @param newStatus
     */
    public void changeStatus(int newStatus) {
        Status = newStatus;
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
            MWLogger.infoLog("System has reached " + gameCount + " games played and is restarting");
            try {
                Thread.sleep(5000);
            }// give people time to vacate
            catch (Exception ex) {
                MWLogger.errLog(ex);
            }
            try {
                stopHost();
                Thread.sleep(5000);
            }// give people time to vacate
            catch (Exception ex) {
                MWLogger.errLog(ex);
            }
            restartDed();
        }

        lastResetCheck = System.currentTimeMillis();
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
                    MWLogger.infoLog("Purging File: " + savedFile.getName() + " Time: " + lastTime + " purge Time: " + (System.currentTimeMillis() - daysInSeconds));
                    savedFile.delete();
                } catch (Exception ex) {
                    MWLogger.errLog("Error trying to delete these files!");
                    MWLogger.errLog(ex);
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
            MWLogger.errLog(ex);
        }

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
            MWLogger.errLog(ex);
        }

    }

    public boolean isDedicated() {
        return true;
    }

    public void updateParam(StringTokenizer ST) {
        try {
            getConfig().setParam(ST.nextToken(), ST.nextToken());
            getConfig().saveConfig();
            setConfig();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public Server getMyServer() {
        return myServer;
    }

    public boolean isUsingAdvanceRepairs() {
        return Boolean.parseBoolean(getserverConfigs("UseAdvanceRepair")) || Boolean.parseBoolean(getserverConfigs("UseSimpleRepair"));
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
                        if (savedFile.exists() && savedFile.isFile() && (lastTime < (System.currentTimeMillis() - twoHours))) {
                            try {
                                MWLogger.infoLog("Purging File: " + savedFile.getName() + " Time: " + lastTime + " purge Time: " + (System.currentTimeMillis() - twoHours));
                                savedFile.delete();
                            } catch (Exception ex) {
                                MWLogger.errLog("Error trying to delete these files!");
                                MWLogger.errLog(ex);
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

    public void errorMessage(String message) {
        // TODO Auto-generated method stub

    }

    public void systemMessage(String message) {
        // TODO Auto-generated method stub

    }

    public void getServerConfigData() {
        try {
            dataFetcher.getServerConfigData(this);
        } catch (Exception ex) {
        }
    }

    public String getserverConfigs(String key) {
        if (serverConfigs.getProperty(key) == null) {
            return "-1";
        }
        return serverConfigs.getProperty(key).trim();
    }

    public Properties getServerConfigs() {
        return serverConfigs;
    }

    public void setBuildingTemplate(Buildings buildingTemplate) {
        this.buildingTemplate = buildingTemplate;
    }

    public Buildings getBuildingTemplate() {
        return buildingTemplate;
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
            MWLogger.errLog("Unable to find MekWarsDed.jar");
        }
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
            MWLogger.errLog(ex);
        }
        System.exit(0);// restart the ded
    }

    protected void sendServerGameUpdate() {
        // Report the mech stat

        // Only send data for units currently on the board.
        // any units removed from play will have already sent thier final
        // update.
        Iterator<Entity> en = myServer.getGame().getEntities();
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

	public static StringBuilder prepareReport(GameInterface myGame, boolean usingAdvancedRepairs, Buildings buildingTemplate) {
		StringBuilder result = new StringBuilder();
        String name = "";
        // Parse the real playername from the Modified In game one..
        String winnerName = "";
        if (myGame.hasWinner()) {

            int numberOfWinners = 0;
            // Multiple Winners
            List<String> winners = myGame.getWinners();
            
            //TODO: Winners is sometimes coming up empty.  Let's see why
            MWLogger.errLog("Finding winners:");
            MWLogger.errLog(winners.toString());
            
            for (String winner: winners){
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
        }

        else {
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
        MWLogger.infoLog("CR|" + result);
		return result;
	}

    private static void createLoggers() {
        logger = MWLogger.getInstance();
        PKLogManager logger = PKLogManager.getInstance();
        logger.addLog("infolog");
        logger.addLog("errlog");
        logger.addLog("debuglog");
    }
    
    protected void sendGameReport() {
        if (myServer == null) {
            return;
        }
        
        //GameReporter.prepareReport(myGame, usingAdvancedRepairs, buildingTemplate)

        StringBuilder result = prepareReport(new GameWrapper(myServer.getGame()), isUsingAdvanceRepairs(), buildingTemplate);
        serverSend("CR|" + result.toString());
        
/*        StringBuilder result = new StringBuilder();
        String name = "";
        // Parse the real playername from the Modified In game one..
        String winnerName = "";
        if (myServer.getGame().getVictoryTeam() != Player.TEAM_NONE) {

            int numberOfWinners = 0;
            // Multiple Winners
            Enumeration<Player> en = myServer.getGame().getPlayers();
            while (en.hasMoreElements()) {
                Player p = en.nextElement();
                if (p.getTeam() == myServer.getGame().getVictoryTeam()) {
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
            if (myServer.getGame().getVictoryPlayerId() == Player.PLAYER_NONE) {
                winnerName = "DRAW#";
            } else {
                winnerName = myServer.getGame().getPlayer(myServer.getGame().getVictoryPlayerId()).getName();
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
        Enumeration<Entity> en = myServer.getGame().getDevastatedEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false, isUsingAdvanceRepairs()));
            result.append("#");
        }
        en = myServer.getGame().getGraveyardEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false, isUsingAdvanceRepairs()));
            result.append("#");

        }
        en = myServer.getGame().getEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false, isUsingAdvanceRepairs()));
            result.append("#");
        }
        en = myServer.getGame().getRetreatedEntities();
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

	@Override
	public void gameClientFeedbackRequest(GameCFREvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
