/*
 * Copyright (C) 2004 Helge Richter (McWizard)
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

package mekwars.common.threads;

import java.awt.KeyboardFocusManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.JFrame;

import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.CloseClientListener;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.util.MegaMekController;
import megamek.codeUtilities.MathUtility;
import megamek.common.KeyBindParser;
import megamek.common.OffBoardDirection;
import megamek.common.board.Board;
import megamek.common.board.BoardDimensions;
import megamek.common.board.Coords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.enums.Gender;
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
import megamek.logging.MMLogger;
import mekwars.common.AdvancedTerrain;
import mekwars.common.I18N.I18NMessages;
import mekwars.common.PlanetEnvironment;
import mekwars.common.Unit;
import mekwars.common.campaign.Buildings;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.UnitUtils;

/**
 * Client-side background thread that bridges a MekWars campaign game (a {@link IClient} connected
 * to a MekWars server) into an actual MegaMek battle: it launches and drives an embedded MegaMek
 * {@link Client} (and its {@link ClientGUI}), connects it to the MegaMek game host for the battle
 * that the MekWars server just set up, configures the game's map/board and planetary conditions
 * from the campaign's current {@link PlanetEnvironment}/{@link AdvancedTerrain}, adds the player's
 * units ({@link #meks}) and any server-generated auto-army units ({@link #autoArmy}) as MegaMek
 * {@link Entity} objects, wires up C3 networks, and optionally spins up a {@link Princess} bot
 * client if the campaign is configured to use bots.
 * <p>
 * This is distinct from the socket reader/writer threads elsewhere in this package: those move raw
 * MekWars campaign protocol text between client and server, whereas this thread drives an entirely
 * separate MegaMek network connection (a second socket, to the MegaMek game host) used only for
 * the actual tabletop battle simulation. All communication back to the MekWars campaign server (for
 * example the {@code RequestOperationSettings} handshake) still goes through the {@link IClient}.
 * <p>
 * The thread is started once (per battle) and its {@link #run()} method does not loop — it performs
 * one-time setup of the MegaMek client/game and then returns, after which the MegaMek client's own
 * threads/UI drive the rest of the battle. This class also implements {@link CloseClientListener} so
 * it's notified ({@link #clientClosed()}) when the embedded MegaMek {@link Client} connection closes,
 * so it can tear down the bot (if any) and notify the {@link IClient} that the game is over.
 */
public class ClientThread extends Thread implements CloseClientListener {
    private final static MMLogger LOGGER = MMLogger.create(ClientThread.class);
    private final static I18NMessages MESSAGES = new I18NMessages(ClientThread.class);

    /** Board starting-edge/compass direction constants used when picking off-board deployment direction. Unused directly (see the switch in {@link #run()}, which uses raw literals instead of these constants). */
    final int N = 0;
    final int NE = 1;
    final int SE = 2;
    final int S = 3;
    final int SW = 4;
    final int NW = 5;

    // VARIABLES
    /** Player/pilot name this MegaMek client connects as; trimmed of surrounding whitespace. */
    private final String myName;
    /** Name of the MekWars campaign server/game, used later when reporting the game as closed. */
    private final String serverName;
    /** TCP port of the MegaMek game host to connect the embedded {@link Client} to. */
    private final int serverPort;
    /** The MekWars campaign client this battle was launched from; used to read campaign/environment state and to report status back to the server. */
    private final IClient client;
    /** Keybinding dispatcher installed on the AWT {@link KeyboardFocusManager} for the embedded MegaMek GUI. */
    private final MegaMekController controller;
    /** The player's own units to add to the MegaMek game as entities. */
    private final ArrayList<Unit> meks;
    /** Server-generated auxiliary/artillery units (e.g. from AutoArmy) to add alongside {@link #meks}. */
    private final ArrayList<CUnit> autoArmy;// from server's
    /** Optional Princess bot client spun up when the campaign is configured to use bots; null otherwise. */
    BotClient bot = null;
    /** Hostname/IP of the MegaMek game host; normalized to the literal "127.0.0.1" if it merely contains that substring. */
    private String serverip;
    /** The embedded MegaMek network client for this battle; null before {@link #run()} connects, and explicitly nulled again in {@link #clientClosed()}. */
    private Client mmClient;
    /** The embedded MegaMek Swing GUI for this battle; created in {@link #run()} and nulled out once run() completes. */
    private ClientGUI swingGui;

