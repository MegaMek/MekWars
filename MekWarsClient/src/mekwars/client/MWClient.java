/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet) Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

package mekwars.client;

// This is the Client used for connecting to the master server.
// @Author: Helge Richter (McWizard@gmx.de)

import java.util.Collections;
import java.util.TreeMap;
import java.util.Vector;

import megamek.client.ui.dialogs.buttonDialogs.GameOptionsDialog;
import megamek.common.CriticalSlot;
import megamek.common.equipment.EquipmentType;
import megamek.common.event.GameCFREvent;
import megamek.common.event.GameEvent;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.server.Server;
import mekwars.client.commands.Command;
import mekwars.client.gui.Browser;
import mekwars.client.gui.commands.IGUICommand;
import mekwars.client.gui.commands.MailGCmd;
import mekwars.client.gui.commands.PingGCmd;
import mekwars.client.protocol.DataFetchClient;
import mekwars.common.*;
import mekwars.common.campaign.Buildings;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.GameHost;
import mekwars.common.campaign.clientutils.SerializeEntity;
import mekwars.common.campaign.clientutils.protocol.CConnector;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.clientutils.protocol.commands.CommPCmd;
import mekwars.common.campaign.clientutils.protocol.commands.IProtCommand;
import mekwars.common.campaign.clientutils.protocol.commands.PingPCmd;
import mekwars.common.campaign.clientutils.protocol.commands.PongPCmd;
import mekwars.common.gui.CCommPanel;
import mekwars.common.gui.CMainFrame;
import mekwars.common.gui.GUIClientConfig;
import mekwars.common.gui.SplashWindow;
import mekwars.common.gui.dialogs.InfluencePointsDialog;
import mekwars.common.gui.dialogs.RewardPointsDialog;
import mekwars.common.util.MWLogger;
import mekwars.common.util.RepairManagmentThread;
import mekwars.common.util.SalvageManagmentThread;
import mekwars.common.util.ThreadManager;
import mekwars.common.util.TokenReader;
import mekwars.common.util.UnitUtils;


public final class MWClient extends GameHost implements IClient {
    public static final String GUI_PREFIX = "/"; // prefix for commands in GUI
    public static final int REFRESH_STATUS = 0;
    public static final int REFRESH_USERLIST = 1;
    public static final int REFRESH_PLAYER_PANEL = 2;
    public static final int REFRESH_BATTLE_TABLE = 4;

    // all client
    // changes @Torren
    public static final int REFRESH_HQ_PANEL = 5;
    public static final int REFRESH_BM_PANEL = 6;
    public static final int IGNORE_PUBLIC = 0;
    public static final int IGNORE_HOUSE = 1;
    public static final int IGNORE_PRIVATE = 2;
    /**
     *
     */
    private static final long serialVersionUID = 6056977040880995791L;
    public static Object mwClientLog;
    private final java.util.HashMap<String, Equipment> blackMarketEquipmentList = new java.util.HashMap<>();
    // Holds campaign data as factions and planets..
    CampaignData data = null;
    DataFetchClient dataFetcher;
    Thread updateDataFetcher;
    MWClient.TimeOutThread TO;
    java.util.Collection<CUser> Users;
    Server myServer = null;
    java.util.List<ClientThread> mmClientThreads = new java.util.ArrayList<>();
    java.util.Vector<IBasicOption> GameOptions = new java.util.Vector<IBasicOption>(1, 1);
    // restart.
    Browser browser;
    boolean SignOff = false;
    boolean packFrame = false;
    boolean SoundMuted = false;
    String password = "";
    String myDedOwners = "";
    int myPort = -1;
    int gameCount = 0; // number of games played on a ded
    long lastResetCheck = System.currentTimeMillis();
    int dedRestartAt = 50; // number of games played on a ded before auto
    long TimeOut = 120;
    long LastPing = 0;
    PlanetEnvironment currentEnvironment;
    AdvancedTerrain aTerrain = null;
    java.util.TreeMap<String, String[]> allOps;// all operations, from OpList.txt
    java.awt.Dimension MapSize;
    int mapMedium = 0;
    SplashWindow splash = null;
    CCampaign theCampaign;
    CPlayer myPlayer;
    CMainFrame MainFrame;
    int Status = STATUS_DISCONNECTED;
    int LastStatus = STATUS_DISCONNECTED;
    java.util.TreeMap<String, IGUICommand> GUICommands = new java.util.TreeMap<String, IGUICommand>();
    /**
     * Maps the task prefixes as FactionStatusScreenUpdateCommand, PL, SP etc. to a command under package cmd. key:
     * String, value: cmd.Command
     */
    java.util.HashMap<String, Command> commands = new java.util.HashMap<>();
    String LastQuery = ""; // receiver of last mail
    java.util.Vector<String> IgnorePublic = new java.util.Vector<>(1, 1); // people whose
    // public messages
    // are ignored
    java.util.Vector<String> IgnoreHouse = new java.util.Vector<>(1, 1); // people whose
    // faction messages
    // are ignored
    java.util.Vector<String> IgnorePrivate = new java.util.Vector<>(1, 1); // people whose
    // private
    // messages are
    // ignored
    java.util.Vector<String> KeyWords = new java.util.Vector<>(1, 1); // words announced with
    private Game game = new Game();
    // sound
    private String cacheDir;
    // Starting edge for players in building ops
    private int playerStartingEdge = Buildings.EDGE_UNKNOWN;
    // Bot commands
    private boolean usingBots = false;
    private boolean botsOnSameTeam = false;
    // Advanced Repair Queue
    private RepairManagmentThread RMT = null;
    private SalvageManagmentThread SMT = null;
    private boolean waitingOnCommand = false;

    public MWClient(GUIClientConfig config) {


        ProtCommands = new TreeMap<>();
        Config = config;

        // set up the splash screen. do this before any
        // other non-main/non-static actions.

        if (isDedicated()) {
            try {
                Runtime runTime = Runtime.getRuntime();
                String[] call = { "java", "-Xmx512m", "-jar", "MekWarsDed.jar" };
                runTime.exec(call);
                System.exit(0);
            } catch (Exception ex) {
                MWLogger.errLog("Unable to find MekWarsDed.jar");
            }
        } else {
            setLookAndFeel(false);
            if (Config.isParam("ENABLESPLASHSCREEN")) {
                splash = new SplashWindow();
            }
        }

        try {
            java.lang.management.RuntimeMXBean rt = java.lang.management.ManagementFactory.getRuntimeMXBean();
            MWLogger.errLog("RT Info: " + rt.getName());
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
        Connector = new CConnector(this);
        Connector.setSplashWindow(splash);// may set null if ded.

        Users = Collections.synchronizedList(new Vector<CUser>(1, 1));

        // Non-ded's get a GUI, show signon dialog, etc.
        if (!isDedicated()) {

            theCampaign = new CCampaign(this);
            myPlayer = theCampaign.getPlayer();
            createProtCommands();
            createGUICommands();

            // indicate that the splash is trying to get data
            if (splash != null) {
                splash.setStatus(splash.STATUS_FETCHING_DATA);
            }

            /*
             * @urgru 11.24.05 SignOnDialog used to be shown in later in
             * construction. This made it impossible for players to change the
             * target IP and/or DATAPORT before attempting to fetch needed data.
             * Although a properly configured serverdata.dat would keep this
             * from bothering end users, it was pissing off server admins who
             * were testing clients against multiple servers. ---- 12.4.05
             * Addition: Show SignOnDialog if username is blank, or if a
             * player's password is unsaved. Tool tip for autoconnect cbox
             * updated to reflect this requirement. Fix for BUG 1275136.
             */
            boolean shouldShowSignOn = false;
            if (!Boolean.parseBoolean(getConfigParam("AUTOCONNECT"))) {
                shouldShowSignOn = true;
            } else if (getConfigParam("SERVERIP").trim().equals("")) {
                shouldShowSignOn = true;
            } else if (getConfigParam("NAME").trim().equals("")) {
                shouldShowSignOn = true;
            } else if (getConfigParam("NAMEPASSWORD").trim().equals("")) {
                shouldShowSignOn = true;
            }

            if (shouldShowSignOn) {
                if (splash != null) {
                    splash.setStatus(splash.STATUS_INPUTWAIT);
                }
                new SignonDialog(this);
                if (splash != null) {
                    splash.setStatus(splash.STATUS_FETCHINGDATA);
                }
            }

            // Start the data fetcher, get ops/map/etc
            dataFetcher = new DataFetchClient(Integer.parseInt(Config.getParam("DATAPORT")),
                  Integer.parseInt(Config.getParam(
                        "SOCKETTIMEOUTDELAY")));

            try {
                java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(
                      new java.io.FileInputStream("data/servers/"
                                                        + Config.getParam("SERVERIP") + "."
                                                        + Config.getParam("SERVERPORT")
                                                        + "/dataLastUpdated.dat")));
                java.util.Date lastTS = new java.util.Date(Long.parseLong(dis.readLine()));
                dataFetcher.setLastTimestamp(lastTS);
                dis.close();
            } catch (Throwable t) {
                MWLogger.infoLog(
                      "Couldn't read timestamp of last datafetch. Will need to fetch all planetchanges since last full update.");
            }
            dataFetcher.setData(Config.getParam("SERVERIP"), getCacheDir());

            /*
             * Now that the data fetcher has been created, get the OpList.txt.
             * Note that this is BEFORE map data and other fetch/checks, because
             * the Ops absolutely must be available in order to contruct the
             * GUI.
             */
            try {
                dataFetcher.checkForMostRecentOpList();
            } catch (java.io.IOException e) {

                Object[] options = { "Exit", "Continue" };
                int selectedValue = javax.swing.JOptionPane
                                          .showOptionDialog(
                                                null,
                                                "No OpList. This usually means that you were unable to "
                                                      + "connect to the server to fetch a copy. Do you wish to exit?",
                                                "Startup " + "error!",
                                                javax.swing.JOptionPane.DEFAULT_OPTION,
                                                javax.swing.JOptionPane.ERROR_MESSAGE, null, options,
                                                options[0]);
                if (selectedValue == 0) {
                    System.exit(0);// exit, if they so choose
                }
            }

            setupAllOps();

        }

        // Dedicated servers have no GUI, no signon dialogs, etc.
        else {
            createProtCommands();
            dataFetcher = new DataFetchClient(Integer.parseInt(Config
                                                                     .getParam("DATAPORT")), Integer.parseInt(Config
                                                                                                                    .getParam(
                                                                                                                          "SOCKETTIMEOUTDELAY")));
            dataFetcher.setData(Config.getParam("SERVERIP"), getCacheDir());
            try {
                dataFetcher.getServerConfigData(this);
            } catch (Exception ex) {
                MWLogger.errLog("Error While getting server config file.");
                MWLogger.errLog(ex);
            }

            dataFetcher.closeDataConnection();

            // Remove any MM option files that deds may have.
            java.io.File localGameOptions = new java.io.File("./mmconf");
            try {
                if (localGameOptions.exists()) {
                    localGameOptions = new java.io.File("./mmconf/gameoptions.xml");
                    if (localGameOptions.exists()) {
                        localGameOptions.delete();
                    }
                }
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }

        }

        System.err.println("Get Data Time: " + System.currentTimeMillis());
        System.err.flush();

        getData();
        System.err.println("Done Getting Data Time: "
                                 + System.currentTimeMillis());
        System.err.flush();

        // set New timestamp
        dataFetcher.setLastTimestamp(new java.util.Date(System.currentTimeMillis()));
        dataFetcher.store();

        myUsername = getConfigParam("NAME");

        // if this is dedicated host, we mark its name with "[Dedicated]" stamp
        if (isDedicated() && !myUsername.startsWith("[Dedicated]")) {
            Config.setParam("NAME", "[Dedicated] " + Config.getParam("NAME"));
            myUsername = Config.getParam("NAME");
        }

        dedRestartAt = Integer.parseInt(getConfigParam("DEDAUTORESTART"));
        savedGamesMaxDays = Integer
                                  .parseInt(getConfigParam("MAXSAVEDGAMEDAYS"));
        myDedOwners = getConfigParam("DEDICATEDOWNERNAME");
        myPort = Integer.parseInt(getConfigParam("PORT"));
        IgnorePublic = splitString(Config.getParam("IGNOREPUBLIC"), ",");
        IgnoreHouse = splitString(Config.getParam("IGNOREHOUSE"), ",");
        IgnorePrivate = splitString(Config.getParam("IGNOREPRIVATE"), ",");
        KeyWords = splitString(Config.getParam("KEYWORDS"), ",");

        /*
         * Start the pruge thread when the client starts, not when the host
         * starts. This prevents the creation of multiple threads when the host
         * is restarted, or after disconnections.
         */
        System.err.println("staring PAS Time: " + System.currentTimeMillis());
        System.err.flush();

        MWLogger.infoLog("Starting pAS");
        mekwars.client.MWClient.PurgeAutoSaves pAS = new mekwars.client.MWClient.PurgeAutoSaves();
        new Thread(pAS).start();

        System.err.println("PAS Started Time: " + System.currentTimeMillis());
        System.err.flush();

        /*
         * Load IP and Port to connect to from the config. In older code the
         * signon dialog was shown at this point. The dialog has been moved, and
         * is now displayed -before- the client attempts to fetch vital data,
         * like the map.
         */
        if (splash != null) {
            splash.setStatus(splash.STATUS_CONNECTING);
        }
        String chatServerIP = "";
        int chatServerPort = -1;
        try {
            chatServerIP = Config.getParam("SERVERIP");
            chatServerPort = Config.getIntParam("SERVERPORT");
        } catch (Exception e) {
            MWLogger.errLog(e);
            System.exit(1);
        }

        /*
         * Non-dedicated. Draw the UI, shut down the splash screen, and then
         * request any stored messages.
         */
        if (!isDedicated()) {

            // make the main frame
            System.err.println("Creating CMainFrame Time: "
                                     + System.currentTimeMillis());
            System.err.flush();
            MainFrame = new CMainFrame(this);
            System.err.println("CMainFrame Created Time: "
                                     + System.currentTimeMillis());
            System.err.flush();

            try {
                MainFrame.setIconImage(((GUIClientConfig) Config).getImage("LOGOUT").getImage());
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }

            System.err.println("Packing/Validating CMainFrame Time: "
                                     + System.currentTimeMillis());
            System.err.flush();

            if (packFrame) {
                MainFrame.pack();
            } else {
                MainFrame.validate();
            }
            System.err.println("done packing/validating CMainFrame Time: "
                                     + System.currentTimeMillis());
            System.err.flush();

            java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            java.awt.Dimension frameSize = MainFrame.getSize();

            MainFrame.setExtendedState(Integer
                                             .parseInt(getConfigParam("WINDOWSTATE")));
            MainFrame.setSize(Integer.parseInt(getConfigParam("WINDOWWIDTH")),
                  Integer.parseInt(getConfigParam("WINDOWHEIGHT")));
            MainFrame.setLocation(
                  Integer.parseInt(getConfigParam("WINDOWLEFT")),
                  Integer.parseInt(getConfigParam("WINDOWTOP")));
            // check for unacceptable dimensions
            if (frameSize.height > screenSize.height) {
                frameSize.height = screenSize.height;
            }
            if (frameSize.width > screenSize.width) {
                frameSize.width = screenSize.width;
                // MainFrame.setLocation((screenSize.width - frameSize.width) /
                // 2,
                // (screenSize.height - frameSize.height) / 2);
            }

            // set the initial mute value
            setSoundMuted(getConfig().isParam("DISABLEALLSOUND"));

            System.err.println("Attack Menu Update Time: "
                                     + System.currentTimeMillis());
            System.err.flush();

            // build the attack menu. at this point we know we have the
            // necessary data.
            MainFrame.updateAttackMenu();
            System.err.println("Attack Menu Update done Time: "
                                     + System.currentTimeMillis());
            System.err.flush();

            System.err.println("MainFrame Visible Time: "
                                     + System.currentTimeMillis());
            System.err.flush();

            System.err.println("Creating Browser Time: "
                                     + System.currentTimeMillis());
            System.err.flush();

            browser = new Browser();
            Browser.init();
            System.err.println("Browser Created Time: "
                                     + System.currentTimeMillis());
            System.err.flush();

            System.err.println("Connecting to Server Time: "
                                     + System.currentTimeMillis());
            System.err.flush();
            connectToServer(chatServerIP, chatServerPort);
            System.err.println("Connected to Server Time: "
                                     + System.currentTimeMillis());
            System.err.flush();
            // make the main frame visible
            try {
                System.err.println("making mainframe visible: "
                                         + System.currentTimeMillis());
                System.err.flush();
                // init the gui
                if (splash != null) {
                    splash.setStatus(splash.STATUS_CONSTRUCTINGGUI);
                }

                MainFrame.setVisible(true);

                if (splash != null) {
                    System.err.println("splash not null: "
                                             + System.currentTimeMillis());
                    System.err.flush();
                    splash.getProgressBar().setValue(9);
                    System.err.println("progress bar set to 9: "
                                             + System.currentTimeMillis());
                    System.err.flush();
                    splash.getProgressBar().setVisible(false);
                    System.err.println("progressbar going bye bye: "
                                             + System.currentTimeMillis());
                    System.err.flush();
                    splash.dispose();
                    System.err.println("splash going bye bye: "
                                             + System.currentTimeMillis());
                    System.err.flush();
                }
                System.err.println("splash going to null: "
                                         + System.currentTimeMillis());
                System.err.flush();

                splash = null;// nuke the splash
                System.err
                      .println("splash null: " + System.currentTimeMillis());
                System.err.flush();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
                MWLogger.errLog("Error closing splash / opening main frame.");
            }
            System.err.println("MainFrame Visible Done Time: "
                                     + System.currentTimeMillis());
            System.err.flush();

            // refresh the GUI views one last time
            refreshGUI(REFRESH_STATUS);
            refreshGUI(REFRESH_PLAYERPANEL);
            refreshGUI(REFRESH_BMPANEL);
            refreshGUI(REFRESH_HQPANEL);

            /*
             * Send client version and saved mail request to the server. Doing
             * this after the main frame is build and visible will (I hope) fix
             * the "PrivateMessageCommand Ping Crash" TT users have with Client 0.1.44.5.
             */
            sendChat(mekwars.client.MWClient.CAMPAIGN_PREFIX + "c setclientversion#"
                           + myUsername.trim() + "#" + CLIENT_VERSION);
            sendChat("/getsavedmail");

            // Lets start the repair thread
            if (Boolean.parseBoolean(getserverConfigs("UseAdvanceRepair"))) {
                RMT = new RepairManagmentThread(
                      Long.parseLong(getserverConfigs("TimeForEachRepairPoint")) * 1000,
                      this);
                RMT.start();
            }
            if (Boolean.parseBoolean(getserverConfigs("UsePartsRepair"))) {
                SMT = new SalvageManagmentThread(
                      Long.parseLong(getserverConfigs("TimeForEachRepairPoint")) * 1000,
                      this);
                SMT.start();
            }
        }
        // repeated connection attempts for dedicated hosts.
        else {
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
        }// end else(is Dedicated host)

        // start checking for timeouts
        TimeOut = Long.parseLong(Config.getParam("TIMEOUT"));
        LastPing = System.currentTimeMillis() / 1000;
        TO = new mekwars.client.MWClient.TimeOutThread(this);
        TO.run();
    }

