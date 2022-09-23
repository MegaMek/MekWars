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

package client;

import java.awt.KeyboardFocusManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import client.campaign.CArmy;
import client.campaign.CUnit;
import common.AdvancedTerrain;
import common.PlanetEnvironment;
import common.Unit;
import common.campaign.Buildings;
import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.client.Client;
import megamek.client.CloseClientListener;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.common.Board;
import megamek.common.BoardDimensions;
import megamek.common.Coords;
import megamek.common.Crew;
import megamek.common.CrewType;
import megamek.common.Entity;
// import megamek.common.IGame;
// import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.KeyBindParser;
import megamek.common.MapSettings;
import megamek.common.OffBoardDirection;
import megamek.common.PlanetaryConditions;
import megamek.common.icons.Camouflage;

// import org.apache.log4j.lf5.LogLevel;
// import org.apache.logging.log4j.Level;
// import org.apache.logging.log4j.LogManager;
import megamek.common.options.IBasicOption;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.BuildingTemplate;

public class ClientThread extends Thread implements CloseClientListener {

    // VARIABLES
    private String myname;
    private String serverip;
    private String serverName;
    private int serverport;
    private MWClient mwclient;
    private Client client;
    private megamek.client.ui.swing.ClientGUI swingGui;
    private megamek.client.ui.swing.util.MegaMekController controller;

    private ArrayList<Unit> mechs = new ArrayList<Unit>();
    private ArrayList<CUnit> autoarmy = new ArrayList<CUnit>();// from server's
    // auto army
    CArmy army = null;
    BotClient bot = null;

    final int N = 0;
    final int NE = 1;
    final int SE = 2;
    final int S = 3;
    final int SW = 4;
    final int NW = 5;

    // CONSTRUCTOR
    public ClientThread(String name, String servername, String ip, int port, MWClient mwclient, ArrayList<Unit> mechs, ArrayList<CUnit> autoarmy) {
        super(name);
        myname = name.trim();
        serverName = servername;
        serverip = ip;
        serverport = port;
        this.mwclient = mwclient;
        this.mechs = mechs;
        this.autoarmy = autoarmy;
        if (serverip.indexOf("127.0.0.1") != -1) {
            serverip = "127.0.0.1";
        }
        controller = new MegaMekController();
        KeyboardFocusManager kbfm = KeyboardFocusManager
                .getCurrentKeyboardFocusManager();
        kbfm.addKeyEventDispatcher(controller);

        KeyBindParser.parseKeyBindings(controller);
    }

    public Client getClient() {
        return client;
    }

    public MegaMekController getMegaMekController() {
        return controller;
    }