    // CONSTRUCTOR
    /**
     * Captures all the state needed to later connect to and set up a MegaMek battle, and installs a
     * fresh {@link MegaMekController} as the current AWT key event dispatcher (parsing key bindings
     * for it immediately). Does not itself connect to anything or start the thread — call
     * {@link #start()} separately to run {@link #run()} on a new thread.
     *
     * @param name       player/pilot name to connect to the MegaMek host as (trimmed)
     * @param servername name of the MekWars server/game, used when reporting the game as closed
     * @param ip         MegaMek game host address; normalized to "127.0.0.1" if it contains that substring
     * @param port       MegaMek game host port
     * @param client     the MekWars campaign client this battle belongs to
     * @param meks       the player's own units to add to the battle
     * @param autoArmy   server-generated auxiliary units to add to the battle
     */
    public ClientThread(String name, String servername, String ip, int port, IClient client, ArrayList<Unit> meks,
          ArrayList<CUnit> autoArmy) {
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

    /**
     * @return the embedded MegaMek network client for this battle, or {@code null} before
     *         {@link #run()} has connected (or after {@link #clientClosed()} has run)
     */
    public Client getMMClient() {
        return mmClient;
    }

    /**
     * @return the MegaMek keybinding controller created for this battle's embedded GUI
     */
    public MegaMekController getMegaMekController() {
        return controller;
    }

    /**
     * One-shot setup routine (not a loop) that stands up the embedded MegaMek battle for this
     * thread's target game and returns once the battle has been initialized. In order, it:
     * <ol>
     *     <li>resets per-game campaign state on {@link #client} (mine counts, bot usage flag,
     *     environment/terrain, starting edge, game options) and tears down any previous
     *     {@link #swingGui}'s local bots;</li>
     *     <li>creates a fresh embedded MegaMek {@link Client} and {@link ClientGUI}
     *     ({@link #swingGui}) and registers this thread as its {@link CloseClientListener};</li>
     *     <li>if the campaign hasn't yet fetched game/operation options from the server, sends a
     *     {@code RequestOperationSettings} campaign command and busy-waits (polling every second)
     *     until {@link IClient#isWaiting()} clears;</li>
     *     <li>connects the embedded {@link Client} to the MegaMek game host at {@link #serverip}:
     *     {@link #serverPort}; if the connection attempt throws, this method aborts by nulling
     *     {@link #mmClient}, showing an info dialog, logging, and returning early;</li>
     *     <li>busy-waits for the local player and game phase to become available/known;</li>
     *     <li>while still in the lobby phase, if a campaign environment is set, builds
     *     {@link MapSettings} (board size, terrain generation parameters or a fixed static board,
     *     buildings, city params) and {@link PlanetaryConditions} from the current
     *     {@link PlanetEnvironment}/{@link AdvancedTerrain} and sends them to the MegaMek host;</li>
     *     <li>if the campaign is configured to use bots, creates and connects a {@link Princess}
     *     bot client, busy-waiting for it to come up the same way as the human client;</li>
     *     <li>still in the lobby phase: loads game options, applies client log/camo preferences,
     *     sets minefield allowances, adds every unit in {@link #meks} and {@link #autoArmy} to the
     *     game as MegaMek {@link Entity} objects (assigning owner, external id, commander flag,
     *     searchlight state for night games, off-board deployment edge, and pilot/crew), links up
     *     any C3 networks declared on the player's locked {@link CArmy}, and sets team/starting
     *     position — sending a single player-info update at the end if anything changed.</li>
     * </ol>
     * Any exception anywhere in this sequence (after the client successfully connects) is caught,
     * logged, and swallowed — the method does not rethrow. In all cases, {@link #swingGui} is set
     * back to {@code null} before returning (the comment above that line notes this is deliberate:
     * the GUI object continues to live and operate on the MegaMek client's own thread, so this
     * thread only needs to release its reference to it). Several busy-wait polling loops here
     * (waiting for local player / game phase / bot readiness) use fixed {@link Thread#sleep} polls
     * rather than a blocking/event-driven approach, and are capped at 1000 iterations of 50ms
     * (~50 seconds) for the phase-detection loops but are otherwise unbounded (e.g. waiting for
     * {@code mmClient.getLocalPlayer()} to become non-null has no timeout at all).
     */
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

        } catch (Exception ex) {
            LOGGER.error(ex, "Error reporting game!");
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

            client.sendChat(String.format("%sc RequestOperationSettings", IClient.CAMPAIGN_PREFIX));
            while (client.isWaiting()) {
                try {
                    client.addToChat("Retrieving Operation Data Please Wait..");
                    Thread.sleep(1000);
                } catch (Exception ignored) {

                }
            }
        }