    protected void createProtCommands() {
        addProtCommand(new CommPCmd(this));
        addProtCommand(new PingPCmd(this));
        addProtCommand(new PongPCmd(this));
        addProtCommand(new AckSignonPCmd(this));
    }

    protected void createGUICommands() {
        addGUICommand(new PingGCmd(this));
        addGUICommand(new MailGCmd(this));
    }

    /**
     * Method which parses OpList.txt in order to set up a tree which conatins names (as keys) and information (as
     * values) for all game types. Various portions of the GUI code use this tree to properly draw themselves. Kept in
     * MWClient in order to be universally available; however, this is poor design ... *sigh*
     */
    public void setupAllOps() {

        allOps = new java.util.TreeMap<String, String[]>();
        try {

            java.io.File f = new java.io.File(cacheDir + "/OpList.txt");
            if (!f.exists()) {
                MWLogger.errLog("Error: OpList.txt does not exist.");
                return;
            }

            java.io.FileInputStream in = new java.io.FileInputStream(cacheDir + "/OpList.txt");
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));

            // skip past the first line - its just a timestamp.
            String currLine = br.readLine();
            if (currLine != null) {
                currLine = br.readLine();
            }

            while (currLine != null) {

                // if there's a hanging line, move to next.
                if (currLine.trim().length() == 0) {
                    currLine = br.readLine();
                    continue;
                }

                // set up tokenizer and make tree entry
                java.util.StringTokenizer st = new java.util.StringTokenizer(currLine, "*");

                String name = st.nextToken();// key
                String range = st.nextToken();
                String color = st.nextToken();
                String hasLong = st.nextToken();// int, not a boolean
                String facInfo = st.nextToken();// "all", "only", "none"
                String homeInfo = st.nextToken();// "all", "only", "none"
                String launchOn = st.nextToken();// int, percentage
                String launchFrom = st.nextToken();// int, percentage
                String minOwn = st.nextToken();// int, percentage
                String maxOwn = st.nextToken();// int, percentage
                String reserveOnly = st.nextToken();// boolean
                String activeOnly = st.nextToken();// boolean

                String legalDefenders = st.nextToken();
                String allowPlanetFlags = st.nextToken();
                String disallowPlanetFlags = st.nextToken();
                String minAccessLevel = st.nextToken();// int
                String minOwnIBD = st.nextToken(); // boolean

                // TODO: Replace explicit numerical references with static ints.
                String[] props = {// value bag
                                  range,// 0
                                  color,// 1
                                  hasLong,// 2
                                  facInfo,// 3
                                  homeInfo,// 4
                                  launchOn,// 5
                                  launchFrom,// 6
                                  minOwn,// 7
                                  maxOwn,// 8
                                  legalDefenders,// 9
                                  allowPlanetFlags,// 10
                                  disallowPlanetFlags,// 11
                                  reserveOnly,// 12
                                  activeOnly,// 13
                                  minAccessLevel, // 14
                                  minOwnIBD // 15
                };
                allOps.put(name, props);

                // load next line
                currLine = br.readLine();

            }// end while (lines remain to tokenize)