    @Override
    public void run() {
        boolean playerUpdate = false;
        boolean nightGame = false;
        CArmy currA = mwclient.getPlayer().getLockedArmy();
        client = new Client(myname, serverip, serverport);
        client.addCloseClientListener(this);
        /*
         * mwclient.getserverConfigs("MMTimeStampLogFile");
         * mwclient.getserverConfigs("MMShowUnitId");
         * mwclient.getserverConfigs("MMKeepGameLog");
         * mwclient.getserverConfigs("MMGameLogName");
         */

        try {

            // clear out everything.
            mwclient.getPlayer().setConventionalMinesAllowed(0);
            mwclient.getPlayer().setVibraMinesAllowed(0);
            mwclient.setUsingBots(false);
            // clear out everything from this game
            mwclient.setEnvironment(null, null, 0);
            mwclient.setAdvancedTerrain(null);
            mwclient.setPlayerStartingEdge(Buildings.EDGE_UNKNOWN);
            mwclient.getGameOptions().clear();
            // get rid of any and all bots.

        }// end try
        catch (Exception ex) {
            MWLogger.errLog("Error reporting game!");
            MWLogger.errLog(ex);
        }

        if (swingGui != null) {
                for (Client client2 : swingGui.getBots().values()) {
                    client2.die();
                }
                swingGui.getBots().clear();
            }

            swingGui = new megamek.client.ui.swing.ClientGUI(client, controller);
            swingGui.initialize();


        if (mwclient.getGameOptions().size() < 1) {
            mwclient.setWaiting(true);

            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c RequestOperationSettings");
            while (mwclient.isWaiting()) {
                try {
                    mwclient.addToChat("Retrieving Operation Data Please Wait..");
                    Thread.sleep(1000);
                } catch (Exception ex) {

                }
            }
        }

        // client.game.getOptions().
        Vector<IBasicOption> xmlGameOptions = mwclient.getGameOptions();

        try {
            client.connect();
        } catch (Exception ex) {
            client = null;
            mwclient.showInfoWindow("Couldn't join this game!");
            MWLogger.infoLog(serverip + " " + serverport);
            return;
        }
        // client.retrieveServerInfo();
        try {
            while (client.getLocalPlayer() == null) {
                Thread.sleep(50);
            }

            // if game is running, shouldn't do the following, so detect the
            // phase
            for (int i = 0; (i < 1000) && (client.getGame().getPhase() == GamePhase.UNKNOWN); i++) {
                Thread.sleep(50);
            }

            // Lets start with the environment set first then do everything
            // else.
            if ((mwclient.getCurrentEnvironment() != null) && (client.getGame().getPhase() == GamePhase.LOUNGE)) {
                // creates the playboard*/
                MapSettings mySettings = MapSettings.getInstance();
                mySettings.setBoardSize((int)mwclient.getMapSize().getWidth(), (int)mwclient.getMapSize().getHeight());
                mySettings.setMapSize(1, 1);  // Note to self: MapSize in MM is boards x boards, not hexes x hexes
                
                AdvancedTerrain aTerrain = mwclient.getCurrentAdvancedTerrain();
                
                PlanetEnvironment pe = mwclient.getCurrentEnvironment();
                if ((pe != null) && pe.isStaticMap()) {
                	mySettings = MapSettings.getInstance();
                	mySettings.setBoardSize((int)pe.getXBoardSize(), (int)pe.getYBoardSize());
                	mySettings.setMapSize((int) pe.getXSize(), (int) pe.getYSize());
                	//mySettings = new MapSettings(pe.getXSize(), pe.getYSize(), pe.getXBoardSize(), pe.getYBoardSize());

                    ArrayList<String> boardvec = new ArrayList<String>();
                    if (pe.getStaticMapName().toLowerCase().endsWith("surprise")) {
                        int maxBoards = pe.getXBoardSize() * pe.getYBoardSize();
                        for (int i = 0; i < maxBoards; i++) {
                            boardvec.add(MapSettings.BOARD_SURPRISE);
                        }

                        mySettings.setBoardsSelectedVector(boardvec);

                        if (pe.getStaticMapName().indexOf("/") > -1) {
                            String folder = pe.getStaticMapName().substring(0, pe.getStaticMapName().lastIndexOf("/"));
                            mySettings.setBoardsAvailableVector(scanForBoards(pe.getXSize(), pe.getYSize(), folder));
                        } else if (pe.getStaticMapName().indexOf("\\") > -1) {
                            String folder = pe.getStaticMapName().substring(0, pe.getStaticMapName().lastIndexOf("\\"));
                            mySettings.setBoardsAvailableVector(scanForBoards(pe.getXSize(), pe.getYSize(), folder));
                        } else {
                            mySettings.setBoardsAvailableVector(scanForBoards(pe.getXSize(), pe.getYSize(), ""));
                        }
                    } else if (pe.getStaticMapName().toLowerCase().endsWith("generated")) {
                        PlanetEnvironment env = mwclient.getCurrentEnvironment();
                        /* Set the map-gen values */
                        mySettings.setElevationParams(env.getHillyness(), env.getHillElevationRange(), env.getHillInvertProb());
                        mySettings.setWaterParams(env.getWaterMinSpots(), env.getWaterMaxSpots(), env.getWaterMinHexes(), env.getWaterMaxHexes(), env.getWaterDeepProb());
                        mySettings.setForestParams(env.getForestMinSpots(), env.getForestMaxSpots(), env.getForestMinHexes(), env.getForestMaxHexes(), env.getForestHeavyProb());
                        mySettings.setRoughParams(env.getRoughMinSpots(), env.getRoughMaxSpots(), env.getRoughMinHexes(), env.getRoughMaxHexes());
                        mySettings.setSwampParams(env.getSwampMinSpots(), env.getSwampMaxSpots(), env.getSwampMinHexes(), env.getSwampMaxHexes());
                        mySettings.setPavementParams(env.getPavementMinSpots(), env.getPavementMaxSpots(), env.getPavementMinHexes(), env.getPavementMaxHexes());
                        mySettings.setIceParams(env.getIceMinSpots(), env.getIceMaxSpots(), env.getIceMinHexes(), env.getIceMaxHexes());
                        mySettings.setRubbleParams(env.getRubbleMinSpots(), env.getRubbleMaxSpots(), env.getRubbleMinHexes(), env.getRubbleMaxHexes());
                        mySettings.setFortifiedParams(env.getFortifiedMinSpots(), env.getFortifiedMaxSpots(), env.getFortifiedMinHexes(), env.getFortifiedMaxHexes());
                        mySettings.setSpecialFX(env.getFxMod(), env.getProbForestFire(), env.getProbFreeze(), env.getProbFlood(), env.getProbDrought());
                        mySettings.setRiverParam(env.getRiverProb());
                        mySettings.setCliffParam(env.getCliffProb());
                        mySettings.setRoadParam(env.getRoadProb());
                        mySettings.setCraterParam(env.getCraterProb(), env.getCraterMinNum(), env.getCraterMaxNum(), env.getCraterMinRadius(), env.getCraterMaxRadius());
                        mySettings.setAlgorithmToUse(env.getAlgorithm());
                        mySettings.setInvertNegativeTerrain(env.getInvertNegativeTerrain());
                        mySettings.setMountainParams(env.getMountPeaks(), env.getMountWidthMin(), env.getMountWidthMax(), env.getMountHeightMin(), env.getMountHeightMax(), env.getMountStyle());
                        mySettings.setSandParams(env.getSandMinSpots(), env.getSandMaxSpots(), env.getSandMinHexes(), env.getSandMaxHexes());
                        mySettings.setPlantedFieldParams(env.getPlantedFieldMinSpots(), env.getPlantedFieldMaxSpots(), env.getPlantedFieldMinHexes(), env.getPlantedFieldMaxHexes());


                        if (env.getTheme().length() > 1) {
                            mySettings.setTheme(env.getTheme());
                        } else {
                            mySettings.setTheme("");
                        }

                        int maxBoards = pe.getXBoardSize() * pe.getYBoardSize();
                        for (int i = 0; i < maxBoards; i++) {
                            boardvec.add(MapSettings.BOARD_GENERATED);
                        }

                        mySettings.setBoardsSelectedVector(boardvec);
                        if (pe.getStaticMapName().indexOf("/") > -1) {
                            String folder = pe.getStaticMapName().substring(0, pe.getStaticMapName().lastIndexOf("/"));
                            mySettings.setBoardsAvailableVector(scanForBoards(pe.getXSize(), pe.getYSize(), folder));
                        } else if (pe.getStaticMapName().indexOf("\\") > -1) {
                            String folder = pe.getStaticMapName().substring(0, pe.getStaticMapName().lastIndexOf("\\"));
                            mySettings.setBoardsAvailableVector(scanForBoards(pe.getXSize(), pe.getYSize(), folder));
                        } else {
                            mySettings.setBoardsAvailableVector(scanForBoards(pe.getXSize(), pe.getYSize(), ""));
                        }

                        if ((mwclient.getBuildingTemplate() != null) && (mwclient.getBuildingTemplate().getTotalBuildings() > 0)) {
                            ArrayList<BuildingTemplate> buildingList = generateRandomBuildings(mySettings, mwclient.getBuildingTemplate());
                            mySettings.setBoardBuildings(buildingList);
                        } else if (!env.getCityType().equalsIgnoreCase("NONE")) {
                            mySettings.setRoadParam(0);
                            mySettings.setCityParams(env.getRoads(), env.getCityType(), env.getMinCF(), env.getMaxCF(), env.getMinFloors(), env.getMaxFloors(), env.getCityDensity(), env.getTownSize());
                        }
                    } else {
                        boardvec.add(pe.getStaticMapName());
                        mySettings.setBoardsSelectedVector(boardvec);
                    }

                    PlanetaryConditions planetCondition = new PlanetaryConditions();

                    planetCondition.setGravity((float) aTerrain.getGravity());
                    planetCondition.setTemperature(aTerrain.getTemperature());
                    planetCondition.setAtmosphere(aTerrain.getAtmosphere());
                    planetCondition.setEMI(aTerrain.hasEMI());
                    planetCondition.setFog(aTerrain.getFog());
                    planetCondition.setLight(aTerrain.getLightConditions());
                    planetCondition.setShiftingWindDirection(aTerrain.hasShifitingWindDirection());
                    planetCondition.setShiftingWindStrength(aTerrain.hasShifitingWindStrength());
                    planetCondition.setTerrainAffected(aTerrain.isTerrainAffected());
                    planetCondition.setWeather(aTerrain.getWeatherConditions());
                    planetCondition.setWindDirection(aTerrain.getWindDirection());
                    planetCondition.setWindStrength(aTerrain.getWindStrength());
                    planetCondition.setMaxWindStrength(aTerrain.getMaxWindStrength());

                    // Check for a night game and set nightGame Variable.
                    // This is needed to be done since it was possible that a
                    // slow connection
                    // would keep the client from getting an update from the
                    // server before the
                    // entities where added to the game.
                    nightGame = aTerrain.getLightConditions() > PlanetaryConditions.L_DUSK;

                    client.sendPlanetaryConditions(planetCondition);

                    mySettings.setMedium(mwclient.getMapMedium());
                    client.sendMapSettings(mySettings);
                    /*the mysettings and planetCondition object refs were
                     *passed to the client, so release the refs to them - BarukKhazad!
                     */
                    mySettings = null;
                    planetCondition = null;

                } else {
                    PlanetEnvironment env = mwclient.getCurrentEnvironment();
                    /* Set the map-gen values */
                    mySettings.setElevationParams(env.getHillyness(), env.getHillElevationRange(), env.getHillInvertProb());
                    mySettings.setWaterParams(env.getWaterMinSpots(), env.getWaterMaxSpots(), env.getWaterMinHexes(), env.getWaterMaxHexes(), env.getWaterDeepProb());
                    mySettings.setForestParams(env.getForestMinSpots(), env.getForestMaxSpots(), env.getForestMinHexes(), env.getForestMaxHexes(), env.getForestHeavyProb());
                    mySettings.setRoughParams(env.getRoughMinSpots(), env.getRoughMaxSpots(), env.getRoughMinHexes(), env.getRoughMaxHexes());
                    mySettings.setSwampParams(env.getSwampMinSpots(), env.getSwampMaxSpots(), env.getSwampMinHexes(), env.getSwampMaxHexes());
                    mySettings.setPavementParams(env.getPavementMinSpots(), env.getPavementMaxSpots(), env.getPavementMinHexes(), env.getPavementMaxHexes());
                    mySettings.setIceParams(env.getIceMinSpots(), env.getIceMaxSpots(), env.getIceMinHexes(), env.getIceMaxHexes());
                    mySettings.setRubbleParams(env.getRubbleMinSpots(), env.getRubbleMaxSpots(), env.getRubbleMinHexes(), env.getRubbleMaxHexes());
                    mySettings.setFortifiedParams(env.getFortifiedMinSpots(), env.getFortifiedMaxSpots(), env.getFortifiedMinHexes(), env.getFortifiedMaxHexes());
                    mySettings.setSpecialFX(env.getFxMod(), env.getProbForestFire(), env.getProbFreeze(), env.getProbFlood(), env.getProbDrought());
                    mySettings.setRiverParam(env.getRiverProb());
                    mySettings.setCliffParam(env.getCliffProb());
                    mySettings.setRoadParam(env.getRoadProb());
                    mySettings.setCraterParam(env.getCraterProb(), env.getCraterMinNum(), env.getCraterMaxNum(), env.getCraterMinRadius(), env.getCraterMaxRadius());
                    mySettings.setAlgorithmToUse(env.getAlgorithm());
                    mySettings.setInvertNegativeTerrain(env.getInvertNegativeTerrain());
                    mySettings.setMountainParams(env.getMountPeaks(), env.getMountWidthMin(), env.getMountWidthMax(), env.getMountHeightMin(), env.getMountHeightMax(), env.getMountStyle());
                    mySettings.setSandParams(0, 0, 0, 0);
                    mySettings.setPlantedFieldParams(0, 0, 0, 0);

                    if (env.getTheme().length() > 1) {
                        mySettings.setTheme(env.getTheme());
                    } else {
                        mySettings.setTheme("");
                    }

                    /* select the map */
                    ArrayList<String> boardvec = new ArrayList<String>();
                    boardvec.add(MapSettings.BOARD_GENERATED);
                    mySettings.setBoardsSelectedVector(boardvec);

                    if ((mwclient.getBuildingTemplate() != null) && (mwclient.getBuildingTemplate().getTotalBuildings() > 0)) {
                        ArrayList<BuildingTemplate> buildingList = generateRandomBuildings(mySettings, mwclient.getBuildingTemplate());
                        mySettings.setBoardBuildings(buildingList);
                    } else if (!env.getCityType().equalsIgnoreCase("NONE")) {
                        mySettings.setRoadParam(0);
                        mySettings.setCityParams(env.getRoads(), env.getCityType(), env.getMinCF(), env.getMaxCF(), env.getMinFloors(), env.getMaxFloors(), env.getCityDensity(), env.getTownSize());
                    }

                    mySettings.setMedium(mwclient.getMapMedium());
                    /* sent to server */
                    client.sendMapSettings(mySettings);

                    if (aTerrain != null) {
                        PlanetaryConditions planetCondition = new PlanetaryConditions();

                        planetCondition.setGravity((float) aTerrain.getGravity());
                        planetCondition.setTemperature(aTerrain.getTemperature());
                        planetCondition.setAtmosphere(aTerrain.getAtmosphere());
                        planetCondition.setEMI(aTerrain.hasEMI());
                        planetCondition.setFog(aTerrain.getFog());
                        planetCondition.setLight(aTerrain.getLightConditions());
                        planetCondition.setShiftingWindDirection(aTerrain.hasShifitingWindDirection());
                        planetCondition.setShiftingWindStrength(aTerrain.hasShifitingWindStrength());
                        planetCondition.setTerrainAffected(aTerrain.isTerrainAffected());
                        planetCondition.setWeather(aTerrain.getWeatherConditions());
                        planetCondition.setWindDirection(aTerrain.getWindDirection());
                        planetCondition.setWindStrength(aTerrain.getWindStrength());
                        planetCondition.setMaxWindStrength(aTerrain.getMaxWindStrength());

                        // Check for a night game and set nightGame Variable.
                        // This is needed to be done since it was possible that
                        // a slow connection
                        // would keep the client from getting an update from the
                        // server before the
                        // entities where added to the game.
                        nightGame = aTerrain.getLightConditions() > PlanetaryConditions.L_DUSK;

                        client.sendPlanetaryConditions(planetCondition);
                        /*the mysettings and planetCondition object refs were
                         *passed to the client, so release the refs to them - BarukKhazad!
                         */
                        mySettings = null;
                        planetCondition = null;
                    }
                }

            }

            /*
             * Add bots, if being used in this game.
             */
            if (mwclient.isUsingBots()) {
                String name = "War Bot" + client.getLocalPlayer().getId();
                bot = new Princess(name, client.getHost(), client.getPort());
                bot.getGame().addGameListener(new BotGUI(bot));
                try {
                    bot.connect();
                    Thread.sleep(125);
                    while (bot.getLocalPlayer() == null) {
                        Thread.sleep(50);
                    }
                    // if game is running, shouldn't do the following, so detect
                    // the phase
                    for (int i = 0; (i < 1000) && (bot.getGame().getPhase() == GamePhase.UNKNOWN); i++) {
                        Thread.sleep(50);
                    }
                } catch (Exception ex) {
                    MWLogger.errLog("Bot Error!");
                    MWLogger.errLog(ex);
                }
//                bot.retrieveServerInfo();
                Thread.sleep(125);

                swingGui.getBots().put(name, bot);

                if (mwclient.isBotsOnSameTeam()) {
                    bot.getLocalPlayer().setTeam(5);
                }
                Random r = new Random();

                bot.getLocalPlayer().setStartingPos(r.nextInt(11));
                bot.sendPlayerInfo();
                Thread.sleep(125);
            }

            if (((client.getGame() != null) && (client.getGame().getPhase() == GamePhase.LOUNGE))) {

                client.getGame().getOptions().loadOptions();
                if ((mechs.size() > 0) && (xmlGameOptions.size() > 0)) {
                    client.sendGameOptions("", xmlGameOptions);
                }

                ClientPreferences cs = PreferenceManager.getClientPreferences();
                cs.setStampFilenames(Boolean.parseBoolean(mwclient.getserverConfigs("MMTimeStampLogFile")));
                cs.setShowUnitId(Boolean.parseBoolean(mwclient.getserverConfigs("MMShowUnitId")));
                cs.setKeepGameLog(Boolean.parseBoolean(mwclient.getserverConfigs("MMKeepGameLog")));
                cs.setGameLogFilename(mwclient.getserverConfigs("MMGameLogName"));
                /*the cs object ref is no longer needed, so release the ref to it- BarukKhazad!
                 */
                 cs = null;

                if (!mwclient.getConfig().getParam("UNITCAMO").equals(Camouflage.NO_CAMOUFLAGE)) {
                	client.getLocalPlayer().setCamouflage(new Camouflage(Camouflage.ROOT_CATEGORY, mwclient.getConfig().getParam("UNITCAMO")));
//                    client.getLocalPlayer().setCategory(Camouflage.ROOT_CATEGORY);
//                    client.getLocalPlayer().setCamoFileName(mwclient.getConfig().getParam("UNITCAMO"));
                    playerUpdate = true;
                }

                if (bot != null) {
                    bot.getLocalPlayer().setNbrMFConventional(mwclient.getPlayer().getConventionalMinesAllowed());
                    bot.getLocalPlayer().setNbrMFVibra(mwclient.getPlayer().getVibraMinesAllowed());
                } else {
                    client.getLocalPlayer().setNbrMFConventional(mwclient.getPlayer().getConventionalMinesAllowed());
                    client.getLocalPlayer().setNbrMFVibra(mwclient.getPlayer().getVibraMinesAllowed());
                }

                for (Unit unit : mechs) {
                    // Get the Mek
                    CUnit mek = (CUnit) unit;
                    // Get the Entity
                    Entity entity = mek.getEntity();
                    // Set the TempID for autoreporting
                    entity.setExternalId(mek.getId());
                    // entity.setId(mek.getId());
                    // Set the owner
                    entity.setOwner(client.getLocalPlayer());
                    // Set if unit is a commander in this army.
                    entity.setCommander(currA.isCommander(mek.getId()));

                    // Set slights based on games light conditions.
                    if ( !entity.hasSearchlight()){
                        entity.getQuirks().getOption("searchlight").setValue(nightGame);
                    }
                    entity.setSearchlightState(nightGame);

                    // Set the correct home edge for off board units
                    if (entity.isOffBoard()) {
                        OffBoardDirection direction = OffBoardDirection.NORTH;
                        switch (mwclient.getPlayerStartingEdge()) {
                            case 4:
                            case 14:
                                direction = OffBoardDirection.EAST;
                                break;
                            case 5:
                            case 6:
                            case 7:
                            case 15:
                            case 16:
                            case 17:
                                direction = OffBoardDirection.SOUTH;
                                break;
                            case 8:
                            case 18:
                                direction = OffBoardDirection.WEST;
                                break;
                            default:
                                direction = OffBoardDirection.NORTH;
                                break;
                        }
                        entity.setOffBoard(entity.getOffBoardDistance(), direction);
                    }

                    // Add Pilot to entity
                    entity.setCrew(UnitUtils.createEntityPilot(mek));
                    // Add Mek to game
                    client.sendAddEntity(entity);
                    // Wait a few secs to not overuse bandwith
                    Thread.sleep(125);
                   /*the entity object ref was passed so release the ref to it- BarukKhazad!
                    * some concern that this "entity" is a keyword of some sort, expect it to puke on conplie if yes
                    * fahr- this represents anything on the map - but in this case is units
                    */
                   entity = null;
                }

                /*
                 * Army mechs already loaded (see previous for loop). Now try to
                 * load the artillery units generated by the server (see
                 * AutoArmy.java in the server.campaign pacakage for generation
                 * details).
                 */
                Iterator<CUnit> autoIt = autoarmy.iterator();
                while (autoIt.hasNext()) {

                    // get the unit
                    CUnit autoUnit = autoIt.next();

                    // get the entity
                    Entity entity = autoUnit.getEntity();

                    // Set slights based on games light conditions.
                   	entity.setExternalSearchlight(nightGame);
                    entity.setSearchlightState(nightGame);

                    // Had issues with Id's so we are now setting them.
                    // entity.setId(autoUnit.getId());
                    entity.setExternalId(autoUnit.getId());

                    // Set the owner
                    if (bot != null) {
                        entity.setOwner(bot.getLocalPlayer());
                    } else {
                        entity.setOwner(client.getLocalPlayer());
                    }

                    if (entity.getCrew().getName().equalsIgnoreCase("Unnamed") || entity.getCrew().getName().equalsIgnoreCase("vacant")) {
                        // set the pilot
                        Crew pilot = new Crew(CrewType.SINGLE, "AutoArtillery", 1, 4, 5);
                        entity.setCrew(pilot);
                    } else {
                        entity.setCrew(UnitUtils.createEntityPilot(autoUnit));
                    }

                    // MWLogger.errLog(entity.getModel()+"
                    // direction "+entity.getOffBoardDirection());
                    // add the unit to the game.
                    if (bot != null) {
                        bot.sendAddEntity(entity);
                    } else {
                        client.sendAddEntity(entity);
                    }

                    // Wait a few secs to not overuse bandwith
                    Thread.sleep(125);
                    /*the entity object ref was passed so release the ref to it- BarukKhazad!
                     * some concern that this "entity" is a keyword of some sort, expect it to puke on conplie if yes
                     */
                    entity = null;
                }// end while(more autoarty)

                if (mwclient.getPlayerStartingEdge() != Buildings.EDGE_UNKNOWN) {
                    client.getLocalPlayer().setStartingPos(mwclient.getPlayerStartingEdge());
                    playerUpdate = true;
                }

                if (mechs.size() > 0) {
                    // check armies for C3Network mechs

                    synchronized (currA) {

                        if (currA.getC3Network().size() > 0) {
                            // Thread.sleep(125);
                            playerUpdate = true;
                            for (int slave : currA.getC3Network().keySet()) {
                                linkMegaMekC3Units(currA, slave, currA.getC3Network().get(slave));
                            }

                            swingGui.chatlounge.refreshEntities();
                        }
                    }
                }

                if (mwclient.getPlayer().getTeamNumber() > 0) {
                    client.getLocalPlayer().setTeam(mwclient.getPlayer().getTeamNumber());
                    playerUpdate = true;
                }

                if (playerUpdate) {
                    client.sendPlayerInfo();
                    if (bot != null) {
                        bot.sendPlayerInfo();
                    }
                }

            }

        } catch (Exception e) {
            MWLogger.errLog(e);
        }
        /*the swingGui object ref was initialized and is
        *active on the client thread, so release the ref to it- BarukKhazad!
        */
        swingGui = null;

    }

