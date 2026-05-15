/*
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

package mekwars.server.campaign;

import java.io.Serial;
import java.util.Hashtable;
import java.util.Properties;

import megamek.client.Client;
import megamek.common.CriticalSlot;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.options.IOption;
import mekwars.common.CampaignData;
import mekwars.common.Equipment;
import mekwars.common.House;
import mekwars.common.Influences;
import mekwars.common.Planet;
import mekwars.common.flags.PlayerFlags;
import mekwars.common.util.UnitUtils;
import mekwars.server.MWServ;
import mekwars.server.campaign.commands.*;
import mekwars.server.campaign.commands.admin.*;
import mekwars.server.campaign.commands.helpers.HireAndMaintainHelper;
import mekwars.server.campaign.commands.helpers.HireAndRequestNewHelper;
import mekwars.server.campaign.commands.helpers.HireAndRequestUsedHelper;
import mekwars.server.campaign.commands.helpers.RemoveAndAddNoPlayHelper;
import mekwars.server.campaign.commands.leader.*;
import mekwars.server.campaign.commands.mod.*;
import mekwars.server.campaign.market.PartsMarket;
import mekwars.server.campaign.mercenaries.ContractInfo;
import mekwars.server.campaign.mercenaries.MercHouse;
import mekwars.server.campaign.operations.OperationManager;
import mekwars.server.campaign.operations.ShortOperation;
import mekwars.server.campaign.operations.newopmanager.I_OperationManager;
import mekwars.server.campaign.operations.newopmanager.NewOperationManager;
import mekwars.server.campaign.pilot.SPilotSkills;
import mekwars.server.campaign.util.*;
import mekwars.server.campaign.util.scheduler.MWScheduler;
import mekwars.server.campaign.votes.VoteManager;
import mekwars.server.dataProvider.Server;
import mekwars.server.util.AutomaticBackup;
import mekwars.server.util.MWPasswd;
import mekwars.server.util.QuirkHandler;
import mekwars.server.util.RepairTrackingThread;
import mekwars.server.util.StringUtil;
import mekwars.server.util.discord.DiscordMessageHandler;
import mekwars.server.util.rss.Feed;
import mekwars.server.util.rss.FeedMessage;

public final class CampaignMain implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = -8671163467590633378L;

    /**
     * I realized, that almost every class needs access to the current global campaign state. So I decided (after
     * consultation with McWizard) to make this back reference obsolete by introducing a public static member (Java's
     * pardon to a global variable). Although this reduces code size, complexity of code and memory footprint, this is
     * still a HACK! Java wasn't invented to step back to the old days of global variables. Object oriented coding
     * should try to minimize cross references.. But someday you gotta do what you gotta do..... Imi.
     */
    public static CampaignMain campaignMain;

    public static DefaultServerOptions defaultServerOptions;

    private final MWServ serv;
    private final Properties config = new Properties();
    private final Hashtable<String, Command> commands = new Hashtable<>();
    private final Hashtable<String, MekStatistics> mekStats = new Hashtable<>();
    private Client megaMekClient = new Client("MWServer", "None", 0);
    private CampaignData data = new CampaignData();
    private java.util.Hashtable<String, String> omniVariantMods = new java.util.Hashtable<>();

    private java.util.Hashtable<String, Equipment> blackMarketEquipmentCostTable = new java.util.Hashtable<String, Equipment>();

    private int gamesCompleted;// used by Tracker

    private int currentUnitID = 1;

    private int currentPilotID = 1;

    private TickThread TThread;

    private SliceThread SThread;

    private ImmunityThread IThread;

    private RepairTrackingThread RTT;

    private AutomaticBackup aub = new AutomaticBackup(System.currentTimeMillis());

    private Market2 market;

    private PartsMarket partsmarket;

    private VoteManager voteManager;

    private I_OperationManager opsManager;

    private java.util.Vector<ContractInfo> unresolvedContracts = new java.util.Vector<ContractInfo>(1, 1);

    private UnitCosts unitCostLists = null;

    //private TreeMap<String, String> NewsFeed = new TreeMap<String, String>();
    private Feed newsFeed = new Feed();

    private boolean isArchiving = false;

    private java.util.Random r = new java.util.Random(System.currentTimeMillis());

    private java.util.Date housePlanetDate = new java.util.Date();

    private java.util.HashMap<String, ChatRoom> chatRooms = new java.util.HashMap<String, ChatRoom>();

    /**
     * This is a hash collection of all the players that have yet to log into their houses This catch all is to keep
     * from having to load the player file over and over again. Once the player has been logged in they are removed from
     * this hash and added to the houses memory.
     */
    private java.util.Hashtable<String, mekwars.server.campaign.SPlayer> lostSouls = new java.util.Hashtable<String, mekwars.server.campaign.SPlayer>();

    private java.util.Vector<String> supportUnits = new java.util.Vector<String>();

    private PlayerFlags defaultPlayerFlags = new PlayerFlags();

    private MWScheduler scheduler;

    private ChristmasHandler christmas;

    private QuirkHandler quirkHandler;

    // CONSTRUCTOR
    public CampaignMain(server.MWServ serv) {

        campaignMain = this;
        this.serv = serv;
        defaultServerOptions = new DefaultServerOptions();
        defaultServerOptions.createDefaults();

        // make sure vital folders exist
        java.io.File f = new java.io.File("./campaign/");
        if (!f.exists()) {
            f.mkdir();
        }
        f = new java.io.File("./campaign/players/");
        if (!f.exists()) {
            f.mkdir();
        }

        /*
         * clear any cache'd unit files. these will be rebuilt later in the
         * start process. clearing @ each start ensures that updates take hold
         * properly.
         */
        java.io.File cache = new java.io.File("./data/mechfiles/units.cache");
        if (cache.exists()) {
            cache.delete();
        }

        // Try to read the config file
        try {
            config.putAll(defaultServerOptions.getServerDefaults());// load all of the defaults
            // into the config file
            // before you load in the
            // campaign stuff
            // if(!isUsingMySQL())
            config.load(new java.io.FileInputStream(this.serv.getConfigParam("CAMPAIGNCONFIG")));
            /*
             * else { if(cm.MySQL.configIsSaved()) cm.MySQL.loadConfig(config);
             * else config.load(new
             * FileInputStream(this.myServer.getConfigParam("CAMPAIGNCONFIG")));
             * }
             */

            // Right here, we're going to try to prune old cruft from the configs
            // Over the course of many years, as config options change, crap never
            // gets removed from campaignconfig.txt.  We're seeing this very badly on
            // MMNet, and probably other servers are, as well.
            java.util.Vector<String> keysToRemove = new java.util.Vector<String>();
            for (Object key : config.keySet()) {
                if (!defaultServerOptions.getServerDefaults().keySet().contains(key) &&
                          !((String) key).endsWith("RewardPointMultiplier")) {
                    MWLogger.errLog("Key " +
                                          (String) key +
                                          " does not exist in DefaultServerConfig.  Pruning from configs.");
                    keysToRemove.add((String) key);
                }
            }

            for (String key : keysToRemove) {
                config.remove(key);
            }

            mekwars.server.campaign.CampaignMain.campaignMain.saveConfigureFile(config,
                  mekwars.server.campaign.CampaignMain.campaignMain.getServer().getConfigParam("CAMPAIGNCONFIG"));
            // Now, in theory, there is no cruft for next boot.  Let's test.

        } catch (Exception ex) {
            MWLogger.errLog("Problems with loading campaign config");
            MWLogger.errLog(ex);
            defaultServerOptions.createConfig();
            try {
                config.load(new java.io.FileInputStream(this.serv.getConfigParam("CAMPAIGNCONFIG")));
            } catch (Exception ex1) {
                MWLogger.errLog("Problems with loading campaing config from defaults");
                MWLogger.errLog(ex1);
                System.exit(1);
            }
        }

        if (!getConfig("AllowedMegaMekVersion").equals("-1")) {
            getConfig().setProperty("AllowedMegaMekVersion", megamek.SuiteConstants.VERSION.toString());
        }

        defaultServerOptions.createConfig(); // save the cofig file so any missed defaults are
        // added

        /*
         * Create the auction environment/market. Notice that the new market
         * implementation does not save a .dat file. While saving the status was
         * a nice idea, it was creating dupes and NPEs after crashes.
         */

        market = new Market2();
        partsmarket = new PartsMarket();

        SPilotSkills.initializePilotSkills();
        // data.clearHouses();

        // Load & Init Data
        data = new CampaignData();

        // load megamek gameoptions;
        MWLogger.infoLog("Loading MegaMek Game Options");
        campaignMain.megaMekClient.getGame().getOptions().loadOptions();

        // Parse Terrain
        // XMLTerrainDataParser tParse =
        new XMLTerrainDataParser("./data/terrain.xml");

        if (new java.io.File("./data/advancedTerrain.xml").exists()) {
            new XMLAdvancedTerrainDataParser("./data/advancedTerrain.xml");
        }

        new XMLAdvancedTerrainDataParser("./data/advterr.xml");


        campaignMain.loadTopUnitID();
        gamesCompleted = 0;

        // Read the data from the SHouse Data File
        loadFactionData();
        loadPlanetData();

        try {
            MekwarsFileReader dis = new MekwarsFileReader("./campaign/banammo.dat");
            while (dis.ready()) {
                String line = dis.readLine();
                loadBanAmmo(line);
            }
            dis.close();
        } catch (java.io.FileNotFoundException fne) {
            MWLogger.mainLog("No banned ammo data found.");
        } catch (Exception ex) {
            MWLogger.errLog("Problems reading banned ammo data.");
        }

        // misc loads.
        campaignMain.loadOmniVariantMods();
        campaignMain.loadBlackMarketSettings();

        campaignMain.loadBannedTargetSystems();
        campaignMain.loadSupportUnitDefinitions();

        // create command hashs
        init();

        if (Boolean.parseBoolean(campaignMain.getConfig("UseCalculatedCosts"))) {
            unitCostLists = new UnitCosts();
            unitCostLists.loadUnitCosts();
            // MWLogger.errLog(unitCostLists.displayUnitCostsLists());
        }

        // Load the Mech-Statistics
        try {
            MekwarsFileReader dis = new MekwarsFileReader("./campaign/mechstat.dat");
            while (dis.ready()) {
                String line = dis.readLine();
                MekStatistics m = new MekStatistics(line);
                mekStats.put(m.getMekFileName(), m);
            }
            dis.close();
        } catch (Exception ex) {
            MWLogger.errLog("Problems reading unit statistics data");
            MWLogger.errLog(ex);
            MWLogger.mainLog("No Mech Statistic Data found");
        }

        if (Boolean.parseBoolean(getConfig("HTMLOUTPUT"))) {
            Statistics.doRanking();
        }

        // Start a VoteManager.
        voteManager = new VoteManager(this);

        /*
         * start an OperationManager. The manager loads all ops files and
         * creates necessary instances of Validators, Resolvers and other helper
         * objects as part of its construction.
         */
        createNewOpsManager();


        // Start up the HTML Sanitizer
        StringUtil.loadSanitizer();

        // Load the default player flags
        defaultPlayerFlags.loadFromDisk();

        //Load the scheduler
        scheduler = MWScheduler.getInstance();
        scheduler.start();

        // Load the Christmas Handler and set the start and end dates
        christmas = ChristmasHandler.getInstance();
        christmas.schedule();

        //@Salient for quirks
        quirkHandler = QuirkHandler.getInstance();

        // create & start a data provider
        int dataport = -1;
        try {
            dataport = Integer.parseInt(this.serv.getConfigParam("DATAPORT"));
        } catch (NumberFormatException e) {
            MWLogger.errLog("Non-number given as dataport. Defaulting to 4867.");
            MWLogger.errLog(e);
            dataport = 4867;
        } finally {
            Server dataProviderServer = new Server(data,
                  dataport,
                  this.serv.getConfigParam("SERVERIP"));
            Thread t = new Thread(dataProviderServer);
            t.start();
        }

        // start tick, slice and immunity threads
        TThread = new TickThread(this, Integer.parseInt(getConfig("TickTime")));
        TThread.start();
        SThread = new SliceThread(this, Integer.parseInt(getConfig("SliceTime")));
        SThread.start();// it slices, it dices, it chops!
        IThread = new ImmunityThread();
        IThread.start();

        // start Advanced Repair, if enabled
        isUsingAdvanceRepair();

        // finally, announce restart in news feed.
        this.addToNewsFeed("MekWars Server Started!", "Server News", "");
    }

    public void loadSupportUnitDefinitions() {
        MWLogger.mainLog("Entering loadSupportUnitDefinitions");

        java.io.File tsFile = new java.io.File("./data/supportunits.txt");
        if (!tsFile.exists()) {
            return;
        }

        java.util.Vector<String> units = new java.util.Vector<String>();
        try {
            MekwarsFileReader dis = new MekwarsFileReader(tsFile);
            while (dis.ready()) {
                String line = dis.readLine();
                line = line.trim().toLowerCase();
                if (line.startsWith("#") || line.length() < 5) {
                    continue;
                }
                if (!units.contains(line)) {
                    units.add(line);
                    MWLogger.mainLog("Adding Support Unit: " + line);
                }
            }
            dis.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        } finally {
            mekwars.server.campaign.CampaignMain.campaignMain.setSupportUnits(units);
        }
    }

    /*
     * public void saveData() { try { data.saveData(new File("campaign")); /
     * MMNetXStream xml = new MMNetXStream(new DomDriver()); for (Iterator i =
     * data.getAllHouses().iterator(); i.hasNext();) { SHouse h = (SHouse)
     * i.next(); xml.toXML(h.getMembers(), new
     * FileWriter("./campaign/members"+h.getName()+".xml")); } } catch
     * (IOException e) { MWLogger.errLog(e); } }
     */

    /**
     * Saves the current campaign state to a file system.
     */
    public void toFile() {

        try {

            // wait for the backup to finsh before you start saving files.
            while (campaignMain.isArchiving()) {
                Thread.sleep(125);
            }

            saveFactionData();
            savePlanetData();

            // Save omni variant mods
            campaignMain.saveOmniVariantMods();

            // Save Mech-Stats
            java.io.FileOutputStream out = new java.io.FileOutputStream("./campaign/mechstat.dat");
            java.io.PrintStream p = new java.io.PrintStream(out);
            for (MekStatistics currStats : mekStats.values()) {
                p.println(currStats.toString());
            }
            p.close();
            out.close();

            try {
                // Save the Readable Mechstats

                out = new java.io.FileOutputStream(getConfig("MechstatPath"));
                p = new java.io.PrintStream(out);
                p.println(
                      "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"format.css\"><style type=\"text/css\"></style></head><body><font face=\"Verdana, Arial, Helvetica, sans-serif\">");
                for (int i = 0; i <= 3; i++) {
                    p.println(Statistics.doGetMechStats(i));
                    p.println("<br>");
                }
                p.println("</font></body></style></html>");
                p.close();
                out.close();
            } catch (java.io.FileNotFoundException efnf) {
                // ignore
            }

            MWLogger.mainLog("STATUS SAVED");

        } catch (Exception ex) {
            MWLogger.errLog("Problems saving configuration to file");
            MWLogger.errLog(ex);
        }
    }

    public double getDoubleConfig(String key) {
        try {
            return Double.parseDouble(campaignMain.getConfig(key));
        } catch (Exception ex) {
            return -1;
        }
    }

    public String getConfig(String key) {

        if (config.getProperty(key) == null) {
            if (defaultServerOptions.getServerDefaults().getProperty(key) == null) {
                MWLogger.mainLog("You're missing the config variable: " + key + " in campaignconfig!");
                MWLogger.errLog("You're missing the config variable: " + key + " in campaignconfig! returning -1");
                return "-1";
            }
            // else
            return defaultServerOptions.getServerDefaults().getProperty(key).trim();
        }
        return config.getProperty(key).trim();
    }

    public float getFloatConfig(String key) {
        try {
            return Float.parseFloat(campaignMain.getConfig(key));
        } catch (Exception ex) {
            return -1;
        }
    }

    public void createNewOpsManager() {
        if (mekwars.server.campaign.CampaignMain.campaignMain.getBooleanConfig("UseNewOpManager")) {
            opsManager = new NewOperationManager();
        } else {
            opsManager = new OperationManager();
        }
    }

    public void fromUser(String text, String Username) {

        // if you don't have a client signon to the server then you do not get
        // to send commands
        if (mekwars.server.campaign.CampaignMain.campaignMain.getServer().getClient(Username) == null) {
            return;
        }

        /*
         * Only a few commands should be accepted from a logged out player.
         * Unless the command is enroll, login, or register, return without
         * further processing. Register won't succeed unless player has a
         * campaign account.
         */
        if (!isLoggedIn(Username) &&
                  (text.toUpperCase().indexOf("ENROLL") == -1) &&
                  (text.toUpperCase().indexOf("LOGIN") == -1) &&
                  (text.toUpperCase().indexOf("REGISTER") == -1) &&
                  (text.toUpperCase().indexOf("GETSERVERCONFIGS") == -1) &&
                  (text.toUpperCase().indexOf("SETCLIENTVERSION") == -1) &&
                  (text.toUpperCase().indexOf("GETSAVEDMAIL") == -1)) {
            toUser("You are not logged in!", Username, true);
            return;
        }

        text = text.substring(2);
        // Date d = new Date(System.currentTimeMillis());
        // MWLogger.mainLog(d + ":" + "Command from User " + Username
        // + ": "
        // + text);
        // MWLogger.cmdLog(Username + ": " + text);

        java.util.StringTokenizer ST = new java.util.StringTokenizer(text, "#");
        if (ST.hasMoreElements()) {

            // check command type
            String task = ((String) ST.nextElement()).toUpperCase();

            // idle checker omit pong command
            if (!task.equals("PONG")) {
                try {
                    this.getPlayer(Username).setLastTimeCommandSent(System.currentTimeMillis());
                } catch (Exception ex) {
                    if (!Username.startsWith("[Dedicated]")) {
                        // commands
                        MWLogger.errLog("Command received from a null player (" + Username + ")?");
                    }
                }
            }

            // New Method (much cleaner)
            if (commands.get(task) != null) {

                // log non-chat commands
                if (task.equals("MAIL") ||
                          task.equals("HOUSEMAIL") ||
                          task.equals("HM") ||
                          task.equals("MODERATORMAIL") ||
                          task.equals("MM") ||
                          task.equals("INCHARACTER") ||
                          task.equals("IC")) {
                    // do nothing
                } else {
                    MWLogger.cmdLog(Username + ": " + text);
                }

                Command c = commands.get(task);
                try {
                    c.process(ST, Username);
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                    mekwars.server.campaign.CampaignMain.campaignMain.toUser("AM:Invalid Syntax: /" +
                                                                                   task +
                                                                                   " " +
                                                                                   c.getSyntax(),
                          Username);
                }
                return;
            }// if the text is a command

        }// end while(more elements)
    }// end fromUser

    public server.MWServ getServer() {
        return serv;
    }

    public boolean isLoggedIn(String Username) {

        // always treat deds as logged in
        if (Username.startsWith("[Dedicated]")) {
            return true;
        }

        /*
         * search all houses, all states, for user with this name. the hash
         * searches are O(1), which means this is actually much faster than the
         * old MMNET way, which was to try a .equals() on every player's name.
         */
        String lowerName = Username.toLowerCase();
        for (House vh : data.getAllHouses()) {
            SHouse h = (SHouse) vh;
            if (h.getReservePlayers().containsKey(lowerName)) {
                return true;
            }
            if (h.getActivePlayers().containsKey(lowerName)) {
                return true;
            }
            if (h.getFightingPlayers().containsKey(lowerName)) {
                return true;
            }
        }

        // we couldnt find the player. return false.
        return false;
    }

    public void toUser(String txt, String Username, boolean isChat) {
        if (isChat) {
            serv.fromCampaignMod("CH|" + txt, Username);
        } else {
            serv.fromCampaignMod(txt, Username);
        }
    }

    /**
     * Get an SPlayer, by name. This searches the reserve, active and fighting hashes of all factions until the player
     * is found or factions are exhausted. If a player is not in a faction, check the to-save hash. Its entirely
     * possible that the player is already in memory, but logged out and is awaiting a purge. If no matching player is
     * found online, the server will attempt to read one in from a text file. If even this fails, a null is returned.
     * NOTE: A player brought into memory using getPlayer is not automatically logged into his house. Temporary loads
     * (ex: commands targetted at offline players) will put the player directly into the save queue, as if he was logged
     * out. This is why the save queue is/must be searched prior to* reading the text file.
     */
    public SPlayer getPlayer(String pName) {
        return getPlayer(pName, true, false);
    }

    public void toUser(String txt, String Username) {
        toUser(txt, Username, true);
    }

    public SPlayer getPlayer(String pName, boolean save, boolean mute) {

        // Fix for Draw games.
        if (pName.equalsIgnoreCase("DRAW") || pName.toUpperCase().startsWith("DRAW#")) {
            return null;
        }

        if (lostSouls.containsKey(pName.toLowerCase())) {
            return lostSouls.get(pName.toLowerCase());
        }

        // look for faction players
        SPlayer result = null;
        for (House vh : data.getAllHouses()) {
            SHouse h = (SHouse) vh;
            result = h.getPlayer(pName);
            if (result != null) {
                // MWLogger.debugLog(pName+" Found in house data");
                return result;
            }
        }

        /*
         * no online player, so try to read from a file.
         */

        result = loadPlayerFile(pName, false, mute);

        if (result != null) {
            lostSouls.put(pName.toLowerCase(), result);
        }

        return result;
    }

    /**
     * Method which loads a player file from text. THIS SHOULD NOT BE USED. CampaignMain.getPlayer(String name) will
     * check to see if a player is already in memory, and then call this loader if the player needs to be brought in
     * from text. If you need to get a player, always use .getPlayer(String name) instead. A player who is loaded is put
     * into the CampaignMain
     */
    private SPlayer loadPlayerFile(String name, boolean explicitName, boolean mute) {

        if (!name.startsWith("[Dedicated]") && !name.startsWith("War Bot")) {

            MekwarsFileReader dis = null;

            try {
                // log the load attempt & create readers
                MWLogger.mainLog("Loading pfile for: " + name);

                java.io.File pFile = null;
                if (explicitName) {
                    pFile = new java.io.File("./campaign/players/" + name);
                } else {
                    pFile = new java.io.File("./campaign/players/" + name.toLowerCase() + ".dat");
                }

                if (!pFile.exists()) {
                    return null;
                }

                dis = new MekwarsFileReader(pFile);

                // create player from string read by dis
                SPlayer p = new SPlayer();
                String pString = dis.readLine();

                if (pString == null) {
                    return null;
                }

                p.fromString(pString);

                return p;
            } catch (java.io.FileNotFoundException fnf) {

                if (!name.toLowerCase().startsWith("nobody") &&
                          !name.equals("SERVER") &&
                          !name.toLowerCase().startsWith("war bot") &&
                          !name.toLowerCase().startsWith("[dedicated]") &&
                          !mute) {
                    MWLogger.errLog("could not find a pfile for " + name);
                    MWLogger.debugLog(fnf);
                    MWLogger.debugLog("could not find a pfile for " + name);
                }
                return null;
            } catch (Exception ex) {
                if (!mute) {
                    MWLogger.errLog(ex);
                    MWLogger.errLog("Unable to load pfile for " + name);
                }
                return null;
            } finally {
                // close the streams and return player
                try {
                    if (dis != null) {
                        dis.close();
                    }
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            }
        }

        return null;

    }

    public SPlanet getPlanetFromPartialString(String PlanetName, String Username) {

        // store matches so we can tell player if there's more than one
        int numMatches = 0;
        SPlanet theMatch = null;

        for (Planet currP : data.getAllPlanets()) {
            SPlanet p = (SPlanet) currP;

            // exact match
            if (p.getName().equals(PlanetName)) {
                return p;
            }

            // store all matches
            if (p.getName().startsWith(PlanetName)) {
                theMatch = p;
                numMatches++;
            }
        }

        // too many matches
        if (numMatches > 1) {
            if (Username != null) {
                toUser("\"" + PlanetName + "\" is not unique [" + numMatches + " matches]. Please be more specific.",
                      Username);
            }
            return null;
        }

        if (numMatches == 0) {
            if (Username != null) {
                toUser("Couldn't find a planet whose name begins with \"" + PlanetName + "\". Try again.",
                      Username,
                      true);
            }
            return null;
        }

        // only one match! send it back.
        return theMatch;
    }

    public void doSendHouseMail(SHouse h, String Username, String text) {

        // send the text to all logged in players
        text = "(Housemail)" + Username + ":" + text;
        this.doSendToAllOnlinePlayers(h, text, true);

        // then add it to the faction's log
        MWLogger.factionLog(h.getName(), text.substring(11));
    }

    /**
     * Send a bit of text to all players in a given faction. Can be chat, or a command/message.
     */
    public void doSendToAllOnlinePlayers(SHouse h, String text, boolean isChat) {

        for (String currName : h.getReservePlayers().keySet()) {
            this.toUser(text, currName, isChat);
        }

        for (String currName : h.getActivePlayers().keySet()) {
            this.toUser(text, currName, isChat);
        }

        for (String currName : h.getFightingPlayers().keySet()) {
            this.toUser(text, currName, isChat);
        }
    }

    /**
     * Loop through all online players (all houses, all three duty modes) and send mail to those players who are mods.
     */
    public void doSendModMail(String Username, String text) {

        int sendCommandLevel = 0;
        int commandLevel = mekwars.server.campaign.CampaignMain.campaignMain.getServerCommands()
                                 .get("MM")
                                 .getExecutionLevel();
        int userLevel = 0;
        try {
            if (Username.equalsIgnoreCase("NOTE")) {
                if (!mekwars.server.campaign.CampaignMain.campaignMain.getBooleanConfig(
                      "AllowLowerLevelUsersToSeeUpperLevelUsersDoings")) {
                    sendCommandLevel = mekwars.server.campaign.CampaignMain.campaignMain.getServer()
                                             .getUserLevel(text.substring(0, text.indexOf(" ")).trim());
                } else {
                    sendCommandLevel = 100;
                }
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

        // Note it to the logs
        MWLogger.modLog(Username + ": " + text);
        text = "(Moderator Mail) " + Username + ": " + text;
        for (House vh : data.getAllHouses()) {
            SHouse h = (SHouse) vh;

            for (String currName : h.getReservePlayers().keySet()) {
                userLevel = mekwars.server.campaign.CampaignMain.campaignMain.getServer().getUserLevel(currName);
                if (userLevel >= commandLevel && userLevel >= sendCommandLevel) {
                    this.toUser(text, currName, true);
                }
            }
            for (String currName : h.getActivePlayers().keySet()) {
                userLevel = mekwars.server.campaign.CampaignMain.campaignMain.getServer().getUserLevel(currName);
                if (userLevel >= commandLevel && userLevel >= sendCommandLevel) {
                    this.toUser(text, currName, true);
                }
            }
            for (String currName : h.getFightingPlayers().keySet()) {
                userLevel = mekwars.server.campaign.CampaignMain.campaignMain.getServer().getUserLevel(currName);
                if (userLevel >= commandLevel && userLevel >= sendCommandLevel) {
                    this.toUser(text, currName, true);
                }
            }
        }
    }

    /**
     * After an error, loop through all online players and send text of the error to anyone who has modmail access.
     */
    public void doSendErrLog(String text) {
        text = "(Error Log): " + text;
        for (House vh : data.getAllHouses()) {
            SHouse h = (SHouse) vh;

            for (String currName : h.getReservePlayers().keySet()) {
                Command command = mekwars.server.campaign.CampaignMain.campaignMain.getServerCommands().get("MM");
                if (mekwars.server.campaign.CampaignMain.campaignMain.getServer().getUserLevel(currName) >=
                          command.getExecutionLevel()) {
                    this.toUser(text, currName, true);
                }
            }
            for (String currName : h.getActivePlayers().keySet()) {
                Command command = mekwars.server.campaign.CampaignMain.campaignMain.getServerCommands().get("MM");
                if (mekwars.server.campaign.CampaignMain.campaignMain.getServer().getUserLevel(currName) >=
                          command.getExecutionLevel()) {
                    this.toUser(text, currName, true);
                }
            }
            for (String currName : h.getFightingPlayers().keySet()) {
                Command command = mekwars.server.campaign.CampaignMain.campaignMain.getServerCommands().get("MM");
                if (mekwars.server.campaign.CampaignMain.campaignMain.getServer().getUserLevel(currName) >=
                          command.getExecutionLevel()) {
                    this.toUser(text, currName, true);
                }
            }

        }
    }

    public java.util.Hashtable<String, Command> getServerCommands() {
        return commands;
    }

    /**
     * @return Returns the mechStats.
     */
    public java.util.Hashtable<String, MekStatistics> getMekStats() {
        return mekStats;
    }

    public void doProcessAutomaticReport(String s, String Username) {

        /*
         * Format should be: Winner#DE#...Unit...#GY#...Units...#AL#...Units...
         */

        /*
         * return if the Username isn't listed if (s.indexOf(Username) == -1)
         * return;
         */

        // Now adays deds and Hosts actually report the game not the players.
        // So we need to check the winner if the winner is NULL due to a DRAW
        // Then check the name of the first player in the report string which
        // is the second element in a * delimited string
        // -Torren
        java.util.TreeSet<String> players = new java.util.TreeSet<String>();

        java.util.StringTokenizer report = new java.util.StringTokenizer(s, "#");
        SPlayer reporter = this.getPlayer(report.nextToken());

        while (report.hasMoreElements()) {
            java.util.StringTokenizer report2 = new java.util.StringTokenizer(report.nextToken(), "*");

            String test = report2.nextToken();

            // dont bother trying to process auto army or MechWarriors.
            if (test.equals("MW") || test.equals("-1")) {
                continue;
            }

            // keep parsing until we find a players name!
            while (report2.hasMoreTokens()) {
                SPlayer player = this.getPlayer(report2.nextToken(), false, true);
                if (player != null) {
                    if (!players.contains(player.getName().toLowerCase())) {
                        players.add(player.getName().toLowerCase());
                    }

                    if (reporter == null) {
                        reporter = player;
                    }
                    break;
                }
            }
        }

        if (reporter == null) {
            MWLogger.errLog("reporter is null! " + s);
            return;
        }

        /*
         * If the player isn't in any ShortOperations, he obviously has no
         * standing to report. Tasks code used to sort winners and losers at
         * this point, but we handle that in the ShortResovler.
         */
        ShortOperation so = getOpsManager().getShortOpForPlayer(reporter);
        if (so == null) {
            return;
        }

        if (!so.validatePlayers(players)) {
            MWLogger.errLog("Unable to validate all players for: " + s);
            return;
        }

        if (so.hasPlayer(reporter)) {
            Operation o = getOpsManager().getOperation(so.getName());
            getOpsManager().resolveShortAttack(o, so, s);
            return;
        }

    }// end doProcessAutomaticReport

    /**
     * Method which pre-processes auto-disconnection info updates. Clients connected to a host send these updates when a
     * unit is removed from play - this does not necessarily mean the unit is dead. It could have fled or been pushed
     * from the field, etc. ClientThread weeds out observers client side.
     */
    public void addInProgressUpdate(String s, String Username) {

        // Return if user isn't an SPlayer.
        SPlayer reporter = this.getPlayer(Username);
        if (reporter == null) {
            return;
        }

        // If the reporting player isnt in a game, toss it.
        ShortOperation so = getOpsManager().getShortOpForPlayer(reporter);
        if (so == null) {
            return;
        }

        // If the short operation has more than two players
        if (so.getAllPlayerNames().size() > 2) {
            return;
        }

        // now that we have a game for the player, pass the destruction
        // string along to the short operation for handling.
        so.addInProgressUpdate(s);
    }

    /**
     * Method that allows other classes to access the opsManager instance via the static CampaignMain.
     */
    public I_OperationManager getOpsManager() {
        return opsManager;
    }

    public java.util.Vector<MercHouse> getMercHouses() {
        java.util.Vector<MercHouse> result = new java.util.Vector<MercHouse>(1, 1);
        for (House currH : data.getAllHouses()) {
            SHouse sh = (SHouse) currH;
            if (sh.isMercHouse()) {
                result.add((MercHouse) currH);
            }
        }
        result.trimToSize();
        return result;
    }

    /**
     * Login a player to the server. Called by login, enroll command and (most commonly) SignOn. If we find that the
     * player is already in a faction, leave things as they are. If the player is not present in a house status
     * hashtable, use this.getPlayer() to check the save queue and, if necessary, read the player in from text. Any
     * player who logs in should be put into the Reserve list. If he is reconnecting, the SignOn command will pass him
     * through a reconnection check and clean up the various Operations threads, etc. Players with no account (null
     * this.getPlayer()) are also handled in SignOn, but we need to check there here as well in case the player ignores
     * the SignOn click-through and attempts to log in anyway.
     */
    public void doLoginPlayer(String Username) {

        // Loop through the houses and make sure he's not already logged in
        for (House vh : data.getAllHouses()) {
            SHouse currH = (SHouse) vh;
            if (currH.isLoggedIntoFaction(Username)) {
                toUser("You are already logged in to " + currH.getColoredNameAsLink() + ".", Username, true);
                return;
            }
        }

        /*
         * He's not in a house. lets look in the save queue and pfiles. If the
         * getPlayer is null, extend an invitation to enroll (same as in
         * SignOn.java, for uniformity).
         */
        SPlayer toLogin = this.getPlayer(Username);

        if (toLogin == null) {
            this.toUser("<font color=\"navy\"><br>---<br>" +
                              "It appears that you haven't signed up for this server's " +
                              "campaign.<br><a href=\"MEKWARS/c enroll\">Click here to get " +
                              "started.</a><br>---<br></font>", Username, true);
            return;
        }

        /*
         * Now that we have a player who needs to be placed in a house. The
         * player holds a faction name in his .dat file, which is used to
         * bootstrap a link to the SHouse into SPlayer at load time. We may
         * assume that this data is valid (if not, we have much deeper problems
         * with the data we're using here) and put the player into the
         * approperiate faction. Note that the old MMNET code looped through the
         * houses until it found one that purported to "own" the player. This is
         * a pretty dramatic reversal of process, and not as OO-appropriate :-(
         */
        SHouse loginHouse = toLogin.getMyHouse();
        if (loginHouse == null) {
            toUser("    . Major problem. Report ASAP.", Username, true);
            mekwars.server.campaign.CampaignMain.campaignMain.doSendModMail("NOTE",
                  toLogin.getName() +
                        " has a null login faction! Moving to " +
                        mekwars.server.campaign.CampaignMain.campaignMain.getConfig("NewbieHouseName"));
            loginHouse = mekwars.server.campaign.CampaignMain.campaignMain.getHouseFromPartialString(
                  mekwars.server.campaign.CampaignMain.campaignMain.getConfig("NewbieHouseName"));
            toLogin.setMyHouse(loginHouse);
        }
        String s = loginHouse.doLogin(toLogin);

        /*
         * String returned from house includes motd, etc. The house performs one
         * last-ditch check to see if the player is already in the house and may
         * return a null if it finds the player present, despite the failure of
         * all of our previous location attempts.
         */
        if (s != null) {

            // send the login message/MOTD
            toUser(s, Username, true);

            // Send the player his basic info (units, techs, etc)
            mekwars.server.campaign.CampaignMain.campaignMain.toUser("PS|" + toLogin.toString(true), Username, false);

            if (isUsingAdvanceRepair()) {

                if (!toLogin.hasRepairingUnits()) {
                    mekwars.server.campaign.CampaignMain.campaignMain.toUser("PL|UTT|" + toLogin.totalTechsToString(),
                          Username,
                          false);
                    mekwars.server.campaign.CampaignMain.campaignMain.toUser("PL|UAT|" + toLogin.totalTechsToString(),
                          Username,
                          false);
                } else {
                    mekwars.server.campaign.CampaignMain.campaignMain.toUser("PL|UTT|" + toLogin.totalTechsToString(),
                          Username,
                          false);
                    mekwars.server.campaign.CampaignMain.campaignMain.toUser("PL|UAT|" +
                                                                                   toLogin.availableTechsToString(),
                          Username,
                          false);
                }
            }

            /*
             * Player is logging in so clear their armies opps and send the
             * player his army eligibilities.
             */
            for (SArmy currA : toLogin.getArmies()) {
                currA.getLegalOperations().clear();
                mekwars.server.campaign.CampaignMain.campaignMain.getOpsManager().checkOperations(currA, false);
            }

            // send all currently online players to the one logging in
            StringBuilder result = new StringBuilder("PI|PL|");
            for (House vh : data.getAllHouses()) {
                SHouse currH = (SHouse) vh;
                for (SPlayer currP : currH.getReservePlayers().values()) {
                    result.append(getPlayerUpdateString(currP) + "|");
                }

                for (SPlayer currP : currH.getActivePlayers().values()) {
                    result.append(getPlayerUpdateString(currP) + "|");
                }

                for (SPlayer currP : currH.getFightingPlayers().values()) {
                    result.append(getPlayerUpdateString(currP) + "|");
                }
            }
            toUser(result.toString(), Username, false);

            // Add the logging in player to everyone who is already online
            this.doSendToAllOnlinePlayers("PI|DA|" + getPlayerUpdateString(toLogin), false);

            /*
             * Once the player is logged in, set his last-command-sent to the
             * current time. This stops the idle-kicking code from immediately
             * logging out players who've just come online and not yet sent any
             * commands.
             */
            toLogin.setLastTimeCommandSent(System.currentTimeMillis());
            toLogin.setLastOnline(System.currentTimeMillis());

            /*
             * Check if Staff Member and send MMOTD if so.
             */
            if (mekwars.server.campaign.CampaignMain.campaignMain.getServer().isModerator(Username)) {
                mekwars.server.campaign.CampaignMain.campaignMain.toUser("(Moderator Mail) Mod MOTD: " +
                                                                               mekwars.server.campaign.CampaignMain.campaignMain.getConfig(
                                                                                     "MMOTD"), Username);
            }

            /*
             * INCREDIBLY BAD HACK! As player's sign into factions, get an IP
             * and add it to the logger. With the demise of nfc.log (removed
             * from NFC2, which was grafted into MekWars), there is a need for a
             * grepable iplog.0 to search for double accounts and re-
             * entering/ban circumventing players. Despite the heinous way we
             * draw the IP, this should work. @urgru 1.29.06 :-(
             */
            MWLogger.ipLog("Name: " +
                                 Username +
                                 " IP: " +
                                 mekwars.server.campaign.CampaignMain.campaignMain.getServer().getIP(Username));
            mekwars.server.campaign.CampaignMain.campaignMain.toUser("PL|SUD|1", Username, false);
            mekwars.server.campaign.CampaignMain.campaignMain.toUser("PL|SHP|" + toLogin.buildHangarPenaltyString(),
                  Username,
                  false);

            // Send him the Tick Counter
            mekwars.server.campaign.CampaignMain.campaignMain.toUser("CC|NT|" +
                                                                           TThread.getRemainingSleepTime() +
                                                                           "|" +
                                                                           false,
                  Username,
                  false);

            // Check for Christmas
            if (ChristmasHandler.getInstance().isItChristmas()) {
                // Check if the user has received his Christmas Gifts
                if (!ChristmasHandler.getInstance().userHasReceivedGifts(Username)) {
                    // He needs his presents!!!
                    ChristmasHandler.getInstance().sendChristmasGifts(this.getPlayer(Username));
                } else {
                    // No presents for you!
                    // CampaignMain.cm.toUser("AM:You have already received presents", Username, true);
                }
            }

        }
    }// end CampaignMain.doLogin(String userName)

    /**
     * Log a player out of the campaign. The CampaignMain portion of logout is markedly simpler than login. All of the
     * more complex code (like chickening and disconnection thread spinning) is dealt with in SHouse. Note that all
     * players who log out are inserted into the savePlayer hash for removal. this.getPlayer() will retreive the memory
     * resident SPlayer from the save queue if the player returns before the purge.
     */

    public void doLogoutPlayer(
          String name) {   //start Baruk Khazad! 20151110   created method so all old doLogoutPlayer calls will continue to work without need for change
        doLogoutPlayer(name, true);
    }

    public void doLogoutPlayer(String name,
          Boolean bSavePlayerOrNot) { //Baruk Khazad! 20151110   added method parameter bSavePlayerOrNot to allow for command.DeleteAccount to skip the SavePlayer call

        // if the name is null or blank, return.
        if (name == null || name.trim().length() == 0) {
            return;
        }

        // if there is not player with the given name, return
        SPlayer toLogout = this.getPlayer(name);
        if (toLogout == null) {
            return;
        }

        /*
         * double check to make sure the SPlayer object does not reside in the
         * lost Souls hash this is incase someone connected but never logged
         * into thier house or never registered and enrolled.
         */
        releaseLostSoul(name);
        // set save, then log the player out of his house
        //start Baruk Khazad! 20151110  put IF wrapper around setSave() so deleted players can be told to logout without being saved(which basically recreates their account
        if (bSavePlayerOrNot) {
            toLogout.setSave();
        }
        //end Baruk Khazad! 20151110
        toLogout.getMyHouse().doLogout(toLogout);// hacky.

        // clear the addon and send the new logged out status to all players
        this.doSendToAllOnlinePlayers("PI|CS|" + name + "|" + SPlayer.STATUS_LOGGEDOUT, false);
        toUser("[*] You've logged out of the campaign.", name, true);
    }

    /**
     * This sends status updates of Player p to all players
     *
     * @param p
     */
    public void sendPlayerStatusUpdate(SPlayer p, boolean sendToAll) {

        // get the player's actual status
        int realStatus = p.getDutyStatus();
        int sendStatus = realStatus;

        // if obfuscating active/deactive status, change sendstatus
        if (realStatus == SPlayer.STATUS_RESERVE && Boolean.parseBoolean(getConfig("HideActiveStatus"))) {
            sendStatus = SPlayer.STATUS_ACTIVE;
        }

        // send the obfuscated status to everyone, and real status to player
        if (sendToAll) {
            this.doSendToAllOnlinePlayers("PI|CS|" + p.getName() + "|" + sendStatus, false);
        }
        this.toUser("CS|" + realStatus, p.getName(), false);
    }

    /**
     * Send a bit of text to all players who are currently online. Can be chat, or a command/message.
     */
    public void doSendToAllOnlinePlayers(String text, boolean isChat) {

        for (House vh : data.getAllHouses()) {
            SHouse h = (SHouse) vh;
            for (String currName : h.getReservePlayers().keySet()) {
                this.toUser(text, currName, isChat);
            }

            for (String currName : h.getActivePlayers().keySet()) {
                this.toUser(text, currName, isChat);
            }

            for (String currName : h.getFightingPlayers().keySet()) {
                this.toUser(text, currName, isChat);
            }
        }
    }

    public void init() {
        MWLogger.modLog("SERVER STARTED");

        // Fill the commands Table
        commands.put("ACCEPTATTACKFROMRESERVE", new AcceptAttackFromReserveCommand());
        commands.put("ACCEPTCONTRACT", new AcceptContractCommand());
        commands.put("ACTIVATE", new ActivateCommand());
        commands.put("ADDLEADER", new AddLeaderCommand());
        commands.put("ADDOMNIVARIANTMOD", new AddOmniVariantModCommand());
        commands.put("ADDPARTS", new AddPartsCommand());
        commands.put("ADDSONG", new AddSongCommand());
        commands.put("ADDTRAIT", new AddTraitCommand());
        commands.put("ADMINADDSERVEROPFLAGS", new AdminAddServerOpFlagsCommand());
        commands.put("ADMINALLOWHOUSEDEFECTION", new AdminAllowHouseDefectionCommand());
        commands.put("ADMINCALCULATEHOUSERANKINGS", new AdminCalculateHouseRankingsCommand());
        commands.put("ADMINCHANGEFACTIONCONFIG", new AdminChangeFactionConfigCommand());
        commands.put("ADMINCHANGEPLANETOWNER", new AdminChangePlanetOwnerCommand());
        commands.put("ADMINCHANGESERVERCONFIG", new AdminChangeServerConfigCommand());
        commands.put("ADMINCREATEFACTION", new AdminCreateFactionCommand());
        commands.put("ADMINCREATEPLANET", new AdminCreatePlanetCommand());
        commands.put("ADMINCREATEFACTORY", new AdminCreateFactoryCommand());
        commands.put("ADMINCREATESOLARIS", new AdminCreateSolarisCommand());
        commands.put("ADMINCREATETERRAIN", new AdminCreateTerrainCommand());
        commands.put("ADMINDESTROYFACTION", new AdminDestroyFactionCommand());
        commands.put("ADMINDESTROYFACTORY", new AdminDestroyFactoryCommand());
        commands.put("ADMINDESTROYPLANET", new AdminDestroyPlanetCommand());
        commands.put("ADMINDESTROYTERRAIN", new AdminDestroyTerrainCommand());
        commands.put("ADMINDONATE", new AdminDonateCommand());
        commands.put("ADMINEXCHANGEPLANETOWNERSHIP", new AdminExchangePlanetOwnershipCommand());
        commands.put("ADMINGETUNITCOMPONENTS", new AdminGetUnitComponentsCommand());
        commands.put("ADMINGRANTCOMPONENTS", new AdminGrantComponentsCommand());
        commands.put("ADMINHOUSEPILOTS", new AdminHousePilotsCommand());
        commands.put("ADMINHOUSESTATUS", new AdminHouseStatusCommand());
        commands.put("ADMINLOCKCAMPAIGN", new AdminLockCampaignCommand());
        commands.put("ADMINLOCKFACTORY", new AdminLockFactoryCommand());
        commands.put("ADMINLISTANDREMOVEOMG", new AdminListAndRemoveOMGCommand());
        commands.put("ADMINLISTHOUSEBANNEDAMMO", new AdminListHouseBannedAmmoCommand());
        commands.put("ADMINLISTSERVERBANNEDAMMO", new AdminListServerBannedAmmoCommand());
        commands.put("ADMINMOVEPLANET", new AdminMovePlanetCommand());
        commands.put("ADMINPASSWORD", new AdminPasswordCommand());
        commands.put("ADMINPLAYERSTATUS", new AdminPlayerStatusCommand());
        commands.put("ADMINPURGEHOUSEBAYS", new AdminPurgeHouseBaysCommand());
        commands.put("ADMINPURGEHOUSECONFIGS", new AdminPurgeHouseConfigsCommand());
        commands.put("ADMINRANDOMLYSETPLANETPRODUCTION", new AdminRandomlySetPlanetProductionCommand());
        commands.put("ADMINRECALCHANGARBVMC", new AdminRecalcHangarBvCommandMC());  //@salient
        commands.put("ADMINRELOADHOUSECONFIGS", new AdminReloadHouseConfigsCommand());
        commands.put("ADMINRELOADHTMLSANITIZERCONFIGS", new AdminReloadHTMLSanitizerConfigsCommand());
        commands.put("ADMINRELOADSUPPORTUNITS", new AdminReloadSupportUnitsCommand());
        commands.put("ADMINREMOVEALLFACTORIES", new AdminRemoveAllFactoriesCommand());
        commands.put("ADMINREMOVEALLTERRAIN", new AdminRemoveAllTerrainCommand());
        commands.put("ADMINREMOVEPLANETOWNERSHIP", new AdminRemovePlanetOwnershipCommand());
        commands.put("ADMINREMOVESERVEROPFLAGS", new AdminRemoveServerOpFlagsCommand());
        commands.put("ADMINREMOVEUNITSONMARKET", new AdminRemoveUnitsOnMarketCommand());
        commands.put("ADMINRENAMEPLANET", new AdminRenamePlanetCommand());
        commands.put("ADMINREQUESTBUILDTABLE", new AdminRequestBuildTableCommand());
        commands.put("ADMINRESETFACTIONCOMPONENTS", new AdminResetFactionComponentsCommand());
        commands.put("ADMINRESETHOUSERANKINGS", new AdminResetHouseRankingsCommand());
        commands.put("ADMINRESETPLAYER", new AdminResetPlayerCommand());
        commands.put("ADMINRESTARTTRACKERTHREAD", new AdminRestartTrackerThreadCommand());
        commands.put("ADMINRETURNPLANETSTOORIGINALOWNERS", new AdminReturnPlanetsToOriginalOwnersCommand());
        commands.put("ADMINSAVE", new AdminSaveCommand());
        commands.put("ADMINSAVEBLACKMARKETCONFIGS", new AdminSaveBlackMarketConfigsCommand());
        commands.put("ADMINSAVECOMMANDLEVELS", new AdminSaveCommandLevelsCommand());
        commands.put("ADMINSAVEFACTIONCONFIGS", new AdminSaveFactionConfigsCommand());
        commands.put("ADMINSAVEPLANETSTOXML", new AdminSavePlanetsToXMLCommand());
        commands.put("ADMINSAVESERVERCONFIGS", new AdminSaveServerConfigsCommand());
        commands.put("ADMINSETBLACKMARKETSETTING", new AdminSetBlackMarketSettingCommand());
        commands.put("ADMINSETCOMMANDLEVEL", new AdminSetCommandLevelCommand());
        commands.put("ADMINSETHOMEWORLD", new AdminSetHomeWorldCommand());
        commands.put("ADMINSETHOUSEABBREVIATION", new AdminSetHouseAbbreviationCommand());
        commands.put("ADMINSETHOUSEFLUFILE", new AdminSetHouseFluFileCommand());
        commands.put("ADMINSETHOUSEPLAYERCOLOR", new AdminSetHousePlayerColorCommand());
        commands.put("ADMINSETHOUSETECHLEVEL", new AdminSetHouseTechLevelCommand());
        commands.put("ADMINSETPLANETBOARDSIZE", new AdminSetPlanetBoardSizeCommand());
        commands.put("ADMINSETPLANETGRAVITY", new AdminSetPlanetGravityCommand());
        commands.put("ADMINSETPLANETOPFLAGS", new AdminSetPlanetOpFlagsCommand());
        commands.put("ADMINSETPLANETORIGINALOWNER", new AdminSetPlanetOriginalOwnerCommand());
        commands.put("ADMINSETPLANETMAPSIZE", new AdminSetPlanetMapSizeCommand());
        commands.put("ADMINSETPLANETTEMPERATURE", new AdminSetPlanetTemperatureCommand());
        commands.put("ADMINSETPLANETVACUUM", new AdminSetPlanetVacuumCommand());
        commands.put("ADMINSETHOUSEAMMOBAN", new AdminSetHouseAmmoBanCommand());
        commands.put("ADMINSETSERVERAMMOBAN", new AdminSetServerAmmoBanCommand());
        commands.put("ADMINSETSERVERTARGETBAN", new AdminSetServerTargetBanCommand());
        commands.put("ADMINSCRAP", new AdminScrapCommand());
        commands.put("ADMINSPOOF", new AdminSpoofCommand());
        commands.put("ADMINTERMINATEALL", new AdminTerminateAllCommand());
        commands.put("ADMINTRANSFER", new AdminTransferCommand());
        commands.put("ADMINUNLOCKCAMPAIGN", new AdminUnlockCampaignCommand());
        commands.put("ADMINUNLOCKUNITSMC", new AdminUnlockUnitsCommandMC());
        commands.put("ADMINUPDATECLIENTPARAM", new AdminUpdateClientParamCommand());
        commands.put("ADMINUPDATEPLANETOWNERSHIP", new AdminUpdatePlanetOwnershipCommand());
        commands.put("ADMINUPDATEDEFAULTPLAYERFLAGS", new AdminUpdateDefaultPlayerFlagsCommand());
        commands.put("ADMINUPLOADBUILDTABLE", new AdminUploadBuildTableCommand());
        commands.put("ADMINVIEWLOG", new AdminViewLogCommand());
        commands.put("ALL", new ArmyLowerLimiterCommand());
        commands.put("ANNOUNCE", new AnnounceCommand());
        commands.put("AOFS", new ArmyOpForceSizeCommand());
        commands.put("AUL", new ArmyUpperLimiterCommand());
        commands.put("ATTACK", new AttackCommand());
        commands.put("ATTACKFROMRESERVE", new AttackFromReserveCommand());
        commands.put("AUTOFILLBLACKMARKETSETTING", new AutoFillBlackMarketSettingCommand());
        commands.put("AUTOPLANETSTATUS", new AutoPlanetStatusCommand());
        commands.put("BID", new BidCommand());
        commands.put("BMSTATUS", new BMStatusCommand());
        commands.put("BUILDTABLELIST", new BuildTableListCommand());
        commands.put("BUILDTABLEVALIDATOR", new BuildTableValidatorCommand());
        commands.put("BUYBAYS", new BuyBaysCommand());
        commands.put("BUYPARTS", new BuyPartsCommand());
        commands.put("BUYPILOTSFROMHOUSE", new BuyPilotsFromHouseCommand());
        commands.put("CALCDIST", new CalcDistCommand());
        commands.put("CAMPAIGNCONFIG", new CampaignConfigCommand());
        commands.put("CANCELOFFER", new CancelOfferCommand());
        commands.put("CHANGEHOUSECOLOR", new ChangeHouseColorCommand());
        commands.put("CHANGENAME", new ChangeNameCommand());
        // Double CA
        commands.put("CHECKATTACK", new CheckAttackCommand());
        commands.put("CA", new CheckAttackCommand());
        //@Salient - used for discord bot
        commands.put("CHATBOT", new ChatBotHelperCommand());
        //
        commands.put("CHECK", new CheckCommand());
        commands.put("CHECKARMYELIGIBILITY", new CheckArmyEligibilityCommand());
        commands.put("CHECKARMYLINK", new CheckArmyLinkCommand());
        commands.put("CHECKDIST", new CheckDistCommand());
        commands.put("COMMENCEOPERATION", new CommenceOperationCommand());
        // Double CRL
        commands.put("CREATEARMY", new CreateArmyCommand());
        commands.put("CRA", new CreateArmyCommand());
        //
        commands.put("CREATEARMYFROMMUL", new CreateArmyFromMulCommand());
        commands.put("CREATECHATROOM", new CreateChatRoomCommand());
        commands.put("CREATEMERCFACTION", new CreateMercFactionCommand());
        commands.put("CREATESUBFACTION", new CreateSubFactionCommand());
        commands.put("CREATEPILOT", new CreatePilotCommand());
        commands.put("CREATEUNIT", new CreateUnitCommand());
        commands.put("DEACTIVATE", new DeactivateCommand());
        commands.put("DECLINEATTACKFROMRESERVE", new DeclineAttackFromReserveCommand());
        commands.put("DEFECT", new DefectCommand());
        commands.put("DEFEND", new DefendCommand());
        commands.put("DELETEACCOUNT", new DeleteAccountCommand());
        commands.put("DEMOTEPILOT", new DemotePilotCommand());
        commands.put("DEMOTEPLAYER", new DemotePlayerCommand());
        commands.put("DIRECTSELLUNIT", new DirectSellUnitCommand());
        commands.put("DISPLAYPLAYERPERSONALPILOTQUEUE", new DisplayPlayerPersonalPilotQueueCommand());
        commands.put("DISPLAYUNITREPAIRJOBS", new DisplayUnitRepairJobsCommand());
        commands.put("DONATE", new DonateCommand());
        commands.put("DONATEPILOT", new DonatePilotCommand());
        commands.put("EC", new EmojiCommand()); //@salient
        // Double EHM
        commands.put("EHM", new EmployeeHouseMailCommand());
        commands.put("EMPLOYEEHOUSEMAIL", new EmployeeHouseMailCommand());
        //
        commands.put("ENDCHRISTMAS", new EndChristmasCommand());
        commands.put("ENROLL", new EnrollCommand());
        // Double EXU
        commands.put("EXCHANGEUNIT", new ExchangeUnitCommand());
        commands.put("EXU", new ExchangeUnitCommand());
        commands.put("EXM", new ExchangeUnitCommand());
        // Exchange Pilots
        commands.put("EXCHANGEPILOTINUNIT", new ExchangePilotInUnitCommand());
        commands.put("EXP", new ExchangePilotInUnitCommand());
        commands.put("FACTION", new HouseCommand());// alias for house command
        commands.put("FACTIONLEADERFLUFF", new FactionLeaderFluffCommand());
        commands.put("FLF", new FactionLeaderFluffCommand());
        commands.put("FACTIONLEADERMUTE", new FactionLeaderMuteCommand());
        commands.put("FLM", new FactionLeaderMuteCommand());
        commands.put("FINDCP", new FindContestedPlanetsCommand()); //BarukKahzad 20151129
        commands.put("FIRETECHS", new FireTechsCommand());
        commands.put("FIXAMMO", new FixAmmoCommand());
        commands.put("FLUFF", new FluffCommand());
        commands.put("FORCEDDEFECT", new ForcedDefectCommand());
        commands.put("FORCEUPDATE", new ForceUpdateCommand());
        commands.put("GAMES", new GamesCommand());
        commands.put("GETCOMPONENTCONVERSION", new GetComponentConversionCommand());
        commands.put("GETFACTIONCONFIGS", new GetFactionConfigsCommand());
        commands.put("GETMODLOG", new GetModLogCommand());
        commands.put("GETOPS", new GetOpsCommand());
        commands.put("GETPLAYERUNITS", new GetPlayerUnitsCommand());
        commands.put("GETSERVERMEGAMEKGAMEOPTIONS", new GetServerMegaMekGameOptionsCommand());
        commands.put("GETSERVEROPFLAGS", new GetServerOpFlagsCommand());
        commands.put("GOOSE", new GooseCommand());
        commands.put("GRANTEXP", new GrantEXPCommand());
        commands.put("GRANTINFLUENCE", new GrantInfluenceCommand());
        commands.put("GRANTMONEY", new GrantMoneyCommand());
        commands.put("GRANTREWARD", new GrantRewardCommand());
        commands.put("GRANTTECHPOINTS", new GrantTechPointsCommand());
        commands.put("GRANTTECHS", new GrantTechsCommand());
        commands.put("HARDTERMINATE", new HardTerminateCommand());
        commands.put("HIREANDMAINTAIN", new HireAndMaintainHelper());
        commands.put("HIREANDREQUESTNEW", new HireAndRequestNewHelper());
        commands.put("HIREANDREQUESTUSED", new HireAndRequestUsedHelper());
        commands.put("HIRETECHS", new HireTechsCommand());
        commands.put("HOUSE", new HouseCommand());
        commands.put("HOUSECONTRACTS", new HouseContractsCommand());
        // Double HM
        commands.put("HOUSEMAIL", new HouseMailCommand());
        commands.put("HM", new HouseMailCommand());
        //
        commands.put("HOUSERANKING", new HouseRankingCommand());
        commands.put("HOUSESTATUS", new HouseStatusCommand());
        // Double IC
        commands.put("INCHARACTER", new InCharacterCommand());
        commands.put("IC", new InCharacterCommand());
        commands.put("INVIS", new InvisCommand());
        commands.put("ISITCHRISTMAS", new IsItChristmasCommand());
        // ISS
        commands.put("ISSTATUS", new ISStatusCommand());// legace commands for
        // the client
        commands.put("ISS", new ISStatusCommand());
        commands.put("UsersCommand", new ISStatusCommand());
        commands.put("UNIVERSESTATUS", new ISStatusCommand());
        //
        commands.put("JOINATTACK", new JoinAttackCommand());
        commands.put("LASTONLINE", new LastOnlineCommand());
        commands.put("LINKUNIT", new LinkUnitCommand());
        commands.put("LISTCOMMANDS", new ListCommandsCommand());
        commands.put("LISTMULS", new ListMulsCommand());
        commands.put("LISTMULTIPLAYERGROUPS", new ListMultiPlayerGroupsCommand());
        commands.put("LISTSERVEROPFLAGS", new ListServerOpFlagsCommand());
        commands.put("LISTSUBFACTIONS", new ListSubFactionCommand());
        commands.put("LOGIN", new LoginCommand());
        commands.put("LOGOUT", new LogoutCommand());
        // Double MStatus
        commands.put("MERCSTATUS", new MercStatusCommand());
        commands.put("MSTATUS", new MercStatusCommand());
        commands.put("MMOTD", new MMOTDCommand());
        //
        // Double MM
        commands.put("MODERATORMAIL", new ModeratorMailCommand());
        commands.put("MM", new ModeratorMailCommand());
        //
        commands.put("MODDEACTIVATE", new ModDeactivateCommand());
        commands.put("MODGAMES", new ModGamesCommand());
        commands.put("MODFULLREPAIR", new ModFullRepairCommand());
        commands.put("MODLOG", new ModLogCommand());
        commands.put("MODNOPLAY", new ModNoPlayCommand());
        commands.put("MODREFRESHFACTORY", new ModRefreshFactoryCommand());
        commands.put("MODTERMINATE", new ModTerminateCommand());
        commands.put("MOTD", new MOTDCommand());
        commands.put("MYBIDS", new MyBidsCommand());
        commands.put("MYSTATUS", new MyStatusCommand());
        commands.put("MYVOTES", new MyVotesCommand());
        commands.put("NAMEARMY", new NameArmyCommand());
        commands.put("NAMEPILOT", new NamePilotCommand());
        commands.put("NOPLAY", new NoPlayCommand());
        commands.put("NOTIFYFIGHTING", new NotifyFightingCommand());
        commands.put("OFFERCONTRACT", new OfferContractCommand());
        commands.put("PLANET", new PlanetCommand());
        commands.put("PLAYERLOCKARMY", new PlayerLockArmyCommand());
        commands.put("PLAYERS", new PlayersCommand());
        commands.put("PLAYERUNLOCKARMY", new PlayerUnlockArmyCommand());
        commands.put("PROMOTEPLAYER", new PromotePlayerCommand());
        commands.put("PROMOTEPILOT", new PromotePilotCommand());
        commands.put("PURCHASEFACTORY", new PurchaseFactoryCommand());
        commands.put("QUIRKCHECK", new QuirkCheckCommand()); //@salient
        commands.put("RANGE", new RangeCommand());
        commands.put("RECALL", new RecallCommand());
        commands.put("RECALLBID", new RecallBidCommand());
        commands.put("REPOD", new RepodCommand());
        commands.put("REPORTSTATUSMC", new ReportStatusMC()); //@salient
        commands.put("REFRESHFACTORY", new RefreshFactoryCommand());
        commands.put("REFUSECONTRACT", new RefuseContractCommand());
        commands.put("RELOADALLAMMO", new ReloadAllAmmoCommand());
        commands.put("REMOVEANDADDNOPLAY", new RemoveAndAddNoPlayHelper());
        // Double RML
        commands.put("REMOVEARMY", new RemoveArmyCommand());
        commands.put("RMA", new RemoveArmyCommand());
        //
        commands.put("REMOVEFACTIONPILOT", new RemoveFactionPilotCommand());
        commands.put("REMOVELEADER", new RemoveLeaderCommand());
        commands.put("REMOVEPARTS", new RemovePartsCommand());
        commands.put("REMOVEPILOT", new RemovePilotCommand());
        commands.put("REMOVESONG", new RemoveSongCommand());
        commands.put("REMOVESUBFACTION", new RemoveSubFactionCommand());
        commands.put("REMOVETRAIT", new RemoveTraitCommand());
        commands.put("REMOVEVOTE", new RemoveVoteCommand());
        commands.put("REPAIRUNIT", new RepairUnitCommand());
        commands.put("REQUEST", new RequestCommand());
        commands.put("REQUESTBUILDTABLE", new RequestBuildTableCommand());
        commands.put("REQUESTDONATED", new RequestDonatedCommand());
        commands.put("REQUESTOPERATIONSETTINGS", new RequestOperationSettingsCommand());
        commands.put("REQUESTSERVERMAIL", new RequestServerMailCommand());
        commands.put("REQUESTSUBFACTIONPROMOTION", new RequestSubFactionPromotionCommand());
        commands.put("RESEARCHTECHLEVEL", new ResearchTechLevelCommand());
        commands.put("RESEARCHUNIT", new ResearchUnitCommand());
        commands.put("RESETFREEMEKS", new AdminResetFreeMeksCommand()); //@Salient added for free build
        commands.put("RESTARTREPAIRTHREAD", new RestartRepairThreadCommand());
        commands.put("RETRIEVEALLOPERATIONS", new RetrieveAllOperationsCommand());
        commands.put("RETRIEVEOPERATION", new RetrieveOperationCommand());
        commands.put("RETRIEVEMUL", new RetrieveMulCommand());
        commands.put("RETRIEVEALLMULS", new RetrieveAllMulsCommand());
        commands.put("RETIREPILOT", new RetirePilotCommand());
        commands.put("SALVAGEUNIT", new SalvageUnitCommand());
        commands.put("SAVETOJSON", new SPlayerToJsonCommand()); //@salient - for discord bot
        commands.put("SCRAP", new ScrapCommand());
        commands.put("SENDCLIENTDATA", new SendClientDataCommand());
        commands.put("SELFPROMOTE", new SelfPromoteCommand()); //@salient - for subfactions
        commands.put("SELL", new SellCommand());
        commands.put("SELLBAYS", new SellBaysCommand());
        commands.put("SENDTOMISC", new SendToMiscCommand());
        commands.put("SERVERVERSION", new ServerVersionCommand());
        commands.put("SERVERGAMEOPTIONS", new ServerGameOptionsCommand());
        commands.put("SETADVANCEDPLANETTERRAIN", new SetAdvancedPlanetTerrainCommand());
        commands.put("SETAUTOEJECT", new SetAutoEjectCommand());
        commands.put("SETAUTOREORDER", new SetAutoReorderCommand());
        commands.put("SETCLIENTVERSION", new SetClientVersionCommand());
        commands.put("SETCOMPONENTCONVERSION", new SetComponentConversionCommand());
        commands.put("SETEDGESKILLS", new SetEdgeSkillsCommand());
        commands.put("SETELO", new SetEloCommand());
        commands.put("SETFACTIONTOFACTIONREWARDPOINTMULTIPLIER", new SetFactionToFactionRewardPointMultiplierCommand());
        commands.put("SETHOUSEBASEPILOTSKILLS", new SetHouseBasePilotSkillsCommand());
        commands.put("SETHOUSEBASEPILOTINGSKILLS", new SetHouseBasePilotingSkillsCommand());
        commands.put("SETHOUSELOGO", new SetHouseLogoCommand());
        commands.put("SETHOUSECONQUER", new SetHouseConquerCommand());
        commands.put("SETHOUSEINHOUSEATTACKS", new SetHouseInHouseAttacksCommand());
        commands.put("SETOPERATION", new SetOperationCommand());
        commands.put("SETMAINTAINED", new SetMaintainedCommand());
        commands.put("SETMMOTD", new SetMMOTDCommand());
        commands.put("SETMOTD", new SetMOTDCommand());
        commands.put("SETMULTIPLAYERGROUP", new SetMultiPlayerGroupCommand());
        commands.put("SETMYLOGO", new SetMyLogoCommand());
        commands.put("SETPLANETCONQUER", new SetPlanetConquerCommand());
        commands.put("SETPLANETCONQUERPOINTS", new SetPlanetConquerPointsCommand());
        commands.put("SETPLANETMINOWNERSHIP", new SetPlanetMinOwnerShipCommand());
        commands.put("SETPLANETWAREHOUSE", new SetPlanetWareHouseCommand());
        commands.put("SETPLANETCOMPPRODUCTION", new SetPlanetCompProductionCommand());
        commands.put("SETPLAYERFLAGS", new SetPlayerFlagsCommand());
        commands.put("SETSUBFACTIONCONFIG", new SetSubFactionConfigCommand());
        commands.put("SETTARGETSYSTEM", new SetTargetSystemCommand());
        commands.put("SETUNITAMMO", new SetUnitAmmoCommand());
        commands.put("SETUNITAMMOBYCRIT", new SetUnitAmmoByCritCommand());
        commands.put("SETUNITBURST", new SetUnitBurstCommand());
        commands.put("SETUNITCOMMANDER", new SetUnitCommanderCommand());
        commands.put("SETUNMAINTAINED", new SetUnmaintainedCommand());
        // Double ShowToHouse
        commands.put("SHOWTOHOUSE", new ShowToHouseCommand());
        commands.put("STH", new ShowToHouseCommand());
        commands.put("SIMPLEREPAIR", new SimpleRepairCommand());
        // Double SingASong
        commands.put("SINGASONG", new SingASongCommand());
        commands.put("SAS", new SingASongCommand());
        //@Salient for sol free build option
        commands.put("SOLCREATEUNIT", new FreeBuildCreateUnitCommand());
        commands.put("SOLDELETEUNIT", new SolDeleteUnitCommand());
        commands.put("STARTCHRISTMAS", new StartChristmasCommand());
        commands.put("STOPREPAIRJOB", new StopRepairJobCommand());
        commands.put("STRIPALLPARTSCACHE", new StripAllPartsCacheCommand());
        commands.put("STRIPUNITS", new StripUnitsCommand());
        commands.put("TERMINATE", new TerminateCommand());
        commands.put("TERMINATECONTRACT", new TerminateContractCommand());
        commands.put("TICK", new TickCommand());
        commands.put("TOGGLEARMYDISABLED", new ToggleArmyDisabledCommand());
        commands.put("TOUCH", new TouchCommand());
        commands.put("TRANSFERMONEY", new TransferMoneyCommand());
        commands.put("TRANSFERPILOT", new TransferPilotCommand());
        commands.put("TRANSFERUNIT", new TransferUnitCommand());
        commands.put("TRANSFERINFLUENCE", new TransferInfluenceCommand()); //@salient
        commands.put("TRANSFERREWARDPOINTS", new TransferRewardPointsCommand());
        commands.put("UPDATEDISCORDINFO", new UpdateDiscordInfoCommand());
        commands.put("UPDATEOPERATIONS", new UpdateOperationsCommand());
        commands.put("UPDATESERVERUNITSCACHE", new UpdateServerUnitsCacheCommand());
        commands.put("UPLOADMUL", new UploadMulCommand());
        commands.put("UNEMPLOYEDMERCS", new UnemployedMercsCommand());
        commands.put("UNENROLL", new UnenrollCommand());
        commands.put("UNITPOSITION", new UnitPositionCommand());
        commands.put("UNLOCKLANCES", new UnlockLancesCommand());
        commands.put("USEREWARDPOINTS", new UseRewardPointsCommand());
        commands.put("USEINFLUENCE", new UseInfluenceCommand());
        commands.put("VIEWFACTIONPARTSCACHE", new ViewFactionPartsCacheCommand());
        commands.put("VIEWPLAYERPARTS", new ViewPlayerPartsCommand());
        commands.put("VIEWPLAYERPERSONALPILOTQUEUE", new ViewPlayerPersonalPilotQueueCommand());
        commands.put("VIEWPLAYERUNIT", new ViewPlayerUnitCommand());
        commands.put("VOTE", new VoteCommand());

        // Old / comamds move to be usable by /c or /
        commands.put("AM", new ServerAnnouncementCommand());
        commands.put("SA", new ServerAnnouncementCommand());
        commands.put("SERVERANNOUNCEMENT", new ServerAnnouncementCommand());
        commands.put("BAN", new BanCommand());
        commands.put("BANIP", new BanIPCommand());
        commands.put("BANLIST", new BanListCommand());
        commands.put("COLOR", new ColorCommand());
        commands.put("COLOUR", new ColorCommand());
        commands.put("CONFIG", new ConfigCommand());
        commands.put("GETSAVEDMAIL", new GetSavedMailCommand());
        commands.put("IGNORE", new IgnoreCommand());
        commands.put("IGNORELIST", new IgnoreListCommand());
        commands.put("IPLIST", new IPListCommand());
        commands.put("KICK", new KickCommand());
        commands.put("MAIL", new MailCommand());
        commands.put("ME", new MeCommand());
        commands.put("ROLL", new RollCommand());
        commands.put("REGISTER", new RegisterCommand());
        commands.put("SHUTDOWN", new ShutdownCommand());
        commands.put("SETSMOTD", new SetSMOTDCommand());
        commands.put("SIGNOFF", new SignOffCommand());
        commands.put("SMOTD", new SMOTDCommand());
        commands.put("UNBAN", new UnBanCommand());
        commands.put("UNBANIP", new UnBanIPCommand());

        // command for testing
        commands.put("CODETEST", new CodeTestCommand());
        // ok we've put all the commands in the command hash now lets set the
        // levels
        try {
            MekwarsFileReader dis = new MekwarsFileReader("./data/commands/commands.dat");
            while (dis.ready()) {
                java.util.StringTokenizer command = new java.util.StringTokenizer(dis.readLine(), "#");
                String commandName = command.nextToken();
                if (commands.containsKey(commandName)) {
                    (commands.get(commandName)).setExecutionLevel(Integer.parseInt(command.nextToken()));
                }
            }
            dis.close();
        } catch (Exception ex) {
            MWLogger.errLog("Unable to find commands.dat. Continuing with defaults in place");
            java.util.TreeMap<String, Command> commandTable = new java.util.TreeMap<String, Command>(campaignMain.getServerCommands());
            java.io.PrintStream p = null;
            try {

                java.io.File fp = new java.io.File("./data/commands");
                if (!fp.exists()) {
                    fp.mkdir();
                }

                java.io.FileOutputStream out = new java.io.FileOutputStream("./data/commands/commands.dat");
                p = new java.io.PrintStream(out);

                for (String commandName : commandTable.keySet()) {

                    Command commandMethod = mekwars.server.campaign.CampaignMain.campaignMain.getServerCommands()
                                                  .get(commandName);
                    if (commandMethod == null) {
                        continue;
                    }
                    p.println(commandName.toUpperCase() + "#" + commandMethod.getExecutionLevel());
                }
            } catch (Exception ex1) {
                MWLogger.errLog(ex1);
                MWLogger.errLog("Unable to save command levels");
            } finally {
                if (p != null) {
                    p.close();
                }
            }
        }

        // Is the server data already there? (config files)? if not, create one
        if (data.getAllPlanets().size() > 0 && data.getAllHouses().size() > 0) {
            return;
        }

        // No SHouse Data yet? Parse the XML file and creathe them
        if (data.getAllHouses().size() == 0) {
            try {
                XMLFactionDataParser parser = new XMLFactionDataParser("./data/factions.xml");
                for (SHouse h : parser.getFactions()) {
                    addHouse(h);
                }
            } catch (Exception ex) {
                MWLogger.errLog("Error while reading faction data -- bailing out");
                MWLogger.errLog(ex);
                MWLogger.mainLog("Error while reading Faction Data!");
                System.exit(1);
            }

            // Add the Newbie-SHouse
            SHouse solaris = new NewbieHouse(data.getUnusedHouseID(),
                  mekwars.server.campaign.CampaignMain.campaignMain.getConfig("NewbieHouseName"),
                  "#33CCCC",
                  4,
                  5,
                  "SOL");
            addHouse(solaris);
            SHouse none = new MercHouse();
            none.createNoneHouse();
            addHouse(none);
        }

        // No Planets Data yet? Parse XML and create world.
        if (data.getAllPlanets().size() == 0) {

            // First, clear out the factions' initialrankings.
            for (House h : data.getAllHouses()) {
                SHouse sh = (SHouse) h;
                sh.setInitialHouseRanking(0);
            }

            try {

                XMLPlanetDataParser parser = new XMLPlanetDataParser("./data/planets.xml");
                for (SPlanet p : parser.getPlanets()) {

                    // add the planet
                    addPlanet(p);

                    // set initial influences
                    for (House h : p.getInfluence().getHouses()) {

                        SHouse sh = (SHouse) h;
                        if (sh == null) {
                            MWLogger.errLog("Null faction found while loading Planets.xml. Planet: " + p.getName());
                            continue;
                        }

                        if (p.getInfluence().getOwner() != null &&
                                  sh.getId() == p.getInfluence().getOwner().intValue()) {
                            sh.addPlanet(p);
                        }

                        sh.setInitialHouseRanking(sh.getInitialHouseRanking() +
                                                        p.getInfluence().getInfluence(sh.getId()));
                    }
                }
            } catch (Exception ex) {
                MWLogger.errLog("Error while reading planet data -- bailing out");
                MWLogger.errLog(ex);
                MWLogger.mainLog("Error while reading Planet Data!");
                System.exit(1);
            }

            java.util.HashMap<Integer, Integer> solFlu = new java.util.HashMap<Integer, Integer>();
            solFlu.put(
                  mekwars.server.campaign.CampaignMain.campaignMain.getHouseFromPartialString(mekwars.server.campaign.CampaignMain.campaignMain.getConfig(
                        "NewbieHouseName"), null).getId(), 100);
            SPlanet newbieP = new SPlanet(0, "Solaris VII", new Influences(solFlu), 0, 0, -3, -2);
            if (data.getPlanetByName("Solaris VII") == null) {
                addPlanet(newbieP);
                mekwars.server.campaign.CampaignMain.campaignMain.getHouseFromPartialString(mekwars.server.campaign.CampaignMain.campaignMain.getConfig(
                      "NewbieHouseName"), null).addPlanet(newbieP);
            }
        }

        // Save it on startup
        toFile();
    }

    public void addHouse(SHouse s) {
        data.addHouse(s);
    }

    public void addPlanet(SPlanet p) {

        if (p.getOriginalOwner().trim().equals("")) {
            if (p.getOwner() == null) {
                p.setOriginalOwner(campaignMain.getConfig("NewbieHouseName"));
            }
            p.setOriginalOwner(p.getOwner().getName());
        }
        if (CampaignData.cd.getPlanet(p.getId()) != null) {
            MWLogger.errLog("Duplicate Planet ID: " +
                                  CampaignData.cd.getPlanet(p.getId()).getName() +
                                  " and " +
                                  p.getName());
        }
        data.addPlanet(p);
    }

    public synchronized void userRoll(String text, String Username) {

        // added by VEGETA 2/8/2003
        // Random random = new Random();
        int dice = 2;
        int sides = 6;
        int total = 0;
        int roll = 0;
        String x = "";

        if (text.trim().length() > 0) {

            java.util.StringTokenizer ST = new java.util.StringTokenizer(text, "d");
            try {
                if (ST.hasMoreElements()) {
                    x = (String) ST.nextElement();
                    dice = Integer.parseInt(x.trim());
                }
                if (ST.hasMoreElements()) {
                    x = (String) ST.nextElement();
                    sides = Integer.parseInt(x.trim());
                }

            } catch (NumberFormatException ex) {
                toUser("AM:/roll: error parsing arguments.", Username, true);
                return;
            } catch (StringIndexOutOfBoundsException ex) {
                toUser("AM:/roll: error parsing arguments.", Username, true);
                return;
            }
        }

        if (dice < 1 || sides < 2) {
            this.doSendToAllOnlinePlayers("AM:" + Username + " loves the smell of napalm in the morning.", true);
            return;
        }

        if (dice > 20 || sides > 100) {
            this.doSendToAllOnlinePlayers("AM:" + Username + " is a stupid haxx0r!", true);
            return;
        }

        StringBuilder diceBuffer = new StringBuilder();

        for (int i = 0; i < dice; i++) {
            // roll = random.nextInt(sides) + 1;
            roll = campaignMain.getRandomNumber(sides) + 1;
            total += roll;

            // for one die, we're all set
            if (dice < 2) {
                diceBuffer.append(roll);
                continue;
            }

            // 2+ dice, use commas and "and"
            if (i < dice - 1) {
                diceBuffer.append(roll);
                diceBuffer.append(", ");
            } else {
                diceBuffer.append("and ");
                diceBuffer.append(roll);
            }
        }
        if (text != "") {
            this.doSendToAllOnlinePlayers("AM:" +
                                                Username +
                                                " rolled " +
                                                diceBuffer +
                                                " for a total of " +
                                                total +
                                                ", using " +
                                                text +
                                                ".", true);
        } else {
            this.doSendToAllOnlinePlayers("AM:" +
                                                Username +
                                                " rolled " +
                                                diceBuffer +
                                                " for a total of " +
                                                total +
                                                ", using 2d6.", true);
        }
    }

    public int getRandomNumber(int seed) {

        if (seed < 1) {
            return seed;
        }

        float answer = r.nextFloat() * (float) seed;

        return (int) Math.floor(answer);
    }

    public void addMechStat(String Filename, int mechsize, int gameplayed, int gamewon, int scrapped) {
        addMechStat(Filename, mechsize, gameplayed, gamewon, scrapped, 0);
    }

    public void addMechStat(String Filename, int mechsize, int gameplayed, int gamewon, int scrapped, int destroyed) {
        MekStatistics m = null;
        if (mekStats.get(Filename) == null) {
            m = new MekStatistics(Filename, mechsize);
        } else {
            m = mekStats.get(Filename);
        }
        m.setOriginalBV(SUnit.loadMech(Filename).calculateBattleValue());

        m.addStats(gameplayed, gamewon, m.getOriginalBV());
        m.setTimesScrapped(m.getTimesScrapped() + scrapped);
        m.setTimesDestroyed(m.getTimesDestroyed() + destroyed);
        mekStats.put(Filename, m);
    }

    /**
     * Private method that sends KI| (kick) commands to idle players. Broken into a seperate method to reduce code
     * repetitiveness in slice().
     */
    private void checkAndRemoveIdle(SPlayer p, long maxIdleTime) {

        // dont boot mods
        if (getServer().isModerator(p.getName())) {
            return;
        }

        // if he's already logged out, who cares?
        // Well, it turns out that some people do care - see RFE 2126734
        if (p.getDutyStatus() <= SPlayer.STATUS_LOGGEDOUT &&
                  !mekwars.server.campaign.CampaignMain.campaignMain.getBooleanConfig("DisconnectIdleUsers")) {
            return;
        }

        // redundant, but never boot fighting players
        if (p.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
            return;
        }

        // reserve or active player. check his times.
        // NOTE: KI| command is actualy campaign logout. GBB| a disco/kill.
        if (System.currentTimeMillis() - p.getLastTimeCommandSent() > maxIdleTime) {
            mekwars.server.campaign.CampaignMain.campaignMain.toUser(
                  "You were logged out by the server (excessive idle time).",
                  p.getName(),
                  true);
            if (!mekwars.server.campaign.CampaignMain.campaignMain.getBooleanConfig("DisconnectIdleUsers")) {
                mekwars.server.campaign.CampaignMain.campaignMain.toUser("KI|idler", p.getName(), false);
            } else {
                mekwars.server.campaign.CampaignMain.campaignMain.toUser("PL|GBB|idler", p.getName(), false);
            }
        }
    }

    /**
     * Slicer. Called by SliceThread @ the end of its config.txt defined wait duration. Gives influence to active
     * players, checks for (and kicks) idle players, and saves player files. Slices are generally much shorter than
     * ticks, and involve players and player data much more heavily than factions/high-end campaign structures. This is
     * the exact opposite of the .tick() (see below).
     */
    public synchronized void slice(int sliceID) {

        // write log header
        MWLogger.mainLog("Slice #" + sliceID + " Started");
        MWLogger.cmdLog("Slice #" + sliceID + " Started");
        MWLogger.infoLog("Slice #" + sliceID + " Started: " + System.currentTimeMillis());

        WhoToHTML who = new WhoToHTML(mekwars.server.campaign.CampaignMain.campaignMain.getConfig("HTMLWhoPath"));

        // loop through all houses
        for (House vh : data.getAllHouses()) {
            SHouse currH = (SHouse) vh;
            //fahr
            MWLogger.infoLog("Slice #" + sliceID + " house: " + currH.getName());

            // load max idle time, converted to ms
            long maxIdleTime = Long.parseLong(mekwars.server.campaign.CampaignMain.campaignMain.getConfig("MaxIdleTime")) *
                                     60000;

            MWLogger.infoLog("Slice #" + sliceID + " house: " + currH.getName() + " reservePlayers");
            for (SPlayer currP : currH.getReservePlayers().values()) {
                if (maxIdleTime > 0) {
                    try {
                        checkAndRemoveIdle(currP, maxIdleTime);
                    } catch (Exception ex) {
                        MWLogger.infoLog("Slice #" +
                                               sliceID +
                                               " house: " +
                                               currH.getName() +
                                               " reservePlayer: " +
                                               currP.getName());
                        MWLogger.errLog(ex);
                    }
                }
                if (!currP.isInvisible()) {
                    who.addPlayer(currP);
                }
            }

            /*
             * Active players get the whole shebang - influence addition,
             * maintainance, and an idle check (if enabled).
             */
            MWLogger.infoLog("Slice #" + sliceID + " house: " + currH.getName() + " ActivePlayers");
            for (SPlayer currP : currH.getActivePlayers().values()) {
                try {
                    currP.doMaintainance();
                    if (!currP.isInvisible()) {
                        who.addPlayer(currP);
                    }
                    if (maxIdleTime > 0) {
                        checkAndRemoveIdle(currP, maxIdleTime);
                    }
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                    MWLogger.infoLog("Slice #" +
                                           sliceID +
                                           " house: " +
                                           currH.getName() +
                                           " activePlayer: " +
                                           currP.getName());
                }
            }

            // fighters only have maint. they get influence grants post-game.
            MWLogger.infoLog("Slice #" + sliceID + " house: " + currH.getName() + " fightingPlayers");
            for (SPlayer currP : currH.getFightingPlayers().values()) {
                try {
                    currP.doMaintainance();
                    if (!currP.isInvisible()) {
                        who.addPlayer(currP);
                    }
                    // People fighting are always up to date
                    if (maxIdleTime > 0) {
                        currP.setLastTimeCommandSent(System.currentTimeMillis() + maxIdleTime);
                    }
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                    MWLogger.infoLog("Slice #" +
                                           sliceID +
                                           " house: " +
                                           currH.getName() +
                                           " fightingPlayer: " +
                                           currP.getName());
                }
            }
        }// end all houses

        if (mekwars.server.campaign.CampaignMain.campaignMain.getBooleanConfig("HTMLOUTPUT")) {
            who.outputHTML();
        }
        who = null;

        // check to see if we should save on this slice
        int saveOnSlice = mekwars.server.campaign.CampaignMain.campaignMain.getIntegerConfig("SaveEverySlice");
        if (saveOnSlice < 1) {
            saveOnSlice = 1;
        }
        if (sliceID % saveOnSlice == 0) {
            MWLogger.infoLog("Slice #" + sliceID + " savePlayers()");
            try {
                savePlayers();// Once all of the saving is done clear
            } catch (Exception ex) {
                MWLogger.errLog(ex);
                MWLogger.infoLog("Slice #" + sliceID + " savePlayers() failed");
            }// everything for the next tick.
            MWLogger.infoLog("Slice #" + sliceID + " saveTopUnitID()");
            try {
                saveTopUnitID();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
                MWLogger.infoLog("Slice #" + sliceID + " saveTopUnitID() failed");
            }
        }

        // write log header
        MWLogger.mainLog("Slice #" + sliceID + " Finished");
        MWLogger.cmdLog("Slice #" + sliceID + " Finished");
        MWLogger.infoLog("Slice #" + sliceID + " Finished: " + System.currentTimeMillis());

    }// end the slice...

    /**
     * Tick is the main timekeeping unit of the server. At each tick, various statistics are checked and shown to
     * players (ex: house ranking) and various portions of the campaign are cleaned up or finalized (ex: market sales).
     * Most tick actions involve meta-functions, houses, the market, and so on. The only tick mechanic that acts
     * directly on players is Mezzo (pricemod) drain.
     */
    public synchronized void tick(boolean real, int tickid) {

        // add header to log
        MWLogger.mainLog("Tick #" + tickid + " Started");
        MWLogger.cmdLog("Tick #" + tickid + " Started");
        MWLogger.infoLog("Tick #" + tickid + " Started");

        // log the number of games underway
        int gameCount = 0;
        for (ShortOperation currO : getOpsManager().getRunningOps().values()) {
            if (currO.getStatus() == ShortOperation.STATUS_INPROGRESS) {
                gameCount++;
            }
        }
        MWLogger.tickLog(gameCount + " games in progress.");

        // tick all houses
        int totalPlayersOnline = 0;
        for (House vh : data.getAllHouses()) {

            // we can safely cast to SHouse
            SHouse currH = (SHouse) vh;

            /*
             * Total faction load for logs.
             */
            int activePs = currH.getActivePlayers().size();
            int fightingPs = currH.getFightingPlayers().size();
            int totalFactionPlayers = currH.getReservePlayers().size() + activePs + fightingPs;
            MWLogger.tickLog(currH.getName() +
                                   " has " +
                                   totalFactionPlayers +
                                   " members online (" +
                                   activePs +
                                   " active, " +
                                   fightingPs +
                                   " fighting)");

            // if there are any faction players online, tick the house
            if (totalFactionPlayers > 0 || real == false) {

                String houseTickInfo = "";

                if (!mekwars.server.campaign.CampaignMain.campaignMain.getBooleanConfig("ProcessHouseTicksAtSlice")) {
                    try {
                        MWLogger.debugLog("Starting Faction Tick");
                        houseTickInfo = currH.tick(real, tickid);
                        MWLogger.debugLog("Finished Faction Tick");
                    } catch (Exception e) {
                        MWLogger.errLog("Problems with faction tick.");
                        MWLogger.errLog(e);
                    }
                }
                // do some things (reset scraps, etc) for players
                for (SPlayer currP : currH.getAllOnlinePlayers().values()) {

                    // Clear up any users that the server still thinks is
                    // connected.
                    if (getServer().getClient(currP.getName()) == null) {
                        MWLogger.debugLog("Logging out Player " + currP.getName());
                        doLogoutPlayer(currP.getName());
                        continue;
                    }

                    totalPlayersOnline++;
                    MWLogger.debugLog("Setting Scraps This tick for " + currP.getName());
                    currP.setScrapsThisTick(0);
                    MWLogger.debugLog("Setting Donations This tick for " + currP.getName());
                    currP.setDonatonsThisTick(0);
                    MWLogger.debugLog("Healing pilots This tick for " + currP.getName());
                    currP.healPilots();

                    MWLogger.debugLog("Updating faction info for " + currP.getName());
                    // return the result of the faction tick to everyone, to
                    // misc tab.
                    toUser("SM|" + houseTickInfo, currP.getName(), false);
                }

            }// end if(there is a player in the faction)
        }// end for(all houses)

        // append the total player count to the logs
        MWLogger.tickLog("Total players: " +
                               getServer().userCount(true) +
                               " online, " +
                               totalPlayersOnline +
                               " logged in.");

        /*
         * Send the latest game reports to the players, and increment the
         * removal-counters.
         */
        String generalResult = "<br>";
        String opsTick = opsManager.tick();
        if (opsTick.length() > 0) {
            generalResult += opsTick + "<br><br>";
        }

        // if the relative house rankings should be shown, do so.
        String rankTick = "";
        if (Boolean.parseBoolean(this.getConfig("ShowFactionRanks"))) {
            rankTick = Statistics.getReadableHouseRanking(true);
        }

        if (rankTick.length() > 0) {
            generalResult += rankTick + "<br><br>";
        }

        // send the combined & spaced string to players
        if (generalResult.toLowerCase().replace("<br>", " ").trim().length() > 0) {
            this.doSendToAllOnlinePlayers("PL|UDT|" + generalResult, false);
        }

        /*
         * Tick the market. This will resolve any auctions w/ 0 ticks remaining
         * and decrement all others.
         */
        try {
            market.tick();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

        MWLogger.tickLog("Parts Market Tick Started");
        try {
            partsmarket.tick();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
        MWLogger.tickLog("Parts Market Tick Finished");

        MWLogger.tickLog("doRanking");
        // output player stats to HTML, if enabled.
        if (Boolean.parseBoolean(getConfig("HTMLOUTPUT"))) {
            Statistics.doRanking();
        }

        MWLogger.tickLog("PurgePlayersFiles");
        // purge old player files
        purgePlayerFiles();

        MWLogger.tickLog("Automated Backup");
        /*
         * finally, check to see if we should back up. note that the thread will
         * die immediately if it is not time to back up (last was written within
         * offset).
         */
        aub = new AutomaticBackup(System.currentTimeMillis());
        // new Thread(aub).start();
        aub.run();

        MWLogger.tickLog("GC");
        // force a GC. this may not be necessary anymore?
        System.gc();

        // mainlog footer
        MWLogger.mainLog("Tick #" + tickid + " Finished");
        MWLogger.cmdLog("Tick #" + tickid + " Finished");
        MWLogger.infoLog("Tick #" + tickid + " Finished");
    }

    /* The Planetary Control Way */
    public java.util.TreeSet<HouseRankingHelpContainer> getHouseRanking() {

        java.util.Hashtable<String, HouseRankingHelpContainer> factionContainer = new java.util.Hashtable<String, HouseRankingHelpContainer>();
        for (House currHouse : data.getAllHouses()) {
            SHouse h = (SHouse) currHouse;
            if (!h.isMercHouse() && !h.isNewbieHouse()) {
                HouseRankingHelpContainer hrc = new HouseRankingHelpContainer(h);
                factionContainer.put(h.getName(), hrc);
            }
        }

        for (Planet p : data.getAllPlanets()) {

            for (House currH : p.getInfluence().getHouses()) {
                SHouse hs = (SHouse) currH;
                if (hs == null) {
                    continue;
                }
                if (!hs.isNewbieHouse() && !hs.isMercHouse()) {
                    factionContainer.get(hs.getName()).addAmount(p.getInfluence().getInfluence(hs.getId()));
                }
            }

        }

        java.util.TreeSet<HouseRankingHelpContainer> s = new java.util.TreeSet<HouseRankingHelpContainer>();
        for (HouseRankingHelpContainer currContainer : factionContainer.values()) {
            s.add(currContainer);
        }

        return s;
    }

    /**
     * Update all player armies that are online This is normally called after operations have been updated.
     */
    public void updateAllOnlinePlayerArmies() {

        doSendToAllOnlinePlayers("PL|UOE|CLEAR", false);
        for (House vh : data.getAllHouses()) {
            SHouse h = (SHouse) vh;
            for (SPlayer currPlayer : h.getReservePlayers().values()) {
                for (SArmy a : currPlayer.getArmies()) {
                    a.getLegalOperations().clear();
                    mekwars.server.campaign.CampaignMain.campaignMain.getOpsManager().checkOperations(a, true);
                }
            }

            for (SPlayer currPlayer : h.getActivePlayers().values()) {
                for (SArmy a : currPlayer.getArmies()) {
                    a.getLegalOperations().clear();
                    mekwars.server.campaign.CampaignMain.campaignMain.getOpsManager().checkOperations(a, true);
                }
            }

            for (SPlayer currPlayer : h.getFightingPlayers().values()) {
                for (SArmy a : currPlayer.getArmies()) {
                    a.getLegalOperations().clear();
                    mekwars.server.campaign.CampaignMain.campaignMain.getOpsManager().checkOperations(a, true);
                }
            }
        }
    }

    /**
     * Method that returns the SHouse that contains a player with a given name. If no factions has such a player online,
     * return a null.
     */
    public SHouse getHouseForPlayer(String Username) {
        String lowerName = Username.toLowerCase();
        for (House vh : data.getAllHouses()) {
            SHouse h = (SHouse) vh;
            if (h.getReservePlayers().containsKey(lowerName)) {
                return h;
            }
            if (h.getActivePlayers().containsKey(lowerName)) {
                return h;
            }
            if (h.getFightingPlayers().containsKey(lowerName)) {
                return h;
            }
        }
        return null;
    }

    public boolean isUsingIncreasedTechs() {
        return (
              mekwars.server.campaign.CampaignMain.campaignMain.getBooleanConfig("UseNonFactionUnitsIncreasedTechs") &&
                    !mekwars.server.campaign.CampaignMain.campaignMain.isUsingAdvanceRepair());
    }

    /*
     * Checks to see if the campaign is using advanced repairs and starts up the
     * thread if it is null
     */
    public boolean isUsingAdvanceRepair() {
        boolean isUsing = campaignMain.getBooleanConfig("UseAdvanceRepair") ||
                                campaignMain.getBooleanConfig("UseSimpleRepair");
        if (isUsing && RTT == null) {
            RTT = new RepairTrackingThread(campaignMain.getLongConfig("TimeForEachRepairPoint") * 1000);
            RTT.start();
        } else if (!isUsing && RTT != null) {
            RTT.interrupt();
            RTT = null;
        }

        return isUsing;
    }

    public void restartRTT() {
        boolean isUsing = campaignMain.getBooleanConfig("UseAdvanceRepair") ||
                                campaignMain.getBooleanConfig("UseSimpleRepair");
        if (isUsing) {
            RTT = null;
            RTT = new RepairTrackingThread(campaignMain.getLongConfig("TimeForEachRepairPoint") * 1000);
            RTT.start();
        }
    }

    public boolean getBooleanConfig(String key) {
        try {
            return Boolean.parseBoolean(campaignMain.getConfig(key));
        } catch (Exception ex) {
            return false;
        }
    }

    public long getLongConfig(String key) {
        try {
            return Long.parseLong(campaignMain.getConfig(key));
        } catch (Exception ex) {
            return -1;
        }
    }

    public java.util.Random getR() {
        return r;
    }

    synchronized public void addToNewsFeed(String s) {
        addToNewsFeed(s, "", "");
    }

    synchronized public void addToNewsFeed(String title, String category, String body) {
        newsFeed.addMessage(new FeedMessage(title, category, body));
    }

    public Market2 getMarket() {
        return market;
    }

    public PartsMarket getPartsMarket() {
        return partsmarket;
    }

    public java.util.Properties getConfig() {
        return config;
    }

    public double getAmmoCost(String ammo) {

        if (blackMarketEquipmentCostTable.containsKey(ammo) &&
                  blackMarketEquipmentCostTable.get(ammo).getMinCost() > 0) {
            return blackMarketEquipmentCostTable.get(ammo).getMinCost();
        }

        return -1.0;
    }

    public ChatRoom getChatRoom(String chatRoomName) {
        return chatRooms.get(chatRoomName);
    }

    public java.util.Collection<ChatRoom> getChatRoomList() {
        return chatRooms.values();
    }

    public void addChatRoom(String chatRoomName, ChatRoom chatRoom) {
        chatRooms.put(chatRoomName.toLowerCase(), chatRoom);
    }

    /**
     * @return the campaign's VoteManager
     */
    public VoteManager getVoteManager() {
        return voteManager;
    }

    /**
     * This retuns the blackMarketEquipmentCostTable This hashTable keeps track of all the mix/max costs and parts
     * production for the Black market. This is used to allow players to buy spare parts to repair Their units.
     *
     * @return blackMarketEquipmentCostTable
     */
    public java.util.Hashtable<String, Equipment> getBlackMarketEquipmentTable() {
        return blackMarketEquipmentCostTable;
    }

    public TickThread getTThread() {
        return TThread;
    }

    public ImmunityThread getIThread() {
        return IThread;
    }

    public java.util.Vector<ContractInfo> getUnresolvedContracts() {
        return unresolvedContracts;
    }

    /**
     * @return Returns the currentUnitID.
     */
    public int getCurrentUnitID() {
        return currentUnitID;
    }

    /**
     * @param currentUnitID The currentUnitID to set.
     */
    public void setCurrentUnitID(int currentUnitID) {
        this.currentUnitID = currentUnitID;
    }

    public synchronized int getAndUpdateCurrentUnitID() {
        currentUnitID++;
        return currentUnitID - 1;
    }

    public int getCurrentPilotID() {
        return currentPilotID;
    }

    public void setCurrentPilotID(int id) {
        currentPilotID = id;
    }

    public synchronized int getAndUpdateCurrentPilotID() {
        return ++currentPilotID;
    }

    public SHouse getHouseFromPartialString(String HouseString) {
        return getHouseFromPartialString(HouseString, null);
    }

    public SHouse getHouseFromDBID(int DBId) {
        for (House currH : data.getAllHouses()) {
            SHouse sh = (SHouse) currH;
            if (sh.getDBId() == DBId) {
                return sh;
            }
        }
        return null;
    }

    public SHouse getHouseById(int id) {
        return (SHouse) data.getHouse(id);
    }

    public SHouse getHouseFromPartialString(String HouseString, String Username) {

        // store matches so we can tell player if there's more than one
        int numMatches = 0;
        SHouse theMatch = null;

        for (House currH : data.getAllHouses()) {
            SHouse sh = (SHouse) currH;

            // exact match
            if (sh.getName().equals(HouseString)) {
                return sh;
            }

            // store all matches
            if (sh.getName().startsWith(HouseString)) {
                theMatch = sh;
                numMatches++;
            }
        }

        // too many matches
        if (numMatches > 1) {
            if (Username != null) {
                toUser("\"" + HouseString + "\" is not unique [" + numMatches + " matches]. Please be more specific.",
                      Username);
            }
            return null;
        }

        if (numMatches == 0) {
            if (Username != null) {
                toUser("Couldn't find a factions whose name begins with \"" + HouseString + "\". Try again.",
                      Username,
                      true);
            }
            return null;
        }

        // only one match! send it back.
        return theMatch;
    }

    /**
     * Private method which writes out players who need to be saved and purges logged out/removable players from RAM.
     * Should be called only from .slice() or forceSave. See this.forceSavePlayers() for more info on admin-initiated
     * player saves.
     */
    private void savePlayers() {

        // go into sleep while the server is archiving player files
        while (isArchiving()) {
            try {
                Thread.sleep(125);
            } catch (Exception ex) {
                // do nothing
            }
        }

        // add log header
        java.util.Date d = new java.util.Date(System.currentTimeMillis());
        MWLogger.infoLog(d + ": Starting Player Saving cycle");
        for (House vh : mekwars.server.campaign.CampaignMain.campaignMain.getData().getAllHouses()) {
            SHouse currH = (SHouse) vh;
            for (SPlayer currP : currH.getAllOnlinePlayers().values()) {
                savePlayerFile(currP);
            }
        }

        // write out log footer
        d = new java.util.Date(System.currentTimeMillis());
        MWLogger.mainLog(d + ": Player save cycle completed.");
        MWLogger.infoLog(d + ": Player saves finished.");

        /*
         * Everyone in the save pile has been saved. This is nice, but not the
         * end of the line. Now we need to purge the savePlayers hash. Loop
         * through and remove everyone we can (some players are not removable
         * b/c of ongoing repairs). If the player is removable AND logged out,
         * we can null his player and save some memory space @ next gc().
         * Iterator<SPlayer> i = savePlayers.values().iterator(); while
         * (i.hasNext()) { SPlayer p = i.next(); if (p.isRemoveable()) {
         * i.remove(); if (p.getDutyStatus() == SPlayer.STATUS_LOGGEDOUT) p =
         * null; } }
         */

    }

    /**
     * Public save method. Used by admins to save all online players and all players who are in the save queue. Is
     * called from /save, /shutdown, and /c adminsave.
     */
    public void forceSavePlayers(String Username) {

        // first, save everyone online
        for (House vh : mekwars.server.campaign.CampaignMain.campaignMain.getData().getAllHouses()) {
            SHouse currH = (SHouse) vh;
            for (SPlayer currP : currH.getAllOnlinePlayers().values()) {
                savePlayerFile(currP);
                if (Username != null) {
                    mekwars.server.campaign.CampaignMain.campaignMain.toUser("AM:" + currP.getName() + " saved",
                          Username,
                          true);
                }
            }
        }
    }

    public CampaignData getData() {
        return data;
    }

    /**
     * Private method which writes a player to the disc. This code was housed in SPlayer; however, it is only called
     * from CampaignMain and (from an OO standpoint) only CMain should know the hardcoded paths which are used.
     *
     * @author nmorris 1/13/06
     */
    private void savePlayerFile(SPlayer p) {

        try {
            String fileName = p.getName().toLowerCase();
            java.io.FileOutputStream pout = new java.io.FileOutputStream("./campaign/players/" +
                                                                               fileName.toLowerCase() +
                                                                               ".dat");
            java.io.PrintStream pfile = new java.io.PrintStream(pout);

            /*
             * Put a lock on the player while saving. Do NOT allow .toString()
             * to set a lock, or we'll get deadlocks.
             */
            synchronized (p) {
                pfile.println(p.toString(false));
            }

            pfile.close();
            pout.close();
        } catch (java.io.FileNotFoundException fnfe) {
            // Since we are saving to disk do nothing.
            // The proccess is most likely already being used.
            return;
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Unable to save " + p.getName().toLowerCase());
        }
    }

    public void loadBanAmmo(String line) {

        try {
            java.util.StringTokenizer st = new java.util.StringTokenizer(line, "#");
            String HouseName = (String) st.nextElement();
            SHouse faction = null;
            if (!HouseName.equalsIgnoreCase("server")) {
                faction = mekwars.server.campaign.CampaignMain.campaignMain.getHouseFromPartialString(HouseName, null);
                while (st.hasMoreTokens()) {
                    faction.getBannedAmmo().put(st.nextToken(), "Banned");
                }
            } else {
                while (st.hasMoreElements()) {
                    mekwars.server.campaign.CampaignMain.campaignMain.getServerBannedAmmo()
                          .put(st.nextToken(), "Banned");
                }
            }
        } catch (Exception ex) {
        }// make it compatible with people that had the old format,without
        // the timestamp on the first line, the first time and now dont.
    }

    public void loadBannedTargetSystems() {
        java.io.File tsFile = new java.io.File("./campaign/bantarget.dat");
        if (!tsFile.exists()) {
            return;
        }

        try {
            MekwarsFileReader dis = new MekwarsFileReader(tsFile);
            java.util.Vector<Integer> bans = new java.util.Vector<Integer>(1, 1);
            String line = dis.readLine();
            java.util.StringTokenizer st = new java.util.StringTokenizer(line, "#");
            while (st.hasMoreTokens()) {
                bans.add(Integer.parseInt(st.nextToken()));
            }
            getData().setBannedTargetingSystems(bans);
            dis.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load the black market settings from file.
     */
    public void loadBlackMarketSettings() {

        try {
            java.io.File bmFile = new java.io.File("./data/blackmarketsettings.dat");

            if (!bmFile.exists()) {
                return;
            }

            MekwarsFileReader dis = new MekwarsFileReader(bmFile);

            // Ignore Time Stamp
            dis.readLine();

            while (dis.ready()) {
                Equipment bme = new Equipment();
                String line = dis.readLine();
                java.util.StringTokenizer data = new java.util.StringTokenizer(line, "#");

                bme.setEquipmentInternalName(data.nextToken());
                bme.setMinCost(Double.parseDouble(data.nextToken()));
                bme.setMaxCost(Double.parseDouble(data.nextToken()));
                bme.setMinProduction(Integer.parseInt(data.nextToken()));
                bme.setMaxProduction(Integer.parseInt(data.nextToken()));

                campaignMain.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);
            }
            dis.close();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public void saveTopUnitID() {

        int topID = campaignMain.getCurrentUnitID();

        try {
            java.io.FileOutputStream pout = new java.io.FileOutputStream("./campaign/topserverid.dat");
            java.io.PrintStream unitIDFile = new java.io.PrintStream(pout);
            unitIDFile.println(topID);
            unitIDFile.println(campaignMain.getCurrentPilotID());
            unitIDFile.close();
            pout.close();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public void loadTopUnitID() {
        try {
            MekwarsFileReader dis = new MekwarsFileReader("./campaign/topserverid.dat");
            campaignMain.setCurrentUnitID(Integer.parseInt(dis.readLine()));
            campaignMain.setCurrentPilotID(Integer.parseInt(dis.readLine()));
            dis.close();
        } catch (java.io.FileNotFoundException FNFE) {
            // Do nothing.
            MWLogger.errLog("Unable to fine/open ./campaign/topserverid.dat. moving on.");
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public void addGamesCompleted(int i) {
        setGamesCompleted(getGamesCompleted() + i);
    }

    public int getGamesCompleted() {
        return gamesCompleted;
    }

    public void setGamesCompleted(int i) {
        gamesCompleted = i;
    }

    public int getMachineGunCount(java.util.ArrayList<Mounted> weaponList) {
        int count = 0;

        for (Mounted weapons : weaponList) {
            WeaponType weapon = (WeaponType) weapons.getType();
            if (weapon.hasFlag(WeaponType.F_MG)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Use to load a factions trait file.
     *
     * @param faction
     *
     * @return
     *
     * @author Torren (Jason Tighe)
     */
    public java.util.Vector<String> getFactionTraits(String faction) {
        java.util.Vector<String> traits = new java.util.Vector<String>(1, 1);
        java.io.File traitNames = new java.io.File("./data/pilotnames/" + faction.toLowerCase() + "traitnames.txt");

        if (!traitNames.exists()) {
            traitNames = new java.io.File("./data/pilotnames/commontraitnames.txt");
        }

        try {

            MekwarsFileReader dis = new MekwarsFileReader(traitNames);
            while (dis.ready()) {
                traits.addElement(dis.readLine());
            }
            dis.close();
        } catch (java.io.FileNotFoundException nf) {
            MWLogger.errLog("File Not Found: " + traitNames);
        } catch (Exception ex) {
            MWLogger.errLog("Error loading Faction Traits: " + faction);
            MWLogger.errLog(ex);
        }

        traits.trimToSize();
        return traits;
    }

    public void saveFactionTraits(String faction, java.util.Vector<String> traits) {

        java.io.File traitFile = new java.io.File("./data/pilotnames/" + faction.toLowerCase() + "traitnames.txt");

        try {

            if (!traitFile.exists()) {
                traitFile.createNewFile();
            }

            java.io.FileOutputStream fos = new java.io.FileOutputStream(traitFile);
            java.io.PrintStream p = new java.io.PrintStream(fos);

            for (String tempTrait : traits) {
                p.println(tempTrait);
            }

            p.close();
            fos.close();

        } catch (Exception ex) {
            MWLogger.errLog("Error while saving trait file for faction: " + faction);
            MWLogger.errLog(ex);
        }
    }

    public java.util.Hashtable<String, String> getOmniVariantMods() {
        return omniVariantMods;
    }

    public void setOmniVariantMods(java.util.Hashtable<String, String> table) {
        omniVariantMods = table;
    }

    public void saveOmniVariantMods() {

        if (omniVariantMods.size() < 1) {
            return;
        }

        try {

            java.io.FileOutputStream out = new java.io.FileOutputStream("./campaign/omnivariantmods.dat");
            java.io.PrintStream p = new java.io.PrintStream(out);

            for (String currKey : campaignMain.getOmniVariantMods().keySet()) {
                String currMod = campaignMain.getOmniVariantMods().get(currKey);
                p.println(currKey + "#" + currMod);
            }

            p.close();
            out.close();
        } catch (Exception ex) {
            MWLogger.errLog("Error while saving omnivariantmods.dat");
            MWLogger.errLog(ex);
        }
    }

    /**
     * @author Torren (Jason Tighe) This method will go through and check all the player files and forceible
     *       unenroll anyone that is over
     *       <code>days</code> idle.
     */
    public void purgePlayerFiles() {
        long days = Long.parseLong(mekwars.server.campaign.CampaignMain.campaignMain.getConfig("PurgePlayerFilesDays"));
        // Turn purging off by setting it to 0 or less days
        if (days <= 0) {
            return;
        }

        // convert days to milliseconds
        days *= 24;
        days *= 60;
        days *= 60;
        days *= 1000;

        java.io.File[] playerList = new java.io.File("./campaign/players").listFiles();

        for (java.io.File player : playerList) {
            if (player.isDirectory()) {
                continue;
            }
            if (player.lastModified() + days < System.currentTimeMillis()) {
                String playerName = player.getName().substring(0, player.getName().indexOf(".dat"));
                SPlayer p = this.getPlayer(playerName, false, true);
                p.addExperience(100, true);
                Command c = mekwars.server.campaign.CampaignMain.campaignMain.getServerCommands().get("UNENROLL");
                c.process(new java.util.StringTokenizer("CONFIRM", "#"), playerName);
                MWLogger.infoLog(playerName + " purged.");
            }
        }
    }

    /**
     * @param money
     * @param shortname
     * @param amount
     *
     * @return String Hokey function to return the correct syntax for long and short money/flu messages to the user.
     *
     * @author Torren (Jason Tighe)
     */
    public String moneyOrFluMessage(boolean money, boolean shortname, int amount) {
        return moneyOrFluMessage(money, shortname, amount, false);
    }

    public String moneyOrFluMessage(boolean money, boolean shortname, int amount, boolean showSign) {
        String result = java.text.NumberFormat.getInstance().format(amount);
        String moneyShort = campaignMain.getConfig("MoneyShortName").toLowerCase();
        String moneyLong = campaignMain.getConfig("MoneyLongName");
        String fluShort = campaignMain.getConfig("FluShortName").toLowerCase();
        String fluLong = campaignMain.getConfig("FluLongName");
        //        String RPShort = cm.getConfig("RPShortName");
        //        String RPLong = cm.getConfig("RPLongName");

        String sign = "+";

        if (amount < 0) {
            amount *= -1;
            sign = "-";
            result = java.text.NumberFormat.getInstance().format(amount);
        }

        if (!shortname) {
            result += " ";
        }

        if (money) {
            if (shortname) {
                if (amount == 1 && moneyShort.endsWith("s")) {
                    result += moneyShort.substring(0, moneyShort.length() - 1);
                } else if (amount > 1 && !moneyShort.endsWith("s")) {
                    result += moneyShort + "s";
                } else {
                    result += moneyShort;
                }
            }// end shortname if
            else {
                if (amount == 1 && moneyLong.endsWith("s")) {
                    result += moneyLong.substring(0, moneyLong.length() - 1);
                } else if (amount > 1 && !moneyLong.endsWith("s")) {
                    result += moneyLong + "s";
                } else {
                    result += moneyLong;
                }
            }// end shortname else
        }// end money if
        else {
            if (shortname) {
                result += fluShort;
            }// end shortname if
            else {
                result += fluLong;
            }// end shortname else
        }// end money else

        // add sign, if set
        if (showSign) {
            return sign + result;
        }

        return result.trim();
    }

    //@salient
    public String getCurrencyName(String cType, boolean shortDescription) {

        switch (cType.toLowerCase().trim()) {
            case "money":
            case "cb":
                if (shortDescription) {return campaignMain.getConfig("MoneyShortName");} else {
                    return campaignMain.getConfig("MoneyLongName");
                }
            case "rewards":
            case "reward":
            case "rp":
                if (shortDescription) {return campaignMain.getConfig("RPShortName");} else {
                    return campaignMain.getConfig("RPLongName");
                }
            case "influence":
            case "flu":
                if (shortDescription) {return campaignMain.getConfig("FluShortName");} else {
                    return campaignMain.getConfig("FluLongName");
                }
            default:
                MWLogger.errLog(cType + "is not a valid currency");
                return null;
        }

    }

    public void updateISPLists(SPlayer player) {
        java.io.BufferedReader buff = null;
        try {
            java.io.File file = new java.io.File("./data/Providers");
            if (!file.exists()) {
                file.mkdir();
            }

            file = new java.io.File("./data/Providers/" + player.getLastISP() + ".prv");

            if (!file.exists()) {
                saveToISPLists(player);
                return;
            }

            java.io.FileInputStream in = new java.io.FileInputStream(file);
            buff = new java.io.BufferedReader(new java.io.InputStreamReader(in));

            while (buff.ready()) {
                String name = buff.readLine();

                if (name.equalsIgnoreCase(player.getName())) {
                    buff.close();
                    in.close();
                    return;
                }
            }
            saveToISPLists(player);

        } catch (Exception ex) {
        } finally {
            try {
                buff.close();
            } catch (java.io.IOException e) {
                MWLogger.errLog(e);
            }
        }

    }

    public void saveToISPLists(SPlayer player) {
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream("./data/Providers/" +
                                                                              player.getLastISP() +
                                                                              ".prv", true);
            java.io.PrintStream p = new java.io.PrintStream(out);
            p.println(player.getName());
            p.close();
            out.close();
        } catch (Exception ex) {
        }

    }

    public void loadOmniVariantMods() {
        try {
            MekwarsFileReader dis = new MekwarsFileReader("./campaign/omnivariantmods.dat");
            while (dis.ready()) {
                java.util.StringTokenizer line = new java.util.StringTokenizer(dis.readLine(), "#");
                campaignMain.getOmniVariantMods().put(line.nextToken(), line.nextToken());
            }
            dis.close();
        } catch (Exception ex) {
        }
    }

    public boolean isArchiving() {
        return isArchiving;
    }

    public void setArchiving(boolean archive) {
        isArchiving = archive;
    }

    public void saveConfigureFile(java.util.Properties config, String fileName) {
        /*
         *
         * if(CampaignMain.cm.isUsingMySQL()) {
         * CampaignMain.cm.MySQL.saveConfig(); return; }
         */
        try {
            java.io.PrintStream ps = new java.io.PrintStream(new java.io.FileOutputStream(fileName));
            ps.println("#Timestamp=" + System.currentTimeMillis());
            config.store(ps, "Server Config");
            ps.close();
        } catch (java.io.FileNotFoundException fe) {
            MWLogger.errLog(fileName + " not found");
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }// end saveConfigureFile

    public UnitCosts getUnitCostLists() {
        return campaignMain.unitCostLists;
    }

    public RepairTrackingThread getRTT() {
        return RTT;
    }

    public int getTotalRepairCosts(Entity unit, java.util.Vector<Integer> techs, java.util.Vector<Integer> rolls,
          int pilotLevel, SHouse house) {
        double cost = 0;
        double totalArmorCost = 0;
        double internalCost = 0;
        double systemsCost = 0;
        double equipmentCost = 0;
        double weaponsCost = 0;
        double engineCost = 0;

        int techType = techs.elementAt(UnitUtils.ARMOR);
        int baseRoll = rolls.elementAt(UnitUtils.ARMOR);

        double pointsToRepair = 0;
        double armorCost = 0;
        double techCost = 0;
        double techWorkMod = 0;

        if (techType != UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(campaignMain.getConfig(UnitUtils.techDescription(techType) + "TechRepairCost"));
            techWorkMod = UnitUtils.getTechRoll(unit,
                  0,
                  UnitUtils.LOC_FRONT_ARMOR,
                  techType,
                  true,
                  house.getTechLevel()) - baseRoll;
        } else {
            techType = pilotLevel;
        }

        techWorkMod = Math.max(techWorkMod, 0);

        for (int location = 0; location < unit.locations(); location++) {
            if (unit.getArmor(location) < unit.getOArmor(location)) {
                pointsToRepair += unit.getOArmor(location) - unit.getArmor(location);
                armorCost = SUnit.getArmorCost(unit, location);
                totalArmorCost += armorCost * pointsToRepair;
                totalArmorCost += techCost * Math.abs(techWorkMod);
                totalArmorCost += techCost;
            }

            if (unit.hasRearArmor(location)) {
                pointsToRepair += unit.getOArmor(location, true) - unit.getArmor(location, true);
                armorCost = SUnit.getArmorCost(unit, location);
                totalArmorCost += armorCost * pointsToRepair;
                totalArmorCost += techCost * Math.abs(techWorkMod);
                totalArmorCost += techCost;
            }
        }

        // Base on what they assigned as the base roll we increase the payout so
        // that it covers the chances of failures. not the greatest but better
        // then nothing.
        totalArmorCost *= payOutIncreaseBasedOnRoll(baseRoll);
        totalArmorCost = Math.max(0, totalArmorCost);

        techType = techs.elementAt(UnitUtils.INTERNAL);
        baseRoll = rolls.elementAt(UnitUtils.INTERNAL);
        pointsToRepair = 0;
        armorCost = SUnit.getStructureCost(unit);
        techCost = 0;
        techWorkMod = 0;

        if (techType != UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(campaignMain.getConfig(UnitUtils.techDescription(techType) + "TechRepairCost"));
        }

        for (int location = 0; location < unit.locations(); location++) {
            if (unit.getInternal(location) < unit.getOInternal(location)) {
                if (techType != UnitUtils.TECH_PILOT) {
                    techWorkMod = UnitUtils.getTechRoll(unit,
                          location,
                          UnitUtils.LOC_INTERNAL_ARMOR,
                          techType,
                          true,
                          house.getTechLevel()) - baseRoll;
                }

                techWorkMod = Math.max(techWorkMod, 0);
                pointsToRepair = unit.getOInternal(location) - unit.getInternal(location);
                internalCost += armorCost * pointsToRepair;
                internalCost += techCost * Math.abs(techWorkMod);
                internalCost += techCost;
            }
        }

        // Base on what they assigned as the base roll we increase the payout so
        // that it covers the chances of failures. not the greatest but better
        // then nothing.
        internalCost *= payOutIncreaseBasedOnRoll(baseRoll);
        internalCost = Math.max(0, internalCost);

        techType = techs.elementAt(UnitUtils.SYSTEMS);
        baseRoll = rolls.elementAt(UnitUtils.SYSTEMS);
        pointsToRepair = 0;
        double critCost = 0;
        techCost = 0;
        techWorkMod = 0;

        if (techType != UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(campaignMain.getConfig(UnitUtils.techDescription(techType) + "TechRepairCost"));
        }

        for (int location = 0; location < unit.locations(); location++) {
            for (int slot = 0; slot < unit.getNumberOfCriticals(location); slot++) {
                CriticalSlot cs = unit.getCritical(location, slot);
                if (cs == null) {
                    continue;
                }
                if (!cs.isBreached() && !cs.isDamaged()) {
                    continue;
                }
                if (cs.getType() == CriticalSlot.TYPE_SYSTEM && cs.getIndex() != Mech.SYSTEM_ENGINE) {
                    if (techType != UnitUtils.TECH_PILOT) {
                        techWorkMod = UnitUtils.getTechRoll(unit,
                              location,
                              slot,
                              techType,
                              true,
                              house.getTechLevel()) - baseRoll;
                    }

                    critCost = SUnit.getCritCost(unit, cs);
                    techWorkMod = Math.max(techWorkMod, 0);
                    pointsToRepair = UnitUtils.getNumberOfCrits(unit, cs);
                    critCost += techCost;
                    systemsCost += critCost * pointsToRepair;
                    systemsCost += techCost * Math.abs(techWorkMod);
                    systemsCost += techCost;

                    // move the slot ahead if the Crit is more then 1 in size.
                    slot += pointsToRepair - 1;
                }
            }
        }

        // Base on what they assigned as the base roll we increase the payout so
        // that it covers the chances of failures. not the greatest but better
        // then nothing.
        systemsCost *= payOutIncreaseBasedOnRoll(baseRoll);
        systemsCost = Math.max(0, systemsCost);

        techType = techs.elementAt(UnitUtils.WEAPONS);
        baseRoll = rolls.elementAt(UnitUtils.WEAPONS);
        pointsToRepair = 0;
        critCost = 0;
        techCost = 0;
        techWorkMod = 0;

        if (techType != UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(campaignMain.getConfig(UnitUtils.techDescription(techType) + "TechRepairCost"));
        }

        for (int location = 0; location < unit.locations(); location++) {
            for (int slot = 0; slot < unit.getNumberOfCriticals(location); slot++) {
                CriticalSlot cs = unit.getCritical(location, slot);
                if (cs == null) {
                    continue;
                }
                if (!cs.isBreached() && !cs.isDamaged()) {
                    continue;
                }
                if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted mounted = cs.getMount();

                    if (mounted.getType() instanceof WeaponType) {
                        if (techType != UnitUtils.TECH_PILOT) {
                            techWorkMod = UnitUtils.getTechRoll(unit,
                                  location,
                                  slot,
                                  techType,
                                  true,
                                  house.getTechLevel()) - baseRoll;
                        }

                        critCost = SUnit.getCritCost(unit, cs);
                        techWorkMod = Math.max(techWorkMod, 0);
                        pointsToRepair = UnitUtils.getNumberOfCrits(unit, cs);
                        critCost += techCost;
                        weaponsCost += critCost * pointsToRepair;
                        weaponsCost += techCost * Math.abs(techWorkMod);
                        weaponsCost += techCost;

                        // move the slot ahead if the Crit is more then 1 in
                        // size.
                        slot += pointsToRepair - 1;
                    }
                }
            }
        }

        // Base on what they assigned as the base roll we increase the payout so
        // that it covers the chances of failures. not the greatest but better
        // then nothing.
        weaponsCost *= payOutIncreaseBasedOnRoll(baseRoll);
        weaponsCost = Math.max(0, weaponsCost);

        techType = techs.elementAt(UnitUtils.EQUIPMENT);
        baseRoll = rolls.elementAt(UnitUtils.EQUIPMENT);
        pointsToRepair = 0;
        critCost = 0;
        techCost = 0;
        techWorkMod = 0;

        if (techType != UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(campaignMain.getConfig(UnitUtils.techDescription(techType) + "TechRepairCost"));
        }

        for (int location = 0; location < unit.locations(); location++) {
            for (int slot = 0; slot < unit.getNumberOfCriticals(location); slot++) {
                CriticalSlot cs = unit.getCritical(location, slot);
                if (cs == null) {
                    continue;
                }
                if (!cs.isBreached() && !cs.isDamaged()) {
                    continue;
                }
                if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted mounted = cs.getMount();

                    if (!(mounted.getType() instanceof WeaponType)) {
                        if (techType != UnitUtils.TECH_PILOT) {
                            techWorkMod = UnitUtils.getTechRoll(unit,
                                  location,
                                  slot,
                                  techType,
                                  true,
                                  house.getTechLevel()) - baseRoll;
                        }

                        critCost = SUnit.getCritCost(unit, cs);
                        techWorkMod = Math.max(techWorkMod, 0);
                        pointsToRepair = UnitUtils.getNumberOfCrits(unit, cs);
                        critCost += techCost;
                        equipmentCost += critCost * pointsToRepair;
                        equipmentCost += techCost * Math.abs(techWorkMod);
                        equipmentCost += techCost;
                        // move the slot ahead if the Crit is more then 1 in
                        // size.
                        slot += pointsToRepair - 1;
                    }
                }
            }
        }

        // Base on what they assigned as the base roll we increase the payout so
        // that it covers the chances of failures. not the greatest but better
        // then nothing.
        equipmentCost *= payOutIncreaseBasedOnRoll(baseRoll);
        equipmentCost = Math.max(0, equipmentCost);

        techType = techs.elementAt(UnitUtils.ENGINES);
        baseRoll = rolls.elementAt(UnitUtils.ENGINES);
        pointsToRepair = 0;
        critCost = 0;
        techCost = 0;
        techWorkMod = 0;

        boolean found = false;
        int location = 0, slot = 0;
        CriticalSlot cs = null;

        if (techType != UnitUtils.TECH_PILOT) {
            techCost = Integer.parseInt(campaignMain.getConfig(UnitUtils.techDescription(techType) + "TechRepairCost"));
        }

        for (int x = UnitUtils.LOC_CT; x <= UnitUtils.LOC_LT; x++) {
            for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                cs = unit.getCritical(x, y);

                if (cs == null) {
                    continue;
                }

                if (!cs.isDamaged() && !cs.isBreached()) {
                    continue;
                }

                if (!UnitUtils.isEngineCrit(cs)) {
                    continue;
                }

                location = x;
                slot = y;
                found = true;
                break;

            }
            if (found) {
                break;
            }
        }

        if (techType != UnitUtils.TECH_PILOT) {
            techWorkMod = UnitUtils.getTechRoll(unit, location, slot, techType, true, house.getTechLevel()) - baseRoll;
        }

        critCost = SUnit.getCritCost(unit, cs);
        techWorkMod = Math.max(techWorkMod, 0);
        pointsToRepair = UnitUtils.getNumberOfCrits(unit, cs);
        critCost += techCost;
        engineCost += critCost * pointsToRepair;
        engineCost += techCost * Math.abs(techWorkMod);
        engineCost += techCost;

        // Base on what they assigned as the base roll we increase the payout so
        // that it covers the chances of failures. not the greatest but better
        // then nothing.
        engineCost *= payOutIncreaseBasedOnRoll(baseRoll);
        engineCost = Math.max(0, engineCost);

        if (!found) {
            engineCost = 0;
        }

        cost = totalArmorCost + engineCost + systemsCost + internalCost + weaponsCost + equipmentCost;
        return (int) cost;
    }

    private double payOutIncreaseBasedOnRoll(int roll) {
        if (roll <= 2) {
            return 1.0;
        } else if (roll > 12) {
            return 36.0;
        }
        final double[] payout = { 1.0, 1.0, 1.0, 1.03, 1.09, 1.20, 1.38, 1.72, 2.40, 3.60, 5.92, 12.0, 36.0 };
        return payout[roll];
    }

    public int getRepairCost(Entity unit, int critLocation, int critSlot, int techType, boolean armor,
          int techWorkMod) {
        return getRepairCost(unit, critLocation, critSlot, techType, armor, techWorkMod, false);
    }

    public int getRepairCost(Entity unit, int critLocation, int critSlot, int techType, boolean armor, int techWorkMod,
          boolean salvage) {
        double totalCost = 1;
        double techCost = 0;
        double cost = 1;
        int totalCrits = 1;
        int year = getIntegerConfig("CampaignYear");

        if (techType < UnitUtils.TECH_PILOT) {
            techCost = mekwars.server.campaign.CampaignMain.campaignMain.getIntegerConfig(UnitUtils.techDescription(
                  techType) +
                                                                                                "TechRepairCost");
        }

        if (Boolean.parseBoolean(campaignMain.getConfig("UseRealRepairCosts"))) {
            double realCost = UnitUtils.getPartCost(unit, critLocation, critSlot, armor, year);
            if (Boolean.parseBoolean(campaignMain.getConfig("UsePartsRepair"))) {
                realCost = 0;
            }

            double costMod = Double.parseDouble(campaignMain.getConfig("RealRepairCostMod"));
            // modify the cost
            if (costMod > 0) {
                realCost *= costMod;
            }

            cost += (techCost * Math.abs(techWorkMod)) + realCost;
        } else {
            if (armor) {
                if (critSlot == UnitUtils.LOC_FRONT_ARMOR) {
                    cost = SUnit.getArmorCost(unit, critLocation);
                    if (unit.getArmor(critLocation) > unit.getOArmor(critLocation)) {
                        // remove the repairing armor so we can get the real
                        // cost.
                        UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, critLocation);
                        cost *= unit.getOArmor(critLocation) - unit.getArmor(critLocation);
                        // Add the repairing armor flag back on.
                        UnitUtils.setArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, critLocation);
                    } else {
                        cost *= unit.getOArmor(critLocation) - unit.getArmor(critLocation);
                    }

                    cost += techCost * Math.abs(techWorkMod);
                    cost += techCost;
                    cost = Math.max(1, cost);
                } else if (critSlot == UnitUtils.LOC_REAR_ARMOR) {
                    // tell the repair command its using rear external armor
                    // Need to move this above the getArmorCost because it's
                    // sending back index to get the loc.
                    // 07 Sept 2011 - Cord Awtry
                    if (critLocation >= UnitUtils.LOC_CTR) {
                        critLocation -= 7;
                    }
                    cost = SUnit.getArmorCost(unit, critLocation);
                    if (unit.getArmor(critLocation, true) > unit.getOArmor(critLocation, true)) {
                        // remove the repairing armor so we can get the real
                        // cost.
                        UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_REAR_ARMOR, critLocation);
                        cost *= unit.getOArmor(critLocation, true) - unit.getArmor(critLocation, true);
                        // Add the repairing armor flag back on.
                        UnitUtils.setArmorRepair(unit, UnitUtils.LOC_REAR_ARMOR, critLocation);
                    } else {
                        cost *= unit.getOArmor(critLocation, true) - unit.getArmor(critLocation, true);
                    }

                    cost += techCost * Math.abs(techWorkMod);
                    cost += techCost;
                    cost = Math.max(1, cost);
                } else {
                    cost = SUnit.getStructureCost(unit);
                    if (unit.getInternal(critLocation) > unit.getOInternal(critLocation)) {
                        // remove the repairing armor so we can get the real
                        // cost.
                        UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_INTERNAL_ARMOR, critLocation);
                        cost *= unit.getOInternal(critLocation) - unit.getInternal(critLocation);
                        // Add the repairing armor flag back on.
                        UnitUtils.setArmorRepair(unit, UnitUtils.LOC_INTERNAL_ARMOR, critLocation);
                    } else {
                        cost *= unit.getOInternal(critLocation) - unit.getInternal(critLocation);
                    }

                    cost += techCost * Math.abs(techWorkMod);
                    cost += techCost;
                    cost = Math.max(1, cost);
                }
            } else {
                CriticalSlot cs = unit.getCritical(critLocation, critSlot);
                if (salvage) {
                    totalCrits = UnitUtils.getNumberOfCrits(unit, cs) -
                                       UnitUtils.getNumberOfDamagedCrits(unit, critSlot, critLocation, armor);
                } else {
                    totalCrits = UnitUtils.getNumberOfDamagedCrits(unit, critSlot, critLocation, armor);
                }
                cost = SUnit.getCritCost(unit, cs);
                totalCost = (int) (totalCrits * cost);
                totalCost += (int) (totalCrits * techCost);
                totalCost += techCost;
                totalCost += techCost * Math.abs(techWorkMod);
                cost = Math.max(1, totalCost);
            }// end critslot else
        }

        if (Boolean.parseBoolean(campaignMain.getConfig("AllowCritRepairsForRewards")) &&
                  techType == UnitUtils.TECH_REWARD_POINTS) {
            cost = totalCrits * Double.parseDouble(campaignMain.getConfig("RewardPointsForCritRepair"));
            cost = Math.max(Math.ceil(cost), 1);
        }

        return (int) cost;
    }

    public int getIntegerConfig(String key) {
        try {
            return Integer.parseInt(campaignMain.getConfig(key));
        } catch (Exception ex) {
            return -1;
        }
    }

    public void saveBannedAmmo() {

        // Save banned ammo
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream("./campaign/banammo.dat");
            java.io.PrintStream p = new java.io.PrintStream(out);

            // server banned ammo
            p.println(System.currentTimeMillis());
            p.print("server#");
            for (String ammo : mekwars.server.campaign.CampaignMain.campaignMain.getServerBannedAmmo().keySet()) {
                p.print(ammo);
                p.print("#");
            }
            p.println();

            // faction banned ammo
            for (House currH : data.getAllHouses()) {

                SHouse h = (SHouse) currH;
                if (h.getBannedAmmo().size() < 1) {
                    continue;
                }

                p.print(h.getName() + "#");
                for (String ammo : h.getBannedAmmo().keySet()) {
                    p.print(ammo);
                    p.print("#");
                }
                p.println();

            }
            p.close();
            out.close();

        } catch (Exception ex) {
            MWLogger.errLog("Error saving banned ammo.");
            MWLogger.errLog(ex);
        }
    }

    public java.util.Hashtable<String, String> getServerBannedAmmo() {
        return campaignMain.getData().getServerBannedAmmo();
    }

    public void saveBannedTargetSystems() {
        java.io.FileOutputStream out = null;
        try {
            out = new java.io.FileOutputStream("./campaign/bantarget.dat");
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
        }
        java.io.PrintStream p = new java.io.PrintStream(out);
        for (int ban : getData().getBannedTargetingSystems()) {
            p.print(ban);
            p.print("#");
        }
        p.println();
        p.close();
        try {
            out.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public void loadPlanetOpFlags() {
        java.io.File configFile = new java.io.File("./campaign/planetOpFlags.dat");
        if (!configFile.exists()) {
            MWLogger.errLog("No planetOpFlags.dat. Skipping.");
            return;
        }

        try {
            MekwarsFileReader dis = new MekwarsFileReader(configFile);
            dis.readLine();// Time Stamp

            String nextLine = dis.readLine();
            if (nextLine == null) {
                MWLogger.errLog("Timestamp-only planetOpFlags.dat. Skipping.");
                return;
            }

            java.util.StringTokenizer st = new java.util.StringTokenizer(nextLine, "#");
            while (st.hasMoreTokens()) {
                data.getPlanetOpFlags().put(st.nextToken(), st.nextToken());
            }

            dis.close();
        } catch (Exception ex) {
            MWLogger.errLog("Error loading Planet Op Flags.");
            MWLogger.errLog(ex);
        }
    }

    public void savePlanetOpFlags() {
        // Save Planet Op Flags
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream("./campaign/planetOpFlags.dat");
            java.io.PrintStream p = new java.io.PrintStream(out);
            p.println(System.currentTimeMillis());
            for (String key : mekwars.server.campaign.CampaignMain.campaignMain.getData().getPlanetOpFlags().keySet()) {
                p.print(key);
                p.print("#");
                p.print(mekwars.server.campaign.CampaignMain.campaignMain.getData().getPlanetOpFlags().get(key));
                p.print("#");
            }
            p.close();
            out.close();
        } catch (Exception ex) {
            MWLogger.errLog("Error saving Planet Op Flags.");
            MWLogger.errLog(ex);
        }
    }

    public void loadFactionData() {
        java.io.File factionFile = new java.io.File("./campaign/factions");

        // Check for new faction save location
        if (!factionFile.exists() || factionFile.listFiles().length < 1) {
            MWLogger.errLog("Unable to find and load faction data");
            MWLogger.errLog("Going to create from XML");
            return;
        }

        // filter out .bak's
        java.io.FilenameFilter filter = new mekwars.server.campaign.CampaignMain.datFileFilter();
        java.io.File[] factionFileList = factionFile.listFiles(filter);

        // load each file
        for (java.io.File faction : factionFileList) {
            try {
                MekwarsFileReader dis = new MekwarsFileReader(faction);
                String line = dis.readLine();
                SHouse h;
                if (line.startsWith("[N][C]")) {
                    line = line.substring(6);
                    h = new NewbieHouse(data.getUnusedHouseID());
                } else if (line.startsWith("[N]")) {
                    line = line.substring(3);
                    h = new NewbieHouse(data.getUnusedHouseID());
                } else if (line.startsWith("[M]")) {
                    line = line.substring(3);
                    h = new MercHouse(data.getUnusedHouseID());
                } else {
                    h = new SHouse(data.getUnusedHouseID());
                }
                h.fromString(line, r);
                if (isUsingIncreasedTechs()) {
                    h.addCommonUnitSupport();
                }
                addHouse(h);
                dis.close();
            } catch (Exception ex) {
                MWLogger.errLog("Unable to load " + faction.getName());
            }
        }

        if (data.getHouse(-1) == null) {
            SHouse none = new MercHouse();
            none.createNoneHouse();
            addHouse(none);
        }

        // load the various construction modifiers for the houses added above
        factionFile = new java.io.File("./campaign/costmodifiers");
        if (!factionFile.exists()) {
            return;// done
        }

        for (House currH : data.getAllHouses()) {

            String saveName = currH.getName().toLowerCase().trim() + ".dat";
            java.io.File faction = new java.io.File("./campaign/costmodifiers/" + saveName);

            if (!faction.exists()) {
                continue;
            }
            try {
                MekwarsFileReader dis = new MekwarsFileReader(faction);

                String currLine = null;
                while ((currLine = dis.readLine()) != null) {
                    java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(currLine, "$");
                    String cost = tokenizer.nextToken();
                    int type = Integer.parseInt(tokenizer.nextToken());
                    int weight = Integer.parseInt(tokenizer.nextToken());
                    int mod = Integer.parseInt(tokenizer.nextToken());

                    if (cost.equals("Price")) {
                        currH.setHouseUnitPriceMod(type, weight, mod);
                    } else if (cost.equals("Flu")) {
                        currH.setHouseUnitFluMod(type, weight, mod);
                    } else if (cost.equals("Comp")) {
                        currH.setHouseUnitComponentMod(type, weight, mod);
                    }
                }
                dis.close();
            } catch (Exception e) {
                MWLogger.errLog("Unable to load cost modifiers for " + currH.getName());
            }
        }
    }

    // Save Houses
    public void saveFactionData() {
        java.io.File factionFile = new java.io.File("./campaign/factions");
        if (!factionFile.exists()) {
            factionFile.mkdir();
            if (isUsingIncreasedTechs()) {
                java.io.File supportFile = new java.io.File("./campaign/factions/support");
                supportFile.mkdir();
            }
        }
        factionFile = new java.io.File("./campaign/costmodifiers");
        if (!factionFile.exists()) {
            factionFile.mkdir();
        }

        synchronized (data.getAllHouses()) {
            for (House currH : data.getAllHouses()) {
                SHouse h = (SHouse) currH;

                String saveName = h.getName().toLowerCase().trim() + ".dat";
                String backupName = h.getName().toLowerCase().trim() + ".bak";

                // standard save
                try {

                    java.io.File faction = new java.io.File("./campaign/factions/" + saveName);

                    if (faction.exists()) {

                        java.io.File backupFile = new java.io.File("./campaign/factions/" + backupName);
                        if (backupFile.exists()) {
                            backupFile.delete();
                        }

                        faction.renameTo(backupFile);
                    }
                    java.io.FileOutputStream out = new java.io.FileOutputStream(faction);
                    java.io.PrintStream p = new java.io.PrintStream(out);

                    p.println(h.toString());

                    try {
                        java.io.File factionCostMod = new java.io.File("./campaign/costmodifiers/" + saveName);
                        if (factionCostMod.exists()) {

                            java.io.File backupFile = new java.io.File("./campaign/costmodifiers/" + backupName);
                            if (backupFile.exists()) {
                                backupFile.delete();
                            }

                            factionCostMod.renameTo(backupFile);
                        }

                        java.io.FileOutputStream costModout = new java.io.FileOutputStream(factionCostMod);
                        java.io.PrintStream costModp = new java.io.PrintStream(costModout);

                        for (int type = 0; type < 5; type++) {
                            for (int weight = 0; weight < 4; weight++) {

                                if (h.getHouseUnitPriceMod(type, weight) != 0) {
                                    costModp.println("Price$" +
                                                           type +
                                                           "$" +
                                                           weight +
                                                           "$" +
                                                           h.getHouseUnitPriceMod(type, weight));
                                }
                                if (h.getHouseUnitFluMod(type, weight) != 0) {
                                    costModp.println("Flu$" +
                                                           type +
                                                           "$" +
                                                           weight +
                                                           "$" +
                                                           h.getHouseUnitFluMod(type, weight));
                                }
                                if (h.getHouseUnitComponentMod(type, weight) != 0) {
                                    costModp.println("Comp$" +
                                                           type +
                                                           "$" +
                                                           weight +
                                                           "$" +
                                                           h.getHouseUnitComponentMod(type, weight));
                                }

                            }
                        }
                        costModp.close();
                        costModout.close();

                    } catch (Exception ex) {
                        MWLogger.errLog("Unable to save Faction: " + saveName + " cost Mods");
                        MWLogger.errLog(ex);
                    }
                    p.close();
                    out.close();
                } catch (Exception ex) {
                    MWLogger.errLog("Unable to save Faction: " + saveName);
                    MWLogger.errLog(ex);
                }
            }
        }
    }

    public void loadPlanetData() {

        loadPlanetOpFlags();

        java.io.File planetFile = new java.io.File("./campaign/planets");
        java.io.FilenameFilter filter = new mekwars.server.campaign.CampaignMain.datFileFilter();

        // Check for faction save dir & ensure dat files exist therein
        if (!planetFile.exists() || planetFile.listFiles(filter).length == 0) {
            MWLogger.errLog("Unable to find and load /planets, or /planets is empty.");
            MWLogger.errLog("Planets will be read from XML during init().");
            return;
        }
        // dir and files exist. read them.
        java.io.File[] planetFileList = planetFile.listFiles(filter);
        for (java.io.File planet : planetFileList) {

            try {
                MekwarsFileReader dis = new MekwarsFileReader(planet);
                String line = dis.readLine();
                SPlanet p;
                if (line.startsWith("[N]")) {
                    line = line.substring(3);
                }
                p = new SPlanet();
                p.fromString(line, r, data);
                addPlanet(p);
                dis.close();
            } catch (Exception ex) {
                MWLogger.errLog("Unable to load " + planet.getName());
                MWLogger.errLog(ex);
            }
        }
    }

    public void updatePlayersAccessLevel(String playerName, int accessLevel) {
        SPlayer player = campaignMain.getPlayer(playerName);

        if (player == null) {
            return;
        }
        try {
            campaignMain.getServer().getClient(playerName).setAccessLevel(accessLevel);
            campaignMain.getServer().getUser(playerName).setLevel(accessLevel);
            campaignMain.getServer().sendRemoveUserToAll(playerName, false);
            campaignMain.getServer().sendNewUserToAll(playerName, false);
            MWPasswd.writeRecord(player.getPassword(), playerName);
            campaignMain.doSendToAllOnlinePlayers("PI|DA|" + campaignMain.getPlayerUpdateString(player), false);
        } catch (Exception ex) {
        }
        forceSavePlayer(player);
    }

    public String getPlayerUpdateString(SPlayer p) {

        StringBuffer result = new StringBuffer();
        if (p == null) {
            return result.toString();
        }

        // Hide Reserve and Active Status
        int Status = p.getDutyStatus();
        if (Status == SPlayer.STATUS_RESERVE && Boolean.parseBoolean(getConfig("HideActiveStatus"))) {
            Status = SPlayer.STATUS_ACTIVE;
        }

        result.append(p.getName());
        result.append("|");
        result.append(p.getExperience());
        result.append("#");
        if (Boolean.parseBoolean(getConfig("HideELO"))) {
            result.append("0");
        } else {
            result.append(p.getRatingRounded());
        }

        result.append("#");
        result.append(Status);
        result.append("#");
        if (p.getFluffText().equals("")) {
            result.append(" #");
        } else {
            result.append(p.getFluffText());
            result.append("#");
        }

        result.append(p.getHouseFightingFor().getName());
        result.append("#");
        result.append(p.getMyHouse().isMercHouse());
        result.append("#");
        result.append(p.getSubFactionName());
        return result.toString();
    }

    /**
     * Public save method to save one player Used by changename and defect commands This is used so the players have a
     * Pfile created right away
     */
    public void forceSavePlayer(SPlayer p) {

        savePlayerFile(p);
    }

    /**
     * this removes a SPlayer object form the global hash. This is called when a player logs into a house, in which case
     * the house now stores the object, or when the player logs off, incase they never bothred to register or login.
     *
     * @param soul
     */
    public void releaseLostSoul(String soul) {
        lostSouls.remove(soul.toLowerCase());
    }

    public java.util.Date getHousePlanetUpdate() {
        return housePlanetDate;
    }

    public void updateHousePlanetUpdate() {
        housePlanetDate = new java.util.Date();
    }

    // Save Planets
    public void savePlanetData() {
        savePlanetOpFlags();
        java.io.File planetFile = new java.io.File("./campaign/planets");
        if (!planetFile.exists()) {
            planetFile.mkdir();
        }
        synchronized (data.getAllPlanets()) {

            for (Planet currP : data.getAllPlanets()) {
                SPlanet p = (SPlanet) currP;
                String saveName = p.getName().toLowerCase().trim() + ".dat";
                String backupName = p.getName().toLowerCase().trim() + ".bak";
                try {
                    java.io.File planet = new java.io.File("./campaign/planets/" + saveName);

                    if (planet.exists()) {

                        java.io.File backupFile = new java.io.File("./campaign/planets/" + backupName);
                        if (backupFile.exists()) {
                            backupFile.delete();
                        }

                        planet.renameTo(backupFile);
                    }

                    java.io.FileOutputStream out = new java.io.FileOutputStream("./campaign/planets/" + saveName);
                    java.io.PrintStream ps = new java.io.PrintStream(out);
                    ps.println(p.toString());
                    ps.close();
                    out.close();
                } catch (Exception ex) {
                    MWLogger.errLog("Unable to save planet: " + saveName);
                    MWLogger.errLog(ex);
                }
            }
        }
    }

    public void saveMegaMekGameOptions(java.util.StringTokenizer gameOptions) {
        java.io.File mmGameOptionsFolder = new java.io.File("./mmconf");

        if (!mmGameOptionsFolder.exists()) {mmGameOptionsFolder.mkdir();}

        java.io.File mmGameOptions = new java.io.File("./mmconf/gameoptions.xml");
        try {
            java.io.FileOutputStream fops = new java.io.FileOutputStream(mmGameOptions);
            java.io.PrintStream out = new java.io.PrintStream(fops);
            while (gameOptions.hasMoreTokens()) {
                out.println(gameOptions.nextToken());
            }
            out.close();
            fops.close();
        } catch (Exception ex) {
            MWLogger.errLog("Unable to save Mega Mek Game Options!");
            MWLogger.errLog(ex);
        }

    }

    public String getMegaMekOptionsToString() {
        StringBuffer result = new StringBuffer();

        java.util.Enumeration<IOption> options = campaignMain.getMegaMekClient().getGame().getOptions().getOptions();

        while (options.hasMoreElements()) {
            IOption option = options.nextElement();

            result.append(option.getName()).append("|").append(option.getValue()).append("|");
        }
        return result.toString();
    }

    public Client getMegaMekClient() {
        return megaMekClient;
    }

    public void setMegaMekClient(Client mmClient) {
        campaignMain.megaMekClient = mmClient;
    }

    /**
     * @return the supportUnits
     */
    public java.util.Vector<String> getSupportUnits() {
        return supportUnits;
    }

    /**
     * @param supportUnits the supportUnits to set
     */
    public void setSupportUnits(java.util.Vector<String> supportUnits) {
        this.supportUnits = supportUnits;
    }

    /**
     * @return the defaultPlayerFlags
     */
    public PlayerFlags getDefaultPlayerFlags() {
        return defaultPlayerFlags;
    }

    /**
     * @return the scheduler
     */
    public MWScheduler getScheduler() {
        return scheduler;
    }

    /**
     * @param scheduler the scheduler to set
     */
    public void setScheduler(MWScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Send a message to a Discord Webhook
     *
     * @param message the message to send
     */
    public void postToDiscord(String message) {
        if (!mekwars.server.campaign.CampaignMain.campaignMain.getBooleanConfig("DiscordEnable")) {
            return;
        }
        DiscordMessageHandler handler = new DiscordMessageHandler();
        handler.post(message);
    }

    class datFileFilter implements java.io.FilenameFilter {
        public boolean accept(java.io.File dir, String name) {
            return (name.endsWith(".dat"));
        }
    }

}