            br.close();
            in.close();

        } catch (Exception e) {
            MWLogger.errLog("Error in setupAllOps()");
            MWLogger.errLog(e);
        }
    }// end setupAllOps

    protected java.util.Vector<String> splitString(String string, String splitter) {
        java.util.Vector<String> vector = new java.util.Vector<String>(1, 1);
        String[] splitted = string.split(splitter);
        for (String element : splitted) {
            vector.add(element.trim());
        }

        /*
         * Remove empty entries from the set. Strip ",," and "" from the vector.
         * Helps with ignore and keyword lists.
         */
        java.util.Iterator<String> i = vector.iterator();
        while (i.hasNext()) {
            String currString = i.next();
            if (currString.trim().length() == 0) {
                i.remove();
            }
        }

        return vector;
    }

    public void setSoundMuted(boolean b) {
        SoundMuted = b;
        MainFrame.setSoundMuted(b);

        // see if the setting should be saved
        if (b != getConfig().isParam("DISABLEALLSOUND")) {
            if (b == false) {
                getConfig().setParam("DISABLEALLSOUND", "false");
            } else {
                getConfig().setParam("DISABLEALLSOUND", "true");
            }

            getConfig().saveConfig();
        }

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

    public String getserverConfigs(String key) {
        if (CampaignData.cd.getServerConfigs().getProperty(key) == null) {
            MWLogger.infoLog("You're missing the config variable: "
                                   + key + " in serverconfig!");
            return "-1";
        }
        return CampaignData.cd.getServerConfigs().getProperty(key).trim();
    }

    protected void addProtCommand(IProtCommand command) {
        ProtCommands.put(command.getName(), command);
    }

    protected void addGUICommand(IGUICommand command) {
        GUICommands.put(command.getName(), command);
        if (command.isAlias()) {
            GUICommands.put(command.getAlias(), command);
        }
    }

    //private static MWLogger logger = MWLogger.getInstance();
    // Main-Method
    public static void main(String[] args) {

        GUIClientConfig config;
        boolean dedicated = false;
        int i;


        createLoggers();
        /*
         * put StdErr and StdOut into ./logs/megameklog.txt, because MegaMek
         * uses StdOut and StdErr, but the part of MegaMek that sets that up
         * does not get called when we launch MegaMek in MekWars Redirect output
         * to logfiles, unless turned off. Moved megameklog.txt to the logs
         * folder -- Torren
         */
        String logFileName = "./logs/megameklog.txt";
        boolean enableSplashScreen = true;
        try {
            java.io.PrintStream ps = new java.io.PrintStream(new java.io.BufferedOutputStream(
                  new java.io.FileOutputStream(logFileName), 64));
            System.setOut(ps);
            System.setErr(ps);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Unable to redirect MegaMek output to "
                                  + logFileName);
        }

        MWLogger.infoLog("Starting MekWars Client Version: "
                               + CLIENT_VERSION);
        try {
            for (i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-dedicated")
                          || args[i].equalsIgnoreCase("-d")) {
                    dedicated = true;
                }
                // add more args?
                else if (args[i].equalsIgnoreCase("-disableSplash")) {
                    enableSplashScreen = false;
                } else if (args[i].equalsIgnoreCase("-enableSplash")) {
                    enableSplashScreen = true;
                }
            }
            config = new GUIClientConfig(dedicated);

            if (!enableSplashScreen) {
                config.setParam("ENABLESPLASHSCREEN", "false");
            } else {
                config.setParam("ENABLESPLASHSCREEN", "true");
            }
            /*
             * clear any cache'd unit files. these will be rebuilt later in the
             * start process. clearing @ each start ensures that updates take
             * hold properly.
             */
            java.io.File cache = new java.io.File("./data/mechfiles/units.cache");
            if (cache.exists()) {
                cache.delete();
            }

            /*
             * Config files have been loaded, and command line args have been
             * parsed. Construct the actual client. NOTE: Client constrtuctor
             * attempts to pull the oplist, campaign config and other
             * non-interactive data over the DATAPORT before client.start()
             * attempts to connect to the chat server on the SERVERPORT.
             */
            new mekwars.client.MWClient(config);

        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Couldn't create Client Object");
            System.exit(1);
        }
    }

    private static void createLoggers() {
        PKLogManager logger = PKLogManager.getInstance();
        logger.addLog("infolog");
        logger.addLog("errlog");
        logger.addLog("debuglog");
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String createFilenameChecksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public byte[] createChecksum(String filename) throws Exception {
        java.io.InputStream fis = new java.io.FileInputStream(filename);

        byte[] buffer = new byte[1024];
        java.security.MessageDigest complete = java.security.MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        return complete.digest();
    }

    /*
     * Actual GUI-mode parseData. Before we started streaming data over the chat
     * part, this was called directly. Now we buffer all incoming non-data chat
     * and spit it out at once when the GUI draws. Once the GUI is up, this is
     * called by a simple pass through from doParseDataInput(), above. Ded's
     * call the helper directly to bypass the buffer.
     */
    private void doParseDataHelper(String input) {
        try {

            // 0-length input is spurious call from MWClient constructor.
            if (input.length() == 0) {
                return;
            }

            java.util.StringTokenizer ST = null;
            String task = null;

            // debug info
            MWLogger.infoLog(input);

            // Create a String Tokenizer to parse the elements of the input
            ST = new java.util.StringTokenizer(input, COMMAND_DELIMITER);
            task = ST.nextToken();

            if (!commands.containsKey(task)) {
                try {
                    Class<?> cmdClass = Class.forName(getClass().getPackage()
                                                            .getName() + ".cmd." + task);
                    java.lang.reflect.Constructor<?> c = cmdClass
                                                               .getConstructor(new Class[] {
                                                                     mekwars.client.MWClient.class });
                    Command cmd = (Command) c
                                                  .newInstance(new Object[] { this });
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

    public void processGUIInput(String input) {
        String s = null;

        if (input.startsWith(GUI_PREFIX)) {
            input = input.substring(GUI_PREFIX.length());
            java.util.StringTokenizer ST = new java.util.StringTokenizer(input, " #");
            s = ST.nextToken();
            if (s.equalsIgnoreCase("c")) {
                s = "c " + ST.nextToken().toLowerCase();
            }
            IGUICommand command = getGUICommand(s);
            if ((command != null) && command.check(s)) {
                if (!command.execute(input)) {
                    MWLogger.infoLog("COMMAND ERROR: wrong command executed.");
                }
                return;
            }
            // else
            input = CAMPAIGN_PREFIX + input;

            sendChat(input);
            s = "Sent command: " + '"'
                      + input.substring(CAMPAIGN_PREFIX.length()) + '"';
            addToChat(s, CCommPanel.CHANNEL_PLOG, null);
        } else {
            sendChat(input);
            String color = getUser(myUsername).getColor();
            String addon = getUser(myUsername).getAddon();
            addon = addon.equals("") ? "" : " [" + addon + "]";
            s = "<font color=\"" + color + "\"><b>" + myUsername + addon
                      + "</b></font><b>:</b> " + input;
            if (Config.isParam("TIMESTAMP")) {
                s = "<font color=\"" + Config.isParam("CHATFONTCOLOR") + "\">"
                          + getShortTime() + "</font>" + s;

            }
            addToChat(s, CCommPanel.CHANNEL_PLOG, null);
            chatCaptureForBot(myUsername, addon, input); //@salient
        }
    }// end processGUIInput

    IGUICommand getGUICommand(String command) {
        return GUICommands.get(command);
    }

    public void addToChat(String s, int channel, String tabName) {

        s = "<BODY  TEXT=\"" + Config.getParam("CHATFONTCOLOR")
                  + "\" BGCOLOR=\"" + Config.getParam("BACKGROUNDCOLOR")
                  + "\"><font size=\"" + Config.getParam("CHATFONTSIZE") + "\">"
                  + s + "</font></BODY>";
        // MWLogger.infoLog("String: "+s);
        try {
            javax.swing.SwingUtilities.invokeLater(new mekwars.client.MWClient.CAddToChat(s, channel, tabName));
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

    }

    private void chatCaptureForBot(String username, String addon, String input) //@salient
    {
        if (!Boolean.parseBoolean(getserverConfigs("Enable_Bot_Chat"))) {return;}

        String temp = getShortTime().trim() + username.trim() + addon.trim() + ":" + input;
        temp = String.format("%s%n", temp);

        //		if(channel !=0)
        //			return;

        //call a new command to capture chat server side
        sendChat(mekwars.client.MWClient.CAMPAIGN_PREFIX + "CHATBOT " + temp);
    }

    public String getLastQuery() {
        return LastQuery;
    }

    public void setLastQuery(String name) {
        LastQuery = name;
    }

    public java.util.Vector<String> getIgnorePublic() {
        return IgnorePublic;
    }

    public java.util.Vector<String> getIgnoreHouse() {
        return IgnoreHouse;
    }

    public java.util.Vector<String> getIgnorePrivate() {
        return IgnorePrivate;
    }

    public boolean hasKeyWords(String input) {
        for (String currS : KeyWords) {
            if (input.toLowerCase().indexOf(currS.toLowerCase()) > -1) {
                return true;
            }
        }
        return (false);
    }

    public String getStatus() {
        if (Status == STATUS_DISCONNECTED) {
            return ("Not connected");
        }
        if (Status == STATUS_LOGGEDOUT) {
            return ("Logged out");
        }
        if (Status == STATUS_RESERVE) {
            return ("Reserve duty");
        }
        if (Status == STATUS_ACTIVE) {
            return ("Active duty");
        }
        if (Status == STATUS_FIGHTING) {
            return ("Fighting");
        }
        return ("");
    }

    public synchronized java.util.ArrayList<String> getPartialUser(String u) {

        String result = "";
        java.util.TreeSet<String> userNames = new java.util.TreeSet<String>();

        // there are spaces in the text so get the last word
        if (u.trim().indexOf(" ") != -1) {
            result = u.substring(u.trim().lastIndexOf(" ")).trim();
            u = u.substring(0, u.trim().lastIndexOf(" ")).trim();
        } else {// The name is the first word.
            result = u.trim();
            u = "";
        }

        if (result.length() < 1) {
            return null;
        }

        int myLevel = getUser(getPlayer().getName()).getUserlevel();
        for (CUser usr : Users) {
            if (usr.getName().toLowerCase().startsWith(result.toLowerCase())
                      && (!usr.isInvis() || (usr.isInvis() && (myLevel >= usr
                                                                                .getUserlevel())))) {
                userNames.add(usr.getName());
            }
        }

        // We have a sorted tree set. Convert to an ArrayList so we can work
        // with them more easily.
        java.util.ArrayList<String> test = new java.util.ArrayList<String>();
        test.addAll(userNames);
        return test;
    }

    public boolean isMuted() {
        return SoundMuted;
    }

    public boolean isUsingBots() {
        return usingBots;
    }

    public void setUsingBots(Boolean using) {
        usingBots = using;
    }

    public boolean isBotsOnSameTeam() {
        return botsOnSameTeam;
    }

    public void setBotsOnSameTeam(Boolean sameTeam) {
        botsOnSameTeam = sameTeam;
    }

    // IClient interface
    @Override
    public void systemMessage(String message) {

        if (!isDedicated()) {
            String sysColour = getConfigParam("SYSMESSAGECOLOR");
            message = "<font color=\"" + sysColour + "\"><b>" + message
                            + "</b></font>";

            addToChat(message, CCommPanel.CHANNEL_SLOG);
            if (Config.isParam("MAINCHANNELSM")) {
                addToChat(message);
            }
        }
    }

    @Override
    public void errorMessage(String message) {
        if (!isDedicated()) {
            javax.swing.JOptionPane.showMessageDialog(MainFrame, message);
        } else {
            MWLogger.errLog("Error: " + message);
        }
    }

    @Override
    public void processIncoming(String incoming) {
        IProtCommand pcommand = null;

        // MWLogger.infoLog("INCOMING: " + incoming);
        if (incoming.startsWith(IClient.PROTOCOL_PREFIX)) {
            incoming = incoming.substring(IClient.PROTOCOL_PREFIX.length());
            java.util.StringTokenizer ST = new java.util.StringTokenizer(incoming,
                  PROTOCOL_DELIMITER);
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
                if (incoming.equalsIgnoreCase("denied	/denied")) {
                    // let them know it's a wrong password
                    javax.swing.JOptionPane.showMessageDialog(getMainFrame(),
                          "Unknown Username/Password combination.");
                }
                return;
            }
        } else {
            MWLogger.infoLog("COMMAND ERROR: received protocol command without protocol prefix.");
            MWLogger.infoLog("COMMAND RECEIVED: " + incoming);
            return;
        }
    }

    IProtCommand getProtCommand(String command) {
        return ProtCommands.get(command);
    }

    @Override
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
                connectToServer(Config.getParam("SERVERIP"),
                      Config.getIntParam("SERVERPORT"));
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
            refreshGUI(REFRESH_STATUS);
        }
    }

    // Stop & send the close game event to the Server
    public void stopHost() {

        serverSend("CG");// send close game to server
        try {
            myServer.die();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Megamek Error:");
        }
        myServer = null;
    }

    @Override
    public void connectionEstablished() {

        LastPing = System.currentTimeMillis() / 1000;
        MWLogger.errLog("Connected. Signing on.");

        String VersionSubID = new java.rmi.dgc.VMID().toString();
        java.util.StringTokenizer ST = new java.util.StringTokenizer(VersionSubID, ":");

        /*
         * If password is blank, send a filler password instead of an empty
         * token. This prevents the no-password "whitescreen" error. HACKY. It
         * would be probably be better to actually fix the server SignOn so an
         * empty password creates a nobody, but this does the trick ...
         */
        String passToSend = getConfigParam("NAMEPASSWORD");
        if ((passToSend == null) || (passToSend.length() == 0)) {
            passToSend = "1337";
        }

        Connector.send(IClient.PROTOCOL_PREFIX + "signon\t" + getConfigParam("NAME")
                             + "\t" + passToSend + "\t" + getProtocolVersion() + "\t"
                             + Config.getParam("COLOR") + "\t" + CLIENT_VERSION + "\t"
                             + ST.nextToken());
        Status = STATUS_LOGGEDOUT;
        if (!isDedicated()) {
            refreshGUI(REFRESH_STATUS);
        }
    }

    public String getProtocolVersion() {
        return "4";
    }

    public void startHost(boolean dedicated, boolean deploy,
          boolean loadSavegame) {

        java.util.ArrayList<Unit> meks;
        java.util.ArrayList<CUnit> autoArmy;

        //@salient - check quirk xml file sizes with server
        if (Boolean.parseBoolean(getserverConfigs("EnableQuirks"))) {
            java.io.File canon = new java.io.File("data" + java.io.File.separator + "canonUnitQuirks.xml");
            java.io.File custom = new java.io.File("data" +
                                                         java.io.File.separator +
                                                         "mmconf" +
                                                         java.io.File.separator +
                                                         "unitQuirksOverride.xml");
            long canonFileLength = canon.length(); // returns 0L if does not exist
            long customFileLength = custom.length();
            sendChat(mekwars.client.MWClient.CAMPAIGN_PREFIX +
                           "c QUIRKCHECK#" +
                           canonFileLength +
                           "#" +
                           customFileLength);
        }

        // reread the config to allow the user to change setting during runtime
        String ip = "127.0.0.1";
        if (!getConfigParam("IP:").equals("")) {// IP Setting set, override IP
            // detection.
            try {
                ip = getConfigParam("IP:");
                java.net.InetAddress IA = java.net.InetAddress.getByName(ip); // Resolve Dyndns
                // Entries
                ip = IA.getHostAddress();
            } catch (Exception ex) {
                showInfoWindow(
                      "Couldn't set IP. Please check the spelling of mwconfig.txt's IP value or comment it out to use autodetection.");
                return;
            }
        }

        String MMVersion = getserverConfigs("AllowedMegaMekVersion");
        if (!MMVersion.equals("-1")
                  && !MMVersion.equalsIgnoreCase(megamek.SuiteConstants.VERSION.toString())) {
            if (isDedicated()) {
                MWLogger.errLog("You are using an invalid version of MegaMek. Please use version "
                                      + MMVersion);
                try {
                    stopHost();
                    goodbye();
                    Runtime runtime = Runtime.getRuntime();
                    String[] call = { "java", "-jar", "MekWarsAutoUpdate.jar",
                                      "DEDICATED" };
                    runtime.exec(call);
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
                System.exit(0);
            } else {
                showInfoWindow("You are using an invalid version of MegaMek. Please use version "
                                     + MMVersion);
            }
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
            myServer = new Server(gpassword, myPort, new GameManager());
            if (loadSavegame) {
                java.awt.FileDialog f = new java.awt.FileDialog(MainFrame, "Load Savegame");
                f.setDirectory(System.getProperty("user.dir") + "/savegames");
                f.setVisible(true);
                myServer.loadGame(new java.io.File(f.getDirectory(), f.getFile()));
            }
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

        ((Game) myServer.getGame()).addGameListener(this);
        // Send the new game info to the Server
        serverSend("NG|"
                         + new MMGame(myUsername, ip, myPort, MaxPlayers,
              megamek.SuiteConstants.VERSION.toString(), comment)
                                 .toString());
        if (!dedicated) {

            if (deploy) {
                meks = myPlayer.getLockedUnits();
                autoArmy = myPlayer.getAutoArmy();
            } else {
                meks = new java.util.ArrayList<Unit>();
                autoArmy = new java.util.ArrayList<CUnit>();
            }
            MWLogger.infoLog("Joining own game!");

            ClientThread MMGameThread = new ClientThread(myUsername,
                  myUsername, "127.0.0.1", myPort, this, meks, autoArmy);
            mmClientThreads.add(MMGameThread);
            ThreadManager.getInstance().runInThreadFromPool(MMGameThread);
            serverSend("JG|" + myUsername);
        } else {
            clearSavedGames();
            purgeOldLogs();
            ClientPreferences cs = PreferenceManager.getClientPreferences();
            cs.setStampFilenames(Boolean
                                       .parseBoolean(getserverConfigs("MMTimeStampLogFile")));
        }
    }

    public boolean isDedicated() {
        return Config.isParam("DEDICATED");
    }

    /*
     * NOTE: this list is ancient. sometimes useful. often out of date. List of
     * Abreviations for the protocol used by the client only: NG = New Game
     * (NG|<IP>|<Port>|<MaxPlayers>|<Version>|<Comment>) CG = Close Game (CG) GB
     * = Goodbye (Client exit) (GB) SO = Sign-On (SO|<Version>|<UserName>) Used
     * by Both: CH = Chat Server news:(CH|<text>) Client Chat:
     * (CH|<UserName>|<Color>|<Text>) Used only by the Server: SL|NG = Games
     * (GS|<MMGame.toString()>|<MMGame.toString()|...) SL|CG = close game SL|JG
     * = add a player to game list SL|LG = remove a player from game list SL|SHS
     * = Set Host Status (SHS|<GameID>|<Status>) US = Users
     * (US|<MMClientInfo.toString()>|<MMClientInfo.toString()>|..) UG = User
     * Gone (UG|<MMClientInfo.toString>|[Gone]) Gone is used when the client
     * didn't just change his name NewUserCommand = New User
     * (NewUserCommand|<MMClientInfo.toString>|[NEW]) NEW is used the same way as GONE in UG
     * ER = Error (Not yet used) (ER|<ErrorLevel>|<description>) NN = New name
     * (My name Change was successful) CT = Campaign Task Offset (CT|Offset) ChangeStatusCommand
     * = Campaign Status (ChangeStatusCommand|Status) GameOptionsCommand = Game Options
     * (GameOptionsCommand|OPTION1NAME|OPTION1VALUE|OPTION2NAME...) PlanetEnvironmentCommand = SPlanet Environment
     * (Used to initialize the MM map generator) FactionStatusScreenUpdateCommand = SHouse Status TI = Tick
     * Info (TI|TIMETILLNEXT) SP = Show PopupWindow SM = Show Miscellaneous
     * (Puts text into Misc Tab)
     */
    public synchronized void doParseDataInput(String input) {

        // non-null main frame, unbuffer or just pass through
        if (decodeBuffer.size() > 0) {
            java.util.Iterator<String> i = decodeBuffer.iterator();
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

        java.util.StringTokenizer st, own;
        String name, owner, command;
        int port;

        /*
         * New users, report requests and data should be sent to standard
         * processor. PrivateMessageCommand's are checked below, and all other commands are tossed
         * (e.g. - CH). Note that ded's bypass the doParseDeda() buffering
         * process (never have a main frame, so no null check or buffer needed)
         * and call doParseDataHelper() directly.
         */
        if (data.startsWith("US|") || data.startsWith("NewUserCommand|")
                  || data.startsWith("UG|") || data.startsWith("RGTS|")
                  || data.startsWith("DSD|") || data.startsWith("USD|")) {
            doParseDataHelper(data);// bypass the buffering process -
            // ded's never have a main fraime
            return;
        }

        // only parse PrivateMessageCommand's for commands
        if (!data.startsWith("PrivateMessageCommand|")) {
            return;
        }

        data = data.substring(3);// strip "PrivateMessageCommand|"
        st = new java.util.StringTokenizer(data, "|");
        own = new java.util.StringTokenizer(myDedOwners, "$");

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
                java.io.File logFile = new java.io.File("./logs/megameklog.txt");
                java.io.FileInputStream fis = new java.io.FileInputStream(logFile);
                java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(
                      fis));
                sendChat(IClient.PROTOCOL_PREFIX + "c sendtomisc#" + name
                               + "#MegaMek Log from " + myUsername);
                int counter = 0;
                while (dis.ready()) {
                    sendChat(IClient.PROTOCOL_PREFIX + "c sendtomisc#" + name + "#"
                                   + dis.readLine());
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
            sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                           + " used the display megamek logs command on " + myUsername);
            return;
        } else if (command.equals("displaydederrorlog")) { // display
            // error.0
            MWLogger.infoLog("display ded error command received from " + name);
            try {
                java.io.File logFile = new java.io.File("./logs/errlog.0");
                java.io.FileInputStream fis = new java.io.FileInputStream(logFile);
                java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(
                      fis));
                sendChat(IClient.PROTOCOL_PREFIX + "c sendtomisc#" + name
                               + "#Error Log from " + myUsername);
                int counter = 0;
                while (dis.ready()) {
                    sendChat(IClient.PROTOCOL_PREFIX + "c sendtomisc#" + name + "#"
                                   + dis.readLine());
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
            sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                           + " used the display ded error log command on "
                           + myUsername);
            return;
        } else if (command.equals("displaydedlog")) { // display
            // log.0
            MWLogger.infoLog("display ded log command received from "
                                   + name);
            try {
                java.io.File logFile = new java.io.File("./logs/infolog.0");
                java.io.FileInputStream fis = new java.io.FileInputStream(logFile);
                java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(
                      fis));
                sendChat(IClient.PROTOCOL_PREFIX + "c sendtomisc#" + name
                               + "#Ded Log from " + myUsername);
                int counter = 0;
                while (dis.ready()) {
                    sendChat(IClient.PROTOCOL_PREFIX + "c sendtomisc#" + name + "#"
                                   + dis.readLine());
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
            sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                           + " used the display ded log command on " + myUsername);
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

            if (myDedOwners.equals("") || name.equals(owner)
                      || (getUser(name).getUserlevel() >= 100)) { // if
                // no
                // owners
                // set,
                // anyone
                // can
                // send
                // commands

                if (command.equals("restart")) { // Restart the dedicated
                    // server

                    MWLogger.infoLog("Restart command received from "
                                           + name);
                    stopHost();// kill the host

                    // Remove any MM option files that deds may have.
                    java.io.File localGameOptions = new java.io.File("./mmconf");
                    try {
                        if (localGameOptions.exists()) {
                            localGameOptions = new java.io.File(
                                  "./mmconf/gameoptions.xml");
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
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the restart command on " + myUsername);

                    try {
                        Runtime runTime = Runtime.getRuntime();
                        if (new java.io.File("MekWarsDed.jar").exists()) {
                            String[] call = { "java", "-Xmx512m", "-jar",
                                              "MekWarsDed.jar" };
                            runTime.exec(call);
                        } else {
                            String[] call = { "java", "-Xmx512m", "-jar",
                                              "MekWarsClient.jar" };
                            runTime.exec(call);
                        }
                        System.exit(0);

                    } catch (Exception ex) {
                        MWLogger.errLog("Unable to find MekWarsDed.jar");
                    }
                    return;

                } else if (command.equals("reset")) { // server reset (like
                    // /reset in MM)

                    MWLogger.infoLog("Reset command received from "
                                           + name);
                    if (myServer != null) {
                        resetGame();
                    }
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the reset command on " + myUsername);
                    return;

                } else if (command.equals("die")) { // shut the dedicated down

                    goodbye();
                    System.exit(0);

                } else if (command.equals("start")) { // start hosting a MM
                    // game

                    MWLogger.infoLog("Start command received from "
                                           + name);
                    if (myServer == null) {
                        startHost(true, false, false);
                    }
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the start command on " + myUsername);
                    return;

                } else if (command.equals("stop")) { // stop MM host, but w/o
                    // killing ded's
                    // connection

                    // stop the host
                    MWLogger.infoLog("Stop command received from "
                                           + name);
                    if (myServer != null) {
                        stopHost();
                    }

                    // sleep, then wait around for a start command ...
                    try {
                        Thread.sleep(5000);
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                    }
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the stop command on " + myUsername);
                    return;

                } else if (command.equals("owners")) { // return a list of
                    // owners

                    MWLogger.infoLog("Owners command received from "
                                           + name);
                    sendChat(IClient.PROTOCOL_PREFIX + "mail " + name + ", My owners: "
                                   + myDedOwners.replace('$', ' '));
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the owners command on " + myUsername);
                    return;

                } else if (command.startsWith("owner ")) { // add new owner(s)

                    MWLogger.infoLog("Owner command received from "
                                           + name);
                    if (!myDedOwners.equals("")) {
                        myDedOwners = myDedOwners + "$";
                    }

                    myDedOwners = myDedOwners
                                        + command.substring(("owner ").length()).trim();
                    getConfig().setParam("DEDICATEDOWNERNAME", myDedOwners);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the owner " + myDedOwners + " command on "
                                   + myUsername);
                    return;

                } else if (command.equals("clearowners")) { // clear owners, and
                    // send feedback.

                    MWLogger.infoLog("Clearowners command received from "
                                           + name);
                    myDedOwners = "";
                    sendChat(IClient.PROTOCOL_PREFIX + "mail " + name + ", My owners: "
                                   + myDedOwners);
                    getConfig().setParam("DEDICATEDOWNERNAME", myDedOwners);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the clear owners command on " + myUsername);
                    return;

                } else if (command.equals("port")) {// return the server's port

                    MWLogger.infoLog("Port command received from "
                                           + name);
                    sendChat(IClient.PROTOCOL_PREFIX + "mail " + name + ", My port: "
                                   + myPort);
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the port command on " + myUsername);
                    return;

                } else if (command.startsWith("port ")) {// new server port

                    MWLogger.infoLog("Port (set) command received from " + name);
                    try {
                        port = Integer.parseInt(command.substring(
                              ("port ").length()).trim());
                    } catch (Exception ex) {
                        MWLogger.infoLog("Command error: " + command
                                               + ": non-numeral port.");
                        return;
                    }

                    if ((port > 0) && (port < 65536)) {
                        myPort = port;
                    }// check for legal port range
                    else {
                        MWLogger.infoLog("Command error: " + command
                                               + ": port out of valid range.");
                    }
                    String portString = Integer.toString(myPort);
                    getConfig().setParam("PORT", portString);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " changed the port for " + myUsername + " to "
                                   + myPort);
                    return;

                } else if (command.equals("savegamepurge")) {// server days
                    // to purge

                    MWLogger.infoLog("Save game purge command received from "
                                           + name);
                    sendChat(IClient.PROTOCOL_PREFIX + "mail " + name
                                   + ", I purge saved games that are "
                                   + savedGamesMaxDays + " days old, or older.");
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the save game purge command on "
                                   + myUsername);
                    return;

                } else if (command.startsWith("savegamepurge ")) { // set
                    // number of
                    // days to
                    // delete is
                    // purge is
                    // called

                    int mySavedGamesMaxDays = 7;
                    MWLogger.infoLog("Savegamepurge command received from "
                                           + name);
                    try {
                        mySavedGamesMaxDays = Integer.parseInt(command
                                                                     .substring(("savegamepurge ").length()).trim());
                    } catch (Exception ex) {
                        MWLogger.infoLog("Command error: " + command
                                               + ": invalid number.");
                        return;
                    }

                    String purgeString = Integer.toString(mySavedGamesMaxDays);
                    getConfig().setParam("MAXSAVEDGAMEDAYS", purgeString);
                    getConfig().saveConfig();
                    setConfig();
                    savedGamesMaxDays = mySavedGamesMaxDays;
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " changed the save game purge for " + myUsername
                                   + " to " + mySavedGamesMaxDays + " days.");
                    return;

                } else if (command.equals("displaysavedgames")) { // display
                    // saved
                    // games

                    MWLogger.infoLog("displaysavedgames command received from "
                                           + name);
                    java.io.File[] fileList;
                    String list = "<br><b>Saved files on " + myUsername
                                        + "</b><br>";
                    String dateTimeFormat = "MM/dd/yyyy HH:mm:ss";
                    java.text.SimpleDateFormat sDF = new java.text.SimpleDateFormat(dateTimeFormat);
                    try {
                        java.io.File tempFile = new java.io.File("./savegames/");
                        fileList = tempFile.listFiles();
                        for (java.io.File dateFile : fileList) {
                            java.util.Date date = new java.util.Date(dateFile.lastModified());
                            String dateTime = sDF.format(date);
                            list += "<a href=\"MEKMAIL" + myUsername
                                          + "*loadgamewithfullpath " + dateFile
                                          + "\">Load " + dateFile + "</a> "
                                          + dateTime + "<br>";
                        }
                    } catch (Exception ex) {
                        // do something?
                    }

                    sendChat(IClient.PROTOCOL_PREFIX + "mail " + name + ", " + list);
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the display saved games command on "
                                   + myUsername);
                    return;

                } else if (command.equals("update")) { // update the dedicated
                    // host using
                    // MWAutoUpdate

                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the update command on " + myUsername);
                    MWLogger.infoLog("Update command received from "
                                           + name);
                    try {
                        if (myServer != null) {
                            myServer.die();
                        }
                        goodbye();
                        Runtime runtime = Runtime.getRuntime();
                        String[] call = { "java", "-jar",
                                          "MekWarsAutoUpdate.jar", "DEDICATED",
                                          getConfigParam("DEDUPDATECOMMANDFILE") };
                        runtime.exec(call);
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                    }
                    System.exit(0);// restart the ded
                    return;

                } else if (command.equals("ping")) { // ping dedicated

                    MWLogger.infoLog("Ping command received from "
                                           + name);
                    String version = mekwars.client.MWClient.CLIENT_VERSION;
                    sendChat(IClient.PROTOCOL_PREFIX + "mail " + name
                                   + ", I'm active with version " + version + ".");
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the ping command on " + myUsername);
                    return;

                }
                if (command.equals("loadgame")
                          || command.startsWith("loadgame ")) { // load
                    // game
                    // from
                    // file

                    MWLogger.infoLog("Loadgame command received from " + name);
                    String filename = "";
                    if (command.startsWith("loadgame ")) {
                        filename = command.substring(("loadgame ").length())
                                         .trim();
                    }
                    if (command.equals("loadgame") || filename.equals("")) {
                        filename = "autosave.sav";
                    }
                    if (myServer != null) {
                        if (!loadGame(filename)) {
                            sendChat(IClient.PROTOCOL_PREFIX + "mail " + name
                                           + ", Unable to load saved game.");
                        } else {
                            sendChat(IClient.PROTOCOL_PREFIX + "mail " + name
                                           + ", Saved game loaded.");
                        }
                    }
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " loaded game " + filename + " on " + myUsername);
                    return;

                } else if (command.startsWith("loadgamewithfullpath ")) { // load
                    // game
                    // from
                    // file,
                    // using
                    // full
                    // path

                    MWLogger.infoLog("Loadgamewithfullpath command received from "
                                           + name);
                    String filename = "";
                    if (command.startsWith("loadgamewithfullpath ")) {
                        filename = command.substring(
                              ("loadgamewithfullpath ").length()).trim();
                    }
                    if (command.equals("loadgamewithfullpath")
                              || filename.equals("")) {
                        filename = "autosave.sav";
                    }
                    if (myServer != null) {
                        if (!loadGameWithFullPath(filename)) {
                            sendChat(IClient.PROTOCOL_PREFIX + "mail " + name
                                           + ", Unable to load saved game.");
                        } else {
                            sendChat(IClient.PROTOCOL_PREFIX + "mail " + name
                                           + ", Saved game loaded.");
                        }
                    }
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " loaded game " + filename + " on " + myUsername);
                    return;

                } else if (command.equals("loadautosave")) { // load the most
                    // recent auto
                    // save file

                    MWLogger.infoLog("Loadautosave command received from "
                                           + name);
                    String filename = "autosave.sav";
                    if (myServer != null) {
                        if (Boolean.parseBoolean(this
                                                       .getserverConfigs("MMTimeStampLogFile"))) {
                            filename = getParanoidAutoSave();
                        }

                        if (!loadGame(filename)) {
                            sendChat(IClient.PROTOCOL_PREFIX + "mail " + name
                                           + ", Unable to load saved game.");
                        } else {
                            sendChat(IClient.PROTOCOL_PREFIX + "mail " + name + ", "
                                           + filename + " loaded.");
                        }
                    }
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name + " loaded "
                                   + filename + " game on " + myUsername);
                    return;

                } else if (command.startsWith("name ")) { // new command
                    // prefix

                    MWLogger.infoLog("Name command received from "
                                           + name);
                    String myComName = command.substring(("name ").length())
                                             .trim();
                    getConfig().setParam("NAME", myComName);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(IClient.PROTOCOL_PREFIX
                                   + "c mm# "
                                   + name
                                   + " used the set name command to change the name to "
                                   + myComName + " command on " + myUsername);
                    Config.setParam("NAME", "[Dedicated] " + myComName);
                    myUsername = Config.getParam("NAME");
                    return;

                } else if (command.startsWith("comment ")) { // new command
                    // prefix

                    MWLogger.infoLog("Prefix command received from "
                                           + name);
                    String myComComment = command.substring(
                          ("comment ").length()).trim();
                    getConfig().setParam("COMMENT", myComComment);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " has set the comment to " + myComComment
                                   + " on " + myUsername);
                    return;

                } else if (command.startsWith("players ")) { // new command
                    // prefix

                    MWLogger.infoLog("Prefix command received from "
                                           + name);
                    try {
                        String numPlayers = command.substring(
                              ("players ").length()).trim();
                        getConfig().setParam("MAXPLAYERS", numPlayers);
                        getConfig().saveConfig();
                        setConfig();
                        sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                       + " has set the max number of players to "
                                       + numPlayers + " on " + myUsername);
                        return;
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                        MWLogger.errLog("Unable to convert number of players to int");
                        return;
                    }

                } else if (command.equals("restartcount")) { // server port

                    MWLogger.infoLog("Restartcount command received from "
                                           + name);
                    sendChat(IClient.PROTOCOL_PREFIX + "mail " + name
                                   + ", My restart count is set to " + dedRestartAt
                                   + " my current game count is " + gameCount);
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the restartcount command on " + myUsername);
                    return;

                } else if (command.startsWith("restartcount ")) {// new
                    // server
                    // port

                    MWLogger.infoLog("restartcount change command received from "
                                           + name);
                    try {
                        dedRestartAt = Integer.parseInt(command.substring(
                              ("restartcount ").length()).trim());
                    } catch (Exception ex) {
                        MWLogger.infoLog("Command error: " + command
                                               + ": bad counter.");
                        return;
                    }
                    String restartString = Integer.toString(dedRestartAt);
                    getConfig().setParam("DEDAUTORESTART", restartString);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " changed the restart count for " + myUsername
                                   + " to " + dedRestartAt);
                    return;

                } else if (command.equals("getupdateurl")) {// find out what url
                    // the ded is set to
                    // update with

                    MWLogger.infoLog("GetUpdateUrl command received from "
                                           + name);
                    String updateURL = getConfigParam("UPDATEURL");
                    sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name
                                   + " used the getUpdateURL command on " + myUsername);
                    sendChat(IClient.PROTOCOL_PREFIX + "mail " + name
                                   + ", My update URL is " + updateURL + ".");
                    return;

                } else if (command.startsWith("setupdateurl ")) {

                    MWLogger.infoLog("setUpdateURL command received from "
                                           + name);
                    String myUpdateURL = command.substring(
                          ("setupdateurl ").length()).trim();
                    getConfig().setParam("UPDATEURL", myUpdateURL);
                    getConfig().saveConfig();
                    setConfig();
                    sendChat(IClient.PROTOCOL_PREFIX
                                   + "c mm# "
                                   + name
                                   + " used the set update url command to change the the update url to "
                                   + myUpdateURL + " on " + myUsername);
                    return;

                }

                MWLogger.infoLog("Command error: " + command
                                       + ": unknown command.");
                return;
            }
        }

        sendChat(IClient.PROTOCOL_PREFIX + "c mm# " + name + " tried to use the "
                       + command + " on " + myUsername
                       + ", but does not have ownership.");
        sendChat(IClient.PROTOCOL_PREFIX + "mail " + name
                       + ", You do not have management rights for this host!");
        MWLogger.infoLog("Command error: " + command
                               + ": access denied for " + name + ".");
    }

    public void setLastPing(long lastping) {
        LastPing = lastping;
    }

    public CPlayer getPlayer() {
        return myPlayer;
    }

    /**
     * @param money
     * @param shortname
     * @param amount
     *
     * @return String Hokey function to return the correct syntax for long and short money/flu messages to the user.
     *       ClientVersion
     *
     * @author Torren (Jason Tighe)
     */
    public String moneyOrFluMessage(boolean money, boolean shortname, int amount) {
        return moneyOrFluMessage(money, shortname, amount, false);
    }

    public String moneyOrFluMessage(boolean money, boolean shortname,
          int amount, boolean showSign) {
        String result = java.text.NumberFormat.getInstance().format(amount);

        String moneyShort = getserverConfigs("MoneyShortName");
        String moneyLong = getserverConfigs("MoneyLongName");
        String fluShort = getserverConfigs("FluShortName");
        String fluLong = getserverConfigs("FluLongName");
        // String RPLong = getserverConfigs("RPLongName");
        // String RPShort = getserverConfigs("RPShortName");

        String sign = "+";

        if (amount < 0) {
            amount *= -1;
            result = "";
            sign = "-";
        }

        if (money) {
            if (shortname) {
                if ((amount == 1) && moneyShort.endsWith("s")) {
                    result += moneyShort.substring(0, moneyShort.length() - 1);
                } else if ((amount > 1) && !moneyShort.endsWith("s")) {
                    result += moneyShort + "s";
                } else {
                    result += moneyShort;
                }
            } else {// longname
                if ((amount == 1) && moneyLong.endsWith("s")) {
                    result += " "
                                    + moneyLong.substring(0, moneyLong.length() - 1);
                } else if ((amount > 1) && !moneyLong.endsWith("s")) {
                    result += " " + moneyLong + "s";
                } else {
                    result += " " + moneyLong;
                }
            }
        } else {// influence
            if (shortname) {
                result += fluShort;
            } else {
                result += " " + fluLong;
            }
        }

        // add sign, if set
        if (showSign) {
            result = sign + result;
        }

        return result.trim();
    }

    /**
     * @return Returns the data.
     */
    public CampaignData getData() {

        if ((data == null) && !isDedicated()) {

            // Lets reload everything from the cache and then pull down and
            // planet changes
            // If a majory campaign change has happened i.e. new terrains/houses
            // then the clients will be able to use the refresh all command via
            // CMainFrame --Torren
            try {
                MWLogger.infoLog("try to import the planetcache");
                // sanity check
                dataFetcher.checkServerVersion(this);
                // data = dataFetcher.getAllData();

                data = dataFetcher.getCacheData(getCacheDir());
                if ((data == null) || (data.getAllPlanets().size() == 0)
                          || (data.getAllHouses().size() == 0)) {
                    throw new Exception("data still empty");
                }
                refreshData();
                dataFetcher.store();
                MWLogger.infoLog("cache data loaded");
            } catch (Throwable e) {

                if (!(e instanceof java.io.FileNotFoundException)) {
                    MWLogger.errLog((Exception) e);
                }
                MWLogger.infoLog("need to fetch all planet data..");
                try {
                    data = dataFetcher.getAllData();
                    dataFetcher.store();
                } catch (java.net.ConnectException e1) {
                    if (splash != null) {
                        splash.setStatus(splash.STATUS_DATAERROR);
                    }
                    MWLogger.errLog(e1);
                    MWLogger.errLog(getCacheDir());
                    Object[] options = { "Exit", "Continue" };
                    int selectedValue = javax.swing.JOptionPane.showOptionDialog(null,
                          "Could not connect to server to fetch map data.",
                          "Connection error!", javax.swing.JOptionPane.DEFAULT_OPTION,
                          javax.swing.JOptionPane.ERROR_MESSAGE, null, options,
                          options[0]);
                    if (selectedValue == 0) {
                        System.exit(0);// exit, if they so choose
                    }
                } catch (java.io.IOException e1) {
                    if (splash != null) {
                        splash.setStatus(splash.STATUS_DATAERROR);
                    }
                    MWLogger.errLog(e1);
                    javax.swing.JOptionPane
                          .showMessageDialog(null,
                                "Server is busy while fetching planet data.\nTry again later.");
                } catch (Throwable e1) {
                    if (splash != null) {
                        splash.setStatus(splash.STATUS_DATAERROR);
                    }
                    MWLogger.errLog((Exception) e1);
                    Object[] options = { "Exit", "Continue" };
                    int selectedValue = javax.swing.JOptionPane
                                              .showOptionDialog(
                                                    null,
                                                    "Unknown error while fetching map data. Please\n"
                                                          + "report this bug, and keep your error logs handy.",
                                                    "Unknown error!",
                                                    javax.swing.JOptionPane.DEFAULT_OPTION,
                                                    javax.swing.JOptionPane.ERROR_MESSAGE, null, options,
                                                    options[0]);
                    if (selectedValue == 0) {
                        System.exit(0);// exit, if they so choose
                    }
                }
                if (splash != null) {
                    splash.setStatus(splash.STATUS_FETCHINGDATA);
                }
            }

            try {
                dataFetcher.getServerConfigData(this);
            } catch (Exception ex) {
                MWLogger.errLog("Unable to fetch Server configs.");
                MWLogger.errLog(ex);
            }

            try {
                dataFetcher.getBannedAmmoData(this);
            } catch (Exception ex) {
                MWLogger.errLog("Unable to fetch server banned ammo data.");
                MWLogger.errLog(ex);
            }

            // close the connection.a
            dataFetcher.closeDataConnection();
        }

        return data;
    }

    public void loadBannedAmmo() {
        try {
            dataFetcher.getBannedAmmoData(this);
        } catch (Exception ex) {
            if (!(ex instanceof java.net.SocketException)) {
                MWLogger.errLog("Error loading Server banned ammo file");
                MWLogger.errLog(ex);
            }
        }
    }

    public boolean getTargetSystemBanStatus(int type) {
        if (getData().getBannedTargetingSystems().contains(type)) {
            return true;
        }
        return false;
    }

    public CMainFrame getMainFrame() {
        return MainFrame;
    }

    public void getBlackMarketSettings() {
        try {
            dataFetcher.getBlackMarketSettings(this);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

    }

    public java.util.HashMap<String, Equipment> getBlackMarketEquipmentList() {
        return blackMarketEquipmentList;
    }

    /**
     * @author jtighe Reload all the server data back into the client. Used when the client runs a command that
     *       changes the server's data (ie - admin makes map change).
     */
    public void reloadData() {
        try {
            data = dataFetcher.getAllData();
        } catch (Exception ex) {
            if (!(ex instanceof java.net.SocketException)) {
                MWLogger.errLog(ex);
            }
        }
        try {
            dataFetcher.getServerConfigData(this);
        } catch (java.io.IOException e1) {
            MWLogger.errLog(e1);
        }
    }

    public void getServerConfigData() {
        try {
            dataFetcher.getServerConfigData(this);
        } catch (Exception ex) {
            if (!(ex instanceof java.net.SocketException)) {
                MWLogger.errLog(ex);
            }
        }
    }

    /**
     * Reloads new planet data from the server. This will be done asynchron, so you may have to wait a bit ;-)
     */
    synchronized public void refreshData() {

        // if the map isnt visible, skip the refresh. waste of bandwidth.
        if (!getConfig().isParam("MAPTABVISIBLE")) {
            MWLogger.infoLog("Map visibility disabled. Skipping map data fetch!");
            return;
        }

        if (!dataFetcher.getPlanetsUpdate(data)) {
            // MWLogger.infoLog("MD5 does not match! Retrieve all
            // planet data again.");
            MWLogger.infoLog("MD5 does not match! But the md5 seems broken anyway...");
            /*
             * try { data = dataFetcher.getAllData(); } catch (IOException e) {
             * MMClient.MWLogger.errLog(e);
             * JOptionPane.showMessageDialog(null,
             * "The map data could not be retrieved. The map will be disabled.\nTry again later."
             * ); getMainFrame().getMainPanel().getMapPanel().setEnabled(false);
             * }
             */
        }
        updateDataFetcher = null;
        // refresh the changed set... if more than one place want
        // to know about, maybe a listener system would be better..
        java.util.Map<Integer, Influences> changesSinceLastRefresh = dataFetcher
                                                                           .getChangesSinceLastRefresh();
        if (getMainFrame() != null) {
            getMainFrame().getMainPanel().getMapPanel().getMap()
                  .dataFetched(changesSinceLastRefresh);
        }
        MWLogger.infoLog("update for new planet data finished");
    }

    public void addToChat(String s) {
        addToChat(s, CCommPanel.CHANNEL_MAIN, null);
    }

    public void addToChat(String s, int channel) {
        addToChat(s, channel, null);
    }

    public GUIClientConfig getConfig() {
        return (GUIClientConfig) (Config);
    }

    /**
     * Does things when a tick is arrived.
     */
    public void processTick(int time) {
        // set tick counter
        getMainFrame().getMainPanel().getPlayerPanel()
              .setNextTick(System.currentTimeMillis() + time);
        getMainFrame().getMainPanel().getMapPanel().getMap().processTick();
        System.gc(); // Decicded to have the client do a GC every tick as
        // well.
    }

    public int getPlayerStartingEdge() {
        return playerStartingEdge;
    }

    public void setPlayerStartingEdge(int edge) {
        playerStartingEdge = edge;
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
            MWLogger.errLog("Unable to find param " + p);
            tparam = "";
        }

        if (tparam.equals("") && p.equals("NAME") && isDedicated()) {
            MWLogger.infoLog("Error: no dedicated name set.");
            System.exit(1);
        }
        return (tparam);
    }

    public void setUsername(String s) {
        myUsername = s.trim();
    }

    public void updateOpData(boolean deleteCache) {
        try {
            if (deleteCache) {
                new java.io.File(cacheDir + "/OpList.txt").delete();
            }

            dataFetcher.checkForMostRecentOpList();
            setupAllOps();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public int getMyStatus() {
        return Status;
    }

    public synchronized java.util.Collection<mekwars.client.CUser> getUsers() {
        return Users;
    }

    /**
     * Method which returns the master list of Operations (as assembled from OpList.txt) for use in display code.
     */
    public java.util.TreeMap<String, String[]> getAllOps() {
        return allOps;
    }

    public boolean isWaiting() {
        return waitingOnCommand;
    }

    public void setWaiting(boolean waiting) {
        waitingOnCommand = waiting;
    }

    /**
     * Return the directory, where all cache files can go into. The dirname depends on the server you connect.
     */
    public String getCacheDir() {
        // if (cacheDir == null) {
        // first access. Check if need to create directory.
        cacheDir = "data/servers/" + Config.getParam("SERVERIP") + "."
                         + Config.getParam("SERVERPORT");
        java.io.File dir = new java.io.File(cacheDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // }
        return cacheDir;
    }

    public void loadServerTraitFiles() {
        try {
            dataFetcher.getServerTraitFiles();
        } catch (Exception ex) {
            if (!(ex instanceof java.net.SocketException)) {

                MWLogger.errLog("Error loading Server Trait files");
                MWLogger.errLog(ex);
            }
        }
    }

    public void loadMegaMekClient() {
        try {
            setWaiting(true);
            sendChat(IClient.PROTOCOL_PREFIX + "c GetServerMegaMekGameOptions");
            try {
                while (isWaiting()) {
                    Thread.sleep(10);
                }
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
            GameOptions gameOptions = new GameOptions();
            gameOptions.loadOptions();
            GameOptionsDialog MMGOD = new GameOptionsDialog(getMainFrame(),
                  gameOptions, true);
            MMGOD.update(gameOptions);
            MMGOD.setEditable(true);
            MMGOD.setVisible(true);
            MMGOD.dispose();
            java.io.File localGameOptions = new java.io.File("mmconf/gameoptions.xml");

            if (localGameOptions.lastModified() >= (System.currentTimeMillis() - 1000)) {
                sendGameOptionsToServer();
            }
        } catch (Exception ex) {
            MWLogger.errLog("Unable to pull server MegaMek Logs");
            MWLogger.errLog(ex);
        }
    }

    public void setConfig() {
        Config = new GUIClientConfig(false);
    }

    public int getUserLevel() {
        return getUser(getUsername()).getUserlevel();
    }

    public RepairManagmentThread getRMT() {
        return RMT;
    }

    public double getAmmoCost(String ammo) {
        EquipmentType eq = EquipmentType.get(ammo);


        if (eq == null) {
            return -1;
        }

        if (!getCampaign().getBlackMarketParts().containsKey(
              eq.getInternalName())) {
            return -1;
        }

        if (getCampaign().getBlackMarketParts().get(eq.getInternalName())
                  .getCost() > 0) {
            return getCampaign().getBlackMarketParts()
                         .get(eq.getInternalName()).getCost();
        }

        return -1.0;
    }

    public SalvageManagmentThread getSMT() {
        return SMT;
    }

    public CCampaign getCampaign() {
        return theCampaign;
    }

    public void setPassword(String s) {
        password = s;
    }

    public void setIgnoreHouse() {
        IgnoreHouse = splitString(Config.getParam("IGNOREHOUSE"), ",");
    }

    public void setIgnorePrivate() {
        IgnorePrivate = splitString(Config.getParam("IGNOREPRIVATE"), ",");

    }

    public void setIgnorePublic() {
        IgnorePublic = splitString(Config.getParam("IGNOREPUBLIC"), ",");
    }

    public void setKeyWords() {
        KeyWords = splitString(Config.getParam("KEYWORDS"), ",");
    }

    /*
     * Rewritten in order to allow ConfigPage to reset the skin on the fly.
     *
     * @urgru 2.21.05
     */
    public void setLookAndFeel(boolean isRedraw) {

        javax.swing.LookAndFeel LAF = new com.incors.plaf.kunststoff.KunststoffLookAndFeel();
        if (Config.getParam("LOOKANDFEEL").equals("metal")) {
            LAF = new javax.swing.plaf.metal.MetalLookAndFeel();
        } else if (Config.getParam("LOOKANDFEEL").equals("metouia")) {
            LAF = new MetouiaLookAndFeel();
        } else if (Config.getParam("LOOKANDFEEL").equals("plastic")) {
            PlasticLookAndFeel.setMyCurrentTheme(new DesertGreen());
            LAF = new Plastic3DLookAndFeel();
        } else if (Config.getParam("LOOKANDFEEL").equals("plasticxp")) {
            LAF = new PlasticXPLookAndFeel();
        } else if (Config.getParam("LOOKANDFEEL").equals("plastic3d")) {
            PlasticLookAndFeel.setMyCurrentTheme(new SkyBlue());
            LAF = new Plastic3DLookAndFeel();
        } else if (Config.getParam("LOOKANDFEEL").equals("skins")) {
            try {
                Skin theSkinToUse = SkinLookAndFeel
                                          .loadThemePack("./data/skins/"
                                                               + Config.getParam("LOOKANDFEELSKIN"));
                SkinLookAndFeel.setSkin(theSkinToUse);
                LAF = new SkinLookAndFeel();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
                LAF = javax.swing.UIManager.getLookAndFeel();
            }
        }

        try {
            if (isRedraw) {
                MainFrame.setVisible(false);
            }

            if (Config.getParam("LOOKANDFEEL").equalsIgnoreCase("system")) {
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager
                                                           .getSystemLookAndFeelClassName());
            } else {
                javax.swing.UIManager.setLookAndFeel(LAF);
            }

            if (isRedraw) {
                javax.swing.SwingUtilities.updateComponentTreeUI(MainFrame);
                // MainFrame.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
                MainFrame.setVisible(true);
                getMainFrame().getMainPanel().getUserListPanel()
                      .resetActivityButton();
            }

        } catch (Exception ex) {
            MWLogger.errLog(ex);
            try {
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager
                                                           .getSystemLookAndFeelClassName());
            } catch (Exception e) {
                MWLogger.errLog(e);
            }
        }// end catch

    }// end setLookAndFeel

    public void showInfoWindow(String Text) {

        // Show a popup with a message
        if (!isDedicated()) {

            // JOptionPane.showInternalMessageDialog(MainFrame.getContentPane(),
            // Error);
            final javax.swing.JDialog dialog = new javax.swing.JDialog(MainFrame, "Message");

            // Add contents to it.
            javax.swing.JLabel label = new javax.swing.JLabel("<html>" + Text + "</html>");
            label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            java.awt.Container contentPane = dialog.getContentPane();
            contentPane.setLayout(new java.awt.GridBagLayout());
            java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.ipadx = 30;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
            contentPane.add(label, gridBagConstraints);
            gridBagConstraints.gridy = 1;
            gridBagConstraints.weightx = 0;
            gridBagConstraints.weighty = 0;
            javax.swing.JPanel panel = new javax.swing.JPanel();
            panel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
            javax.swing.JButton okButton = new javax.swing.JButton("OK");
            panel.add(okButton);
            contentPane.add(panel, gridBagConstraints);

            okButton.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent event) {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            });

            // Show it.
            java.awt.Dimension d = dialog.getPreferredSize();
            d.setSize(d.getWidth() + 20, d.getHeight() + 40);
            dialog.setSize(d);
            dialog.setLocationRelativeTo(MainFrame);
            dialog.setVisible(true);

        } else {
            MWLogger.errLog("-----------");
            MWLogger.errLog(Text);
            MWLogger.errLog("-----------");
        }
    }

    public Game getGame() {
        return game;
    }

    /**
     * Sets the current advanced terrain and map size that will be used on next playboard
     */
    public void setAdvancedTerrain(AdvancedTerrain aTerrain) {
        this.aTerrain = aTerrain;
    }

    public void refreshGUI(int mode) {
        try {
            javax.swing.SwingUtilities.invokeLater(new mekwars.client.MWClient.CRefreshGUI(mode));
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public boolean isIgnored(String name, int type) {

        // Do not ignore the staff.
        if (getUser(name.trim()).getUserlevel() >= 100) {
            return false;
        }

        // return true if non-mod is ignored
        for (String next : getIgnored(type)) {
            if (name.trim().equalsIgnoreCase(next.trim())) {
                return true;
            }
        }

        // otherwise, return false
        return false;
    }

    public String getShortTime() {
        mytime = new java.util.Date();
        java.util.StringTokenizer s = new java.util.StringTokenizer(mytime.toString());
        s.nextElement();
        s.nextElement();
        s.nextElement();
        String t = (String) s.nextElement();
        s = new java.util.StringTokenizer(t, ":");
        String result = "[" + s.nextElement() + ":" + s.nextElement() + "] ";
        return result;
    }

    public void doPlaySound(String filename) {
        doPlaySound(filename, true);
    }

    // This can happen quite often, since no check is made if the config option
    // is set
    public void doPlaySound(String filename, boolean inThread) {

        if (SoundMuted) {
            return;
        }

        try {
            if (inThread) {
                mekwars.client.AePlayWave player = new mekwars.client.AePlayWave(filename);
                player.start();
            } else {
                mekwars.client.AePlayWave.AePlayWaveNonThreaded(filename);
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public java.util.Vector<IBasicOption> getGameOptions() {
        return GameOptions;
    }

    /**
     * Sets the current environment, map size and map medium that will be used on next playboard
     */
    public void setEnvironment(PlanetEnvironment pe, java.awt.Dimension map,
          int mapMedium) {
        currentEnvironment = pe;
        MapSize = map;
        this.mapMedium = mapMedium;
    }

    public java.util.Vector<String> getIgnored(int type) {
        if (type == IGNORE_PUBLIC) {
            return IgnorePublic;
        }
        if (type == IGNORE_HOUSE) {
            return IgnoreHouse;
        }
        if (type == IGNORE_PRIVATE) {
            return IgnorePrivate;
        }
        return (new java.util.Vector<String>(1, 1));
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void rewardPointsDialog() {
        new RewardPointsDialog(this);
    }

    //@Salient
    public void influencePointsDialog() {
        new InfluencePointsDialog(this);
    }

    // IClient interface
    public void connectToServer() {
        connectToServer(Config.getParam("SERVERIP"),
              Config.getIntParam("SERVERPORT"));
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
            boolean loaded = myServer.loadGame(new java.io.File("./savegames/",
                  filename));
            ((Game) myServer.getGame()).addGameListener(this);
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
            boolean loaded = myServer.loadGame(new java.io.File(filename));
            ((Game) myServer.getGame()).addGameListener(this);
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

    public void startClient(String hostName, boolean deploy) {

        java.util.ArrayList<Unit> meks = new java.util.ArrayList<Unit>();
        java.util.ArrayList<CUnit> autoArmy = new java.util.ArrayList<CUnit>();

        // If a row is selected
        if ((servers.size() > 0) && (hostName != null)
                  && (hostName.trim().length() > 0)) {

            // get server from tree
            MMGame toJoin = servers.get(hostName);

            // allow people to re-enter games they're in
            if ((toJoin.getCurrentPlayers().size() >= toJoin.getMaxPlayers())
                      && !toJoin.getCurrentPlayers().contains(myUsername)
                      && !isMod()) {
                showInfoWindow("This game is already full");
                return;
            }

            String serverip = toJoin.getIp();
            int serverport = toJoin.getPort();

            // if player is joining his OWN host, use loopback
            if (myUsername.equalsIgnoreCase(toJoin.getHostName())) {
                serverip = "localhost";
            }

            if (deploy) {
                meks = myPlayer.getLockedUnits();
                autoArmy = myPlayer.getAutoArmy();
            }

            ClientThread tmpThread = new ClientThread(Config.getParam("NAME"),
                  hostName, serverip, serverport, this, meks, autoArmy);
            mmClientThreads.add(tmpThread);
            ThreadManager.getInstance().runInThreadFromPool(tmpThread);
            serverSend("JG|" + toJoin.getHostName());
            toJoin = null;
        } else {
            showInfoWindow("You have to select a game!");
        }
    }

    public void closingGame(String hostName) {

        // update battles tab for all players, via server
        MWLogger.infoLog("Leaving " + hostName);
        serverSend("LG|" + hostName);

        System.gc();
    }

    public java.awt.Dimension getMapSize() {
        return MapSize;
    }

    public int getMapMedium() {
        return mapMedium;
    }

    //@Salient ... ugh... how can i get to the damn house configs
    //    public String getHouseConfigs(String key)
    //    {
    //    	//CampaignData.cd.ge
    //    	SHouse house = CampaignData.cd.getHouseByName(this.getPlayer().getHouse());
    //
    //    	return CampaignData.cd.getServerConfigs().getProperty(key).trim();
    //    }

    public PlanetEnvironment getCurrentEnvironment() {
        return currentEnvironment;
    }

    public AdvancedTerrain getCurrentAdvancedTerrain() {
        return aTerrain;
    }

    public Browser getBrowser() {
        return browser;
    }

    public java.util.Map<Integer, Influences> getChangesSinceLastRefresh() {
        return dataFetcher.getChangesSinceLastRefresh();
    }

    public void loadServerCommmands() {
        try {
            dataFetcher.getAccessLevels(getData());
        } catch (Exception ex) {
            if (!(ex instanceof java.net.SocketException)) {
                MWLogger.errLog("Error loading Server Commands files");
                MWLogger.errLog(ex);
            }
        }
    }

    /**
     * Changes the duty to a new status.
     *
     * @param newStatus
     */
    public void changeStatus(int newStatus) {
        LastStatus = Status;
        Status = newStatus;

        if (Status == mekwars.client.MWClient.STATUS_RESERVE) {
            // there commands now send as part of the MWClient contructor.
            // sendChat(MWClient.CAMPAIGN_PREFIX + "c setclientversion#" +
            // this.myUsername+ "#" + CLIENT_VERSION);
            // this.sendChat("/getsavedmail");
        } else if (Status == STATUS_LOGGEDOUT) {
            clearUserCampaignData();
        }

        // update the activity button
        if (Status == mekwars.client.MWClient.STATUS_FIGHTING) {
            // this.getMainFrame().getMainPanel().getUserListPanel().setActivityButton(false);
            getMainFrame().getMainPanel().getUserListPanel()
                  .setActivityButtonEnabled(false);
        } else if (Status == mekwars.client.MWClient.STATUS_ACTIVE) {
            if (LastStatus != mekwars.client.MWClient.STATUS_FIGHTING) {
                getMainFrame().getMainPanel().getUserListPanel()
                      .setActivityButton(false);
            }
            getMainFrame().getMainPanel().getUserListPanel()
                  .setActivityButtonEnabled(true);
        } else if (Status == STATUS_DISCONNECTED) {
            getMainFrame().getMainPanel().getUserListPanel()
                  .setActivateButtonText("Disconnected");
            getMainFrame().getMainPanel().getUserListPanel()
                  .setActivityButtonEnabled(false);
        } else if (Status == mekwars.client.MWClient.STATUS_LOGGEDOUT) {
            getMainFrame().getMainPanel().getUserListPanel()
                  .setActivateButtonText("Login");
            getMainFrame().getMainPanel().getUserListPanel()
                  .setActivityButtonEnabled(true);
        } else if (Status == mekwars.client.MWClient.STATUS_RESERVE) {
            if (LastStatus != mekwars.client.MWClient.STATUS_LOGGEDOUT) {
                getMainFrame().getMainPanel().getUserListPanel()
                      .setActivityButton(true);
            }
            getMainFrame().getMainPanel().getUserListPanel()
                  .setActivityButtonEnabled(true);
        }

        // update the CMainFrame Attack menu
        getMainFrame().updateAttackMenu();

        refreshGUI(REFRESH_STATUS);
        refreshGUI(REFRESH_HQPANEL);
        refreshGUI(REFRESH_PLAYERPANEL);
    }

    public synchronized void clearUserCampaignData() {
        for (CUser currUser : Users) {
            currUser.clearCampaignData();
        }
    }

    public java.util.Properties getserverConfigs() {
        return CampaignData.cd.getServerConfigs();
    }

    public boolean isLeader() {
        return getUserLevel() >= Integer
                                       .parseInt(getserverConfigs("factionLeaderLevel"));
    }

    public void clearSavedGames() {

        long daysInSeconds = ((long) savedGamesMaxDays) * 24 * 60 * 60 * 1000;

        java.io.File saveFiles = new java.io.File("./savegames/");
        if (!saveFiles.exists()) {
            return;
        }
        java.io.File[] fileList = saveFiles.listFiles();
        for (java.io.File savedFile : fileList) {
            long lastTime = savedFile.lastModified();
            if (savedFile.exists()
                      && savedFile.isFile()
                      && (lastTime < (System.currentTimeMillis() - daysInSeconds))) {
                try {
                    MWLogger.infoLog("Purging File: "
                                           + savedFile.getName() + " Time: " + lastTime
                                           + " purge Time: "
                                           + (System.currentTimeMillis() - daysInSeconds));
                    savedFile.delete();
                } catch (Exception ex) {
                    MWLogger.errLog("Error trying to delete these files!");
                    MWLogger.errLog(ex);
                }
            }
        }
    }

    public String getParanoidAutoSave() {

        java.io.File tempFile = new java.io.File("./savegames/");
        java.io.FilenameFilter filter = new mekwars.client.MWClient.AutoSaveFilter();
        java.io.File[] fileList = tempFile.listFiles(filter);
        long time = 0;
        String saveFile = "autosave.sav";
        for (java.io.File newFile : fileList) {
            if (newFile.lastModified() > time) {
                time = newFile.lastModified();
                saveFile = newFile.getName();
            }
        }
        return saveFile;
    }

    public void clearBanAmmo() {
        getData().getServerBannedAmmo().clear();

        for (House faction : getData().getAllHouses()) {
            faction.getBannedAmmo().clear();
        }
    }

    public void clearBanTargeting() {
        getData().getBannedTargetingSystems().clear();
    }

    public void loadBanTargeting(String line) {
        java.util.StringTokenizer st = new java.util.StringTokenizer(line, "#");
        while (st.hasMoreTokens()) {
            getData().getBannedTargetingSystems().add(
                  Integer.parseInt(st.nextToken()));
        }
    }

    public void saveBannedTargetingSystems(String timestamp) {
        // Save banned targeting systems
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(cacheDir
                                                                              + "/bantargeting.dat");
            java.io.PrintStream p = new java.io.PrintStream(out);
            p.println(timestamp);
            for (Integer targetingSytem : getData().getBannedTargetingSystems()) {
                p.print(targetingSytem);
                p.print("#");
            }
            p.close();
            out.close();
        } catch (Exception ex) {
        }
    }

    public void loadBanAmmo(String line) {

        try {
            java.util.StringTokenizer st = new java.util.StringTokenizer(line, "#");
            String HouseName = (String) st.nextElement();
            House faction = null;
            if (!HouseName.equalsIgnoreCase("server")) {
                faction = getData().getHouseByName(HouseName);
                while (st.hasMoreTokens()) {
                    faction.getBannedAmmo().put(st.nextToken(), "Banned");
                }
            } else {
                while (st.hasMoreElements()) {
                    getData().getServerBannedAmmo().put(st.nextToken(),
                          "Banned");
                }
            }
        } catch (Exception ex) {
        }// make it compatible with people that had the old format,without
        // the timestamp on the first line, the first time and now dont.
    }

    public void saveBannedAmmo(String timestamp) {
        // Save banned ammo
        try {

            // output streams
            java.io.FileOutputStream out = new java.io.FileOutputStream(cacheDir
                                                                              + "/banammo.dat");
            java.io.PrintStream p = new java.io.PrintStream(out);

            // timestamp
            p.println(timestamp);

            // server-wide bans
            p.print("server#");
            for (String currBan : data.getServerBannedAmmo().keySet()) {
                p.print(currBan);
                p.print("#");
            }

            p.println();// newline

            // faction-only bans
            for (House h : data.getAllHouses()) {

                if (h.getBannedAmmo().size() < 1) {
                    continue;
                }

                p.print(h.getName() + "#");
                for (String currBan : h.getBannedAmmo().keySet()) {
                    p.print(currBan);
                    p.print("#");
                }

                p.println();
            }

            // close streams
            p.close();
            out.close();

        } catch (Exception ex) {
            // TODO: Log error?
        }
    }

    public int getMinPlanetOwnerShip(Planet p) {

        if (p.getMinPlanetOwnerShip() == -1) {
            return Integer.parseInt(getserverConfigs("MinPlanetOwnerShip"));
        }

        return p.getMinPlanetOwnerShip();
    }

    public int getTotalRepairCosts(Entity unit) {

        int cost = 0;
        int systemCrits = 0;
        int engineCrits = 0;

        for (int critLocation = 0; critLocation < unit.locations(); critLocation++) {
            // These three location have rear armor so the user might be
            // selecting that armor instead of crit.
            if ((critLocation == Mech.LOC_CT) || (critLocation == Mech.LOC_LT)
                      || (critLocation == Mech.LOC_RT)) {
                if (unit.getArmor(critLocation, false) != unit.getOArmor(
                      critLocation, false)) {
                    cost += CUnit.getArmorCost(unit, this, critLocation)
                                  * (unit.getOArmor(critLocation, false) - unit
                                                                                 .getArmor(critLocation, false));
                }
                if (unit.getArmor(critLocation, true) != unit.getOArmor(
                      critLocation, true)) {
                    cost += CUnit.getArmorCost(unit, this, critLocation)
                                  * (unit.getOArmor(critLocation, false) - unit
                                                                                 .getArmor(critLocation, false));
                }
                if (unit.getInternal(critLocation) != unit
                                                            .getOInternal(critLocation)) {
                    cost += CUnit.getStructureCost(unit, this)
                                  * (unit.getOInternal(critLocation) - unit
                                                                             .getInternal(critLocation));
                }
            }// end toros armor
            else {
                if (unit.getArmor(critLocation, false) != unit.getOArmor(
                      critLocation, false)) {
                    cost += CUnit.getArmorCost(unit, this, critLocation)
                                  * (unit.getOArmor(critLocation, false) - unit
                                                                                 .getArmor(critLocation, false));
                }
                if (unit.getInternal(critLocation) != unit
                                                            .getOInternal(critLocation)) {
                    cost += CUnit.getStructureCost(unit, this)
                                  * (unit.getOInternal(critLocation) - unit
                                                                             .getInternal(critLocation));
                }
            }// end armor

            for (int critSlot = 0; critSlot < unit
                                                    .getNumberOfCriticals(critLocation); critSlot++) {

                CriticalSlot cs = unit.getCritical(critLocation, critSlot);

                if (cs == null) {
                    continue;
                }

                if (cs.isBreached()) {
                    continue;
                }

                if (!cs.isDamaged()) {
                    continue;
                }

                if (UnitUtils.isEngineCrit(cs)) {
                    engineCrits = UnitUtils.getNumberOfEngineCrits(unit);
                } else if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
                    systemCrits++;
                } else {
                    cost += CUnit.getCritCost(unit, this, cs);
                }
            }// end slot for
        }// end location for

        cost += Integer.parseInt(this.getserverConfigs("SystemCritRepairCost"))
                      * systemCrits;
        cost += Integer.parseInt(this.getserverConfigs("EngineCritRepairCost"))
                      * engineCrits;

        return cost;
    }

    public void setServerOpFlags(java.util.StringTokenizer st) {
        java.util.TreeMap<String, String> map = new java.util.TreeMap<String, String>();

        try {
            while (st.hasMoreTokens()) {
                map.put(st.nextToken(), st.nextToken());
            }
            getData().getPlanetOpFlags().clear();
            getData().getPlanetOpFlags().putAll(map);
        } catch (Exception ex) {
        }
    }

    public int getTechLaborCosts(Entity unit, int techType) {
        int cost = 0;
        int techCost = Integer.parseInt(getserverConfigs(UnitUtils
                                                               .techDescription(techType) + "TechRepairCost"));
        int totalCrits = 0;
        boolean damagedEngine = false;

        for (int critLocation = 0; critLocation < unit.locations(); critLocation++) {
            // These three location have rear armor so the user might be
            // selecting that armor instead of crit.
            if ((critLocation == Mech.LOC_CT) || (critLocation == Mech.LOC_LT)
                      || (critLocation == Mech.LOC_RT)) {
                if (unit.getArmor(critLocation, false) != unit.getOArmor(
                      critLocation, false)) {
                    cost += techCost;
                }
                if (unit.getArmor(critLocation, true) != unit.getOArmor(
                      critLocation, true)) {
                    cost += techCost;
                }
                if (unit.getInternal(critLocation) != unit
                                                            .getOInternal(critLocation)) {
                    cost += techCost;
                }
            }// end toros armor
            else {
                if (unit.getArmor(critLocation, false) != unit.getOArmor(
                      critLocation, false)) {
                    cost += techCost;
                }
                if (unit.getInternal(critLocation) != unit
                                                            .getOInternal(critLocation)) {
                    cost += techCost;
                }
            }// end armor

            // check for damage system crits.
            for (int critSlot = 0; critSlot < unit
                                                    .getNumberOfCriticals(critLocation); critSlot++) {

                CriticalSlot cs = unit.getCritical(critLocation, critSlot);

                if (cs == null) {
                    continue;
                }

                if (cs.isBreached()) {
                    continue;
                }

                if (!cs.isDamaged()) {
                    continue;
                }

                if (UnitUtils.isEngineCrit(cs)) {
                    damagedEngine = true;
                    continue;
                }
                totalCrits++;

            }// end slot for
        }// end location for

        // check for damaged engines
        if (damagedEngine) {
            totalCrits = +UnitUtils.getNumberOfEngineCrits(unit);
        }

        cost += (techCost * totalCrits) + techCost;

        return cost;
    }

    public void retrieveOpData(String type, String data) {

        java.util.StringTokenizer st = new java.util.StringTokenizer(data, "#");

        String opName = st.nextToken();

        java.io.File opFile = new java.io.File("./data/operations/" + type);

        if (!opFile.exists()) {
            opFile.mkdirs();
        }

        opFile = new java.io.File("./data/operations/" + type + "/" + opName + ".txt");
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(opFile);
            java.io.PrintStream p = new java.io.PrintStream(out);
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

        java.util.StringTokenizer st = new java.util.StringTokenizer(data, "#");

        String mulName = st.nextToken();

        java.io.File mulFile = new java.io.File("./data/armies/");

        if (!mulFile.exists()) {
            mulFile.mkdirs();
        }

        mulFile = new java.io.File("./data/armies/" + mulName);
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(mulFile);
            java.io.PrintStream p = new java.io.PrintStream(out);
            while (st.hasMoreTokens()) {
                p.println(st.nextToken().replaceAll("\\(pound\\)", "#"));
            }
            p.close();
            out.close();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

    }

    public void updateParam(java.util.StringTokenizer ST) {
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

    public void updatePartsBlackMarket(String data, int year) {

        java.util.StringTokenizer ST = new java.util.StringTokenizer(data, "#");
        boolean allowTechCrossOver = Boolean.parseBoolean(this
                                                                .getserverConfigs("AllowCrossOverTech"));
        int houseTechLevel = getData().getHouseByName(getPlayer().getHouse())
                                   .getTechLevel();

        getCampaign().getBlackMarketParts().clear();

        while (ST.hasMoreTokens()) {

            BMEquipment bme = new BMEquipment();
            boolean error = false;
            boolean disallowed = false;
            try {
                error = false;
                disallowed = false;
                bme.setEquipmentInternalName(ST.nextToken());
                bme.setAmount(Integer.parseInt(ST.nextToken()));
                bme.setCost(Double.parseDouble(ST.nextToken()));
                bme.setCostUp(Boolean.parseBoolean(ST.nextToken()));

                bme.getTech(year);

                if (!allowTechCrossOver
                          && !UnitUtils
                                    .isSameTech(bme.getTechLevel(), houseTechLevel)) {
                    disallowed = true;
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                MWLogger.errLog("Exception in Parts BM");
                MWLogger.errLog(e.getLocalizedMessage());
                error = true;
            }

            if (!error && !disallowed) {
                getCampaign().getBlackMarketParts().put(
                      bme.getEquipmentInternalName(), bme);
            }
        }

        getMainFrame().getMainPanel().refreshBME();
    }

    public void updatePlayerPartsCache(String data) {

        java.util.StringTokenizer ST = new java.util.StringTokenizer(data, "#");
        String key = ST.nextToken();
        int value = Integer.parseInt(ST.nextToken());

        if (value < 1) {
            getPlayer().getPartsCache().remove(key, Math.abs(value));
        } else {
            getPlayer().getPartsCache().add(key, value);
        }

        getMainFrame().getMainPanel().refreshBME();
    }

    public void updateClient() {
        try {
            // this.stopHost();
            goodbye();
            Runtime runtime = Runtime.getRuntime();
            String[] call = { "java", "-jar", "MekWarsAutoUpdate.jar", "PLAYER" };
            runtime.exec(call);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
        System.exit(0);
    }

    public void goodbye() {
        SignOff = true;
        if (!isDedicated() && (Status > STATUS_LOGGEDOUT)) {
            getConfig().setParam(
                  "PANELDIVIDER",
                  Integer.toString(getMainFrame().getMainPanel()
                                         .getTabSPane().getDividerLocation()));
            getConfig().setParam(
                  "VERTICALDIVIDER",
                  Integer.toString(getMainFrame().getMainPanel()
                                         .getMainSPane().getDividerLocation()));
            getConfig().setParam(
                  "PLAYERPANELDIVIDER",
                  Integer.toString(getMainFrame().getMainPanel()
                                         .getSideSPane().getDividerLocation()));
            getConfig().setParam("WINDOWSTATE",
                  Integer.toString(getMainFrame().getExtendedState()));
            getConfig().setParam("WINDOWHEIGHT",
                  Integer.toString(getMainFrame().getHeight()));
            getConfig().setParam("WINDOWWIDTH",
                  Integer.toString(getMainFrame().getWidth()));
            getConfig().setParam("WINDOWLEFT",
                  Integer.toString(getMainFrame().getX()));
            getConfig().setParam("WINDOWTOP",
                  Integer.toString(getMainFrame().getY()));
            getConfig().saveConfig();
        }
        if (Status != STATUS_DISCONNECTED) {
            // serverSend("GB");
            Connector.send(IClient.PROTOCOL_PREFIX + "signoff");
            dataFetcher.closeDataConnection();
            Connector.closeConnection();
        }

        if (getConfig().isParam("ENABLEEXITCLIENTSOUND")) {
            doPlaySound(getConfigParam("SOUNDONEXITCLIENT"), false);
        }
    }

    public void createNewHouse(java.util.StringTokenizer st) {
        House house = new House();

        house.setId(TokenReader.readInt(st));
        house.setName(TokenReader.readString(st));
        house.setLogo(TokenReader.readString(st));
        house.setBaseGunner(TokenReader.readInt(st));
        house.setBasePilot(TokenReader.readInt(st));
        house.setHouseColor(TokenReader.readString(st));
        house.setHousePlayerColors(TokenReader.readString(st));
        house.setAbbreviation(TokenReader.readString(st));
        house.setConquerable(TokenReader.readBoolean(st));
        house.setTechLevel(TokenReader.readInt(st));
        house.setHouseDefectionFrom(TokenReader.readBoolean(st));
        house.setHouseDefectionTo(TokenReader.readBoolean(st));
        house.setUsedMekBayMultiplier(TokenReader.readFloat(st));
        getData().addHouse(house);
    }

    /**
     * redundant code since MM does not always send a discon event.
     */
    public void gamePlayerStatusChange(GameEvent e) {
    }

    public java.util.List<ClientThread> getMMClients() {
        return mmClientThreads;
    }

    @Override
    public void gameClientFeedbackRequest(GameCFREvent arg0) {
        // TODO Auto-generated method stub

    }

    public boolean isUsingAdvanceRepairs() {
        return Boolean.parseBoolean(getserverConfigs("UseAdvanceRepair"))
                     || Boolean.parseBoolean(getserverConfigs("UseSimpleRepair"));
    }

    protected void sendServerGameUpdate() {
        // Report the mech stat

        // Only send data for units currently on the board.
        // any units removed from play will have already sent thier final
        // update.
        java.util.Iterator<Entity> en = ((Game) myServer.getGame()).getEntities();
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

    protected void sendGameReport() {
        if (myServer == null) {
            return;
        }

        StringBuilder result = prepareReport(
              new GameWrapper(((Game) myServer.getGame())), isUsingAdvanceRepairs(),
              getBuildingTemplate());

        // send the autoreport
        serverSend("CR|" + result.toString());

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
        dummyUser.setColor(Config.getParam("CHATFONTCOLOR"));
        return dummyUser;
    }

    public static StringBuilder prepareReport(GameInterface myGame,
          boolean usingAdvancedRepairs, Buildings buildingTemplate) {
        StringBuilder result = new StringBuilder();
        String name = "";
        // Parse the real playername from the Modified In game one..
        String winnerName = "";
        if (myGame.hasWinner()) {

            int numberOfWinners = 0;
            // Multiple Winners
            java.util.List<String> winners = myGame.getWinners();

            // TODO: Winners is sometimes coming up empty. Let's see why
            MWLogger.errLog("Finding winners:");
            MWLogger.errLog(winners.toString());

            for (String winner : winners) {
                java.util.StringTokenizer st = new java.util.StringTokenizer(winner, "~");
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
        java.util.Enumeration<Entity> en = myGame.getDevastatedEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false,
                  usingAdvancedRepairs));
            result.append("#");
        }
        en = myGame.getGraveyardEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false,
                  usingAdvancedRepairs));
            result.append("#");

        }
        java.util.Iterator<Entity> en2 = myGame.getEntities();
        while (en2.hasNext()) {
            Entity ent = en2.next();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false,
                  usingAdvancedRepairs));
            result.append("#");
        }
        en = myGame.getRetreatedEntities();
        while (en.hasMoreElements()) {
            Entity ent = en.nextElement();
            if (ent.getOwner().getName().startsWith("War Bot")) {
                continue;
            }
            result.append(SerializeEntity.serializeEntity(ent, true, false,
                  usingAdvancedRepairs));
            result.append("#");
        }

        if (buildingTemplate != null) {
            result.append("BL*" + buildingTemplate);
        }
        MWLogger.infoLog("CR|" + result);
        return result;
    }

    public Buildings getBuildingTemplate() {
        return buildingTemplate;
    }

    public void setBuildingTemplate(Buildings buildingTemplate) {
        this.buildingTemplate = buildingTemplate;
    }

    // this adds 1 to the number of games played and if it matched the restart
    // amount it restarts the ded.
    public void checkForRestart() {
        gameCount++;

        // only check for restart once every 30 seconds.
        if ((System.currentTimeMillis() - 30000) < lastResetCheck) {
            return;
        }

        if (gameCount >= dedRestartAt) {
            MWLogger.infoLog("System has reached " + gameCount
                                   + " games played and is restarting");
            try {
                Thread.sleep(5000);
            }// give people time to vacate
            catch (Exception ex) {
                MWLogger.errLog(ex);
            }
            stopHost();
            try {
                Thread.sleep(5000);
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
            try {
                Runtime runTime = Runtime.getRuntime();
                if (new java.io.File("MekWarsDed.jar").exists()) {
                    String[] call = { "java", "-Xmx512m", "-jar",
                                      "MekWarsDed.jar" };
                    runTime.exec(call);
                } else {
                    String[] call = { "java", "-Xmx512m", "-jar",
                                      "MekWarsClient.jar" };
                    runTime.exec(call);
                }
                System.exit(0);

            } catch (Exception ex) {
                MWLogger.errLog("Unable to find MekWarsDed.jar");
            }
        }

        lastResetCheck = System.currentTimeMillis();
    }

    /*
     * INNER CLASSES
     */
    static class AutoSaveFilter implements java.io.FilenameFilter {
        @Override
        public boolean accept(java.io.File dir, String name) {
            return (name.startsWith("autosave"));
        }
    }

    private static class PurgeAutoSaves implements Runnable {

        public PurgeAutoSaves() {
            super();
        }

        @Override
        public void run() {
            long twoHours = 2 * 60 * 60 * 1000;
            try {
                while (true) {
                    java.io.File saveFiles = new java.io.File("./savegames");
                    if (!saveFiles.exists()) {
                        return;
                    }
                    java.io.FilenameFilter filter = new mekwars.client.MWClient.AutoSaveFilter();
                    java.io.File[] fileList = saveFiles.listFiles(filter);
                    for (java.io.File savedFile : fileList) {
                        long lastTime = savedFile.lastModified();
                        if (savedFile.exists()
                                  && savedFile.isFile()
                                  && (lastTime < (System.currentTimeMillis() - twoHours))) {
                            try {
                                MWLogger.infoLog("Purging File: "
                                                       + savedFile.getName()
                                                       + " Time: "
                                                       + lastTime
                                                       + " purge Time: "
                                                       + (System.currentTimeMillis() - twoHours));
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

    protected class CAddToChat implements Runnable {
        String input = "";
        int channel = -1;
        String tabName = "";

        public CAddToChat(String tinput, int tchannel, String ttabName) {
            input = tinput;
            channel = tchannel;
            tabName = ttabName;
        }

        @Override
        public void run() {
            (MainFrame.getMainPanel().getCommPanel()).setChat(input, channel,
                  tabName);
        }
        //@salient discord bot chat capture
        //if i wanted to capture multiple channels
        //i'd have to do it here?
    }

    protected class CRefreshGUI implements Runnable {
        protected int mode = -1;

        public CRefreshGUI(int tmode) {
            mode = tmode;
        }

        @Override
        public void run() {
            if (MainFrame == null) {
                return;
            }// return if main frame not yet drawn (still fetching data)
            try {
                switch (mode) {
                    case REFRESH_USERLIST:
                        MainFrame.getMainPanel().getUserListPanel().refresh();
                        break;
                    case REFRESH_PLAYERPANEL:
                        MainFrame.getMainPanel().getPlayerPanel().refresh();
                        break;
                    case REFRESH_BATTLETABLE:
                        MainFrame.refreshBattleTable();
                        break;
                    case REFRESH_HQPANEL:
                        MainFrame.getMainPanel().getHQPanel().refresh();
                        break;
                    case REFRESH_STATUS:
                        MainFrame.changeStatus(Status, LastStatus);
                        break;
                    case REFRESH_BMPANEL:
                        MainFrame.getMainPanel().getBMPanel().refresh();
                        break;
                }
            } catch (Exception ex) {
                // do nothing
            }
        }
    }

    protected class TimeOutThread extends Thread {

        mekwars.client.MWClient mwclient;

        public TimeOutThread(mekwars.client.MWClient client) {
            mwclient = client;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(TimeOut * 100);
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
                if (Status != STATUS_DISCONNECTED) {
                    long timeout = (System.currentTimeMillis() / 1000)
                                         - LastPing;
                    if (timeout > TimeOut) {
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