    /*
     * from megamek.client.CloseClientListener clientClosed() Thanks to MM for
     * adding the listener. And to MMNet for the poorly documented code change.
     */
    @Override
	public void clientClosed() {

        PreferenceManager.getInstance().save();

        if (bot != null) {
            bot.die();
            bot = null;
        }

        // client.die();
        client = null;// explicit null of the MM client. Wasn't/isn't being
        // GC'ed.
        mwclient.closingGame(serverName);
        System.gc();

    }

    /**
     * @author jtighe
     * @param army
     * @param slaveid
     * @param masterid
     *            This function goes through and makes sure the slave is linked
     *            to the master unit
     */
    public void linkMegaMekC3Units(CArmy army, Integer slaveid, Integer masterid) {
        Entity c3Unit = null;
        Entity c3Master = null;

        while ((c3Unit == null) || (c3Master == null)) {
            try {

                for (Entity en : client.getGame().getEntitiesVector()) {
                    if ((c3Unit == null) && (en.getExternalId() == slaveid)) {
                        c3Unit = en;
                    }

                    if ((c3Master == null) && (en.getExternalId() == masterid)) {
                        c3Master = en;
                    }
                }
                Thread.sleep(10);// give the queue time to refresh
            } catch (Exception ex) {
                MWLogger.errLog("Error in linkMegaMekC3Units");
                MWLogger.errLog(ex);
            }
        }

        // catch for some funky stuff
        if ((c3Unit == null) || (c3Master == null)) {
            MWLogger.errLog("Null Units c3Unit: " + c3Unit + " C3Master: " + c3Master);
            return;
        }

        try {
            CUnit masterUnit = (CUnit) army.getUnit(masterid);
            // MWLogger.errLog("Master Unit:
            // "+masterUnit.getModelName());
            // MWLogger.errLog("Slave Unit:
            // "+c3Unit.getModel());
            if (!masterUnit.hasC3SlavesLinkedTo(army) && masterUnit.hasBeenC3LinkedTo(army) && ((masterUnit.getC3Level() == Unit.C3_MASTER) || (masterUnit.getC3Level() == Unit.C3_MMASTER))) {
                // MWLogger.errLog("Unit:
                // "+c3Master.getModel()+" id: "+c3Master.getExternalId());
                if (c3Master.getC3MasterId() == Entity.NONE) {
                    c3Master.setShutDown(false);
                    c3Master.setC3Master(c3Master, false);
                    client.sendUpdateEntity(c3Master);
                }
                /*
                 * if ( c3Master.hasC3MM() )
                 * MWLogger.errLog("hasC3MM"); else
                 * MWLogger.errLog("!hasC3MM");
                 */
            } else if (c3Master.getC3MasterId() != Entity.NONE) {
                c3Master.setShutDown(false);
                c3Master.setC3Master(Entity.NONE, false);
                client.sendUpdateEntity(c3Master);
            }
            // MWLogger.errLog("c3Unit: "+c3Unit.getModel()+"
            // Master: "+c3Master.getModel());
            c3Unit.setShutDown(false);
            c3Unit.setC3Master(c3Master, false);
            // MWLogger.errLog("c3Master Set to
            // "+c3Unit.getC3MasterId()+" "+c3Unit.getC3NetId());
            client.sendUpdateEntity(c3Unit);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Error in setting up C3Network");
        }
    }