        Vector<IBasicOption> xmlGameOptions = client.getGameOptions();

        try {
            mmClient.connect();
        } catch (Exception ex) {
            mmClient = null;
            client.showInfoWindow(MESSAGES.getString("run.CouldNotJoinGame"));
            LOGGER.info("{}:{}", serverip, serverPort);
            return;
        }

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
                mySettings.setBoardSize((int) client.getMapSize().getWidth(), (int) client.getMapSize().getHeight());
                mySettings.setMapSize(1, 1);  // Note to self: MapSize in MM is boards x boards, not hexes x hexes

                AdvancedTerrain aTerrain = client.getCurrentAdvancedTerrain();

                PlanetEnvironment planetEnvironment = client.getCurrentEnvironment();

                if ((planetEnvironment != null) && planetEnvironment.isStaticMap()) {
                    mySettings = MapSettings.getInstance();
                    mySettings.setBoardSize(planetEnvironment.getXBoardSize(), planetEnvironment.getYBoardSize());
                    mySettings.setMapSize(planetEnvironment.getXSize(), planetEnvironment.getYSize());

                    ArrayList<String> boardVector = new ArrayList<>();
                    if (planetEnvironment.getStaticMapName().toLowerCase().endsWith("surprise")) {
                        int maxBoards = planetEnvironment.getXBoardSize() * planetEnvironment.getYBoardSize();

                        for (int i = 0; i < maxBoards; i++) {
                            boardVector.add(MapSettings.BOARD_SURPRISE);
                        }

                        mySettings.setBoardsSelectedVector(boardVector);

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
                            boardVector.add(MapSettings.BOARD_GENERATED);
                        }

                        mySettings.setBoardsSelectedVector(boardVector);
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
                            ArrayList<BuildingTemplate> buildingList = generateRandomBuildings(mySettings,
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
                        boardVector.add(planetEnvironment.getStaticMapName());
                        mySettings.setBoardsSelectedVector(boardVector);
                    }

                    PlanetaryConditions planetCondition = new PlanetaryConditions();

                    planetCondition.setGravity((float) aTerrain.getGravity());
                    planetCondition.setTemperature(aTerrain.getTemperature());
                    planetCondition.setAtmosphere(aTerrain.getAtmosphere());
                    planetCondition.setEMI(aTerrain.hasEMI());
                    planetCondition.setFog(aTerrain.getFog());
                    planetCondition.setLight(aTerrain.getLightConditions());
                    planetCondition.setShiftingWindDirection(aTerrain.hasShiftingWindDirection());
                    planetCondition.setShiftingWindStrength(aTerrain.hasShiftingWindStrength());
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
                        planetCondition.setShiftingWindDirection(aTerrain.hasShiftingWindDirection());
                        planetCondition.setShiftingWindStrength(aTerrain.hasShiftingWindStrength());
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
                    }
                }

            }

            /*
             * Add bots, if being used in this game.
             */
            if (client.isUsingBots()) {
                String name = MESSAGES.getString("run.WarBotName", mmClient.getLocalPlayer().getId());
                bot = new Princess(name, mmClient.getHost(), mmClient.getPort());
                bot.getGame().addGameListener(new BotGUI(new JFrame(), bot));
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
                    LOGGER.error(ex, "Bot Error!");
                }

                Thread.sleep(125);

                swingGui.getLocalBots().put(name, bot);

                if (client.isBotsOnSameTeam()) {
                    bot.getLocalPlayer().setTeam(5);
                }
                Random random = new Random();

                bot.getLocalPlayer().setStartingPos(random.nextInt(11));
                bot.sendPlayerInfo();
                Thread.sleep(125);
            }

            if (((mmClient.getGame() != null) && (mmClient.getGame().getPhase() == GamePhase.LOUNGE))) {
                mmClient.getGame().getOptions().loadOptions();

                if ((!meks.isEmpty()) && (!xmlGameOptions.isEmpty())) {
                    mmClient.sendGameOptions("", xmlGameOptions);
                }

                ClientPreferences cs = PreferenceManager.getClientPreferences();
                cs.setStampFilenames(MathUtility.parseBoolean(client.getServerConfigs("MMTimeStampLogFile"), false));
                cs.setShowUnitId(MathUtility.parseBoolean(client.getServerConfigs("MMShowUnitId"), false));
                cs.setKeepGameLog(MathUtility.parseBoolean(client.getServerConfigs("MMKeepGameLog"), false));
                cs.setGameLogFilename(client.getServerConfigs("MMGameLogName"));

                if (!client.getConfig().getParam("UNIT_CAMO").equals(Camouflage.NO_CAMOUFLAGE)) {
                    mmClient.getLocalPlayer()
                          .setCamouflage(new Camouflage(Camouflage.ROOT_CATEGORY,
                                client.getConfig().getParam("UNIT_CAMO")));
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
                    // Set the TempID for auto reporting
                    entity.setExternalId(mek.getId());
                    // Set the owner
                    entity.setOwner(mmClient.getLocalPlayer());
                    // Set if unit is a commander in this army.
                    entity.setCommander(currA.isCommander(mek.getId()));

                    // Set slights based on games light conditions.
                    if (!entity.hasSearchlight()) {
                        entity.getQuirks().getOption("searchlight").setValue(nightGame);
                    }

                    entity.setSearchlightState(nightGame);

                    // Set the correct home edge for off-board units
                    if (entity.isOffBoard()) {
                        OffBoardDirection direction = switch (client.getPlayerStartingEdge()) {
                            case 4, 14 -> OffBoardDirection.EAST;
                            case 5, 6, 7, 15, 16, 17 -> OffBoardDirection.SOUTH;
                            case 8, 18 -> OffBoardDirection.WEST;
                            default -> OffBoardDirection.NORTH;
                        };
                        entity.setOffBoard(entity.getOffBoardDistance(), direction);
                    }

                    // Add Pilot to entity
                    entity.setCrew(UnitUtils.createEntityPilot(mek));
                    List<Entity> entities = new ArrayList<>();
                    entities.add(entity);
                    // Add Mek to game
                    mmClient.sendAddEntity(entities);
                }

                /*
                 * Army meks already loaded (see previous for loop). Now try to
                 * load the artillery units generated by the server (see
                 * AutoArmy.java in the server.campaign package for generation
                 * details).
                 */
                for (CUnit autoUnit : autoArmy) {
                    // get the unit
                    // get the entity
                    Entity entity = autoUnit.getEntity();

                    // Set slights based on game light conditions.
                    entity.setExternalSearchlight(nightGame);
                    entity.setSearchlightState(nightGame);

                    // Had issues with Id's so we are now setting them.
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
                        Crew pilot = new Crew(CrewType.SINGLE, "AutoArtillery", 1, 4, 5, Gender.RANDOMIZE, false, null);
                        entity.setCrew(pilot);
                    } else {
                        entity.setCrew(UnitUtils.createEntityPilot(autoUnit));
                    }

                    if (bot != null) {
                        bot.sendAddEntity(List.of(entity));
                    } else {
                        mmClient.sendAddEntity(List.of(entity));
                    }
                }

                if (client.getPlayerStartingEdge() != Buildings.EDGE_UNKNOWN) {
                    mmClient.getLocalPlayer().setStartingPos(client.getPlayerStartingEdge());
                    playerUpdate = true;
                }

                if (!meks.isEmpty()) {
                    // check armies for C3Network meks
                    synchronized (currA) {
                        if (!currA.getC3Network().isEmpty()) {
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
            LOGGER.error(e, "Error in Client Thread: {}", e.getLocalizedMessage());
        }
        /*the swingGui object ref was initialized and is
         *active on the mmClient thread, so release the ref to it- BarukKhazad!
         */
        swingGui = null;

    }

    /**
     * Scans the {@code data/boards} directory (optionally a subfolder of it) for {@code .board}
     * files matching the given dimensions and returns the list of available board names (without
     * the {@code .board} extension), prefixed with the special {@link MapSettings#BOARD_SURPRISE}
     * and {@link MapSettings#BOARD_GENERATED} pseudo-board entries when at least one real board was
     * found, sorted case-insensitively.
     *
     * @param boardWidth  required board width in hexes
     * @param boardHeight required board height in hexes
     * @param folder      subfolder of {@code data/boards} to scan (empty string for the top level)
     * @return list of selectable board name entries; if no matching boards exist, contains only
     *         {@link MapSettings#BOARD_GENERATED}; if {@code folder} doesn't exist as a directory,
     *         returns an empty list instead
     */
    private ArrayList<String> scanForBoards(int boardWidth, int boardHeight, String folder) {
        BoardDimensions dimension = new BoardDimensions(boardWidth, boardHeight);
        ArrayList<String> boards = new ArrayList<>();

        File boardDir = new File("data/boards", folder);

        // just a check...
        if (!boardDir.isDirectory()) {
            return boards;
        }

        // scan files
        String[] fileList = boardDir.list();
        Vector<String> tempList = new Vector<>(1, 1);
        Comparator<? super String> sortComp = ClientThread.stringComparator();
        if (fileList != null) {
            for (String path : fileList) {
                if (!path.contains(".board")) {
                    continue;
                }

                if (!folder.trim().isEmpty()) {
                    path = folder + "/" + path;
                }

                if (Board.boardIsSize(new File(path), dimension)) {
                    tempList.addElement(path.substring(0, path.lastIndexOf(".board")));
                }
            }
        }

        // if there are any boards, add these:
        if (!tempList.isEmpty()) {
            boards.add(MapSettings.BOARD_SURPRISE);
            boards.add(MapSettings.BOARD_GENERATED);
            tempList.sort(sortComp);

            for (int loop = 0; loop < tempList.size(); loop++) {
                boards.add(tempList.elementAt(loop));
            }
        } else {
            boards.add(MapSettings.BOARD_GENERATED);
        }

        return boards;
    }

    /**
     * Randomly places the number of buildings specified by {@code buildingTemplate} onto the board,
     * restricting placement to a 5-hex-wide/tall strip near the appropriate map edge when the
     * template specifies a starting edge (north/south/east/west), and picking random,
     * non-duplicate coordinates (retrying up to 100 times per building before giving up and
     * doubling the construction factor as a fallback — see the {@code CFx2} flag below). Each
     * building's floor count and construction factor (CF) are randomized within the template's
     * min/max range (or fixed at the min if max &lt;= min).
     * <p>
     * Note: if a spot can't be found after 100 attempts for a given building, the loop just breaks
     * out and uses whatever coordinate was last generated (which may duplicate another building's
     * location, since {@code tempMap.add(stringCoord)} is called unconditionally afterward) — it
     * does not skip the building or retry with a fresh strategy; it only doubles that building's CF.
     *
     * @param mapSettings      the board's settings, used to read/clamp against actual board width/height
     * @param buildingTemplate describes how many buildings to place, their type, and floor/CF ranges
     * @return the list of randomly-placed {@link BuildingTemplate} entries to apply to the board
     */
    private ArrayList<BuildingTemplate> generateRandomBuildings(MapSettings mapSettings, Buildings buildingTemplate) {
        ArrayList<BuildingTemplate> buildingList = new ArrayList<>();
        ArrayList<BuildingType> buildingTypes = new ArrayList<>();

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

        Random random = new Random();

        TreeSet<String> tempMap = new TreeSet<>();
        Coords coord = new Coords(0, 0);
        String stringCoord = "";

        for (int count = 0; count < buildingTemplate.getTotalBuildings(); count++) {
            int loops = 0;
            boolean CFx2 = false;
            ArrayList<Coords> cordList = new ArrayList<>();
            do {
                if (loops++ > 100) {
                    CFx2 = true;
                    break;
                }

                int x = random.nextInt(width) + minWidth;
                int y = random.nextInt(height) + minHeight;

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

                stringCoord = String.format("%d, %d", x, y);
            } while (tempMap.contains(stringCoord));

            tempMap.add(stringCoord);
            cordList.add(coord);

            int floors = buildingTemplate.getMaxFloors() - buildingTemplate.getMinFloors();

            if (floors <= 0) {
                floors = buildingTemplate.getMinFloors();
            } else {
                floors = random.nextInt(floors) + buildingTemplate.getMinFloors();
            }

            int totalCF = buildingTemplate.getMaxCF() - buildingTemplate.getMinCF();

            if (totalCF <= 0) {
                totalCF = buildingTemplate.getMinCF();
            } else {
                totalCF = random.nextInt(totalCF) + buildingTemplate.getMinCF();
            }

            if (CFx2) {
                totalCF *= 2;
            }

            BuildingType type = buildingTemplate.getBuildingType();
            buildingList.add(new BuildingTemplate(type, cordList, totalCF, floors, -1));
        }

        return buildingList;
    }

    /*
     * Taken from Megamek Code for use with MekWars The call was private and was
     * needed. Thanks to Ben Mazur and all the MM coders, we hope for a long
     * and happy relationship. Torren.
     */

    /**
     * Finds the MegaMek {@link Entity} objects (by external id) corresponding to a C3 slave/master
     * pair declared in the campaign army's C3 network map, and wires up the actual MegaMek C3 link
     * between them (powering on and setting {@code setC3Master} on both sides as needed, then
     * pushing the update to the server via {@code sendUpdateEntity}).
     * <p>
     * Because entities are added to the MegaMek game asynchronously (see the {@code sendAddEntity}
     * calls in {@link #run()}), this method busy-waits in a loop — sleeping 10ms between
     * attempts, with no timeout/retry limit — re-scanning {@code mmClient.getGame().getEntitiesVector()}
     * until both the slave and master entities are found by external id. Any exception while
     * scanning is logged and the loop simply retries (it does not exit early on error).
     *
     * @param army    the campaign army whose C3 network mapping (slave id -> master id) is being applied
     * @param slaveID external id of the C3 slave unit
     * @param masterID external id of the C3 master unit
     * @author jtighe
     */
    public void linkMegaMekC3Units(CArmy army, Integer slaveID, Integer masterID) {
        Entity c3Unit = null;
        Entity c3Master = null;

        while ((c3Unit == null) || (c3Master == null)) {
            try {

                for (Entity entity : mmClient.getGame().getEntitiesVector()) {
                    if ((c3Unit == null) && (entity.getExternalId() == slaveID)) {
                        c3Unit = entity;
                    }

                    if ((c3Master == null) && (entity.getExternalId() == masterID)) {
                        c3Master = entity;
                    }
                }
                Thread.sleep(10);// give the queue time to refresh
            } catch (Exception ex) {
                LOGGER.error(ex, "Error in linkMegaMekC3Units");
            }
        }

        // catch for some funky stuff
        if ((c3Unit == null) || (c3Master == null)) {
            LOGGER.debug("Null Units c3Unit: {} C3Master: {}", c3Unit, c3Master);
            return;
        }

        try {
            CUnit masterUnit = (CUnit) army.getUnit(masterID);
            if (!masterUnit.hasC3SlavesLinkedTo(army) &&
                      masterUnit.hasBeenC3LinkedTo(army) &&
                      ((masterUnit.getC3Level() == Unit.C3_MASTER) || (masterUnit.getC3Level() == Unit.C3M_MASTER))) {
                if (c3Master.getC3MasterId() == Entity.NONE) {
                    c3Master.setShutDown(false);
                    c3Master.setC3Master(c3Master, false);
                    mmClient.sendUpdateEntity(c3Master);
                }
            } else if (c3Master.getC3MasterId() != Entity.NONE) {
                c3Master.setShutDown(false);
                c3Master.setC3Master(Entity.NONE, false);
                mmClient.sendUpdateEntity(c3Master);
            }
            c3Unit.setShutDown(false);
            c3Unit.setC3Master(c3Master, false);

            mmClient.sendUpdateEntity(c3Unit);
        } catch (Exception ex) {
            LOGGER.error(ex, "Error in setting up C3Network");
        }
    }

    /**
     * Case-insensitive string comparator used to sort board names in {@link #scanForBoards(int, int, String)}.
     * The raw type ({@code Object} rather than {@code String}) means a {@link ClassCastException}
     * will be thrown at comparison time if used against a collection containing non-{@code String}
     * elements.
     *
     * @return a comparator that lower-cases both operands before comparing them
     */
    public static Comparator<? super Object> stringComparator() {
        return (Comparator<Object>) (o1, o2) -> {
            String s1 = ((String) o1).toLowerCase();
            String s2 = ((String) o2).toLowerCase();
            return s1.compareTo(s2);
        };
    }

    /*
     * from megamek.mmClient.CloseClientListener clientClosed() Thanks to MM for
     * adding the listener. And to MMNet for the poorly documented code change.
     */
    /**
     * Callback invoked by the embedded MegaMek {@link Client} (registered as a
     * {@link CloseClientListener} in {@link #run()}) when the MegaMek connection/game closes —
     * i.e. when the battle ends or the connection drops. Saves MegaMek's client preferences, kills
     * and clears any running {@link #bot}, explicitly drops the reference to {@link #mmClient} (the
     * comment on that line notes it wasn't reliably being garbage collected otherwise), notifies the
     * MekWars campaign client that this game has closed via {@link IClient#closingGame(String)}, and
     * then explicitly requests a GC. Note {@code mmClient.die()} itself is commented out here — only
     * the local reference is cleared, the client object's own shutdown is presumably handled
     * elsewhere (or relies on this listener callback having originated from that shutdown already
     * happening).
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
