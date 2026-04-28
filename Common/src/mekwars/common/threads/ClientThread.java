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

package mekwars.common.threads;

import java.awt.KeyboardFocusManager;
import java.util.ArrayList;
import java.util.List;

import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.CloseClientListener;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.util.MegaMekController;
import megamek.common.KeyBindParser;
import megamek.common.OffBoardDirection;
import megamek.common.board.Board;
import megamek.common.board.BoardDimensions;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.icons.Camouflage;
import megamek.common.loaders.MapSettings;
import megamek.common.options.IBasicOption;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.util.BuildingTemplate;
import mekwars.common.AdvancedTerrain;
import mekwars.common.PlanetEnvironment;
import mekwars.common.Unit;
import mekwars.common.campaign.Buildings;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.MWLogger;
import mekwars.common.util.UnitUtils;

public class ClientThread extends Thread implements CloseClientListener {

    final int N = 0;
    final int NE = 1;
    final int SE = 2;
    final int S = 3;
    final int SW = 4;
    final int NW = 5;

    // VARIABLES
    private final String myName;
    private final String serverName;
    private final int serverPort;
    private final IClient client;
    private final MegaMekController controller;
    private final ArrayList<Unit> meks;
    private final ArrayList<CUnit> autoArmy;// from server's
    BotClient bot = null;
    private String serverip;
    private Client mmClient;
    private ClientGUI swingGui;

    // CONSTRUCTOR
    public ClientThread(String name, String servername, String ip, int port, IClient client,
          java.util.ArrayList<Unit> meks, java.util.ArrayList<CUnit> autoArmy) {
        super(name);
        myName = name.trim();
        serverName = servername;
        serverip = ip;
        serverPort = port;
        this.client = client;
        this.meks = meks;
        this.autoArmy = autoArmy;
        if (serverip.contains("127.0.0.1")) {
            serverip = "127.0.0.1";
        }
        controller = new MegaMekController();
        KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        keyboardFocusManager.addKeyEventDispatcher(controller);

        KeyBindParser.parseKeyBindings(controller);
    }

    public Client getMMClient() {
        return mmClient;
    }

    public MegaMekController getMegaMekController() {
        return controller;
    }