    /*
     * Taken form Megamek Code for use with MekWars The call was private and was
     * needed. Thanks to Ben Mazur and all of the MM coders we hope for a long
     * and happy relation ship. Torren.
     */

    public static Comparator<? super Object> stringComparator() {
        return new Comparator<Object>() {
            @Override
			public int compare(Object o1, Object o2) {
                String s1 = ((String) o1).toLowerCase();
                String s2 = ((String) o2).toLowerCase();
                return s1.compareTo(s2);
            }
        };
    }

    /**
     * Scans the boards directory for map boards of the appropriate size and
     * returns them.
     */
    private ArrayList<String> scanForBoards(int boardWidth, int boardHeight, String folder) {
        BoardDimensions dimension = new BoardDimensions(boardWidth, boardHeight);
        ArrayList<String> boards = new ArrayList<String>();
        // Board Board = client.game.getBoard();

        File boardDir = new File("data/boards/" + folder);

        // just a check...
        if (!boardDir.isDirectory()) {
            return boards;
        }

        // scan files
        String[] fileList = boardDir.list();
        Vector<String> tempList = new Vector<String>(1, 1);
        Comparator<? super String> sortComp = ClientThread.stringComparator();
        for (String path : fileList) {
            if (path.indexOf(".board") == -1) {
                continue;
            }

            if (folder.trim().length() > 0) {
                path = folder + "/" + path;
            }

            if (Board.boardIsSize(new File(path), dimension)) {
                tempList.addElement(path.substring(0, path.lastIndexOf(".board")));
            }
        }

        // if there are any boards, add these:
        if (tempList.size() > 0) {
            boards.add(MapSettings.BOARD_SURPRISE);
            boards.add(MapSettings.BOARD_GENERATED);
            Collections.sort(tempList, sortComp);
            for (int loop = 0; loop < tempList.size(); loop++) {
                boards.add(tempList.elementAt(loop));
            }
        } else {
            boards.add(MapSettings.BOARD_GENERATED);
        }

        return boards;
    }

