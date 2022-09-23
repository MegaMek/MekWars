/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - nmorris (urgru@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

/*
 * ShortOperations are holders for real game information - players, type, target, etc. EVERY game is managed by a short operation.
 *
 * Creation of a ShortOperation on will fail unless: 1) Operation is 1 game in length, or 2) LongOperation of the type, initiatiated by player's faction, is
 * already under way on target world.
 *
 * In some senses, the ShortOp is the closest thing the Operations system as a whole has to the old MMNET/early-MekWars style Task; however, many Task functions
 * are carried out by the Manager, Validators and Resolvers.
 *
 * Like the old Task, ShortOp handles game options, board settings, and other pertinent Client-loaded info.
 */

package server.campaign.operations;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import common.AdvancedTerrain;
import common.CampaignData;
import common.Continent;
import common.PlanetEnvironment;
import common.Unit;
import common.UnitFactory;
import common.campaign.Buildings;
import common.campaign.operations.Operation;
import common.util.MWLogger;
import common.util.StringUtils;
import common.util.UnitUtils;
import megamek.common.PlanetaryConditions;
import server.campaign.AutoArmy;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.SUnitFactory;
import server.campaign.autoresolve.BattleResolver;
import server.campaign.mercenaries.ContractInfo;
import server.campaign.mercenaries.MercHouse;
import server.campaign.operations.resolvers.NewShortResolver;
import server.campaign.operations.resolvers.ShortOpPlayers;
import server.campaign.pilot.SPilot;
import server.util.StringUtil;

// IMPORTS

public class ShortOperation implements Comparable<Object> {

    // IVARS

    // progress info. finished short ops removed from
    // running and IDs released on ticks.
    public static int STATUS_WAITING = 0;
    public static int STATUS_INPROGRESS = 1;
    public static int STATUS_REPORTING = 2;
    public static int STATUS_FINISHED = 4;

    // Starting values. Used in /c modgames and in ShortResolver. Increased by
    // addAttacker/Defender.
    int startingBV = 0;
    int startingUnits = 0;

    // Finishing values. Use in /c modgames and *set* by the ShortResolver.
    int finishingBV = 0;

    // Header for modgames info.
    String modHeader = "";

    // holding vars for pertinant game info
    private SPlanet targetWorld;
    private boolean fromReserve;
    private int shortID = -1;
    private int longID = -1;// id of parent long op, if one exists

    private TreeMap<String, Integer> defenders;
    private TreeMap<String, Integer> attackers;
    private TreeMap<String, SPlayer> winners;
    private TreeMap<String, SPlayer> losers;
    private ArrayList<SArmy> pdlist;
    private TreeMap<String, OpsChickenThread> chickenThreads;

    private TreeMap<Integer, OperationEntity> unitsInProgress;
    private TreeMap<Integer, SPilot> pilotsInProgress;

    private SPlayer initiator;// player who sends the command to start an op
    private Continent playContinent;
    private PlanetEnvironment playEnvironment;
    private StringBuilder cityBuilder = new StringBuilder();

    // intel info
    private AdvancedTerrain aTerrain = null;
    private boolean intelVacuum = false;
    private boolean doubleBlind = false;
    private double intelGravity = 0;
    private int intelTemp = 0;
    private int intelTimeFrame = PlanetaryConditions.L_DAY;
    private int intelVisibility = 999;
    private int intelWeather = PlanetaryConditions.WE_NONE;
    private int intelWind = PlanetaryConditions.WI_NONE;

    private TreeSet<String> cancellingPlayers = new TreeSet<String>();
    /*
     * For the time being, we're only allowing 1 v 1 games. This will change in
     * the future, and MOST of the code is already multiplayer friendly.
     */
    private int maxAttackers = 1;
    private int maxDefenders = 1;
    private int minAttackers = 1;
    private int minDefenders = 1;

    // string set by Resolver. Returned for getFinishedInfo()
    private String completeFinishedString;
    private String incompleteFinishedString;

    private int currentStatus;
    private int showsToClear;// number of tick-shows remaining before removal
    private long startTime = -1;
    private long completionTime = -1;

    /*
     * The GameOptions and Attacker/Defender AUtoArmies. Generated when
     * switching to INPROGRESS status. Save in case the game needs to be
     * rehosted, player quits and returns, etc.
     */
    private StringBuffer gameOptions = new StringBuffer();
    private String attackerAutoString = null;
    private String defenderAutoString = null;
    private String attackerAutoEmplacementsString = null;
    private String defenderAutoEmplacementsString = null;
    private String attackerAutoMinesString = null;
    private String defenderAutoMinesString = null;
    private String attackArtDesc = "";
    private String defendArtDesc = "";
    private float defenderArmyCount = 0;
    private float attackerArmyCount = 0;

    private HashMap<String, String> MULHash = new HashMap<String, String>();

    /*
     * Building Options for any building destruction tasks
     */
    private boolean isBuildingOperation = false;
    private String buildingOptions = "";
    private int[] mapEdge = { Buildings.NORTH, Buildings.SOUTH, Buildings.EAST, Buildings.WEST };
    private int[] mapEdgeReverse = { Buildings.SOUTH, Buildings.NORTH, Buildings.WEST, Buildings.EAST };

    private int[] playerEdge = { Buildings.NORTHWEST, Buildings.NORTH, Buildings.NORTHEAST, Buildings.EAST, Buildings.SOUTHEAST, Buildings.SOUTH, Buildings.SOUTHWEST, Buildings.WEST, Buildings.EDGE, Buildings.CENTER, Buildings.NORTHWESTDEEP, Buildings.NORTHDEEP, Buildings.NORTHEASTDEEP, Buildings.EASTDEEP, Buildings.SOUTHEASTDEEP, Buildings.SOUTHDEEP, Buildings.SOUTHWESTDEEP, Buildings.WESTDEEP };
    private int[] playerEdgeReverse = { Buildings.SOUTHEAST, Buildings.SOUTH, Buildings.SOUTHWEST, Buildings.WEST, Buildings.NORTHWEST, Buildings.NORTH, Buildings.NORTHEAST, Buildings.EAST, Buildings.CENTER, Buildings.EDGE, Buildings.SOUTHEASTDEEP, Buildings.SOUTHDEEP, Buildings.SOUTHWESTDEEP, Buildings.WESTDEEP, Buildings.NORTHWESTDEEP, Buildings.NORTHDEEP, Buildings.NORTHEASTDEEP, Buildings.EASTDEEP };

    private int attackerEdge = -1;
    private int defenderEdge = -1;
    private int totalBuildings = -1;
    private int minBuildings = -1;
    private int[] teamEdge = { Buildings.NORTH, Buildings.SOUTH, Buildings.EAST, Buildings.WEST, Buildings.NORTHWEST, Buildings.SOUTHEAST, Buildings.NORTHEAST, Buildings.SOUTHWEST };
    private boolean isTeamOp = false;

    // The map size, to save. Default to 2x1 FASA. Store to resend after logout.
    private Dimension mapsize = new Dimension(32, 17);

    /*
     * last, but certainly not least, holders for the underlying operation and
     * (if extent) player ModifyingOperation names.
     *
     * use names instead of direct references. Resolver will be fed latest
     * version of named ops/mods by Manager when game ends.
     */
    private String opName;
    private TreeMap<String, String> playerModifyingOps;

    // autoReport String
    private String autoReportString = null;
    private int playersReported = 0;
    private String bots;
    private String botTeams;

    public Vector<SUnit> preCapturedUnits = null;

    private OperationReporter reporter = new OperationReporter();
    private NewShortResolver resolver;


    // CONSTRUCTOR
    public ShortOperation(String opName, SPlanet target, SPlayer initiator, SArmy attackingArmy, ArrayList<SArmy> possibleDefenders, int shortID, int longID, boolean fromReserve) {

        // save params
        targetWorld = target;
        this.initiator = initiator;
        this.opName = opName;
        this.shortID = shortID;
        this.longID = longID;
        this.fromReserve = fromReserve;

        // construct the treemaps
        playerModifyingOps = new TreeMap<String, String>();
        chickenThreads = new TreeMap<String, OpsChickenThread>();
        attackers = new TreeMap<String, Integer>();
        defenders = new TreeMap<String, Integer>();
        winners = new TreeMap<String, SPlayer>();
        losers = new TreeMap<String, SPlayer>();

        // fetch an environment to play in
        playContinent = targetWorld.getEnvironments().getRandomEnvironment(CampaignMain.cm.getR());
        playEnvironment = playContinent.getEnvironment().getEnvironments().firstElement();


        // initiator is always an attacker, so add
        addAttacker(initiator, attackingArmy, "");

        // set up the death trees, for use in auto-disconnection-reporting
        unitsInProgress = new TreeMap<Integer, OperationEntity>();
        pilotsInProgress = new TreeMap<Integer, SPilot>();

        // set initial counter, status, strings, etc
        showsToClear = 3;// 3 tick default
        currentStatus = STATUS_WAITING;
        gameOptions.append("GO|");
        gameOptions.append(CampaignMain.cm.getMegaMekOptionsToString());

        // add to gamelog
        String toLog = "Attack: #" + shortID + "/" + initiator.getName() + "/" + opName + "/" + target.getName() + ".<br> - Potential Defenders: ";
        for (SArmy currA : possibleDefenders) {
            toLog += currA.getName() + "/" + currA.getID() + " ";
        }
        MWLogger.gameLog(toLog);

        Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);

        preCapturedUnits = new Vector<SUnit>(1, 1);


        pdlist = possibleDefenders;

        // inform the defenders
        // Faction Team Ops have a delay in defender informing.
        if ((!fromReserve && !o.getBooleanValue("TeamOperation") && !o.getBooleanValue("TeamsMustBeSameFaction")) || (o.getBooleanValue("TeamOperation") && !o.getBooleanValue("TeamsMustBeSameFaction"))) {
            informPossibleDefenders();
        }
    }

    // METHODS
    /**
     * Method which adds an attacker to the short. Should only be called after validation.
     *
     * NOTE: Ops store a player's NAME, not a special player like the old TaskPlayer. Instead of a special player. The ID# of the army used in the game is keyed
     * to the player name.
     */
    public void addAttacker(SPlayer p, SArmy a, String modName) {
        Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);

        attackers.put(p.getName().toLowerCase(), a.getID());
        reporter.addAttacker(p.getName(), a.getID());
        if (!modName.equals("")) {
            playerModifyingOps.put(p.getName().toLowerCase(), modName);
        }

        // also, lock the participating army and update the client GUI
        p.lockArmy(a.getID());

        // increase the unit and BV counts
        startingBV += a.getOperationsBV(null);
        startingUnits += a.getAmountOfUnits();

        if (o.getBooleanValue("TeamOperation")) {

            if (o.getBooleanValue("TeamsMustBeSameFaction")) {
                int maxPlayers = o.getIntValue("TeamSize");
                int maxBV = o.getIntValue("MaxAttackerBV");
                if ((getAttackers().size() >= maxPlayers) || (getAttackersBV() >= maxBV)) {
                    informPossibleDefenders();
                }
            } else {
                int maxTeams = o.getIntValue("NumberOfTeams");
                int maxPlayersPerTeam = o.getIntValue("TeamSize");
                int maxPlayers = Math.max(2, Math.min(8, maxTeams)) * maxPlayersPerTeam;

                // MWLogger.errLog("Max Teams: "+maxTeams+" Players
                // Per
                // Team: "+maxPlayersPerTeam+" Max Players: "+maxPlayers+"
                // Current Players: "+this.getAllPlayerNames().size());
                if (getAllPlayerNames().size() >= maxPlayers) {
                    changeStatus(STATUS_INPROGRESS);
                }

            }
        }

    }

    private void submitPlayerList() {
    	ShortOpPlayers sop = new ShortOpPlayers();
    	HashMap<Integer, Vector<SPlayer>> teams = new HashMap<Integer, Vector<SPlayer>>();

    	for(String pName : getAllPlayerNames()) {
    		SPlayer p = CampaignMain.cm.getPlayer(pName);
    		int teamID = p.getTeamNumber();

    		if (teams.containsKey(teamID)) {
    			teams.get(teamID).add(p);
    		} else {
    			 Vector<SPlayer> v = new Vector<SPlayer>();
    			 v.add(p);
    			 teams.put(teamID, v);
    		}
    	}

    	// Now, add the teams to sop
    	for (int teamID : teams.keySet()) {
    		sop.addTeam(teamID, teams.get(teamID));
    	}

    	resolver = new NewShortResolver(shortID, targetWorld, this, sop);


    	sop.reportTeams();
    }

    /**
     * Method which removes an attacker, and all of his armies, from a ShortOperation.
     */
    public void removeAttacker(SPlayer p) {

        // remove from maps
        String pNameLower = p.getName().toLowerCase();
        if (attackers.containsKey(pNameLower)) {
            int armyID = attackers.remove(pNameLower);

            // decrease starting values
            startingBV -= p.getArmy(armyID).getOperationsBV(null);
            startingUnits -= p.getArmy(armyID).getAmountOfUnits();
        }
        playerModifyingOps.remove(pNameLower);
    }

    /**
     * Method which adds a defender to the short. Should only be called after validation.
     */
    public void addDefender(SPlayer p, SArmy a, String modName) {
        Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);
        defenders.put(p.getName().toLowerCase(), a.getID());
        reporter.addDefender(p.getName(), a.getID());
        if (!modName.equals("")) {
            playerModifyingOps.put(p.getName().toLowerCase(), modName);
        }

        // also, lock the participating army and update the client GUI
        p.lockArmy(a.getID());

        // increase the unit and BV counts
        startingBV += a.getOperationsBV(null);
        startingUnits += a.getAmountOfUnits();

        /*
         * Check for Team games if a team game then launch when the max players
         * are reached, unless the attacker launches the task sooner. If not a
         * team game then check for FFA
         */

        if (o.getBooleanValue("TeamOperation")) {
            int maxTeams = o.getIntValue("NumberOfTeams");
            int maxPlayersPerTeam = o.getIntValue("TeamSize");
            int maxPlayers = Math.max(2, Math.min(8, maxTeams)) * maxPlayersPerTeam;

            isTeamOp = true;
            // MWLogger.errLog("Max Teams: "+maxTeams+" Players Per
            // Team:
            // "+maxPlayersPerTeam+" Max Players: "+maxPlayers+" Current
            // Players: "+this.getAllPlayerNames().size());
            if (getAllPlayerNames().size() >= maxPlayers) {
                changeStatus(STATUS_INPROGRESS);
            } else if (o.getBooleanValue("TeamsMustBeSameFaction") && checkDefendersAndLaunch(maxTeams, maxPlayersPerTeam)) {
                changeStatus(STATUS_INPROGRESS);
            }
        } else if (!o.getBooleanValue("FreeForAllOperation")) {
            changeStatus(STATUS_INPROGRESS);
        }
    }

    /**
     * Method which removes an attacker, and all of his armies, from a ShortOperation.
     */
    public void removeDefender(SPlayer p) {

        // remove from maps
        String pNameLower = p.getName().toLowerCase();
        if (defenders.containsKey(pNameLower)) {
            int armyID = defenders.remove(pNameLower);

            // decrease starting values
            startingBV -= p.getArmy(armyID).getOperationsBV(null);
            startingUnits -= p.getArmy(armyID).getAmountOfUnits();
        }
        playerModifyingOps.remove(pNameLower);
    }

    /**
     * Method which returns the name of this operation. Used to pull an Operation (parameter bag) from the manager.
     */
    public String getName() {
        return opName;
    }

    /**
     * Method which returns the map of attacking SPlayers.
     */
    public TreeMap<String, Integer> getAttackers() {
        return attackers;
    }

    /**
     * Method which returns the map of defending SPlayers.
     */
    public TreeMap<String, Integer> getDefenders() {
        return defenders;
    }

    public TreeMap<String, OpsChickenThread> getChickenThreads() {
        return chickenThreads;
    }

    /**
     * Method which returns current op status.
     */
    public int getStatus() {
        return currentStatus;
    }

    public int getTotalBuildings() {
        return totalBuildings;
    }

    public int getMinBuildings() {
        return minBuildings;
    }

    /**
     * <b>Note:</b> Any variables added here that are sent to the player must be added to <b>sendReconnectInfoToPlayer</b>
     *
     * <br>
     * Method which changes the status of the Operation. This is analagous to the switchTo...() methods in the old Task class.
     *
     * <br>
     * Absolutely massive method :-(
     *
     * <br>
     * Should the contents here be moved to the OperationManager?
     *
     */
    public void changeStatus(int newStatus) {

    	Random r = CampaignMain.cm.getR();

        /*
         * Never change to waiting mode. First actual switch is to INPROGRESS.
         */

        /*
         * INPROGRESS change is analagous to Task's switchToRunning. It sends
         * server options to the players, generates autoarmies and generally
         * does everything imaginable.
         */
        if (newStatus == STATUS_INPROGRESS) {

            // get the op we are setting up.
            Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);

            // Should always have at least 1 attacker if not well then this is
            // More fubared then anything I can come up with.
            if (defenders.size() < 1) {
                if (!o.getBooleanValue("AttackerAllowAgainstUnclaimedLand")) {
                    for (String currP : attackers.keySet()) {
                        CampaignMain.cm.toUser("No defenders are listed for this op, someone screwed up!", currP);
                    }
                }
                return;
            }

            //Should the battle be autoresolved?
            if (o.getBooleanValue("AutoresolveBattle")){
            	currentStatus = STATUS_INPROGRESS;
            	switchPlayerStatusToFighting();
            	BattleResolver.getInstance().resolve(this);
            	return;
            }

            isBuildingOperation = o.getIntValue("TotalBuildings") > 0;
            /*
             * Check to see if this is a building operation. If so, build an
             * info string that can be sent to all players and reconnectors.
             */
            if (isBuildingOperation) {

                totalBuildings = o.getIntValue("TotalBuildings");
                minBuildings = o.getIntValue("MinBuildingsForOp");
                buildingOptions = "RBP|" + totalBuildings;
                buildingOptions += "|" + minBuildings;
                buildingOptions += "|" + o.getValue("MinFloors");
                buildingOptions += "|" + o.getValue("MaxFloors");
                buildingOptions += "|" + o.getValue("MinCF");
                buildingOptions += "|" + o.getValue("MaxCF");
                buildingOptions += "|" + o.getValue("BuildingType");

                if (o.getBooleanValue("BuildingsStartOnMapEdge")) {
                    int pos = CampaignMain.cm.getRandomNumber(mapEdge.length);
                    defenderEdge = mapEdge[pos];
                    attackerEdge = mapEdgeReverse[pos];
                    buildingOptions += "|" + defenderEdge;
                } else {
                    buildingOptions += "|" + Buildings.EDGE_UNKNOWN;
                }
            }

            /*
             * Building ops always assign edged. If this is not a building op,
             * check to see if edges should be assigned to players. If not, we
             * can assume that the server lets people pick their own deployment
             * areas. Probably by rolling.
             */
            else if (o.getBooleanValue("RandomDeployment")) {
                int pos = getRandomDeployment(o);
                attackerEdge = playerEdge[pos];
                defenderEdge = playerEdgeReverse[pos];
                if (pos >= Buildings.NORTHWESTDEEP) {
                    gameOptions.append("|deep_deployment|true");
                }
            }

            if (o.getBooleanValue("TeamOperation")) {
                for (String currN : getAllPlayerNames()) {
                    SPlayer cPlayer = CampaignMain.cm.getPlayer(currN);
                    String edge = "GMEP|" + teamEdge[cPlayer.getTeamNumber() - 1];
                    CampaignMain.cm.toUser(edge, currN, false);
                }
            }
            // FFA's everyone gets a random edge.
            // Make sure every position is filled before we go through and give
            // duplicate positions.
            // This way if you have 9 or less players each gets it own starting
            // position
            // More then 9 and you start doubling up.
            else if (o.getBooleanValue("FreeForAllOperation") && (getAllPlayerNames().size() > 2)) { // 2
                // player FFA start on opposite ends
                ArrayList<Integer> edges = new ArrayList<Integer>(10);
                int countDown = 9;
                boolean found = false;
                int pos = 0;

                for (pos = 0; pos < 10; pos++) {
                    edges.add(0);
                }
                pos = 0;

                for (String currN : getAllPlayerNames()) {

                    while (!found) {
                        pos = CampaignMain.cm.getRandomNumber(9) + 1;
                        if (edges.get(pos) != 1) {
                            found = true;
                            edges.add(pos, 1);
                        }
                    }
                    found = false;
                    countDown--;

                    // Position 9 is edge so we are bumping it up one to 10 for
                    // CTR(center)
                    if (pos == 9) {
                        pos++;
                    }

                    String edge = "GMEP|" + pos;
                    CampaignMain.cm.toUser(edge, currN, false);
                    if (countDown <= 0) {
                        edges.clear();
                        countDown = 9;
                        for (pos = 0; pos < 10; pos++) {
                            edges.add(0);
                        }
                        pos = 0;
                    }
                }

            }

            // not a free for all. use standard edges.
            else {
                if (defenderEdge != -1) {
                    for (String currN : defenders.keySet()) {
                        String edge = "GMEP|" + defenderEdge;
                        CampaignMain.cm.toUser(edge, currN, false);
                    }
                }

                if (attackerEdge != -1) {
                    for (String currN : attackers.keySet()) {
                        String edge = "GMEP|" + attackerEdge;
                        CampaignMain.cm.toUser(edge, currN, false);
                    }
                }
            }

            /*
             * Check to see if autoartillery should be assigned to either group
             * of players. If so, check the group modifiers and boundaries, and
             * then assign the tubes.
             */
            boolean attackerArty = o.getBooleanValue("AttackerReceivesAutoArtillery");
            boolean defenderArty = o.getBooleanValue("DefenderReceivesAutoArtillery");

            // if we're going to be adding arty to someone, total the BVs in use
            int totalBV = 0;
            if (attackerArty || defenderArty) {

                for (String currN : defenders.keySet()) {
                    SPlayer currPlayer = CampaignMain.cm.getPlayer(currN);
                    totalBV += currPlayer.getArmy(defenders.get(currN)).getBV();
                }
                for (String currN : attackers.keySet()) {
                    SPlayer currPlayer = CampaignMain.cm.getPlayer(currN);
                    totalBV += currPlayer.getArmy(attackers.get(currN)).getBV();
                }

            }

            // check the attacker adjustments and boundarier
            int attackArtBV = 0;
            if (attackerArty) {
                attackArtBV = totalBV;
                // load adjustments
                int flatMod = o.getIntValue("AttackerFlatArtilleryModifier");
                double percMod = o.getDoubleValue("AttackerPercentArtilleryModifier");

                // load the boundaries
                int attackArtyMin = o.getIntValue("MinAttackerArtilleryBV");
                int attackArtyMax = o.getIntValue("MaxAttackerArtilleryBV");

                // adjust. percent first (rounding error), then the flat mod.
                if (percMod > 0) {
                    attackArtBV = (int) (totalBV * percMod);
                }
                attackArtBV = attackArtBV + flatMod;

                // make sure we're within the configured bounds
                if (attackArtBV < attackArtyMin) {
                    attackArtBV = attackArtyMin;
                } else if (attackArtBV > attackArtyMax) {
                    attackArtBV = attackArtyMax;
                }
            }

            // check the defender adjustments and boundarier
            int defendArtBV = 0;
            if (defenderArty) {
                defendArtBV = totalBV;
                // load adjustments
                int flatMod = o.getIntValue("DefenderFlatArtilleryModifier");
                double percMod = o.getDoubleValue("DefenderPercentArtilleryModifier");

                // load the boundaries
                int defendArtyMin = o.getIntValue("MinDefenderArtilleryBV");
                int defendArtyMax = o.getIntValue("MaxDefenderArtilleryBV");

                // adjust. percent first (rounding error), then the flat mod.
                if (percMod > 0) {
                    defendArtBV = (int) (defendArtBV * percMod);
                }
                defendArtBV = defendArtBV + flatMod;

                // make sure we're within the configured bounds
                if (defendArtBV < defendArtyMin) {
                    defendArtBV = defendArtyMin;
                } else if (defendArtBV > defendArtyMax) {
                    defendArtBV = defendArtyMax;
                }
            }

            // send attacker autoarmy, and save string for future use
            if (attackArtBV > 0) {

                // all attackers will have same autoarmy
                AutoArmy currAutoArmy = new AutoArmy(attackArtBV, false);
                attackerAutoString = "PL|AAA|" + currAutoArmy.toString("|");
                attackArtDesc = "[Bonus Arty: " + currAutoArmy.getUnits().size() + " pieces, " + currAutoArmy.getBV() + "BV]<br>";

                // so send it to each attacker ...
                for (String currP : attackers.keySet()) {
                    CampaignMain.cm.toUser(attackerAutoString, currP, false);
                }
            } else {
                attackerAutoString = "PL|AAA|CLEAR";
                // so send it to each attacker ...
                for (String currP : attackers.keySet()) {
                    CampaignMain.cm.toUser(attackerAutoString, currP, false);
                }
            }

            // send defender autoarmy, and save for future use
            if (defendArtBV > 0) {

                // all defenders will have same autoarmy
                AutoArmy currAutoArmy = new AutoArmy(defendArtBV, false);
                defenderAutoString = "PL|AAA|" + currAutoArmy.toString("|");
                defendArtDesc = "[Bonus Arty: " + currAutoArmy.getUnits().size() + " pieces, " + currAutoArmy.getBV() + "BV]<br>";

                // so send it to each defender ...
                for (String currP : defenders.keySet()) {
                    CampaignMain.cm.toUser(defenderAutoString, currP, false);
                }
            } else {
                defenderAutoString = "PL|AAA|CLEAR";

                // so send it to each defender ...
                for (String currP : defenders.keySet()) {
                    CampaignMain.cm.toUser(defenderAutoString, currP, false);
                }
            }

            /*
             * Check to see if anyone gets Gun Emplacements
             */
            boolean attackerGuns = o.getBooleanValue("AttackerReceivesGunEmplacement");
            boolean defenderGuns = o.getBooleanValue("DefenderReceivesGunEmplacement");

            // if we're going to be adding arty to someone, total the BVs in use
            totalBV = 0;
            if (attackerGuns || defenderGuns) {

                for (String currN : defenders.keySet()) {
                    SPlayer currPlayer = CampaignMain.cm.getPlayer(currN);
                    totalBV += currPlayer.getArmy(defenders.get(currN)).getBV();
                }
                for (String currN : attackers.keySet()) {
                    SPlayer currPlayer = CampaignMain.cm.getPlayer(currN);
                    totalBV += currPlayer.getArmy(attackers.get(currN)).getBV();
                }

            }

            // check the attacker adjustments and boundarier
            int attackGunBV = 0;
            if (attackerGuns) {
                attackGunBV = totalBV;
                // load adjustments
                int flatMod = o.getIntValue("AttackerFlatGunEmplacementModifier");
                double percMod = o.getDoubleValue("AttackerPercentGunEmplacementModifier");

                // load the boundaries
                int attackGunsMin = o.getIntValue("MinAttackerGunEmplacementBV");
                int attackGunsMax = o.getIntValue("MaxAttackerGunEmplacementBV");

                // adjust. percent first (rounding error), then the flat mod.
                if (percMod > 0) {
                    attackGunBV = (int) (attackGunBV * percMod);
                }
                attackGunBV = attackGunBV + flatMod;

                // make sure we're within the configured bounds
                if (attackGunBV < attackGunsMin) {
                    attackGunBV = attackGunsMin;
                } else if (attackGunBV > attackGunsMax) {
                    attackGunBV = attackGunsMax;
                }
            }

            // check the defender adjustments and boundarier
            int defendGunBV = 0;
            if (defenderGuns) {
                defendGunBV = totalBV;
                // load adjustments
                int flatMod = o.getIntValue("DefenderFlatGunEmplacementModifier");
                double percMod = o.getDoubleValue("DefenderPercentGunEmplacementModifier");

                // load the boundaries
                int defendGunsMin = o.getIntValue("MinDefenderGunEmplacementBV");
                int defendGunsMax = o.getIntValue("MaxDefenderGunEmplacementBV");

                // adjust. percent first (rounding error), then the flat mod.
                if (percMod > 0) {
                    defendGunBV = (int) (defendGunBV * percMod);
                }
                defendGunBV = defendGunBV + flatMod;

                // make sure we're within the configured bounds
                if (defendGunBV < defendGunsMin) {
                    defendGunBV = defendGunsMin;
                } else if (defendGunBV > defendGunsMax) {
                    defendGunBV = defendGunsMax;
                }
            }

            // send attacker autoarmy, and save string for future use
            if (attackGunBV > 0) {

                // all attackers will have same autoarmy
                AutoArmy currAutoArmy = new AutoArmy(attackGunBV, true);
                attackerAutoEmplacementsString = "PL|GEA|" + currAutoArmy.toString("|");
                attackArtDesc += "[Bonus Gun Emplacements: " + currAutoArmy.getUnits().size() + " pieces, " + currAutoArmy.getBV() + "BV]<br>";

                // so send it to each attacker ...
                for (String currP : attackers.keySet()) {
                    CampaignMain.cm.toUser(attackerAutoEmplacementsString, currP, false);
                }
            } else {
                attackerAutoEmplacementsString = "PL|GEA|CLEAR";
                for (String currP : attackers.keySet()) {
                    CampaignMain.cm.toUser(attackerAutoEmplacementsString, currP, false);
                }
            }

            // send defender autoarmy, and save for future use
            if (defendGunBV > 0) {

                // all defenders will have same autoarmy
                AutoArmy currAutoArmy = new AutoArmy(defendGunBV, true);
                defenderAutoEmplacementsString = "PL|GEA|" + currAutoArmy.toString("|");
                defendArtDesc += "[Bonus Gun Emplacements: " + currAutoArmy.getUnits().size() + " pieces, " + currAutoArmy.getBV() + "BV]<br>";

                // so send it to each defender ...
                for (String currP : defenders.keySet()) {
                    CampaignMain.cm.toUser(defenderAutoEmplacementsString, currP, false);
                }
            } else {
                defenderAutoEmplacementsString = "PL|GEA|CLEAR";
                // so send it to each defender ...
                for (String currP : defenders.keySet()) {
                    CampaignMain.cm.toUser(defenderAutoEmplacementsString, currP, false);
                }
            }

            /*
             * Check to see if mines should be assigned to either group of
             * players. If so, check the group modifiers to find out how many.
             */

            boolean attackerMines = o.getBooleanValue("AttackerReceivesMines");
            boolean defenderMines = o.getBooleanValue("DefenderReceivesMines");

            // if we're going to be adding mines to someone, total the BVs in
            // use and tonnage
            totalBV = 0;
            int totalTonnage = 0;
            if (attackerMines || defenderMines) {

                for (String currN : defenders.keySet()) {
                    SPlayer currPlayer = CampaignMain.cm.getPlayer(currN);
                    totalBV += currPlayer.getArmy(defenders.get(currN)).getBV();
                    totalTonnage += currPlayer.getArmy(defenders.get(currN)).getTotalTonnage();
                }
                for (String currN : attackers.keySet()) {
                    SPlayer currPlayer = CampaignMain.cm.getPlayer(currN);
                    totalBV += currPlayer.getArmy(attackers.get(currN)).getBV();
                    totalTonnage += currPlayer.getArmy(attackers.get(currN)).getTotalTonnage();
                }

            }

            // check the attacker mines
            int attackerVibraMines = 0;
            int attackerConventionalMines = 0;
            if (attackerMines) {

                // Conventional
                int ConventionalBVMod = o.getIntValue("AttackerBVPerConventional");
                int ConventionalTonMod = o.getIntValue("AttackerTonPerConventional");

                // Vibra
                int VibraBVMod = o.getIntValue("AttackerBVPerVibra");
                int VibraTonMod = o.getIntValue("AttackerTonPerVibra");

                if (ConventionalBVMod > 0) {
                    attackerConventionalMines += totalBV / ConventionalBVMod;
                }
                if (ConventionalTonMod > 0) {
                    attackerConventionalMines += totalTonnage / ConventionalTonMod;
                }

                if (VibraBVMod > 0) {
                    attackerVibraMines += totalBV / VibraBVMod;
                }
                if (VibraTonMod > 0) {
                    attackerVibraMines += totalTonnage / VibraTonMod;
                }

            }

            // check the defender mines
            int defenderVibraMines = 0;
            int defenderConventionalMines = 0;
            if (defenderMines) {

                // Conventional
                int ConventionalBVMod = o.getIntValue("DefenderBVPerConventional");
                // DefenderTonPerConventional
                int ConventionalTonMod = o.getIntValue("DefenderTonPerConventional");

                // Vibra
                int VibraBVMod = o.getIntValue("DefenderBVPerVibra");
                int VibraTonMod = o.getIntValue("DefenderTonPerVibra");

                if (ConventionalBVMod > 0) {
                    defenderConventionalMines += totalBV / ConventionalBVMod;
                }
                if (ConventionalTonMod > 0) {
                    defenderConventionalMines += totalTonnage / ConventionalTonMod;
                }

                if (VibraBVMod > 0) {
                    defenderVibraMines += totalBV / VibraBVMod;
                }
                if (VibraTonMod > 0) {
                    defenderVibraMines += totalTonnage / VibraTonMod;
                }

            }

            // send attacker Mines
            if ((attackerConventionalMines > 0) || (attackerVibraMines > 0)) {

                // all attackers will have same number of mines
                attackerAutoMinesString = "PL|AAM|" + attackerConventionalMines + "|" + attackerVibraMines;
                attackArtDesc += "[Bonus Mines: Conventional: " + attackerConventionalMines + " Vibra: " + attackerVibraMines + "]<br>";

                // so send it to each attacker ...
                for (String currP : attackers.keySet()) {
                    CampaignMain.cm.toUser(attackerAutoMinesString, currP, false);
                }
            } else {
                attackerAutoMinesString = "PL|AAM|0|0";
                // so send it to each attacker ...
                for (String currP : attackers.keySet()) {
                    CampaignMain.cm.toUser(attackerAutoMinesString, currP, false);
                }
            }

            // send defender autoarmy, and save for future use
            if ((defenderConventionalMines > 0) || (defenderVibraMines > 0)) {

                // all defenders will have same autoarmy
                defenderAutoMinesString = "PL|AAM|" + defenderConventionalMines + "|" + defenderVibraMines;
                defendArtDesc += "[Bonus Mines: Conventional: " + defenderConventionalMines + " Vibra: " + defenderVibraMines + "]<br>";

                // so send it to each defender ...
                for (String currP : defenders.keySet()) {
                    CampaignMain.cm.toUser(defenderAutoMinesString, currP, false);
                }
            } else {
                defenderAutoMinesString = "PL|AAM|0|0";
                // so send it to each defender ...
                for (String currP : defenders.keySet()) {
                    CampaignMain.cm.toUser(defenderAutoMinesString, currP, false);
                }
            }

            bots = "PL|UB|" + o.getBooleanValue("BotControlsAll");
            botTeams = "PL|BOST|" + o.getBooleanValue("BotsAllOnSameTeam");

            for (String currN : defenders.keySet()) {
                CampaignMain.cm.toUser(bots, currN, false);
                CampaignMain.cm.toUser(botTeams, currN, false);
            }

            for (String currN : attackers.keySet()) {
                CampaignMain.cm.toUser(bots, currN, false);
                CampaignMain.cm.toUser(botTeams, currN, false);
            }

            /*
             * Check for Mul Armies
             */

            boolean attackerHasMUL = o.getBooleanValue("AttackerReceivesMULArmy");
            boolean defenderHasMUL = o.getBooleanValue("DefenderReceivesMULArmy");

            if (attackerHasMUL || defenderHasMUL) {
                for (String currPlayer : attackers.keySet()) {
                    String attackerMULs = "";
                    if (attackerHasMUL) {

                        attackerMULs = "PL|SMA|";

                        boolean stolenUnits = o.getBooleanValue("AttackerUnitsTakenBeforeFightStarts");
                        attackerMULs += generateMULList(o.getIntValue("MinAttackerMulArmies"), o.getIntValue("MaxAttackerMulArmies"), o.getValue("AttackerMulArmyList"), stolenUnits, o);
                        attackerMULs += generateMULList(o.getIntValue("MinAttackerMulMeks"), o.getIntValue("MaxAttackerMulMeks"), o.getValue("AttackerMulMekList"), stolenUnits, o);
                        attackerMULs += generateMULList(o.getIntValue("MinAttackerMulVehicles"), o.getIntValue("MaxAttackerMulVehicles"), o.getValue("AttackerMulVehicleList"), stolenUnits, o);
                        attackerMULs += generateMULList(o.getIntValue("MinAttackerMulInf"), o.getIntValue("MaxAttackerMulInf"), o.getValue("AttackerMulInfList"), stolenUnits, o);
                        attackerMULs += generateMULList(o.getIntValue("MinAttackerMulBA"), o.getIntValue("MaxAttackerMulBA"), o.getValue("AttackerMulBAList"), stolenUnits, o);
                        attackerMULs += generateMULList(o.getIntValue("MinAttackerMulAero"), o.getIntValue("MaxAttackerMulAero"), o.getValue("AttackerMulAeroList"), stolenUnits, o);
                        attackerMULs += generateMULList(o.getIntValue("MinAttackerMulProto"), o.getIntValue("MaxAttackerMulProto"), o.getValue("AttackerMulProtoList"), stolenUnits, o);

                    } else {
                        if (o.getBooleanValue("AttackerUnitsTakenBeforeFightStarts")) {
                            attackerMULs += generateMULList(-1, -1, "", true, o);
                        } else {
                            attackerMULs = "PL|SMA|CLEAR";
                        }
                    }
                    CampaignMain.cm.toUser(attackerMULs, currPlayer, false);
                    MULHash.put(currPlayer, attackerMULs);
                }

                for (String currPlayer : defenders.keySet()) {
                    String defenderMULs = "";
                    if (defenderHasMUL) {

                        defenderMULs = "PL|SMA|";

                        defenderMULs += generateMULList(o.getIntValue("MinDefenderMulArmies"), o.getIntValue("MaxDefenderMulArmies"), o.getValue("DefenderMulArmyList"), false, o);
                        defenderMULs += generateMULList(o.getIntValue("MinDefenderMulMeks"), o.getIntValue("MaxDefenderMulMeks"), o.getValue("DefenderMulMekList"), false, o);
                        defenderMULs += generateMULList(o.getIntValue("MinDefenderMulVehicles"), o.getIntValue("MaxDefenderMulVehicles"), o.getValue("DefenderMulVehicleList"), false, o);
                        defenderMULs += generateMULList(o.getIntValue("MinDefenderMulInf"), o.getIntValue("MaxDefenderMulInf"), o.getValue("DefenderMulInfList"), false, o);
                        defenderMULs += generateMULList(o.getIntValue("MinDefenderMulBA"), o.getIntValue("MaxDefenderMulBA"), o.getValue("DefenderMulBAList"), false, o);
                        defenderMULs += generateMULList(o.getIntValue("MinDefenderMulAero"), o.getIntValue("MaxDefenderMulAero"), o.getValue("DefenderMulAeroList"), false, o);
                        defenderMULs += generateMULList(o.getIntValue("MinDefenderMulProto"), o.getIntValue("MaxDefenderMulProto"), o.getValue("DefenderMulProtoList"), false, o);
                    } else {
                        defenderMULs = "PL|SMA|CLEAR";
                    }
                    CampaignMain.cm.toUser(defenderMULs, currPlayer, false);
                    MULHash.put(currPlayer, defenderMULs);

                }
            } else {
                for (String currPlayer : attackers.keySet()) {
                    String attackerMULs = "";
                    if (o.getBooleanValue("AttackerUnitsTakenBeforeFightStarts")) {

                        attackerMULs = "PL|SMA|";
                        attackerMULs += generateMULList(-1, -1, "", true, o);
                        CampaignMain.cm.toUser(attackerMULs, currPlayer, false);
                        MULHash.put(currPlayer, attackerMULs);

                    } else {
                        attackerMULs = "PL|SMA|CLEAR";
                    }
                }
                String defenderMULs = "PL|SMA|CLEAR";
                for (String currPlayer : defenders.keySet()) {
                    CampaignMain.cm.toUser(defenderMULs, currPlayer, false);
                    MULHash.put(currPlayer, defenderMULs);
                }
            }

            /*
             * Apply certain things to all players ... 1) Set busy status 2)
             * Cancel any outstanding chicken threads 3) Add the game info to
             * client-side logs
             */
            currentStatus = STATUS_INPROGRESS;
            switchPlayerStatusToFighting();
            // Build player list and submit to NewShortResolver
            submitPlayerList();




            /*
             * Send GameOptions to the player.
             *
             * Support for player configurable options was present at this point
             * in the old Task system, but those settings had been unmaintained
             * for awhile and are/were barely functional. Leaving player
             * config'ed options out for now, but when they get rewritten, this
             * is where they should be matched and sent to the players.
             */

            // look for blind drop first. MM defaults this to false, so only
            // look for true.
            if (CampaignMain.cm.getBooleanConfig("UseBlindDrops")) {
                gameOptions.append("|real_blind_drop|true");
            } else if (o.getBooleanValue("RealBlindDrop")) {
                gameOptions.append("|double_blind|true");
                gameOptions.append("|real_blind_drop|true");
                doubleBlind = true;
            } else {
                gameOptions.append("|real_blind_drop|false");
            }

            // if Op isn't set to double blind check to see if the game option
            // is.
            if (!doubleBlind) {
                doubleBlind = CampaignMain.cm.getMegaMekClient().getGame().getOptions().booleanOption("double_blind");
            }

            // autoset offboard arty to homeedge. always.
            gameOptions.append("|set_arty_player_homeedge|");
            gameOptions.append(true);

            boolean useWeather = !CampaignMain.cm.getBooleanConfig("DisableWeather");

            // set the temp gravity and vacuum from the terrain configs
            if (useWeather) {
            	aTerrain = playContinent.getAdvancedTerrain().clone();
            } else {
            	aTerrain = new AdvancedTerrain();
            }

            if (useWeather) {
            // determine temp. Add a random number from 0-(Diff b/w Max
            // and Min Temp) to the low temperature
            	int highTemp = aTerrain.getHighTemp();
            	int lowTemp = aTerrain.getLowTemp();
            	int tempdiff = highTemp - lowTemp;
            	int tempToSet = lowTemp;

            	// only get random if there's an actual temp diff
            	if (tempdiff > 0) {
            		tempToSet = CampaignMain.cm.getRandomNumber(tempdiff) + lowTemp;
            	}

            	if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getDuskChance()) {
            		tempToSet -= Math.abs(aTerrain.getNightTempMod()) / 2;
            		intelTimeFrame = PlanetaryConditions.L_DUSK;
            		aTerrain.setLightConditions(PlanetaryConditions.L_DUSK);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getNightChance()) {
            		tempToSet -= Math.abs(aTerrain.getNightTempMod());
            		intelTimeFrame = PlanetaryConditions.L_FULL_MOON;
            		aTerrain.setLightConditions(PlanetaryConditions.L_FULL_MOON);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getMoonLessNightChance()) {
            		tempToSet -= Math.abs(aTerrain.getNightTempMod());
            		intelTimeFrame = PlanetaryConditions.L_MOONLESS;
            		aTerrain.setLightConditions(PlanetaryConditions.L_MOONLESS);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getPitchBlackNightChance()) {
            		tempToSet -= Math.abs(aTerrain.getNightTempMod());
            		intelTimeFrame = PlanetaryConditions.L_PITCH_BLACK;
            		aTerrain.setLightConditions(PlanetaryConditions.L_PITCH_BLACK);
            	}
            	// else normal daylight conditions
            	else {
            		intelTimeFrame = PlanetaryConditions.L_DAY;
            	}

            	if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getLightRainfallChance()) {
            		aTerrain.setWeatherConditions(PlanetaryConditions.WE_LIGHT_RAIN);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getModerateRainFallChance()) {
            		aTerrain.setWeatherConditions(PlanetaryConditions.WE_MOD_RAIN);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getHeavyRainfallChance()) {
            		aTerrain.setWeatherConditions(PlanetaryConditions.WE_HEAVY_RAIN);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getDownPourChance()) {
            		aTerrain.setWeatherConditions(PlanetaryConditions.WE_DOWNPOUR);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getLightSnowfallChance()) {
            		aTerrain.setWeatherConditions(PlanetaryConditions.WE_LIGHT_SNOW);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getModerateSnowFallChance()) {
            		aTerrain.setWeatherConditions(PlanetaryConditions.WE_MOD_SNOW);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getHeavySnowfallChance()) {
            		aTerrain.setWeatherConditions(PlanetaryConditions.WE_HEAVY_SNOW);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getSleetChance()) {
            		aTerrain.setWeatherConditions(PlanetaryConditions.WE_SLEET);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getIceStormChance()) {
            		aTerrain.setWeatherConditions(PlanetaryConditions.WE_ICE_STORM);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getLightHailChance()) {
            		aTerrain.setWeatherConditions(PlanetaryConditions.WE_LIGHT_HAIL);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getHeavyHailChance()) {
            		aTerrain.setWeatherConditions(PlanetaryConditions.WE_HEAVY_HAIL);
            	}

            	if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getLightWindsChance()) {
            		aTerrain.setWindStrength(PlanetaryConditions.WI_LIGHT_GALE);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getModerateWindsChance()) {
            		aTerrain.setWindStrength(PlanetaryConditions.WI_MOD_GALE);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getStrongWindsChance()) {
            		aTerrain.setWindStrength(PlanetaryConditions.WI_STRONG_GALE);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getStormWindsChance()) {
            		aTerrain.setWindStrength(PlanetaryConditions.WI_STORM);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getTornadoF13WindsChance()) {
            		aTerrain.setWindStrength(PlanetaryConditions.WI_TORNADO_F13);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getTornadoF4WindsChance()) {
            		aTerrain.setWindStrength(PlanetaryConditions.WI_TORNADO_F4);
            	}

            	boolean wind = false;

            	if (aTerrain.getLightWindsChance() > 0) {
            		wind = true;
            		aTerrain.setMaxWindStrength(PlanetaryConditions.WI_LIGHT_GALE);
            	}

            	if (aTerrain.getModerateWindsChance() > 0) {
            		wind = true;
            		aTerrain.setMaxWindStrength(PlanetaryConditions.WI_MOD_GALE);
            	}
            	if (aTerrain.getStrongWindsChance() > 0) {
            		wind = true;
            		aTerrain.setMaxWindStrength(PlanetaryConditions.WI_STRONG_GALE);
            	}
            	if (aTerrain.getStormWindsChance() > 0) {
            		wind = true;
            		aTerrain.setMaxWindStrength(PlanetaryConditions.WI_STORM);
            	}
            	if (aTerrain.getTornadoF13WindsChance() > 0) {
            		wind = true;
            		aTerrain.setMaxWindStrength(PlanetaryConditions.WI_TORNADO_F13);
            	}
            	if (aTerrain.getTornadoF4WindsChance() > 0) {
            		wind = true;
            		aTerrain.setMaxWindStrength(PlanetaryConditions.WI_TORNADO_F4);
            	}

            	if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getLightFogChance()) {
            		aTerrain.setFog(PlanetaryConditions.FOG_LIGHT);
            	} else if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getHeavyFogChance()) {
            		aTerrain.setFog(PlanetaryConditions.FOG_HEAVY);
            	}

            	if ((CampaignMain.cm.getRandomNumber(1000) + 1) <= aTerrain.getEMIChance()) {
            		aTerrain.setEMI(true);
            	}

            	if (aTerrain.getAtmosphere() <= PlanetaryConditions.ATMO_TRACE) {
            		gameOptions.append("|fire|false");
            		aTerrain.setShiftingWindDirection(false);
            		aTerrain.setShiftingWindStrength(false);
            	} else if (CampaignMain.cm.getMegaMekClient().getGame().getOptions().booleanOption("tacops_start_fire") && !wind) {
            		aTerrain.setShiftingWindDirection(true);
            		aTerrain.setShiftingWindStrength(true);
            		aTerrain.setMaxWindStrength(PlanetaryConditions.WI_LIGHT_GALE);
            	}



            	aTerrain.setTemperature(tempToSet);

            }

            PlanetaryConditions pc = new PlanetaryConditions();

            pc.setAtmosphere(aTerrain.getAtmosphere());
            pc.setFog(aTerrain.getFog());
            pc.setLight(aTerrain.getLightConditions());
            pc.setWindStrength(aTerrain.getWindStrength());
            pc.setWeather(aTerrain.getWeatherConditions());

            intelWeather = aTerrain.getWeatherConditions();
            intelWind = aTerrain.getWindStrength();
        	intelTemp = aTerrain.getTemperature();
        	intelGravity = aTerrain.getGravity();
        	intelVacuum = aTerrain.getAtmosphere() <= PlanetaryConditions.ATMO_TRACE;
            intelVisibility = pc.getVisualRange(null, false);



            // if this is a DB game then exclusive_db_deployment needs to be
            // turned off as players
            // maybe getting random edges assigned to them
            if (o.getBooleanValue("FreeForAllOperation")) {
                gameOptions.append("|exclusive_db_deployment|false");
            }

            // Check if this operation is using victory conditions. If so then
            // Send the pertinate data.
            if (((o.getIntValue("NumberOfVictoryConditions") > 0) && (o.getBooleanValue("UseDestroyEnemyBV"))) || o.getBooleanValue("UseBVRatioPercent") || o.getBooleanValue("UseUnitCommander") || o.getBooleanValue("UseGameTurnLimit") || o.getBooleanValue("UseKillCount")) {
                gameOptions.append("|check_victory|true");
                gameOptions.append("|achieve_conditions|");
                gameOptions.append(o.getValue("NumberOfVictoryConditions"));
                gameOptions.append("|use_bv_destroyed|");
                gameOptions.append(o.getValue("UseDestroyEnemyBV"));
                gameOptions.append("|bv_destroyed_percent|");
                gameOptions.append(o.getValue("DestroyEnemyBV"));
                gameOptions.append("|use_bv_ratio|");
                gameOptions.append(o.getValue("UseBVRatioPercent"));
                gameOptions.append("|bv_ratio_percent|");
                gameOptions.append(o.getValue("BVRatioPercent"));
                gameOptions.append("|commander_killed|");
                gameOptions.append(o.getValue("UseUnitCommander"));
                gameOptions.append("|use_game_turn_limit|");
                gameOptions.append(o.getValue("UseGameTurnLimit"));
                gameOptions.append("|game_turn_limit|");
                gameOptions.append(o.getValue("GameTurnLimit"));
                gameOptions.append("|use_kill_count|");
                gameOptions.append(o.getValue("UseKillCount"));
                gameOptions.append("|game_kill_count|");
                gameOptions.append(o.getValue("KillCount"));
            } else {
                gameOptions.append("|check_victory|true|use_bv_destroyed|false|use_bv_ratio|false|use_game_turn_limit|false|use_kill_count|false");
            }

            // if your using bots then turn off exclusive db deployment.
            if (o.getBooleanValue("BotControlsAll") || o.getBooleanValue("BotsAllOnSameTeam")) {
                gameOptions.append("|exclusive_db_deployment|false");
            }

            gameOptions.append("|individual_initiative|");
            gameOptions.append(o.getValue("IndividualInit"));

            // If Server is not using Force Size mod then turn off the option In
            // MM
            // This will allow for closer BV's between MW and MM
            //gameOptions.append("|no_force_size_mod|");
            //gameOptions.append(!CampaignMain.cm.getBooleanConfig("UseOperationsRule"));
            gameOptions.append("|year|");
            gameOptions.append(CampaignMain.cm.getIntegerConfig("CampaignYear"));


            /*
             * Stop all repairs on units
             */

            if (CampaignMain.cm.isUsingAdvanceRepair()) {
                // defending units
                for (String currN : defenders.keySet()) {
                    SPlayer currP = CampaignMain.cm.getPlayer(currN);
                    SArmy currA = currP.getArmy(defenders.get(currN));
                    Enumeration<Unit> units = currA.getUnits().elements();
                    while (units.hasMoreElements()) {
                        SUnit u = (SUnit) units.nextElement();
                        if (UnitUtils.isRepairing(u.getEntity())) {
                            CampaignMain.cm.getRTT().stopAllRepairJobs(u.getId(), currP);
                        }
                    }
                }

                // attacking units
                for (String currN : attackers.keySet()) {
                    SPlayer currP = CampaignMain.cm.getPlayer(currN);
                    SArmy currA = currP.getArmy(attackers.get(currN));
                    Enumeration<Unit> units = currA.getUnits().elements();
                    while (units.hasMoreElements()) {
                        SUnit u = (SUnit) units.nextElement();
                        if (UnitUtils.isRepairing(u.getEntity())) {
                            CampaignMain.cm.getRTT().stopAllRepairJobs(u.getId(), currP);
                        }
                    }
                }

            }
            /*
             * Determine the map size.
             */
            int totalWeight = 0;

            // defending units
            for (String currN : defenders.keySet()) {
                SPlayer currP = CampaignMain.cm.getPlayer(currN);
                SArmy currA = currP.getArmy(defenders.get(currN));
                Enumeration<Unit> units = currA.getUnits().elements();
                defenderArmyCount += currA.getRawForceSize();

                while (units.hasMoreElements()) {
                    SUnit u = (SUnit) units.nextElement();
                    totalWeight += SUnit.getMapSizeModification(u);
                }
            }

            // attacking units
            for (String currN : attackers.keySet()) {
                SPlayer currP = CampaignMain.cm.getPlayer(currN);
                SArmy currA = currP.getArmy(attackers.get(currN));
                attackerArmyCount += currA.getRawForceSize();

                Enumeration<Unit> units = currA.getUnits().elements();
                while (units.hasMoreElements()) {
                    SUnit u = (SUnit) units.nextElement();
                    totalWeight += SUnit.getMapSizeModification(u);
                }
            }

            // do the actual math ...
            totalWeight = (int) Math.sqrt(totalWeight);

            int sizeX = Math.max(totalWeight + 3, o.getIntValue("MapSizeX"));
            int sizeY = Math.max(totalWeight - 2, o.getIntValue("MapSizeY"));
            mapsize = new Dimension(sizeX, sizeY);



            /*
             * Send the options to all of the players. The sendReconnectInfoTo()
             * method is used to retransmit this info to a specified player if
             * he leaves/reconnects.
             */

            // Check to see if we are using the Random City Generator for MM
            // If so then sent the playEnvironment
            if (o.getBooleanValue("RCGUseCityGenerator")) {
                cityBuilder.append("0$0$");// min max buildings old code.
                cityBuilder.append(o.getValue("RCGMinCF") + "$");
                cityBuilder.append(o.getValue("RCGMaxCF") + "$");
                cityBuilder.append(o.getValue("RCGMinFloors") + "$");
                cityBuilder.append(o.getValue("RCGMaxFloors") + "$");
                cityBuilder.append(o.getValue("RCGCityDensity") + "$");
                cityBuilder.append(o.getValue("RCGCityType") + "$");
                cityBuilder.append(o.getValue("RCGCityBlocks") + "$");
            }

            //Now that everything is setup, send the intel reports!
            SendIntelReports();

            for (String currN : getAllPlayerNames()) {

                // send options
                CampaignMain.cm.toUser(gameOptions.toString(), currN, false);

                // send terrain
                if (aTerrain != null) {
                    CampaignMain.cm.toUser("APE|" + aTerrain.toStringPlanetaryConditions(), currN, false);
                    CampaignMain.cm.toUser("PE|" + playEnvironment.toString(cityBuilder.toString()) + "|" + mapsize.width + "|" + mapsize.height + "|" + o.getValue("MapMedium"), currN, false);
                } else {
                    CampaignMain.cm.toUser("PE|" + playEnvironment.toString(cityBuilder.toString()) + "|" + mapsize.width + "|" + mapsize.height + "|" + o.getValue("MapMedium"), currN, false);
                }

                if (buildingOptions.length() > 1) {
                    CampaignMain.cm.toUser(buildingOptions, currN, false);
                }

                // set save flag. this is a holdover from task. why do we do it?
                CampaignMain.cm.getPlayer(currN).setSave();
            }

            // save the starting time
            startTime = System.currentTimeMillis();

            /*
             * Last step in IN_PROGRESS is to build a mod header
             */
            // Attackers
            modHeader += "[Attacker" + StringUtils.addAnS(attackers.size()) + ": ";
            for (String currName : attackers.keySet()) {
                SPlayer currP = CampaignMain.cm.getPlayer(currName);
                modHeader += currP.getColoredName() + ", ";
            }
            modHeader = modHeader.substring(0, modHeader.length() - 2);

            // Defenders
            modHeader += " / Defender" + StringUtils.addAnS(defenders.size()) + ":";
            for (String currName : defenders.keySet()) {
                SPlayer currP = CampaignMain.cm.getPlayer(currName);
                modHeader += currP.getColoredName() + ", ";
            }
            modHeader = modHeader.substring(0, modHeader.length() - 2);

            // Generic info
            modHeader += " / Type: " + getName();
            modHeader += " / Start BV: " + startingBV;

            reporter.setUpOperation(getName(), getTargetWorld().getName(), getEnvironment().getName(), getEnvironment().getTheme());

        }// end newStatus = IN_PROGRESS

        /*
         * Setting to Reporting status doesn't do anything directly; however, it
         * does stop additional autoreports from being processed.
         */
        else if (newStatus == STATUS_REPORTING) {
            currentStatus = newStatus;
        } else if (newStatus == STATUS_FINISHED) {

            currentStatus = newStatus;

            // we interrupt this broadcast to remove disconnection info...
            CampaignMain.cm.getOpsManager().clearAllDisconnectionTracks(this);

            // save the completion time
            completionTime = System.currentTimeMillis();

            // add duration, names and BV's to the results log
            StringBuilder toStore = new StringBuilder();
            toStore.append("#" + getShortID() + " [" + getName() + "]" + " [" + targetWorld.getName() + "] {" +  aTerrain.getHumanReadableWeather() + "} Duration: " + StringUtil.readableTime(completionTime - startTime) + " / Players: " );
            boolean firstPlayer = true;
            for (String currName : getAllPlayerNames()) {
                if (firstPlayer) {
                    toStore.append(currName);
                    firstPlayer = false;
                } else {
                    toStore.append(" + " + currName);
                }

            }
            toStore.append("/ Start BV: " + startingBV + " / Finish BV: " + finishingBV + " ");
            if (aTerrain != null) {
                toStore.append("/ Terrain: " + aTerrain.getDisplayName() + " ");
            } else {
                toStore.append("/ Terrain: " + playEnvironment.getName() + " ");
            }
            toStore.append("/ Theme: " + playEnvironment.getTheme());
            if (CampaignMain.cm.getBooleanConfig("UseOperationsRule")) {
                if (getWinners().containsKey(getAttackers().firstKey())) {
                    toStore.append(" / FSM (");
                    toStore.append(attackerArmyCount);
                    toStore.append(") ");
                    toStore.append(defenderArmyCount);
                } else {
                    toStore.append(" / FSM ");
                    toStore.append(attackerArmyCount);
                    toStore.append(" (");
                    toStore.append(defenderArmyCount);
                    toStore.append(")");
                }
            }
            MWLogger.resultsLog(toStore.toString());

            /*
             * send a Finished Game entry to faction mates. same as when
             * starting a game, sift through the factions to avoid spam'ing a
             * housechan with duplicate entries.
             */
            TreeMap<String, SHouse> houseMap = new TreeMap<String, SHouse>();
            for (String currN : getAllPlayerNames()) {
                SHouse currH = CampaignMain.cm.getPlayer(currN).getHouseFightingFor();
                if (houseMap.get(currH.getName()) == null) {
                    houseMap.put(currH.getName(), currH);
                }
            }

            // check to see if complete or incomplete info should be shown to
            // housemates
            String toSend = "";
            if (CampaignMain.cm.getBooleanConfig("ShowCompleteGameInfoOnTick")) {
                toSend = completeFinishedString.replaceAll("<br>", " ");
            } else {
                toSend = incompleteFinishedString.replaceAll("<br>", " ");
            }
            for (SHouse currH : houseMap.values()) {
                CampaignMain.cm.doSendHouseMail(currH, "Finished Game", toSend);
            }
        }

    }

	public void switchPlayerStatusToFighting() {
		Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);
		for (String currN : getAllPlayerNames()) {
		    SPlayer currP = CampaignMain.cm.getPlayer(currN);

		    currP.setFighting(true);
		    CampaignMain.cm.sendPlayerStatusUpdate(currP, true);// send
		    // fighting
		    // info to
		    // all
		    CampaignMain.cm.toUser("TL|" + getInfo(true, false), currN, false);

		    CampaignMain.cm.getOpsManager().removePlayerFromAllPossibleDefenderLists(currN, false);
		    CampaignMain.cm.getOpsManager().removePlayerFromAllDefenderLists(currP, this, true);
		    CampaignMain.cm.getOpsManager().removePlayerFromAllAttackerLists(currP, this, true);
		}

		// Do not let people get chickened anymore - stop all the threads.
		terminateChickenThreads();

		/*
         * Now that the Operation is actually running, it will return
         * nifty/useful info. We want to show that info to the houses of all
         * attacking/defending players, but don't want to spam housechannels
         * if there is more than one participant per house. Set up a quick
         * TreeMap in order to filtouer out duplicates, then send to all
         * involved houses.
         */
        TreeMap<String, SHouse> houseMap = new TreeMap<String, SHouse>();
        for (String currN : getAllPlayerNames()) {
            SHouse currH = CampaignMain.cm.getPlayer(currN).getHouseFightingFor();
            if (houseMap.get(currH.getName()) == null) {
                houseMap.put(currH.getName(), currH);
            }
        }
        for (SHouse currH : houseMap.values()) {
            CampaignMain.cm.doSendHouseMail(currH, "New Game:", getInfo(true, false));
        }

	}

    public void SendIntelReports() {
		Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);

    	/*
         * Send logos, intel reports and detailed game info to the invovled
         * players. Logos are those of the first player in the attacker and
         * defender maps, respectively.
         */
        String firstAttackName = attackers.firstKey();
        String firstDefendName = defenders.firstKey();
        SPlayer firstAttPlayer = CampaignMain.cm.getPlayer(firstAttackName);
        SPlayer firstDefPlayer = CampaignMain.cm.getPlayer(firstDefendName);

        // Players logo stores House logo as default if they don't have one.
        String attackLogo = "<img height=\"150\" width=\"150\" src =\"" + firstAttPlayer.getMyLogo() + "\">";
        String defendLogo = "<img height=\"150\" width=\"150\" src =\"" + firstDefPlayer.getMyLogo() + "\">";

        // save the logo string ...
        String logos = attackLogo + " vs " + defendLogo + "<br>";

     // average the attacker and defender rankings, then compare for an
        // intel report.
        double averageAttackELO = 0;
        double averageDefendELO = 0;

        for (String currN : defenders.keySet()) {
            SPlayer currP = CampaignMain.cm.getPlayer(currN);
            averageDefendELO += currP.getRating();
        }
        for (String currN : attackers.keySet()) {
            SPlayer currP = CampaignMain.cm.getPlayer(currN);
            averageAttackELO += currP.getRating();
        }

        averageAttackELO = averageAttackELO / (attackers.size());
        averageDefendELO = averageDefendELO / (defenders.size());
        double difference = Math.abs(averageAttackELO - averageDefendELO);

        String better = "";
        String worse = "";

        /*
         * TODO: Come up with better intelligence messages.
         */
        if (difference <= 50.0) {
            better = "<b>Intel Report:</b> This appears to be a balanced fight.";
            worse = "<b>Intel Report:</b> This appears to be a balanced fight.";
        } else if (difference <= 100.0) {
            better = "<b>Intel Report:</b> It appears that our forces have slightly more experience than the enemy.";
            worse = "<b>Intel Report:</b> It appears that our forces are slightly less experienced than the enemy.";
        } else if (difference <= 150.0) {
            better = "<b>Intel Report:</b> The opposing force is somewhat inexperienced. The advantage is ours.";
            worse = "<b>Intel Report:</b> The opposing force has seen a fair amount of combat, and will not be easily dispatched.";
        } else if (difference <= 210.0) {
            better = "<b>Intel Report:</b> Our forces are considerably more experienced than the enemy. Victory is likely, but not assured.";
            worse = "<b>Intel Report:</b> The enemy has sent a hardened, veteran force. This will be a difficult battle.";
        } else {
            better = "<b>Intel Report:</b> The opposing force is little more than ragtag militia - be merciful.";
            worse = "<b>Intel Report:</b> The enemy force is elite. Victory will be difficult to achieve, but would bring great honor.";
        }

        String attackIntel = "";
        String defendIntel = "";
        if (!CampaignMain.cm.getBooleanConfig("HideELO")) {
            if (averageAttackELO > averageDefendELO) {
                attackIntel = better;
                defendIntel = worse;
            } else {
                attackIntel = worse;
                defendIntel = better;
            }
        } else {
            attackIntel = "<b>Intel Report:</b> Nothing is known about your enemy. Good luck!";
            defendIntel = "<b>Intel Report:</b> Nothing is known about your enemy. Good luck!";
        }

        if (attackArtDesc.length() > 0) {
            attackIntel += "<br>" + attackArtDesc;
        }

        if (defendArtDesc.length() > 0) {
            defendIntel += "<br>" + defendArtDesc;
        }

        if( !o.getValue("AttackerBriefing").equals("")) {
            attackIntel += "<br><b>Briefing:</b> " + o.getValue("AttackerBriefing");
        }
        if( !o.getValue("DefenderBriefing").equals("")) {
            defendIntel += "<br><b>Briefing:</b> " + o.getValue("DefenderBriefing");
        }


        if (isBuildingOperation) {
            attackIntel += "<br><b>Mission Objectives:</b><br>You must destroy " + o.getValue("MinBuildingsForOp") + " out of " + o.getValue("TotalBuildings") + " facilities.";
            defendIntel += "<br><b>Mission Objectives:</b><br>You must defend all " + o.getValue("TotalBuildings") + " of your facilities.";
        }

        if (o.getBooleanValue("AttackerUnitsTakenBeforeFightStarts") && (preCapturedUnits.size() > 0)) {

            StringBuilder unitList = new StringBuilder();

            attackIntel += "<br><b>You've managed to steal the following ";
            if (preCapturedUnits.size() > 1) {
                unitList.append("units ");
            } else {
                unitList.append("unit ");
            }

            for (SUnit unit : preCapturedUnits) {
                unitList.append(unit.getModelName());
                unitList.append(", ");
            }

            unitList.replace(unitList.length() - 2, unitList.length(), ".");

            attackIntel += unitList.toString();
            attackIntel += "  Now you need to get away with them!";

            defendIntel += "<br><b>The attackers have managed to steal the following ";
            defendIntel += unitList.toString();
            defendIntel += "  You must stop them, at all costs, before they get away!";
        }


        //XXX

     // send the logos and intel to all players, then send .getInfo()
        for (String currN : attackers.keySet()) {
            if (CampaignMain.cm.getBooleanConfig("AllowPreliminaryOperationsReports")) {
                SPlayer currP = CampaignMain.cm.getPlayer(currN);
                CampaignMain.cm.toUser(logos + planetIntel(attackIntel, currP.getHouseFightingFor()), currP.getName(), true);
            } else {
                CampaignMain.cm.toUser(logos + attackIntel, currN, true);
            }

            CampaignMain.cm.toUser(getInfo(true, false), currN, true);
        }
        for (String currN : defenders.keySet()) {
            if (CampaignMain.cm.getBooleanConfig("AllowPreliminaryOperationsReports")) {
                SPlayer currP = CampaignMain.cm.getPlayer(currN);
                CampaignMain.cm.toUser(logos + planetIntel(defendIntel, currP.getHouseFightingFor()), currP.getName(), true);
            } else {
                CampaignMain.cm.toUser(logos + defendIntel, currN, true);
            }
            CampaignMain.cm.toUser(getInfo(true, false), currN, true);
        }


	}

    /**
     * Method which sends all game information - autoarty, map info, planet info, game options, etc. Used when a participant disconnects and then returns to the
     * campaign server.
     */
    public void sendReconnectInfoToPlayer(SPlayer p) {

        String lowerName = p.getName().toLowerCase();

        /*
         * If the player is IN_PROGRESS, he may need game options and other info
         * resent in order to be properly loaded into MegaMek (autoarty, etc).
         * Most of what needs to be sent was saved during
         * changeStatus(IN_PROGRESS)
         */
        if (currentStatus != STATUS_INPROGRESS) {
            return;
        }

        /*
         * Lock info is cleared if the player is purged from memory while
         * offline, so make sure that the player's army is still locked and send
         * the lock status.
         */
        p.lockArmy(getAllPlayersAndArmies().get(p.getName().toLowerCase()));

        // send options
        CampaignMain.cm.toUser(gameOptions.toString(), lowerName, false);

        Operation o = CampaignMain.cm.getOpsManager().getOperation(CampaignMain.cm.getOpsManager().getShortOpForPlayer(p).getName());
        // send terrain
        if (aTerrain != null) {
            CampaignMain.cm.toUser("APE|" + aTerrain.toStringPlanetaryConditions(), lowerName, false);
            CampaignMain.cm.toUser("PE|" + playEnvironment.toString(cityBuilder.toString()) + "|" + mapsize.width + "|" + mapsize.height + "|" + o.getValue("MapMedium"), lowerName, false);
        } else {
            CampaignMain.cm.toUser("PE|" + playEnvironment.toString(cityBuilder.toString()) + "|" + mapsize.width + "|" + mapsize.height + "|" + o.getValue("MapMedium"), lowerName, false);
        }

        if (isTeamOp) {
            CampaignMain.cm.toUser("PL|STN|" + p.getTeamNumber(), lowerName, false);
            MWLogger.debugLog(p.getName() + " Team: " + p.getTeamNumber());
            CampaignMain.cm.toUser("GMEP|" + teamEdge[p.getTeamNumber() - 1], lowerName, false);
            MWLogger.debugLog("Sent team edge to " + p.getName());
        }
        // send starting edge and autoarmy
        else if (defenders.containsKey(lowerName)) {
            CampaignMain.cm.toUser("GMEP|" + defenderEdge, lowerName, false);
            CampaignMain.cm.toUser(defenderAutoString, lowerName, false);
            CampaignMain.cm.toUser(defenderAutoEmplacementsString, lowerName, false);
            CampaignMain.cm.toUser(defenderAutoMinesString, lowerName, false);
            CampaignMain.cm.toUser(MULHash.get(lowerName), lowerName, false);
        } else if (attackers.containsKey(lowerName)) {
            CampaignMain.cm.toUser("GMEP|" + attackerEdge, lowerName, false);
            CampaignMain.cm.toUser(attackerAutoString, lowerName, false);
            CampaignMain.cm.toUser(attackerAutoEmplacementsString, lowerName, false);
            CampaignMain.cm.toUser(attackerAutoMinesString, lowerName, false);
            CampaignMain.cm.toUser(MULHash.get(lowerName), lowerName, false);
        }

        // send building options
        if (buildingOptions.length() > 1) {
            CampaignMain.cm.toUser(buildingOptions, lowerName, false);
        }

        CampaignMain.cm.toUser(bots, lowerName, false);
        CampaignMain.cm.toUser(botTeams, lowerName, false);

        // reset the player's fighting status
        p.setFightingNoOppList();

        // tell all players that we're fighting
        CampaignMain.cm.sendPlayerStatusUpdate(p, true);// send fighting info to
        // all

    }// end send reconnectInfo

    /**
     * Method which returns the maxattackers. Generally used by JoinAttackCommand to make sure an additional attacker is still allowed.
     */
    public int getMaxAttackers() {
        return maxAttackers;
    }

    /**
     * Method which returns the maxdefenders. Generally used by DefendCommand to make sure an additional defender is allowed.
     */
    public int getMaxDefenders() {
        return maxDefenders;
    }

    /**
     * Method which returns the mindefenders. Used to ensure that we have enough defenders before cahnging to RUNNING status.
     */
    public int getMinAttackers() {
        return minAttackers;
    }

    /**
     * Method which returns the mindefenders. Used to ensure that we have enough defenders before cahnging to RUNNING status.
     */
    public int getMinDefedners() {
        return minDefenders;
    }

    public PlanetEnvironment getEnvironment() {
        return playEnvironment;
    }

    /**
     * Method which returns world targetted by this op.
     */
    public SPlanet getTargetWorld() {
        return targetWorld;
    }

    /**
     * Method which returns short ID # of this op.
     */
    public int getShortID() {
        return shortID;
    }

    /**
     * Method which sets the short ID # of this op. This should be called ONLY from the Manager.
     */
    public void setShortID(int newID) {
        shortID = newID;
    }

    /**
     * Method which returns underlying long ID # of this op. Note that -1 is sent to constructor if no longop underpins.
     */
    public int getLongID() {
        return longID;
    }

    /**
     * Method which updates the ShortOperation's removedTree and pilotTree. String taken as a param is sent from a Client when a unit is removed, via MWServ and
     * CampaignMain.
     *
     * Each string will contain info for one, and only one, Entity, which may be either a "real" unit or a pilot.
     */
    public void addInProgressUpdate(String s) {

        StringTokenizer tokenizer = new StringTokenizer(s, "*");
        MWLogger.debugLog("IPU Sent: " + s);

        // see if we're dealing with a pilot or unit
        if (s.startsWith("MW*")) {

            tokenizer.nextToken();// strip the "MW"

            int originalID = Integer.parseInt(tokenizer.nextToken());
            int pickupID = Integer.parseInt(tokenizer.nextToken());
            boolean isDead = Boolean.parseBoolean(tokenizer.nextToken());

            SPilot mw = SPilot.getMekWarrior(originalID, pickupID);
            mw.setDeath(isDead);

            pilotsInProgress.put(originalID, mw);// key to host unit
        } else {

            OperationEntity oe = new OperationEntity(s);
            unitsInProgress.put(oe.getID(), oe);
        }
    }

    /**
     * Method which returns the death tree. Should be called only from the ShortResolver and only when resolving a disconnection.
     */
    public TreeMap<Integer, OperationEntity> getUnitsInProgress() {
        return unitsInProgress;
    }

    /**
     * Method which returns running info on pilots. Should only be called from the ShortResolver and only when completing a dropped game.
     */
    public TreeMap<Integer, SPilot> getPilotsInProgress() {
        return pilotsInProgress;
    }

    /**
     * Method which sets up the chicken tree.
     *
     * The chicken threads send defense links to players when they are started. We can assume that the waitingInfo is a good.
     *
     * It might be more logical to set up the chicken threads in the manager, from an OO standpoint, but this allows the Operation to easily terminate its
     * chicken threads when its status is changed from WAITING to RUNNING, after a /c term command is sent by the attacking player, etc.
     */
    public void informPossibleDefenders() {

        // look at every army in the potential defender list
        for (SArmy currArmy : pdlist) {

            SPlayer currPlayer = CampaignMain.cm.getPlayer(currArmy.getPlayerName());
            String playername = currPlayer.getName().toLowerCase();

            /*
             * Look for the current player in the chicken tree. If he's there,
             * just add the army to his thread. If not, set up a new thread and
             * then add the army.
             */
            if (chickenThreads.containsKey(playername)) {
                OpsChickenThread pThread = chickenThreads.get(playername);
                pThread.addArmy(currArmy);
            } else {
                String defenderMess = getDefenderMessage();
                OpsChickenThread newThread = new OpsChickenThread(currPlayer, shortID, opName, defenderMess);
                newThread.addArmy(currArmy);
                chickenThreads.put(playername, newThread);

                // add creation to log
                MWLogger.gameLog("Created chicken thread for " + shortID + "/" + currPlayer.getName() + "(" + opName + ")");
            }
        }// end while(poss defenders remain)

        /*
         * Now that we've set up all the possible threads and accounted for
         * those players who have multiple armies able to defend, start the
         * threads.
         */
        MWLogger.gameLog("Starting all chicken threads for #" + shortID + " (" + opName + ")");
        for (OpsChickenThread ct : chickenThreads.values()) {
            ct.start();
        }
    }

    /**
     * Method which removes a potential defender from the chicken tree. The term flag is set on the thread, then it is removed from the chicken tree. If
     * necessary, the player is penalized.
     *
     * This is generally called from within the ShortOperation, when it moves to RUNNING status, from DeactivateCommand when a player leaves active status, or
     * from a ChickenThread (via SPlayer) when it deactivates a player.
     */
    public void removePossibleDefender(String name, boolean penalize) {
        String currName = name.toLowerCase();
        OpsChickenThread currT = chickenThreads.get(currName);
        if (currT == null) {
            return;
        }

        if (penalize) {
            currT.doPenalty();
        }
        currT.stopChicken();
        chickenThreads.remove(currName);
    }

    /**
     * Method which is called when a ShortOp is moved to running status or terminated. Completely closes the chickenTree.
     *
     * Should only be called internally (status) or from OpsManager (term command, or noattacker/nodef).
     */
    public void terminateChickenThreads() {
        for (OpsChickenThread currT : chickenThreads.values()) {
            currT.stopChicken();
        }
        chickenThreads.clear();
    }

    /**
     * Method which returns information (string) on this ShortOperation. Intelligently determines which type of info to send by looking at status.
     *
     * NOTE: This method is public. Other info methods are private.
     */
    public String getInfo(boolean complete, boolean mod) {

        if (currentStatus == STATUS_WAITING) {
            return getWaitingInfo(complete);
        } else if ((currentStatus == STATUS_INPROGRESS) || (currentStatus == STATUS_REPORTING)) {
            return getInProgressInfo(complete, mod);
        } else if (currentStatus == STATUS_FINISHED) {
            return getFinishedInfo(complete, mod);
        } else {
            return "";
        }
    }

    /**
     * Private method which generates the first portion of a notification message for defenders. This is sent to chickenThreads as a constructor paramater. They
     * complete the message by adding relevant /c defend# links.
     *
     * Very similar to getWaitingInfo().
     */
    private String getDefenderMessage() {

        // String to return
        String resultString = "";

        // numattackers, for formatting
        int numAttackers = attackers.size();

        // add attackers to result
        resultString += getAttackerString(false, false);

        // add connector ("are" or "is")
        if (numAttackers > 1) {
            resultString += " are ";
        } else {
            resultString += " is ";
        }

        // add planet
        resultString += " attacking " + targetWorld.getNameAsColoredLink() + "! ";

        // Check and add op name if need be.
        if (CampaignMain.cm.getBooleanConfig("DisplayOperationName")) {
            resultString += "Operation: " + getName() + ". ";
        }

        // add unit info
        if (numAttackers == 1) {
            SArmy attackArm = CampaignMain.cm.getPlayer(attackers.firstKey()).getArmy(attackers.get(attackers.firstKey()));
            resultString += attackArm.getInaccurateDescription();// show same
            // for
            // complete
            // and
            // incomplete,
            // for now?
        }

        else if (numAttackers > 1) {

            int totalBV = 0;
            int totalUnits = 0;

            for (String currN : attackers.keySet()) {
                SArmy currArmy = CampaignMain.cm.getPlayer(currN).getArmy(attackers.get(currN));
                totalBV = totalBV + currArmy.getOperationsBV(null);
                totalUnits = totalUnits + currArmy.getAmountOfUnits();
            }

            resultString += "(Total Units: " + totalUnits + " / Total BV: " + totalBV + ")";
        }

        return resultString;
    }

    /**
     * Method which returns info for an operation which is waiting for players to join.
     */
    private String getWaitingInfo(boolean complete) {

        // strings
        String resultString = "";

        // start the return
        resultString += "#" + getShortID() + " ";

        // numattackers, for formatting
        int numAttackers = attackers.size();

        // add attackers to result
        resultString += getAttackerString(complete, false);

        // add connector ("are" or "is")
        if (numAttackers > 1) {
            resultString += " are ";
        } else {
            resultString += " is ";
        }

        // add planet
        resultString += " attacking " + targetWorld.getNameAsColoredLink() + "! ";

        // check and add Op name.
        if (CampaignMain.cm.getBooleanConfig("DisplayOperationName")) {
            resultString += "Operation: " + getName() + ". ";
        }

        // add unit info
        if (numAttackers == 1) {

            SArmy attackArm = CampaignMain.cm.getPlayer(attackers.firstKey()).getArmy(attackers.get(attackers.firstKey()));
            if (complete) {
                resultString += attackArm.getInaccurateDescription();// show
                // same
                // for
                // complete
                // and
                // incomplete,
                // for
                // now?
            } else {
                resultString += attackArm.getInaccurateDescription();// show
                // same
                // for
                // complete
                // and
                // incomplete,
                // for
                // now?
            }
        }

        else if (numAttackers > 1) {

            int totalBV = 0;
            int totalUnits = 0;

            for (String currN : attackers.keySet()) {
                SArmy currArmy = CampaignMain.cm.getPlayer(currN).getArmy(attackers.get(currN));
                totalBV = totalBV + currArmy.getOperationsBV(null);
                totalUnits = totalUnits + currArmy.getAmountOfUnits();
            }

            resultString += "(Total Units: " + totalUnits + " / Total BV: " + totalBV + ")";
        }

        // TODO: Add defender block for multiplayer games

        return resultString;
    }// end getWaitingInfo

    /**
     * Method which returns info for an operation which is in-progress.
     */
    private String getInProgressInfo(boolean complete, boolean mod) {

        // strings
        String resultString = "";
        String multiPlayerString = "";

        // pertinent campaign configs
        boolean blindDrop = CampaignMain.cm.getBooleanConfig("UseBlindDrops");
        Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);

        if (o.getBooleanValue("RealBlindDrop")) {
            blindDrop = true;
        }

        // start the return
        resultString += "#" + getShortID() + " ";

        // num attackers/defenders, for formatting
        int numAttackers = attackers.size();
        int numDefenders = defenders.size();

        // if this is a mod request, append the duration to pre-built header
        if (mod) {
            Long duration = System.currentTimeMillis() - startTime;
            String readableDuration = StringUtil.readableTime(duration);
            resultString += modHeader + " / Duration: " + readableDuration + "]<br>";
        }

        // add attackers to result
        resultString += getAttackerString(complete, true);

        // add connector ("are" or "is")
        if (numAttackers > 1) {
            resultString += " are ";
        } else {
            resultString += " is ";
        }

        // add planet
        resultString += " attempting " + StringUtils.aOrAn(opName, true) + " on " + targetWorld.getNameAsColoredLink() + " ";

        // add unit info
        if ((numAttackers == 1) && (numDefenders < 2)) {

            SArmy attackArm = CampaignMain.cm.getPlayer(attackers.firstKey()).getArmy(attackers.get(attackers.firstKey()));
            SArmy defendArm = CampaignMain.cm.getPlayer(defenders.firstKey()).getArmy(defenders.get(defenders.firstKey()));
            if (mod || (complete && !blindDrop)) {
                resultString += " with " + attackArm.getDescription(true, defendArm);
            } else {
                resultString += attackArm.getInaccurateDescription();
            }
        }

        else if ((numAttackers > 1) || (numDefenders > 1)) {

            int totalBV = 0;
            int totalUnits = 0;

            for (String currN : attackers.keySet()) {
                SArmy currArmy = CampaignMain.cm.getPlayer(currN).getArmy(attackers.get(currN));
                totalBV = totalBV + currArmy.getOperationsBV(null);
                totalUnits = totalUnits + currArmy.getAmountOfUnits();
                if (mod || (complete && !blindDrop)) {
                    resultString += "with " + currArmy.getDescription(true, null);
                }
            }

            multiPlayerString += " - Total Attacker Units: " + totalUnits + " / BV: " + totalBV + "<br>";
        }

        resultString += ". " + getDefenderString(complete, mod);

        // add "are" or "is"
        if (numDefenders > 1) {
            resultString += " are ";
        } else {
            resultString += " is ";
        }

        resultString += "defending ";

        // add unit info
        if ((numDefenders == 1) && (numAttackers < 2)) {
            try {
                SArmy attackArm = CampaignMain.cm.getPlayer(attackers.firstKey()).getArmy(attackers.get(attackers.firstKey()));
                SArmy defendArm = CampaignMain.cm.getPlayer(defenders.firstKey()).getArmy(defenders.get(defenders.firstKey()));

                if (mod || (complete && !blindDrop)) {
                    resultString += " with " + defendArm.getDescription(true, attackArm);
                } else {
                    resultString += defendArm.getInaccurateDescription();
                }
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
        }

        else if ((numAttackers > 1) || (numDefenders > 1)) {

            int totalBV = 0;
            int totalUnits = 0;

            for (String currN : defenders.keySet()) {
                try {
                    SArmy currArmy = CampaignMain.cm.getPlayer(currN).getArmy(defenders.get(currN));
                    totalBV = totalBV + currArmy.getOperationsBV(null);
                    totalUnits = totalUnits + currArmy.getAmountOfUnits();
                    if (mod || (complete && !blindDrop)) {
                        resultString += "with " + currArmy.getDescription(true, null);
                    }

                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            }

            multiPlayerString += " - Total Defender Units: " + totalUnits + " / BV: " + totalBV;
        }

        // now, if multiString actually exists ...
        if (!multiPlayerString.equals("")) {
            resultString += "<br>" + multiPlayerString;
        }

        return resultString;
    }

    /**
     * Method which returns info for an operation which has finished. String set by Resolver.
     */
    private String getFinishedInfo(boolean complete, boolean mod) {

        Long age = System.currentTimeMillis() - completionTime;
        String toReturn = StringUtil.readableTime(age) + " ago: ";

        // determine how much to return (player or faction names)
        if (mod) {

            Long duration = completionTime - startTime;
            String readableDuration = StringUtil.readableTime(duration);
            toReturn += modHeader + " / Duration: " + readableDuration + "]<br>";
            toReturn += completeFinishedString;

        } else if (complete) {
            toReturn += completeFinishedString;
        } else {
            toReturn += incompleteFinishedString;
        }

        return toReturn;
    }

    /**
     * Method which is used (by Resolver) to set the complete finishedString for post-game display.
     */
    public void setCompleteFinishedInfo(String toSet) {
        completeFinishedString = toSet;
    }

    /**
     * Method which is used (by Resolver) to set the incomplete finishedString for post-game display.
     */
    public void setIncompleteFinishedInfo(String toSet) {
        incompleteFinishedString = toSet;
    }

    /**
     * Method which may be used to set up attacker strings. NOTE: Private.
     *
     * boolean - complete info boolean - always show name (used by in-progress)
     */
    private String getAttackerString(boolean complete, boolean showName) {

        // set up attacker string
        int currAttacker = 0;
        int numAttackers = attackers.size();

        // pertinent campaign configs
        boolean blindDrop = CampaignMain.cm.getBooleanConfig("UseBlindDrops");
        Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);

        if (o.getBooleanValue("RealBlindDrop")) {
            blindDrop = true;
        }

        // string to return
        String attackString = "";

        // show everything in faction, unless using blind drops
        if (complete && !blindDrop) {
            Iterator<String> i = attackers.keySet().iterator();
            while (i.hasNext()) {

                // load next (or first) player
                SPlayer currPlayer = CampaignMain.cm.getPlayer(i.next());
                currAttacker++;// increment counter

                attackString += currPlayer.getColoredName() + currPlayer.getHouseFightingFor().getColoredAbbreviation(true);

                if ((numAttackers == 2) && i.hasNext()) {
                    attackString += " and ";
                } else if ((numAttackers > 2) && i.hasNext()) {
                    if ((currAttacker + 1) < numAttackers) {
                        attackString += ", ";
                    } else if ((currAttacker + 1) == numAttackers) {
                        attackString += " and ";
                    }
                }
            }// end while (i.hasNext)
        } else {// not complete

            String nameString;
            if (showName && (numAttackers == 1)) {
                nameString = initiator.getColoredName() + initiator.getHouseFightingFor().getColoredAbbreviation(true);
            } else {
                nameString = initiator.getHouseFightingFor().getColoredNameAsLink();
            }

            if (numAttackers == 1) {
                attackString += nameString;
            } else if (numAttackers > 1) {
                attackString += numAttackers + " " + nameString + " players ";
            }
        }

        return attackString;
    }

    /**
     * Method which may be used to set up defender strings NOTE: Private
     */
    private String getDefenderString(boolean complete, boolean mod) {

        // set up defender string
        int currDefender = 0;
        int numDefenders = defenders.size();

        // pertinent campaign configs
        boolean blindDrop = CampaignMain.cm.getBooleanConfig("UseBlindDrops");

        Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);

        if (!o.getBooleanValue("RealBlindDrop")) {
            blindDrop = true;
        }

        // string to return
        String defendString = "";

        // show everything in faction
        if (mod || (complete && !blindDrop)) {
            Iterator<String> i = defenders.keySet().iterator();
            while (i.hasNext()) {

                // load next (or first) player
                SPlayer currPlayer = CampaignMain.cm.getPlayer(i.next());
                currDefender++;// increment counter

                defendString += currPlayer.getColoredName() + currPlayer.getHouseFightingFor().getColoredAbbreviation(true);

                if ((numDefenders == 2) && i.hasNext()) {
                    defendString += " and ";
                } else if ((numDefenders > 2) && i.hasNext()) {
                    if ((currDefender + 1) < numDefenders) {
                        defendString += ", ";
                    } else if ((currDefender + 1) == numDefenders) {
                        defendString += " and ";
                    }
                }
            }// end while (i.hasNext)

        } else {// not complete

            try {
                // get *a* defender. all are from same faction, or he'll be the
                // only one.
                SPlayer defender = CampaignMain.cm.getPlayer(defenders.firstKey());

                String nameString;
                if (numDefenders == 1) {
                    nameString = defender.getColoredName() + defender.getHouseFightingFor().getColoredAbbreviation(true);
                } else {

                    TreeMap<String, Integer> houseSorting = new TreeMap<String, Integer>();

                    for (String playerName : defenders.keySet()) {
                        defender = CampaignMain.cm.getPlayer(playerName);

                        if (houseSorting.containsKey(defender.getHouseFightingFor().getColoredNameAsLink())) {
                            houseSorting.put(defender.getHouseFightingFor().getColoredNameAsLink(), houseSorting.get(defender.getHouseFightingFor().getColoredNameAsLink()) + 1);
                        } else {
                            houseSorting.put(defender.getHouseFightingFor().getColoredNameAsLink(), 1);
                        }
                    }

                    nameString = "";
                    for (String houseName : houseSorting.keySet()) {
                        nameString += houseSorting.get(houseName) + " " + houseName + ", ";
                    }

                    nameString = nameString.substring(0, nameString.lastIndexOf(","));
                }

                if (numDefenders == 1) {
                    defendString += nameString;
                } else if (numDefenders > 1) {
                    defendString += nameString + " players ";
                }
            } catch (Exception ex) {
                MWLogger.errLog("Unable to find defenders for operation: " + opName);
                MWLogger.errLog(ex);
            }
        }

        return defendString;
    }

    /**
     * Method which loops through the player collection looking for a given SPlayer.
     */
    public boolean hasPlayer(SPlayer p) {
        for (String currN : getAllPlayerNames()) {
            if (currN.equalsIgnoreCase(p.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method which loops through the attacker and defender maps in order to determine if any of the participating players is from a given house.
     */
    public boolean hasPlayerFrom(SHouse h) {
        for (String currN : getAllPlayerNames()) {
            SPlayer currP = CampaignMain.cm.getPlayer(currN);
            if (currP.getHouseFightingFor().equals(h)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method which determined whether or not the operation involves a player whose faction name begins with a given string.
     *
     * Used by GamesCommand to filter output. We assume that s is all lowercase, b/c Games lowercases its faction filter @ parse.
     */
    public boolean hasPlayerWhoseHouseBeginsWith(String s) {
        for (String currN : getAllPlayerNames()) {
            SPlayer currP = CampaignMain.cm.getPlayer(currN);
            if (currP.getHouseFightingFor().getName().toLowerCase().startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method which returns a collection of all players involved in the Operation.
     *
     * Suitable for iterating.
     */
    public Collection<String> getAllPlayerNames() {
        TreeMap<String, Integer> c = new TreeMap<String, Integer>();
        c.putAll(attackers);
        c.putAll(defenders);
        return c.keySet();
    }

    /**
     * Method which returns a collection of all players involved in the Operation.
     *
     * Suitable for iterating.
     */
    public TreeMap<String, Integer> getAllPlayersAndArmies() {
        TreeMap<String, Integer> c = new TreeMap<String, Integer>();
        c.putAll(attackers);
        c.putAll(defenders);
        return c;
    }

    /**
     * Method which decrements) the showsToClear counter. Called on ticks. If the counter drops below 0, return true (id should be released), otherwise return
     * false and show again on the next tick.
     */
    public boolean decrementShowsToClear() {
        showsToClear--;
        if (showsToClear < 0) {
            return true;
        }
        return false;
    }

    /**
     * Method which returns the completion time. Used to age-sort shortops in /c games.
     */
    public long getCompletionTime() {
        return completionTime;
    }

    /**
     * compareTo required for compliance with Comparable interface.
     */
    public int compareTo(Object o) {

        if (o instanceof ShortOperation) {
            ShortOperation compOp = (ShortOperation) o;
            if (compOp.getShortID() > getShortID()) {
                return 1;
            } else if (compOp.getShortID() == getShortID()) {
                return 0;
            } else {
                return -1;
            }
        }

        // else
        return 0;
    }

    public String planetIntel(String intel, SHouse house) {
        String result = intel;

        int factionOwnerShip = getTargetWorld().getInfluence().getInfluence(house.getId());
        int basedOwnerShip = CampaignMain.cm.getIntegerConfig("MinChanceForAccurateOperationsReports");
        int chanceVacuum = CampaignMain.cm.getRandomNumber(100);
        int chanceGravity = CampaignMain.cm.getRandomNumber(100);
        int chanceTemp = CampaignMain.cm.getRandomNumber(100);
        int chanceTime = CampaignMain.cm.getRandomNumber(100);

        factionOwnerShip = Math.max(factionOwnerShip, basedOwnerShip);

        // Extra planet intel
        // Attacker
        result += "<br><B>Planet Name:</b> " + getTargetWorld().getName() + "<br>";
        result += "<b>Terrain Type:</b> " + playEnvironment.getName() + " (" + playContinent.getAdvancedTerrain().getName() + ") <br>";

        if (intelVacuum && (factionOwnerShip >= chanceVacuum)) {
            result += "<b>Atmosphere:</b> Missing<br>";
        }
        if ((intelGravity != 1.0) && (factionOwnerShip >= chanceGravity)) {
            result += "<b>Gravity:</b> " + intelGravity + "<br>";
        }
        if (((intelTemp > 50) || (intelTemp < -30)) && (factionOwnerShip >= chanceTemp)) {
            result += "<b>Current Temp:</b> " + intelTemp + "<br>";
        }
        if ((intelTimeFrame != PlanetaryConditions.L_DAY) && (factionOwnerShip >= chanceTime)) {
            result += "<b>Current Time of Day:</b> " + PlanetaryConditions.getLightDisplayableName(intelTimeFrame) + "<br>";
        }
        if (doubleBlind && (factionOwnerShip >= chanceTime)) {
            result += "<b>Visibility is :</b> " + (intelVisibility * 30) + " meters<br>";
        }

        if (intelWeather != PlanetaryConditions.WE_NONE) {
            result += ("<b>Weather Condition: </b>");
            result += (PlanetaryConditions.getWeatherDisplayableName(intelWeather));
        }

        if (intelWind != PlanetaryConditions.WI_NONE) {
            result += ("<b>Wind Condition: </b>");
            result += (PlanetaryConditions.getWindDisplayableName(intelWind));
        }

        return result;

    }

    public void setAutoReport(String report) {
        autoReportString = report;
    }

    public String getAutoReport() {
        return autoReportString;
    }

    public void setPlayersReported(int players) {
        playersReported = players;
    }

    public int getPlayersReported() {
        return playersReported;
    }

    public int getStartingBV() {
        return startingBV;
    }

    public int getStartingUnits() {
        return startingUnits;
    }

    public int getFinishingBV() {
        return finishingBV;
    }

    public void setFinishingBV(int i) {
        finishingBV = i;
        modHeader += " / Finish BV: " + i;
    }

    public boolean isFromReserve() {
        return fromReserve;
    }

    public String checkTeam(int teamNumber) {
        return checkTeam(teamNumber, 0, false);
    }

    public String checkTeam(int teamNumber, int bv, boolean attacker) {
        Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);
        int maxPlayersPerTeam = o.getIntValue("TeamSize");
        int teamCount = 0;
        int totalBV = 0;
        //int maxOpBV = CampaignMain.cm.getIntegerConfig("MaxBVDifference");
        int maxOpBV = o.getIntValue("MaxBVDifference");
        if (attacker) {
            maxOpBV += o.getIntValue("MaxAttackerBV");
        } else {
            maxOpBV += o.getIntValue("MaxDefenderBV");
        }

        for (String playerName : getAllPlayerNames()) {
            SPlayer player = CampaignMain.cm.getPlayer(playerName);
            if (player.getTeamNumber() == teamNumber) {
                teamCount++;

                if (player.getLockedArmy() != null) {
                    totalBV += player.getLockedArmy().getBV();
                }
            }
        }
        // teams full return false.
        if (teamCount >= maxPlayersPerTeam) {
            return "The Team is already Full";
        }

        if ((totalBV + bv) >= maxOpBV) {
            return "The BV of your selected army is too large!";
        }

        return "";
    }

    public int getAttackersTeam() {
        if (attackers.size() < 1) {
            return -1;
        }

        return CampaignMain.cm.getPlayer(attackers.firstKey()).getTeamNumber();

    }

    /**
     * Get the team number of the current faction for this op.
     *
     * @param factionName
     * @return Factions team number or an unused team number.
     */
    public int getFactionTeam(String factionName) {
        int team = 1;

        for (String playerName : getAllPlayerNames()) {
            SPlayer player = CampaignMain.cm.getPlayer(playerName);

            if (factionName.equalsIgnoreCase(player.getHouseFightingFor().getName())) {
                return player.getTeamNumber();
            }
            // Increase the current team number incase this is the first faction
            // memeber to join
            // That way they will get an unused team number.
            if (team == player.getTeamNumber()) {
                team++;
            }
        }
        return team;
    }

    /**
     * Gets the total bv for the Combined Attacking forces. This is check to start the attack.
     *
     * @return Attackers Total BV
     */
    public int getAttackersBV() {
        int bv = 0;

        for (String attacker : getAttackers().keySet()) {
            SPlayer player = CampaignMain.cm.getPlayer(attacker);

            if (player != null) {
                SArmy army = player.getArmy(getAttackers().get(attacker));
                if (army != null) {
                    bv += army.getBV();
                }
            }

        }
        return bv;
    }

    /**
     * Loop through all the defending teams and see if they are all ready to launch They must have either the Max number of Team members or meet the Max
     * Defender BV or both for to the Operation to launch. This code should allow multiple teams to defend against a single team attack.
     *
     * @param teams
     * @param players
     * @return
     */
    private boolean checkDefendersAndLaunch(int teams, int players) {
        Operation o = CampaignMain.cm.getOpsManager().getOperation(opName);
        int maxDefenderBV = o.getIntValue("MaxDefenderBV");
        int teamCount = 0;
        int teamBV = 0;

        for (int teamNumber = 1; teamNumber <= teams; teamNumber++) {
            for (String defenders : getDefenders().keySet()) {
                SPlayer defender = CampaignMain.cm.getPlayer(defenders);
                if (defender == null) {
                    continue;
                }
                if (defender.getTeamNumber() == teamNumber) {
                    SArmy army = defender.getArmy(getDefenders().get(defenders));
                    if (army == null) {
                        continue;
                    }
                    teamCount++;
                    teamBV += army.getBV();
                }
            }
            if ((teamCount < players) && (teamBV < maxDefenderBV)) {
                return false;
            }
            teamCount = 0;
            teamBV = 0;
        }
        return true;
    }

    public SPlayer getInitiator() {
        return initiator;
    }

    public TreeMap<String, SPlayer> getWinners() {
        return winners;
    }

    public TreeMap<String, SPlayer> getLosers() {
        return losers;
    }

    public TreeSet<String> getCancelledPlayers() {
        return cancellingPlayers;
    }

    private int getRandomDeployment(Operation o) {
        int position = 1;
        String[] positionNames = { "DeployNorthwest", "DeployNorth", "DeployNortheast", "DeployEast", "DeploySoutheast", "DeploySouth", "DeploySouthwest", "DeployWest", "DeployEdge", "DeployCenter", "DeployNorthwestdeep", "DeployNorthdeep", "DeployNortheastdeep", "DeployEastdeep", "DeploySoutheastdeep", "DeploySouthdeep", "DeploySouthwestdeep", "DeployWestdeep" };

        Vector<Integer> deploymentChoices = new Vector<Integer>();

        for (int pos = 0; pos < positionNames.length; pos++) {
            int chances = o.getIntValue(positionNames[pos]);
            for (int count = 0; count < chances; count++) {
                deploymentChoices.add(pos);
            }
        }
        deploymentChoices.trimToSize();

        if (deploymentChoices.size() < 1) {
            return position;
        }

        if (deploymentChoices.size() == 1) {
            return deploymentChoices.firstElement();
        }

        int rand = CampaignMain.cm.getRandomNumber(deploymentChoices.size());
        position = deploymentChoices.elementAt(rand);

        return position;
    }

    private Vector<SUnit> createMulArmy(int number, String list) {

        StringTokenizer st = new StringTokenizer(list, ";");
        Vector<String> mulFileList = new Vector<String>(1, 1);
        Vector<SUnit> returnList = new Vector<SUnit>(1, 1);

        while (st.hasMoreElements()) {
            mulFileList.add(st.nextToken());
        }

        // something happened
        if (mulFileList.size() < 1) {
            return returnList;
        }

        for (; number > 0; number--) {
            if (mulFileList.size() == 1) {
                returnList.addAll(SUnit.createMULUnits(mulFileList.firstElement()));
            } else {
                returnList.addAll(SUnit.createMULUnits(mulFileList.remove(CampaignMain.cm.getRandomNumber(mulFileList.size()))));
            }
        }

        returnList.trimToSize();

        return returnList;
    }

    public ArrayList<SUnit> captureUnits(Operation o, int unitsToCapture, SPlanet target, SHouse losingHouse, boolean forced) {
        // for (all unit types, mek preferred)
        int numCaptured = 0;

        /*
         * Try every factory on the world, at random, until we've taken what we
         * can. This may mean getting inf or vehs on a planet than can produce
         * assault mechs.
         */
        ArrayList<UnitFactory> factoriesSearched = new ArrayList<UnitFactory>(target.getUnitFactories());
        ArrayList<SUnit> capturedUnits = new ArrayList<SUnit>();
        while ((factoriesSearched.size() > 0) && (numCaptured < unitsToCapture)) {

            // get a random factory
            SUnitFactory currFacility = (SUnitFactory) factoriesSearched.remove(CampaignMain.cm.getRandomNumber(factoriesSearched.size()));

            int currWeight = currFacility.getWeightclass();

            for (int type = Unit.MEK; type < Unit.MAXBUILD; type++) {

                // skip this type if the facility cannot
                // produce
                if (!currFacility.canProduce(type)) {
                    continue;
                }

                // skip if the operation doesn't allow capturing of this unit type
                if (!currFacility.canBeRaided(type, o)) {
                	MWLogger.debugLog("Can not capture unit type (" + type + ") for operation '" + o.getName() + "' as it is not allowed.");
                	continue;
                }

                // skip if the operation doesn't allow capturing of this unit type
                if (!currFacility.canBeRaided(type, o)) {
                	MWLogger.debugLog("Can not capture unit type (" + type + ") for operation '" + o.getName() + "' as it is not allowed.");
                	continue;
                }

            	// Check if there are enough PPs
            	int ppAvailable = losingHouse.getPP(currFacility.getWeightclass(), type);
            	int ppNeed = currFacility.getPPCost(currFacility.getWeightclass(), type);
            	if (ppNeed > ppAvailable) {
            		MWLogger.debugLog("Not enough PP to capture a unit.  Needed: " + ppNeed + ", available: " + ppAvailable);
            		continue;
            	}

                boolean noUnits = false;
                while (!noUnits && (numCaptured < unitsToCapture)) {
                    SPilot pilot = new SPilot("Vacant", 99, 99);
                    Vector<SUnit> captured = new Vector<SUnit>(1, 1);
                    if (forced) {
                        captured.addAll(currFacility.getMechProduced(type, pilot));
                        losingHouse.addPP(currFacility.getWeightclass(), type, -ppNeed, false);
                    } else {
                        SUnit capturedUnit = losingHouse.getEntity(currWeight, type);
                        if ((capturedUnit != null) && UnitUtils.canStartUp(capturedUnit.getEntity())) {
                            captured.add(capturedUnit);
                        }
                    }

                    //Make sure we have no OMG-UR-FDs or we have gotten
                    //a unit type we can't have because of a factory
                    //that makes multiple unit types
                    //13 Sept 2011 - Cord Awtry
                    for (int i = captured.size() - 1; i >= 0; i--) {
                    	SUnit unit = captured.get(i);

                    	if (unit.isOMGUnit()) {
                    		MWLogger.debugLog("Removing an OMG-UR-FD from captured units for operation '" + o.getName() + "'.");
                    		captured.remove(i);
                    	} else if (!unit.canBeCapturedInOperation(o)) {
                    		MWLogger.debugLog("Removing an '" + unit.getModelName() + "' from captured units for operation '" + o.getName() + "'.");
                    		captured.remove(i);
                    	}
                    }

                    if (captured.size() < 1) {
                        noUnits = true;
                    } else {
                        capturedUnits.addAll(captured);
                        numCaptured += captured.size();
                    }
                }// end while(units remain in this
                // factories' pool)
            }// end for(all types)

        }// end while(factories remain)

        return capturedUnits;
    }

	public Vector<SUnit> getPreOperationUnits(Operation o) {
        Vector<SUnit> units = new Vector<SUnit>(1, 1);

        int unitCaptureCap = o.getIntValue("UnitCaptureCap");
        SHouse defendingHouse = CampaignMain.cm.getPlayer(defenders.firstKey()).getHouseFightingFor();

        int unitsToCapture = o.getIntValue("AttackerBaseUnitsTaken");
        int unitUnitAdjust = o.getIntValue("AttackerUnitsUnitAdjustment");
        int unitBVAdjust = o.getIntValue("AttackerUnitsBVAdjustment");

        if (unitUnitAdjust > 0) {
            unitsToCapture += Math.floor(getStartingUnits() / unitUnitAdjust);
        }
        if (unitBVAdjust > 0) {
            unitsToCapture += Math.floor(getStartingBV() / unitBVAdjust);
        }
        if (unitsToCapture > unitCaptureCap) {
            unitsToCapture = unitCaptureCap;
        }

        units.addAll(captureUnits(o, unitsToCapture, targetWorld, defendingHouse, false));

        unitsToCapture = o.getIntValue("AttackerBaseFactoryUnitsTaken");
        unitUnitAdjust = o.getIntValue("AttackerFactoryUnitsUnitAdjustment");
        unitBVAdjust = o.getIntValue("AttackerFactoryUnitsBVAdjustment");

        if (unitUnitAdjust > 0) {
            unitsToCapture += Math.floor(getStartingUnits() / unitUnitAdjust);
        }
        if (unitBVAdjust > 0) {
            unitsToCapture += Math.floor(getStartingBV() / unitBVAdjust);
        }
        if (unitsToCapture > unitCaptureCap) {
            unitsToCapture = unitCaptureCap;
        }

        units.addAll(captureUnits(o, unitsToCapture, targetWorld, defendingHouse, true));

        StringBuilder results = new StringBuilder("HS|");

        for (SUnit unit : units) {

            if (unit.hasVacantPilot()) {
                SHouse attackingHouse = initiator.getHouseFightingFor();
                SPilot pilot = new SPilot(SPilot.getRandomPilotName(CampaignMain.cm.getR()), attackingHouse.getBaseGunner(Unit.MEK), attackingHouse.getBasePilot(Unit.MEK));
                unit.setPilot(pilot);
            }
            results.append(defendingHouse.removeUnit(unit, false));
        }

        CampaignMain.cm.doSendToAllOnlinePlayers(defendingHouse, results.toString(), false);
        return units;
    }

    public boolean validatePlayers(TreeSet<String> players) {

        // TreeSet<String> allPlayerNames = new
        // TreeSet<String>(getAllPlayerNames());

        Collection<String> allPlayersNames = getAllPlayerNames();
        for (String name : allPlayersNames) {
            if (!players.contains(name)) {
                return false;
            }
        }

        return true;
    }

    public void checkMercContracts(SPlayer player, int contractType, int amount) {

        if (!player.getMyHouse().isMercHouse()) {
            return;
        }

        ContractInfo contract = (((MercHouse) player.getMyHouse()).getContractInfo(player));

        if (contract == null) {
            return;
        }

        if (contract.getType() != contractType) {
            return;
        }

        contract.setEarnedAmount(contract.getEarnedAmount() + amount);

    }

    public OperationReporter getReporter() {
        return reporter;
    }

    public String generateMULList(int minArmies, int maxArmies, String armyList, boolean unitsStolenBeforeFight, Operation o) {

        StringBuffer returnString = new StringBuffer();
        if ((minArmies > 0) && (armyList != null) && (armyList.trim().length() > 0) && (maxArmies >= minArmies)) {
            int numOfArmies = CampaignMain.cm.getRandomNumber(maxArmies - minArmies) + minArmies;

            Vector<SUnit> units = new Vector<SUnit>(1, 1);
            units.addAll(createMulArmy(numOfArmies, armyList));

            if (unitsStolenBeforeFight) {
                preCapturedUnits.addAll(getPreOperationUnits(o));
                units.addAll(preCapturedUnits);
            }
            for (SUnit unit : units) {
                returnString.append(unit.toString(true));
                returnString.append("|");
            }
        } else if (unitsStolenBeforeFight) {

            preCapturedUnits.addAll(getPreOperationUnits(o));

            for (SUnit unit : preCapturedUnits) {
                returnString.append(unit.toString(true));
                returnString.append("|");
            }

        }

        return returnString.toString();
    }
}// end OperationsManager class