    @Override
    public void run() {
        boolean playerUpdate = false;
        boolean nightGame = false;
        CArmy currA = client.getPlayer().getLockedArmy();
        mmClient = new Client(myName, serverip, serverPort);
        mmClient.addCloseClientListener(this);

        try {

            // clear out everything.
            client.getPlayer().setConventionalMinesAllowed(0);
            client.getPlayer().setVibraMinesAllowed(0);
            client.setUsingBots(false);
            // clear out everything from this game
            client.setEnvironment(null, null, 0);
            client.setAdvancedTerrain(null);
            client.setPlayerStartingEdge(Buildings.EDGE_UNKNOWN);
            client.getGameOptions().clear();
            // get rid of any bots.

        }// end try
        catch (Exception ex) {
            MWLogger.errLog("Error reporting game!");
            MWLogger.errLog(ex);
        }

        if (swingGui != null) {
            for (AbstractClient client2 : swingGui.getLocalBots().values()) {
                client2.die();
            }

            swingGui.getLocalBots().clear();
        }

        swingGui = new ClientGUI(mmClient, controller);
        swingGui.initialize();


        if (client.getGameOptions().isEmpty()) {
            client.setWaiting(true);

            client.sendChat(IClient.CAMPAIGN_PREFIX + "c RequestOperationSettings");
            while (client.isWaiting()) {
                try {
                    client.addToChat("Retrieving Operation Data Please Wait..");
                    Thread.sleep(1000);
                } catch (Exception ex) {

                }
            }
        }

        List<IBasicOption> xmlGameOptions = client.getGameOptions();

        try {
            mmClient.connect();
        } catch (Exception ex) {
            mmClient = null;
            client.showInfoWindow("Couldn't join this game!");
            MWLogger.infoLog(serverip + " " + serverPort);
            return;
        }
        // mmClient.retrieveServerInfo();
        try {
            while (mmClient.getLocalPlayer() == null) {
                Thread.sleep(50);
            }

            // if game is running, shouldn't do the following, so detect the phase
            for (int i = 0; (i < 1000) && (mmClient.getGame().getPhase() == GamePhase.UNKNOWN); i++) {
                Thread.sleep(50);
            }

            // Let's start with the environment set first then do everything else.
            if ((client.getCurrentEnvironment() != null) && (mmClient.getGame().getPhase() == GamePhase.LOUNGE)) {
                // creates the playboard*/
                MapSettings mySettings = MapSettings.getInstance();
                mySettings.setBoardSize(client.getMapSize().getWidth(), client.getMapSize().getHeight());
                mySettings.setMapSize(1, 1);  // Note to self: MapSize in MM is boards x boards, not hexes x hexes

                AdvancedTerrain aTerrain = client.getCurrentAdvancedTerrain();

                PlanetEnvironment planetEnvironment = client.getCurrentEnvironment();

                if ((planetEnvironment != null) && planetEnvironment.isStaticMap()) {
                    mySettings = MapSettings.getInstance();
                    mySettings.setBoardSize(planetEnvironment.getXBoardSize(), planetEnvironment.getYBoardSize());
                    mySettings.setMapSize(planetEnvironment.getXSize(), planetEnvironment.getYSize());

                    ArrayList<String> boardvec = new ArrayList<>();
                    if (planetEnvironment.getStaticMapName().toLowerCase().endsWith("surprise")) {
                        int maxBoards = planetEnvironment.getXBoardSize() * planetEnvironment.getYBoardSize();

                        for (int i = 0; i < maxBoards; i++) {
                            boardvec.add(MapSettings.BOARD_SURPRISE);
                        }

                        mySettings.setBoardsSelectedVector(boardvec);

                        if (planetEnvironment.getStaticMapName().contains("/")) {
                            String folder = planetEnvironment.getStaticMapName()
                                                  .substring(0, planetEnvironment.getStaticMapName().lastIndexOf("/"));
                            mySettings.setBoardsAvailableVector(scanForBoards(planetEnvironment.getXSize(),
                                  planetEnvironment.getYSize(),
                                  folder));
                        } else if (planetEnvironment.getStaticMapName().contains("\\")) {
                            String folder = planetEnvironment.getStaticMapName()
                                                  .substring(0, planetEnvironment.getStaticMapName().lastIndexOf("\\"));
                            mySettings.setBoardsAvailableVector(scanForBoards(planetEnvironment.getXSize(),
                                  planetEnvironment.getYSize(),
                                  folder));
                        } else {
                            mySettings.setBoardsAvailableVector(scanForBoards(planetEnvironment.getXSize(),
                                  planetEnvironment.getYSize(),
                                  ""));
                        }
                    } else if (planetEnvironment.getStaticMapName().toLowerCase().endsWith("generated")) {
                        PlanetEnvironment currentEnvironment = client.getCurrentEnvironment();
                        /* Set the map-gen values */
                        mySettings.setElevationParams(currentEnvironment.getHillyness(),
                              currentEnvironment.getHillElevationRange(),
                              currentEnvironment.getHillInvertProb());
                        mySettings.setWaterParams(currentEnvironment.getWaterMinSpots(),
                              currentEnvironment.getWaterMaxSpots(),
                              currentEnvironment.getWaterMinHexes(),
                              currentEnvironment.getWaterMaxHexes(),
                              currentEnvironment.getWaterDeepProb());
                        mySettings.setForestParams(currentEnvironment.getForestMinSpots(),
                              currentEnvironment.getForestMaxSpots(),
                              currentEnvironment.getForestMinHexes(),
                              currentEnvironment.getForestMaxHexes(),
                              currentEnvironment.getForestHeavyProb(),
                              0);
                        mySettings.setRoughParams(currentEnvironment.getRoughMinSpots(),
                              currentEnvironment.getRoughMaxSpots(),
                              currentEnvironment.getRoughMinHexes(),
                              currentEnvironment.getRoughMaxHexes()
                              , 0);
                        mySettings.setSwampParams(currentEnvironment.getSwampMinSpots(),
                              currentEnvironment.getSwampMaxSpots(),
                              currentEnvironment.getSwampMinHexes(),
                              currentEnvironment.getSwampMaxHexes());
                        mySettings.setPavementParams(currentEnvironment.getPavementMinSpots(),
                              currentEnvironment.getPavementMaxSpots(),
                              currentEnvironment.getPavementMinHexes(),
                              currentEnvironment.getPavementMaxHexes());
                        mySettings.setIceParams(currentEnvironment.getIceMinSpots(),
                              currentEnvironment.getIceMaxSpots(),
                              currentEnvironment.getIceMinHexes(),
                              currentEnvironment.getIceMaxHexes());
                        mySettings.setRubbleParams(currentEnvironment.getRubbleMinSpots(),
                              currentEnvironment.getRubbleMaxSpots(),
                              currentEnvironment.getRubbleMinHexes(),
                              currentEnvironment.getRubbleMaxHexes(),
                              0);
                        mySettings.setFortifiedParams(currentEnvironment.getFortifiedMinSpots(),
                              currentEnvironment.getFortifiedMaxSpots(),
                              currentEnvironment.getFortifiedMinHexes(),
                              currentEnvironment.getFortifiedMaxHexes());
                        mySettings.setSpecialFX(currentEnvironment.getFxMod(),
                              currentEnvironment.getProbForestFire(),
                              currentEnvironment.getProbFreeze(),
                              currentEnvironment.getProbFlood(),
                              currentEnvironment.getProbDrought());
                        mySettings.setRiverParam(currentEnvironment.getRiverProb());
                        mySettings.setCliffParam(currentEnvironment.getCliffProb());
                        mySettings.setRoadParam(currentEnvironment.getRoadProb());
                        mySettings.setCraterParam(currentEnvironment.getCraterProb(),
                              currentEnvironment.getCraterMinNum(),
                              currentEnvironment.getCraterMaxNum(),
                              currentEnvironment.getCraterMinRadius(),
                              currentEnvironment.getCraterMaxRadius());
                        mySettings.setAlgorithmToUse(currentEnvironment.getAlgorithm());
                        mySettings.setInvertNegativeTerrain(currentEnvironment.getInvertNegativeTerrain());
                        mySettings.setMountainParams(currentEnvironment.getMountPeaks(),
                              currentEnvironment.getMountWidthMin(),
                              currentEnvironment.getMountWidthMax(),
                              currentEnvironment.getMountHeightMin(),
                              currentEnvironment.getMountHeightMax(),
                              currentEnvironment.getMountStyle());
                        mySettings.setSandParams(currentEnvironment.getSandMinSpots(),
                              currentEnvironment.getSandMaxSpots(),
                              currentEnvironment.getSandMinHexes(),
                              currentEnvironment.getSandMaxHexes());
                        mySettings.setPlantedFieldParams(currentEnvironment.getPlantedFieldMinSpots(),
                              currentEnvironment.getPlantedFieldMaxSpots(),
                              currentEnvironment.getPlantedFieldMinHexes(),
                              currentEnvironment.getPlantedFieldMaxHexes());


                        if (currentEnvironment.getTheme().length() > 1) {
                            mySettings.setTheme(currentEnvironment.getTheme());
                        } else {
                            mySettings.setTheme("");
                        }

                        int maxBoards = planetEnvironment.getXBoardSize() * planetEnvironment.getYBoardSize();
                        for (int i = 0; i < maxBoards; i++) {
                            boardvec.add(MapSettings.BOARD_GENERATED);
                        }

                        mySettings.setBoardsSelectedVector(boardvec);
                        if (planetEnvironment.getStaticMapName().contains("/")) {
                            String folder = planetEnvironment.getStaticMapName()
                                                  .substring(0, planetEnvironment.getStaticMapName().lastIndexOf("/"));
                            mySettings.setBoardsAvailableVector(scanForBoards(planetEnvironment.getXSize(),
                                  planetEnvironment.getYSize(),
                                  folder));
                        } else if (planetEnvironment.getStaticMapName().contains("\\")) {
                            String folder = planetEnvironment.getStaticMapName()
                                                  .substring(0, planetEnvironment.getStaticMapName().lastIndexOf("\\"));
                            mySettings.setBoardsAvailableVector(scanForBoards(planetEnvironment.getXSize(),
                                  planetEnvironment.getYSize(),
                                  folder));
                        } else {
                            mySettings.setBoardsAvailableVector(scanForBoards(planetEnvironment.getXSize(),
                                  planetEnvironment.getYSize(),
                                  ""));
                        }

                        if ((client.getBuildingTemplate() != null) &&
                                  (client.getBuildingTemplate().getTotalBuildings() > 0)) {
                            java.util.ArrayList<BuildingTemplate> buildingList = generateRandomBuildings(mySettings,
                                  client.getBuildingTemplate());
                            mySettings.setBoardBuildings(buildingList);
                        } else if (!currentEnvironment.getCityType().equalsIgnoreCase("NONE")) {
                            mySettings.setRoadParam(0);
                            mySettings.setCityParams(currentEnvironment.getRoads(),
                                  currentEnvironment.getCityType(),
                                  currentEnvironment.getMinCF(),
                                  currentEnvironment.getMaxCF(),
                                  currentEnvironment.getMinFloors(),
                                  currentEnvironment.getMaxFloors(),
                                  currentEnvironment.getCityDensity(),
                                  currentEnvironment.getTownSize());
                        }
                    } else {
                        boardvec.add(planetEnvironment.getStaticMapName());
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
                    planetCondition.setWindMin(aTerrain.getWindStrength());
                    planetCondition.setWindMax(aTerrain.getMaxWindStrength());

                    // Check for a night game and set nightGame Variable. This is needed to be done since it was
                    // possible that a slow connection would keep the mmClient from getting an update from the
                    // server before the entities where added to the game.
                    nightGame = aTerrain.getLightConditions().ordinal() > Light.DUSK.ordinal();

                    mmClient.sendPlanetaryConditions(planetCondition);

                    mySettings.setMedium(client.getMapMedium());
                    mmClient.sendMapSettings(mySettings);
                } else {
                    PlanetEnvironment env = client.getCurrentEnvironment();
                    /* Set the map-gen values */
                    mySettings.setElevationParams(env.getHillyness(),
                          env.getHillElevationRange(),
                          env.getHillInvertProb());
                    mySettings.setWaterParams(env.getWaterMinSpots(),
                          env.getWaterMaxSpots(),
                          env.getWaterMinHexes(),
                          env.getWaterMaxHexes(),
                          env.getWaterDeepProb());
                    mySettings.setForestParams(env.getForestMinSpots(),
                          env.getForestMaxSpots(),
                          env.getForestMinHexes(),
                          env.getForestMaxHexes(),
                          env.getForestHeavyProb(),
                          0);
                    mySettings.setRoughParams(env.getRoughMinSpots(),
                          env.getRoughMaxSpots(),
                          env.getRoughMinHexes(),
                          env.getRoughMaxHexes(),
                          0);
                    mySettings.setSwampParams(env.getSwampMinSpots(),
                          env.getSwampMaxSpots(),
                          env.getSwampMinHexes(),
                          env.getSwampMaxHexes());
                    mySettings.setPavementParams(env.getPavementMinSpots(),
                          env.getPavementMaxSpots(),
                          env.getPavementMinHexes(),
                          env.getPavementMaxHexes());
                    mySettings.setIceParams(env.getIceMinSpots(),
                          env.getIceMaxSpots(),
                          env.getIceMinHexes(),
                          env.getIceMaxHexes());
                    mySettings.setRubbleParams(env.getRubbleMinSpots(),
                          env.getRubbleMaxSpots(),
                          env.getRubbleMinHexes(),
                          env.getRubbleMaxHexes(),
                          0);
                    mySettings.setFortifiedParams(env.getFortifiedMinSpots(),
                          env.getFortifiedMaxSpots(),
                          env.getFortifiedMinHexes(),
                          env.getFortifiedMaxHexes());
                    mySettings.setSpecialFX(env.getFxMod(),
                          env.getProbForestFire(),
                          env.getProbFreeze(),
                          env.getProbFlood(),
                          env.getProbDrought());
                    mySettings.setRiverParam(env.getRiverProb());
                    mySettings.setCliffParam(env.getCliffProb());
                    mySettings.setRoadParam(env.getRoadProb());
                    mySettings.setCraterParam(env.getCraterProb(),
                          env.getCraterMinNum(),
                          env.getCraterMaxNum(),
                          env.getCraterMinRadius(),
                          env.getCraterMaxRadius());
                    mySettings.setAlgorithmToUse(env.getAlgorithm());
                    mySettings.setInvertNegativeTerrain(env.getInvertNegativeTerrain());
                    mySettings.setMountainParams(env.getMountPeaks(),
                          env.getMountWidthMin(),
                          env.getMountWidthMax(),
                          env.getMountHeightMin(),
                          env.getMountHeightMax(),
                          env.getMountStyle());
                    mySettings.setSandParams(0, 0, 0, 0);
                    mySettings.setPlantedFieldParams(0, 0, 0, 0);

                    if (env.getTheme().length() > 1) {
                        mySettings.setTheme(env.getTheme());
                    } else {
                        mySettings.setTheme("");
                    }

                    /* select the map */
                    java.util.ArrayList<String> boardvec = new java.util.ArrayList<String>();
                    boardvec.add(MapSettings.BOARD_GENERATED);
                    mySettings.setBoardsSelectedVector(boardvec);

                    if ((client.getBuildingTemplate() != null) &&
                              (client.getBuildingTemplate().getTotalBuildings() > 0)) {
                        java.util.ArrayList<BuildingTemplate> buildingList = generateRandomBuildings(mySettings,
                              client.getBuildingTemplate());
                        mySettings.setBoardBuildings(buildingList);
                    } else if (!env.getCityType().equalsIgnoreCase("NONE")) {
                        mySettings.setRoadParam(0);
                        mySettings.setCityParams(env.getRoads(),
                              env.getCityType(),
                              env.getMinCF(),
                              env.getMaxCF(),
                              env.getMinFloors(),
                              env.getMaxFloors(),
                              env.getCityDensity(),
                              env.getTownSize());
                    }

                    mySettings.setMedium(client.getMapMedium());
                    /* sent to server */
                    mmClient.sendMapSettings(mySettings);

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
                        planetCondition.setWindMin(aTerrain.getWindStrength());
                        planetCondition.setWindMax(aTerrain.getMaxWindStrength());

                        // Check for a night game and set nightGame Variable.
                        // This is needed to be done since it was possible that
                        // a slow connection
                        // would keep the mmClient from getting an update from the
                        // server before the
                        // entities where added to the game.
                        nightGame = aTerrain.getLightConditions().ordinal() > Light.DUSK.ordinal();

                        mmClient.sendPlanetaryConditions(planetCondition);
                    }
                }

            }

            /*
             * Add bots, if being used in this game.
             */
            if (client.isUsingBots()) {
                String name = "War Bot" + mmClient.getLocalPlayer().getId();
                bot = new Princess(name, mmClient.getHost(), mmClient.getPort());
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

                if (client.isBotsOnSameTeam()) {
                    bot.getLocalPlayer().setTeam(5);
                }
                java.util.Random r = new java.util.Random();

                bot.getLocalPlayer().setStartingPos(r.nextInt(11));
                bot.sendPlayerInfo();
                Thread.sleep(125);
            }

            if (((mmClient.getGame() != null) && (mmClient.getGame().getPhase() == GamePhase.LOUNGE))) {

                mmClient.getGame().getOptions().loadOptions();
                if ((meks.size() > 0) && (xmlGameOptions.size() > 0)) {
                    mmClient.sendGameOptions("", xmlGameOptions);
                }

                ClientPreferences cs = PreferenceManager.getClientPreferences();
                cs.setStampFilenames(Boolean.parseBoolean(client.getserverConfigs("MMTimeStampLogFile")));
                cs.setShowUnitId(Boolean.parseBoolean(client.getserverConfigs("MMShowUnitId")));
                cs.setKeepGameLog(Boolean.parseBoolean(client.getserverConfigs("MMKeepGameLog")));
                cs.setGameLogFilename(client.getserverConfigs("MMGameLogName"));
                /*the cs object ref is no longer needed, so release the ref to it- BarukKhazad!
                 */
                cs = null;

                if (!client.getConfig().getParam("UNITCAMO").equals(Camouflage.NO_CAMOUFLAGE)) {
                    mmClient.getLocalPlayer()
                          .setCamouflage(new Camouflage(Camouflage.ROOT_CATEGORY,
                                client.getConfig().getParam("UNITCAMO")));
                    //                    mmClient.getLocalPlayer().setCategory(Camouflage.ROOT_CATEGORY);
                    //                    mmClient.getLocalPlayer().setCamoFileName(mmClient.getConfig().getParam("UNITCAMO"));
                    playerUpdate = true;
                }

                if (bot != null) {
                    bot.getLocalPlayer().setNbrMFConventional(client.getPlayer().getConventionalMinesAllowed());
                    bot.getLocalPlayer().setNbrMFVibra(client.getPlayer().getVibraMinesAllowed());
                } else {
                    mmClient.getLocalPlayer().setNbrMFConventional(client.getPlayer().getConventionalMinesAllowed());
                    mmClient.getLocalPlayer().setNbrMFVibra(client.getPlayer().getVibraMinesAllowed());
                }

                for (Unit unit : meks) {
                    // Get the Mek
                    CUnit mek = (CUnit) unit;
                    // Get the Entity
                    Entity entity = mek.getEntity();
                    // Set the TempID for autoreporting
                    entity.setExternalId(mek.getId());
                    // entity.setId(mek.getId());
                    // Set the owner
                    entity.setOwner(mmClient.getLocalPlayer());
                    // Set if unit is a commander in this army.
                    entity.setCommander(currA.isCommander(mek.getId()));

                    // Set slights based on games light conditions.
                    if (!entity.hasSearchlight()) {
                        entity.getQuirks().getOption("searchlight").setValue(nightGame);
                    }
                    entity.setSearchlightState(nightGame);

                    // Set the correct home edge for off board units
                    if (entity.isOffBoard()) {
                        OffBoardDirection direction = OffBoardDirection.NORTH;
                        switch (client.getPlayerStartingEdge()) {
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
                    mmClient.sendAddEntity(entity);
                    // Wait a few secs to not overuse bandwith
                    Thread.sleep(125);
                    /*the entity object ref was passed so release the ref to it- BarukKhazad!
                     * some concern that this "entity" is a keyword of some sort, expect it to puke on conplie if yes
                     * fahr- this represents anything on the map - but in this case is units
                     */
                    entity = null;
                }

                /*
                 * Army meks already loaded (see previous for loop). Now try to
                 * load the artillery units generated by the server (see
                 * AutoArmy.java in the server.campaign pacakage for generation
                 * details).
                 */
                java.util.Iterator<CUnit> autoIt = autoArmy.iterator();
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
                        entity.setOwner(mmClient.getLocalPlayer());
                    }

                    if (entity.getCrew().getName().equalsIgnoreCase("Unnamed") ||
                              entity.getCrew().getName().equalsIgnoreCase("vacant")) {
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
                        mmClient.sendAddEntity(entity);
                    }

                    // Wait a few secs to not overuse bandwith
                    Thread.sleep(125);
                    /*the entity object ref was passed so release the ref to it- BarukKhazad!
                     * some concern that this "entity" is a keyword of some sort, expect it to puke on conplie if yes
                     */
                    entity = null;
                }// end while(more autoarty)

                if (client.getPlayerStartingEdge() != Buildings.EDGE_UNKNOWN) {
                    mmClient.getLocalPlayer().setStartingPos(client.getPlayerStartingEdge());
                    playerUpdate = true;
                }

                if (meks.size() > 0) {
                    // check armies for C3Network meks

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

                if (client.getPlayer().getTeamNumber() > 0) {
                    mmClient.getLocalPlayer().setTeam(client.getPlayer().getTeamNumber());
                    playerUpdate = true;
                }

                if (playerUpdate) {
                    mmClient.sendPlayerInfo();
                    if (bot != null) {
                        bot.sendPlayerInfo();
                    }
                }

            }

        } catch (Exception e) {
            MWLogger.errLog(e);
        }
        /*the swingGui object ref was initialized and is
         *active on the mmClient thread, so release the ref to it- BarukKhazad!
         */
        swingGui = null;

    }

    /**
     * Scans the boards directory for map boards of the appropriate size and returns them.
     */
    private java.util.ArrayList<String> scanForBoards(int boardWidth, int boardHeight, String folder) {
        BoardDimensions dimension = new BoardDimensions(boardWidth, boardHeight);
        java.util.ArrayList<String> boards = new java.util.ArrayList<String>();
        // Board Board = mmClient.game.getBoard();

        java.io.File boardDir = new java.io.File("data/boards/" + folder);

        // just a check...
        if (!boardDir.isDirectory()) {
            return boards;
        }

        // scan files
        String[] fileList = boardDir.list();
        java.util.Vector<String> tempList = new java.util.Vector<String>(1, 1);
        java.util.Comparator<? super String> sortComp = mekwars.common.threads.ClientThread.stringComparator();
        for (String path : fileList) {
            if (path.indexOf(".board") == -1) {
                continue;
            }

            if (folder.trim().length() > 0) {
                path = folder + "/" + path;
            }

            if (Board.boardIsSize(new java.io.File(path), dimension)) {
                tempList.addElement(path.substring(0, path.lastIndexOf(".board")));
            }
        }

        // if there are any boards, add these:
        if (tempList.size() > 0) {
            boards.add(MapSettings.BOARD_SURPRISE);
            boards.add(MapSettings.BOARD_GENERATED);
            java.util.Collections.sort(tempList, sortComp);
            for (int loop = 0; loop < tempList.size(); loop++) {
                boards.add(tempList.elementAt(loop));
            }
        } else {
            boards.add(MapSettings.BOARD_GENERATED);
        }

        return boards;
    }

    private java.util.ArrayList<BuildingTemplate> generateRandomBuildings(MapSettings mapSettings,
          Buildings buildingTemplate) {

        java.util.ArrayList<BuildingTemplate> buildingList = new java.util.ArrayList<BuildingTemplate>();
        java.util.ArrayList<String> buildingTypes = new java.util.ArrayList<String>();

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

        java.util.StringTokenizer types = new java.util.StringTokenizer(buildingTemplate.getBuildingType(), ",");

        while (types.hasMoreTokens()) {
            buildingTypes.add(types.nextToken());
        }

        int typeSize = buildingTypes.size();

        java.util.Random r = new java.util.Random();

        java.util.TreeSet<String> tempMap = new java.util.TreeSet<String>();
        Coords coord = new Coords(0, 0);
        String stringCoord = "";

        for (int count = 0; count < buildingTemplate.getTotalBuildings(); count++) {
            int loops = 0;
            boolean CFx2 = false;
            java.util.ArrayList<Coords> coordList = new java.util.ArrayList<Coords>();
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

    /*
     * Taken form Megamek Code for use with MekWars The call was private and was
     * needed. Thanks to Ben Mazur and all of the MM coders we hope for a long
     * and happy relation ship. Torren.
     */

    /**
     * @param army
     * @param slaveid
     * @param masterid This function goes through and makes sure the slave is linked to the master unit
     *
     * @author jtighe
     */
    public void linkMegaMekC3Units(CArmy army, Integer slaveid, Integer masterid) {
        Entity c3Unit = null;
        Entity c3Master = null;

        while ((c3Unit == null) || (c3Master == null)) {
            try {

                for (Entity en : mmClient.getGame().getEntitiesVector()) {
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
            if (!masterUnit.hasC3SlavesLinkedTo(army) &&
                      masterUnit.hasBeenC3LinkedTo(army) &&
                      ((masterUnit.getC3Level() == Unit.C3_MASTER) || (masterUnit.getC3Level() == Unit.C3_MMASTER))) {
                // MWLogger.errLog("Unit:
                // "+c3Master.getModel()+" id: "+c3Master.getExternalId());
                if (c3Master.getC3MasterId() == Entity.NONE) {
                    c3Master.setShutDown(false);
                    c3Master.setC3Master(c3Master, false);
                    mmClient.sendUpdateEntity(c3Master);
                }
                /*
                 * if ( c3Master.hasC3MM() )
                 * MWLogger.errLog("hasC3MM"); else
                 * MWLogger.errLog("!hasC3MM");
                 */
            } else if (c3Master.getC3MasterId() != Entity.NONE) {
                c3Master.setShutDown(false);
                c3Master.setC3Master(Entity.NONE, false);
                mmClient.sendUpdateEntity(c3Master);
            }
            // MWLogger.errLog("c3Unit: "+c3Unit.getModel()+"
            // Master: "+c3Master.getModel());
            c3Unit.setShutDown(false);
            c3Unit.setC3Master(c3Master, false);
            // MWLogger.errLog("c3Master Set to
            // "+c3Unit.getC3MasterId()+" "+c3Unit.getC3NetId());
            mmClient.sendUpdateEntity(c3Unit);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Error in setting up C3Network");
        }
    }

    public static java.util.Comparator<? super Object> stringComparator() {
        return new java.util.Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                String s1 = ((String) o1).toLowerCase();
                String s2 = ((String) o2).toLowerCase();
                return s1.compareTo(s2);
            }
        };
    }

    /*
     * from megamek.mmClient.CloseClientListener clientClosed() Thanks to MM for
     * adding the listener. And to MMNet for the poorly documented code change.
     */
    @Override
    public void clientClosed() {

        PreferenceManager.getInstance().save();

        if (bot != null) {
            bot.die();
            bot = null;
        }

        // mmClient.die();
        mmClient = null;// explicit null of the MM mmClient. Wasn't/isn't being
        // GC'ed.
        client.closingGame(serverName);
        System.gc();

    }

}