    private ArrayList<BuildingTemplate> generateRandomBuildings(MapSettings mapSettings, Buildings buildingTemplate) {

        ArrayList<BuildingTemplate> buildingList = new ArrayList<BuildingTemplate>();
        ArrayList<String> buildingTypes = new ArrayList<String>();

        int width = mapSettings.getBoardWidth();
        int height = mapSettings.getBoardHeight();
        int minHeight = 0;
        int minWidth = 0;

        switch (buildingTemplate.getStartingEdge()) {
            case Buildings.NORTH:
                height = 5;
                minHeight = 1;
                break;
            case Buildings.SOUTH:
                if (height > 5) {
                    minHeight = height - 5;
                }
                height = 5;
                break;
            case Buildings.EAST:
                if (width > 5) {
                    minWidth = width - 5;
                }
                width = 5;
                break;
            case Buildings.WEST:
                width = 5;
                minWidth = 1;
                break;
            default:
                break;
        }

        StringTokenizer types = new StringTokenizer(buildingTemplate.getBuildingType(), ",");

        while (types.hasMoreTokens()) {
            buildingTypes.add(types.nextToken());
        }

        int typeSize = buildingTypes.size();

        Random r = new Random();

        TreeSet<String> tempMap = new TreeSet<String>();
        Coords coord = new Coords(0,0);
        String stringCoord = "";

        for (int count = 0; count < buildingTemplate.getTotalBuildings(); count++) {
            int loops = 0;
            boolean CFx2 = false;
            ArrayList<Coords> coordList = new ArrayList<Coords>();
            do {
                if (loops++ > 100) {
                    CFx2 = true;
                    break;
                }

                int x = r.nextInt(width) + minWidth;
                int y = r.nextInt(height) + minHeight;

                if (x >= mapSettings.getBoardWidth()) {
                    x = mapSettings.getBoardWidth() - 2;
                } else if (x <= 1) {
                    x = 2;
                }

                if (y >= mapSettings.getBoardHeight()) {
                    y = mapSettings.getBoardHeight() - 2;
                } else if (y <= 1) {
                    y = 2;
                }

                coord = new Coords(x, y);

                stringCoord = x + "," + y;
            } while (tempMap.contains(stringCoord));

            tempMap.add(stringCoord);
            coordList.add(coord);

            int floors = buildingTemplate.getMaxFloors() - buildingTemplate.getMinFloors();

            if (floors <= 0) {
                floors = buildingTemplate.getMinFloors();
            } else {
                floors = r.nextInt(floors) + buildingTemplate.getMinFloors();
            }

            int totalCF = buildingTemplate.getMaxCF() - buildingTemplate.getMinCF();

            if (totalCF <= 0) {
                totalCF = buildingTemplate.getMinCF();
            } else {
                totalCF = r.nextInt(totalCF) + buildingTemplate.getMinCF();
            }

            if (CFx2) {
                totalCF *= 2;
            }

            int type = 1;
            try {
                if (typeSize == 1) {
                    type = Integer.parseInt(buildingTypes.get(0));
                } else {
                    type = Integer.parseInt(buildingTypes.get(r.nextInt(typeSize)));
                }
            } catch (Exception ex) {
            } // someone entered a bad building type.

            buildingList.add(new BuildingTemplate(type, coordList, totalCF, floors, -1));
        }

        return buildingList;
    }

}
