package mekwars.client.protocol;

import megamek.logging.MMLogger;
import mekwars.client.MWClient;
import mekwars.common.CampaignData;
import mekwars.common.Equipment;
import mekwars.common.House;
import mekwars.common.Influences;
import mekwars.common.Planet;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.CMainFrame;
import mekwars.common.persistence.BinReader;
import mekwars.common.persistence.BinWriter;

/**
 * Calls to the data retrieving server and gets data for planets and factions
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class DataFetchClient {
    private static final MMLogger LOGGER = MMLogger.create(DataFetchClient.class);

    private String hostAddr;
    private String cacheDir;
    private CampaignData data;
    private java.util.Map<Integer, Influences> changesSinceLastRefresh;
    private java.util.Date lastTimestamp = null;
    // private Date latestTimeStamp = null;
    private int dataPort = 4867;
    private java.net.Socket dataSocket = null;
    private int socketDelayTime = 2000;

    /**
     * Constructor. This will not setup the connection. To actually transfer data use the get*() methods. Remember to
     * set the host address before calling any get* methods. This cannot be set here, because DataFetchClient is used
     * with xstream and persistance and we want the users to change the address in the config, not the cache file.
     */
    public DataFetchClient(int dataport, int socketDelayTime) {
        dataPort = dataport;
        changesSinceLastRefresh = new java.util.HashMap<Integer, Influences>();

        if (socketDelayTime > 0) {
            this.socketDelayTime = socketDelayTime;
        } else {
            this.socketDelayTime = 2000;
        }

    }

    /**
     * Transfer the server configuration files. Used to set up verious portions of the GUI, determing proper Money/Flu
     * names, and more.
     */
    public void getServerConfigData(MWClient mwclient) throws java.io.IOException {

        /*
         * Look for an existing serverconfig.txt in the appropriate data dir. If
         * it exists, MD5 is and request the MD5 of its server side analog.
         *
         * If the MD5's don't match, or the local file doesnt exist, force a
         * refresh from the data feeder.
         */
        boolean timestampMatch = false;
        boolean localConfigExists = false;
        java.io.File localConfig = new java.io.File(cacheDir + "/campaignconfig.txt");

        // loacl read first
        if (localConfig.exists()) {

            // note that the config exists
            localConfigExists = true;

            // get the local timetamp
            String localConfigTimestamp = "";

            try {
                java.io.FileInputStream in = new java.io.FileInputStream(cacheDir + "/campaignconfig.txt");
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                String tempTime = br.readLine();
                br.close();
                in.close();

                localConfigTimestamp = tempTime.substring(11);// remove
                // "#Timestamp="
            } catch (Exception e) {
                LOGGER.error("Problems reading timestamp from local configuration.");
            }

            // now get the Server MD5
            String serverConfigTimestamp = "error";
            try {
                BinReader in = openConnection("ConfigTimestamp");
                serverConfigTimestamp = in.readLine("ConfigTimestamp");
            } catch (Exception e) {
                LOGGER.error("Problems connecting to server to get config timestamp.");
            }

            LOGGER.error("Local Config: " + localConfigTimestamp + " Server Config: " + serverConfigTimestamp);
            if (localConfigTimestamp.equals(serverConfigTimestamp)) {
                timestampMatch = true;
                try {
                    java.io.FileInputStream configFile = new java.io.FileInputStream(cacheDir + "/campaignconfig.txt");
                    mwclient.getserverConfigs().load(configFile);
                    configFile.close();
                } catch (Exception ex) {
                    timestampMatch = false;
                }
            }

        }// end if(localConfigExists, get MD5)

        /*
         * If the config is missing, or the timestamps dont match, update
         */
        if (!timestampMatch || !localConfigExists) {

            // delete the old file, if it exists
            if (localConfigExists) {
                java.io.File f = new java.io.File(cacheDir + "/campaignconfig.txt");
                f.delete();
            }

            // open the connection to the server, and write out the config
            try {

                BinReader in = openConnection("ServerConfig");
                java.io.FileOutputStream fops = new java.io.FileOutputStream(cacheDir + "/campaignconfig.txt");
                java.io.PrintStream out = new java.io.PrintStream(fops);

                // keep reading until there is an error.
                try {
                    while (true) {
                        out.println(in.readLine("ConfigLine"));
                    }
                } catch (Exception e) {

                    // close the streams
                    // in.close();
                    out.close();
                    fops.close();

                    // read in the "complete" config
                    try {
                        java.io.FileInputStream configFile = new java.io.FileInputStream(cacheDir +
                                                                                               "/campaignconfig.txt");
                        mwclient.getserverConfigs().load(configFile);
                        configFile.close();
                    } catch (Exception ex) {
                        LOGGER.error(ex, "");
                    }
                }// end catch for read-in
            }

            // failed to open connection. try to load local defaults.
            catch (Exception exe) {
                if (!mwclient.getConfig().isParam("DEDICATED")) {
                    Object[] options = { "Exit", "Continue" };
                    int selectedValue = javax.swing.JOptionPane.showOptionDialog(null,
                          "No campaignconfig.txt. This usually means that you were unable to\n\r" +
                                "connect to the server to fetch a copy. Do you wish to exit?",
                          "Startup\n\r" + "error!",
                          javax.swing.JOptionPane.DEFAULT_OPTION,
                          javax.swing.JOptionPane.ERROR_MESSAGE,
                          null,
                          options,
                          options[0]);
                    if (selectedValue == 0) {
                        System.exit(0);// exit, if they so choose
                    }
                }
            }// end catch(Connection Failure)

        }// end if(!md5Match || !localConfigExists)
    }

    /**
     * Transfer the Black Market Settings Only called from Admin.ComponentDisplayDialog
     */
    public void getBlackMarketSettings(client.MWClient mwclient) throws java.io.IOException {

        // open the connection to the server, and write out the config
        try {

            BinReader in = openConnection("BMSetting");

            int count = in.readInt("BMSetting");

            // keep reading until there is an error.
            try {
                for (; count > 0; count--) {

                    Equipment bme = new Equipment();

                    bme.setEquipmentInternalName(in.readLine("BMSetting"));
                    bme.setMinCost(in.readDouble("BMSetting"));
                    bme.setMaxCost(in.readDouble("BMSetting"));
                    bme.setMinProduction(in.readInt("BMSetting"));
                    bme.setMaxProduction(in.readInt("BMSetting"));

                    // updated is used for when the data is update in the
                    // dialog.
                    bme.setUpdated(false);
                    mwclient.getBlackMarketEquipmentList().put(bme.getEquipmentInternalName(), bme);
                }
            } catch (Exception e) {
                LOGGER.error(e, "");
            }// end catch for read-in
        }

        // failed to open connection. try to load local defaults.
        catch (Exception exe) {
            LOGGER.error(exe, "");
        }// end catch(Connection Failure)
    }

    /**
     * Transfers server wide banned ammo data from the server to the client.
     *
     */
    public void getBannedAmmoData(client.MWClient mwclient) throws java.io.IOException {

        boolean timestampMatch = false;
        java.io.File localban = new java.io.File(cacheDir + "/banammo.dat");
        if (localban.exists()) {

            // get the local timetamp
            String localListTimestamp = "";

            try {
                java.io.FileInputStream in = new java.io.FileInputStream(localban);
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                localListTimestamp = br.readLine();
                br.close();
                in.close();

                BinReader binreader = openConnection("BannedAmmoTimeStamp");
                String serverTimeStamp = binreader.readLine("BannedAmmoTimeStamp");

                LOGGER.error("Local Ban: " + localListTimestamp + " Server Ban: " + serverTimeStamp);
                if (localListTimestamp.equals(serverTimeStamp)) {
                    timestampMatch = true;
                }
            } catch (Exception e) {
                LOGGER.error("Problems reading timestamp from local banammo.dat.");
            }

        }

        // clear the hash so we can add all the new stuff --Torren
        mwclient.clearBanAmmo();
        if (!timestampMatch) {
            BinReader in = openConnection("BannedAmmo");
            String timestamp = "-1";
            try {
                timestamp = in.readLine("BannedAmmo");// TIMESTAMP
                while (true) {
                    mwclient.loadBanAmmo(in.readLine("BannedAmmo"));
                }
            } catch (Exception ex) {
            }// Bin empty
            mwclient.saveBannedAmmo(timestamp);
        } else {// load from the banned file.
            try {
                java.io.FileInputStream fis = new java.io.FileInputStream(mwclient.getCacheDir() + "/banammo.dat");
                java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
                while (dis.ready()) {
                    String line = dis.readLine();
                    mwclient.loadBanAmmo(line);
                }
                dis.close();
                fis.close();
            } catch (Exception ex) {
                LOGGER.error(ex, "");
            }
        }

    }

    /**
     * Transfers server wide banned targeting systems from the server to the client.
     *
     */
    public void getBanTargetingData(client.MWClient mwclient) throws java.io.IOException {

        boolean timestampMatch = false;
        java.io.File localban = new java.io.File(cacheDir + "/bantargeting.dat");
        if (localban.exists()) {

            // get the local timetamp
            String localListTimestamp = "";

            try {
                java.io.FileInputStream in = new java.io.FileInputStream(localban);
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                localListTimestamp = br.readLine();
                br.close();
                in.close();

                BinReader binreader = openConnection("BanTargetingTimeStamp");
                String serverTimeStamp = binreader.readLine("BanTargetingTimeStamp");

                LOGGER.error("Local BanT: " + localListTimestamp + " Server BanT: " + serverTimeStamp);
                if (localListTimestamp.equals(serverTimeStamp)) {
                    timestampMatch = true;
                }
            } catch (Exception e) {
                LOGGER.error("Problems reading timestamp from local bantargeting.dat.");
            }

        }

        // clear the hash so we can add all the new stuff --Torren
        //client.clearBanTargeting();
        if (!timestampMatch) { // BanTargeting
            BinReader in = openConnection("BanTargeting");
            String timestamp = "-1";
            try {
                timestamp = in.readLine("BanTargeting");// TIMESTAMP
                mwclient.loadBanTargeting(in.readLine("BanTargeting"));
            } catch (Exception ex) {
            }// Bin empty
            mwclient.saveBannedTargetingSystems(timestamp);
        } else {// load from the banned file.
            try {
                java.io.FileInputStream fis = new java.io.FileInputStream(mwclient.getCacheDir() + "/bantargeting.dat");
                java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
                dis.readLine();
                mwclient.loadBanTargeting(dis.readLine());
                dis.close();
                fis.close();
            } catch (Exception ex) {
            }
        }

    }

    /**
     * Check Server version against client if it doesn't match you can't connect
     *
     */
    public void checkServerVersion(IClient client) throws java.io.IOException {

        boolean mustUpdate = false;
        String clientVersion = client.MWClient.CLIENT_VERSION;

        clientVersion = clientVersion.substring(0, clientVersion.lastIndexOf("."));

        BinReader binreader = openConnection("ServerVersion");
        String serverVersion = binreader.readLine("ServerVersion");

        serverVersion = serverVersion.substring(0, serverVersion.lastIndexOf("."));

        LOGGER.error("client Version: " + clientVersion + " Server Version: " + serverVersion);
        mustUpdate = !serverVersion.equalsIgnoreCase(clientVersion);

        // If the versions dont match then the client has to update anyways
        if (!mustUpdate) {
            binreader = openConnection("ForceUpdateKey");
            String forceUpdateKey = binreader.readLine("ForceUpdateKey");
            String clientUpdateKey = client.getConfigParam("UPDATEKEY");

            LOGGER.error("Server Key: " + forceUpdateKey);
            // the server update key starts out blank. So the update only works
            // after a key is set server side.
            if (forceUpdateKey.trim().length() > 1) {
                mustUpdate = !forceUpdateKey.equals(clientUpdateKey);
            }
        }

        if (mustUpdate) {
            int update = javax.swing.JOptionPane.NO_OPTION;
            if (!client.isDedicated()) {
                update = javax.swing.JOptionPane.showConfirmDialog(null,
                      "You have an invalid version\n\rof the MekWars client\n\rWould you like to update now?",
                      "Invalid client update now!",
                      javax.swing.JOptionPane.YES_NO_OPTION);

                if (update == javax.swing.JOptionPane.YES_OPTION) {
                    try {
                        client.goodbye();
                        Runtime runtime = Runtime.getRuntime();
                        String[] call = { "java", "-jar", "./MekWarsAutoUpdate.jar", "PLAYER" };
                        runtime.exec(call);
                        LOGGER.error("Starting Update!");
                    } catch (Exception ex) {
                        LOGGER.error(ex, "");
                    }
                }

            } else {// is Ded
                try {
                    client.stopHost();
                    client.goodbye();
                    Runtime runtime = Runtime.getRuntime();
                    String[] call = { "java", "-jar", "MekWarsAutoUpdate.jar", "DEDICATED" };
                    runtime.exec(call);
                } catch (Exception ex) {
                    LOGGER.error(ex, "");
                }

            }

            System.exit(0);

        }
    }

    /**
     * Transfer the server configuration files. Used to set up verious portions of the GUI, determing proper Money/Flu
     * names, and more.
     */
    public void checkForMostRecentOpList() throws java.io.IOException {

        /*
         * Look for an existing OpList.txt in the appropriate data dir. If it
         * exists, MD5 it and request the MD5 of its server side analog.
         *
         * If the timestamps don't match, force a refresh from the data feeder.
         */
        boolean timestampMatch = false;
        java.io.File localList = new java.io.File(cacheDir + "/OpList.txt");
        if (localList.exists()) {

            // get the local timetamp
            String localListTimestamp = "";

            try {
                java.io.FileInputStream in = new java.io.FileInputStream(cacheDir + "/OpList.txt");
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                String tempTime = br.readLine();
                br.close();
                in.close();

                localListTimestamp = tempTime.substring(11);// remove
                // "#Timestamp="
            } catch (Exception e) {
                LOGGER.error("Problems reading timestamp from local OpList.");
            }

            // now get the server list's timestamp ...
            BinReader in = openConnection("OpListTimestamp");
            String serverListTimestamp = in.readLine("OpListTimestamp");
            LOGGER.error("Local OpList: " + localListTimestamp + " Server OpList: " + serverListTimestamp);
            if (localListTimestamp.equals(serverListTimestamp)) {
                timestampMatch = true;
            }

        }// end if(localList.exists)

        /*
         * If the MD5s dont match, update
         */
        if (!timestampMatch) {

            // delete the old file, if it exists
            java.io.File f = new java.io.File(cacheDir + "/OpList.txt");
            if (f.exists()) {
                f.delete();
            }

            // open the connection to the server, and write out the list
            try {

                BinReader in = openConnection("OpList");
                java.io.FileOutputStream fops = new java.io.FileOutputStream(cacheDir + "/OpList.txt");
                java.io.PrintStream out = new java.io.PrintStream(fops);
                try {

                    // keep reading new lines until there is an error.
                    while (true) {
                        out.println(in.readLine("ListLine"));
                    }

                } catch (Exception e) {

                    // close the streams
                    // //in.close();
                    out.close();
                    fops.close();

                }
            } catch (Exception exe) {
                LOGGER.error(exe, "");
            }
        }// end if(!md5Match)
    }// end getOpListMD5

    /**
     * Transfer trait data. Used to generate the Trait dialogs in Help menu.
     * <p>
     * Regardless of the data sent, if there is a 0% chance for meks to get the trait skill the help menu will not be
     * shown.
     *
     * @see CMainFrame.java
     */
    public void getServerTraitFiles() throws java.io.IOException {

        try {
            // MMClient.mwClientLog.clientErrLog("- opening connection to datafeed. requesting Trait Files");
            BinReader in = openConnection("ServerTrait");

            // keep reading until there is an error.
            try {
                while (true) {
                    String faction = in.readLine("TraitLine");
                    int count = in.readInt("TraitLine");
                    java.io.FileOutputStream fops = new java.io.FileOutputStream(cacheDir +
                                                                                       "/" +
                                                                                       faction.toLowerCase() +
                                                                                       "traitnames.txt");
                    java.io.PrintStream out = new java.io.PrintStream(fops);
                    for (; count > 0; count--) {
                        out.println(in.readLine("TraitLine"));
                    }
                    out.flush();
                    out.close();
                    fops.flush();
                    fops.close();
                }
            } catch (Exception e) {

                // close the streams
                // in.close();
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }

    }

    /**
     * Transfer the whole planet data xml.
     */
    public CampaignData getAllData() throws java.io.IOException {
        BinReader in = openConnection("All");
        CampaignData data = new CampaignData(in);
        // in.close();
        getAccessLevels(data);
        this.data = data;

        store();

        return data;
    }

    /**
     * Transfer the data from cache.
     */
    public CampaignData getCacheData(String cachePath) throws java.io.IOException {
        BinReader in = new BinReader(new java.io.FileReader(cachePath + "/data.dat"));
        CampaignData data = new CampaignData(in);
        in.close();
        this.data = data;
        store();

        return data;
    }

    /**
     * Store itself to disk.
     */
    public void store() {

        if (lastTimestamp != null) {
            try {
                java.io.FileWriter fw = new java.io.FileWriter(cacheDir + "/dataLastUpdated.dat");
                // write the time out in Milliseconds
                // lastTimestamp = latestTimeStamp;

                fw.write(Long.toString(lastTimestamp.getTime()));
                fw.close();
            } catch (java.io.IOException e) {
                LOGGER.error(e, "");
            }
        }
        try {
            BinWriter binOut = new BinWriter(new java.io.PrintWriter(new java.io.FileWriter(cacheDir + "/data.dat")));
            data.binOut(binOut);
            binOut.close();
        } catch (Exception ex) {
            LOGGER.error(ex, "");
            LOGGER.error("Error saving data.");
        }
    }

    /**
     * Transfer only the differential planets since last timestamp.
     */
    public boolean getPlanetsUpdate(CampaignData Data) {
        try {
            BinReader in = openConnection("PDiff", 60000);
            data = Data;
            if (data == null) {
                LOGGER.error("data is null getPlanetsUpdate");
                return false;
            }
            try {

                data.clearHouses();
                int size = in.readInt("houses.size");
                for (int count = 0; count < size; count++) {
                    House house = new House(in);
                    data.addHouse(house);
                }

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
                lastTimestamp = sdf.parse(in.readLine("lasttimestamp"));
                boolean fullUpdate = in.readBoolean("FullUpdate");

                if (fullUpdate) {
                    data.clearPlanets();
                }

                size = in.readInt("planets.size");
                for (int count = 0; count < size; count++) {
                    Planet planet = new Planet();
                    planet.binIn(in, data);
                    data.addPlanet(planet);
                    changesSinceLastRefresh.put(planet.getId(), planet.getInfluence());
                }

            } catch (Exception ex) {
                LOGGER.error(ex, "");
            }// Bin empty

            /*
             * changesSinceLastRefresh = new HashMap();
             * data.decodeMutablePlanets(in, changesSinceLastRefresh); String
             * serverMD5 = in.readLine("md5");
             * MWLogger.infoLog("read MD5 checksum: "+serverMD5);
             * MD5OutputStream md5 = new MD5OutputStream(); BinWriter md5Writer
             * = new BinWriter(new PrintWriter(md5)); data.binOut(md5Writer);
             * md5Writer.close();
             * MWLogger.infoLog("own checksum: "+md5.getHashString());
             * if (!serverMD5.equals(md5.getHashString())) { md5.close(); return
             * false; } //else md5.close(); //in.close();
             */
        } catch (java.io.IOException e) {
            LOGGER.error(e, "");
            return false;
        } catch (RuntimeException e) {
            LOGGER.error(e, "");
            return false;
        }
        // this.data = data;

        store();
        return true;
    }

    /**
     * Transfer the Access levels of all the commands but only save the ones that matchs the users.
     *
     * @author Torren (Jason Tighe)
     */
    public boolean getAccessLevels(CampaignData Data) {
        try {
            BinReader in = openConnection("CommandAccessLevels");

            Data.importAccessLevels(in);
            // in.close();
        } catch (java.io.IOException e) {
            LOGGER.error(e, "");
            return false;
        } catch (RuntimeException e) {
            LOGGER.error(e, "");
            return false;
        }
        return true;
    }

    private BinReader openConnection(String cmd) throws java.io.IOException {
        return openConnection(cmd, socketDelayTime);
    }

    /**
     * Open a connection to the server.
     *
     * @return
     */
    private BinReader openConnection(String cmd, int timeout) throws java.io.IOException {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        LOGGER.info("Command: " + cmd);
        if (dataSocket == null ||
                  dataSocket.isClosed() ||
                  dataSocket.isInputShutdown() ||
                  dataSocket.isOutputShutdown()) {
            closeDataConnection();
            LOGGER.info("Trying to connect to " + hostAddr + " at port " + dataPort);
            dataSocket = new java.net.Socket(hostAddr, dataPort);
            dataSocket.setKeepAlive(true);
        } else {// clean out any old data first.
            dataSocket.getOutputStream().flush();
        }
        dataSocket.setSoTimeout(timeout);
        BinWriter out = new BinWriter(new java.io.PrintWriter(dataSocket.getOutputStream()));
        out.println(cmd, "cmd");
        if (lastTimestamp == null) {
            out.println("", "lasttimestamp");
        } else {
            LOGGER.info("writing timestamp " + sdf.format(lastTimestamp));
            out.println(sdf.format(lastTimestamp), "lasttimestamp");
        }
        out.flush();
        BinReader in = null;
        try {
            in = new BinReader(new java.io.InputStreamReader(dataSocket.getInputStream()));
            // lastTimestamp =
            sdf.parse(in.readLine("lasttimestamp"));
        } catch (java.text.ParseException e) {
            LOGGER.error(e, "");
            LOGGER.info("Timestamp could not be parsed.. left unchanged.");
        } catch (java.net.SocketException se) {
            LOGGER.error("Socket Exception Error: DataFetchClient");
            LOGGER.error(se, "");
            closeDataConnection();
            return openConnection(cmd, timeout);
        } catch (NullPointerException NPE) {
            closeDataConnection();
            return openConnection(cmd, timeout);
        }
        return in;
    }

    /**
     * @param hostAddr The hostAddr to set.
     */
    public void setData(String hostAddr, String cacheDir) {
        this.hostAddr = hostAddr;
        this.cacheDir = cacheDir;
    }

    /**
     * @return Returns the changesSinceLastRefresh.
     */
    public java.util.Map<Integer, Influences> getChangesSinceLastRefresh() {
        return changesSinceLastRefresh;
    }

    /**
     * @param lastTimestamp The lastTimestamp to set.
     */
    public void setLastTimestamp(java.util.Date lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }

    public void closeDataConnection() {
        try {
            if (dataSocket == null) {
                return;
            }
            LOGGER.info("Closing Socket.");
            dataSocket.shutdownInput();
            dataSocket.shutdownOutput();
            dataSocket.close();
            dataSocket = null;
        } catch (Exception ex) {
            LOGGER.error(ex, "");
            dataSocket = null;
        }

    }

}
