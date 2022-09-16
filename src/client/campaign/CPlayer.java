/*
 * MekWars - Copyright (C) 2004, 2005, 2006
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
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

package client.campaign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import client.MWClient;
import client.util.CArmyComparator;
import client.util.CUnitComparator;
import common.House;
import common.Player;
import common.SubFaction;
import common.Unit;
import common.util.MWLogger;
import common.util.TokenReader;
import common.util.UnitComponents;
import common.util.UnitUtils;
import megamek.common.CriticalSlot;
import megamek.common.OffBoardDirection;

/**
 * Class for Player object used by Client
 */
public class CPlayer extends Player {

    public static final String PREFIX = "PL"; // prefix for player strings
    public static final String DELIMITER = "#"; // delimiter for player strings

    private MWClient mwclient;
    private String Name;
    private String House;
    private String myLogo = "";

    private int Exp;
    private int Money;
    private int Bays;
    private int FreeBays;
    private int Influence;
    private int Techs;
    private int TechCost;
    private int RewardPoints;
    private double Rating;
    private int hangarPenalty;
    private int hangarPurchasePenalties[][] = new int[6][4];

    private Vector<CUnit> Hangar;
    private Vector<CArmy> Armies;
    private ArrayList<CUnit> AutoArmy;

    private ArrayList<String> adminExcludes;
    private ArrayList<String> playerExcludes;

    private CPersonalPilotQueues personalPilotQueue;

    private House myHouse = null;
    private House houseFightingFor = null;

    private ArrayList<Integer> totalTechs = new ArrayList<Integer>(4);
    private ArrayList<Integer> availableTechs = new ArrayList<Integer>(4);

    private int repairLocation = 0;
    private int repairTechType = 0;
    private int repairRetries = 0;

    private int conventionalMinesAllowed = 0;
    private int vibraMinesAllowed = 0;

    private UnitComponents partsCache = new UnitComponents();

    private String subFactionName = "";

    public CPlayer(MWClient client) {
        mwclient = client;
        Name = "";
        Exp = 0;
        Money = 0;
        Bays = 0;
        FreeBays = 0;
        Influence = 0;
        Rating = 0;
        House = "";
        Hangar = new Vector<CUnit>(1, 1);
        Armies = new Vector<CArmy>(1, 1);
        AutoArmy = new ArrayList<CUnit>();
        personalPilotQueue = new CPersonalPilotQueues();
        adminExcludes = new ArrayList<String>();
        playerExcludes = new ArrayList<String>();
        myHouse = new House();
        houseFightingFor = new House();
        for (int x = 0; x < 4; x++) {
            availableTechs.add(0);
            totalTechs.add(0);
        }
    }

    public boolean decodeCommand(String command) {
        StringTokenizer ST;
        String element;

        ST = new StringTokenizer(command, "|");
        element = TokenReader.readString(ST);
        if (!element.equals("PL")) {
            return (false);
        }
        element = TokenReader.readString(ST);
        command = command.substring(3);

        if (element.equals("DA")) {// is a PI|DA
            if (!setData(command)) {
                return (false);
            }
            return (true);
        }
        return (false);
    }

    /**
     * Called from PL after PL|SAD received. Adds a new army OR replaces an old
     * army's data with new dump.
     */
    public void setArmyData(String data) {

        CArmy newArmy = new CArmy();
        newArmy.fromString(data, this, "%", mwclient);

        // Save the old army's legal operations.
        CArmy oldArmy = getArmy(newArmy.getID());
        if (oldArmy != null) {
            newArmy.setLegalOperations(oldArmy.getLegalOperations());
        }

        // swap the armies
        removeArmy(newArmy.getID());
        if (Armies.size() < newArmy.getID()) {
            Armies.add(newArmy);
        } else {
            Armies.add(newArmy.getID(), newArmy);
        }
    }

    /**
     * Complete setData command. Called in response to a PS| sent by the server.
     *
     * @param - data
     * @return - success
     */
    public boolean setData(String data) {
        StringTokenizer ST;
        String element;
        CUnit tmek;
        int i, Armiescount, Hangarcount;

        ST = new StringTokenizer(data, "~");
        element = TokenReader.readString(ST);
        if (!element.equals("CP")) {
            return false;
        }

        for (int x = 0; x < UnitUtils.TECH_ELITE; x++) {
            setTotalTechs(x, 0);
            setAvailableTechs(x, 0);
        }

        Armies.clear();
        Hangar.clear();

        Name = TokenReader.readString(ST);

        Money = TokenReader.readInt(ST);
        Exp = TokenReader.readInt(ST);

        Hangarcount = TokenReader.readInt(ST);
        for (i = 0; i < Hangarcount; i++) {
            tmek = new CUnit(mwclient);
            if (tmek.setData(TokenReader.readString(ST))) {
                Hangar.add(tmek);
            }
        }

        Armiescount = (TokenReader.readInt(ST));
        for (i = 0; i < Armiescount; i++) {
            CArmy army = new CArmy();
            army.fromString(TokenReader.readString(ST), this, "%", mwclient);
            Armies.add(army);
        }

        Bays = TokenReader.readInt(ST);
        FreeBays = TokenReader.readInt(ST);
        Rating = Double.parseDouble(TokenReader.readString(ST));
        Influence = TokenReader.readInt(ST);
        setTechnicians(TokenReader.readInt(ST));
        doPayTechniciansMath();
        RewardPoints = TokenReader.readInt(ST);
        String string = TokenReader.readString(ST);
       	setMekToken(Integer.parseInt(string));
        House = TokenReader.readString(ST);
        setHouseFightingFor(TokenReader.readString(ST));
        setLogo(TokenReader.readString(ST));
        setInvisible(TokenReader.readBoolean(ST));

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UsePartsRepair"))) {
            partsCache.fromString(TokenReader.readString(ST), "|");
        } else {
            TokenReader.readString(ST);
        }

        setAutoReorder(TokenReader.readBoolean(ST));

        //flags.loadDefaults(mwclient.getPlayer().getDefaultPlayerFlags().export());
        //flags.loadPersonal(TokenReader.readString(ST));
        MWLogger.infoLog("My Player Flags: " + flags.export());
        // traps run. sort the HQ. this isn't duplicative, b/c
        // direct lods (PS instead of PL) don't trigger sorts.
        sortHangar();
        return true;
    }

    /**
     * Called by PL|HD - adds a single unit to the hangar.
     */
    public void setHangarData(String data) {
        try {
            CUnit unit = new CUnit(mwclient);
            if (unit.setData(data)) {
                Hangar.add(unit);
                sortHangar();// sort it!
            }
        } catch (Exception e) {
            MWLogger.errLog(e);
            return;
        }
    }

    /**
     * Called by PL|UU - updates a unit's data.
     */
    public void updateUnitData(StringTokenizer st) {
        try {
            CUnit currUnit = getUnit(TokenReader.readInt(st));
            currUnit.setData(TokenReader.readString(st));
            sortHangar();// properties have changes. sort. YARR!
        } catch (Exception e) {
            MWLogger.errLog(e);
            return;
        }
    }

    public void updateUnitMachineGuns(StringTokenizer st) {
        try {
            CUnit currUnit = getUnit(TokenReader.readInt(st));
            int location = TokenReader.readInt(st);
            int slot = TokenReader.readInt(st);
            boolean selection = TokenReader.readBoolean(st);

            CriticalSlot crit = currUnit.getEntity().getCritical(location, slot);
            crit.getMount().setRapidfire(selection);

            sortHangar();// properties have changes. sort. YARR!
        } catch (Exception e) {
            MWLogger.errLog(e);
            return;
        }
    }

    /**
     * Remove an army from a player's set. This can be called directly from a
     * PL|RA command, or indirectly by PL|SAD via CPlayer.setArmyData(), which
     * removes all old instances of an army before adding the new data.
     */
    public boolean removeArmy(int lanceID) {

        for (Iterator<CArmy> i = Armies.iterator(); i.hasNext();) {
            if (i.next().getID() == lanceID) {
                i.remove();
                mwclient.getMainFrame().updateAttackMenu();// removing an army
                // needs to reset
                // menu
                return (true);
            }
        }
        return (false);
    }

    /**
     * Remove a unit from the player's hangar. Called from PL after receipt of a
     * PL|RU|ID (RemoveUnit#ID) command.
     *
     * Note that there is NOT an analagous addUnit() method. Single additions
     * are sent to the clients using (obtusely enough) the PL|HD (hangar data)
     * command. See .setHangarData()'s comments, as well as those in
     * SUnit.addUnit(), for details/explanation.
     */
    public boolean removeUnit(int unitID) {

        for (Iterator<CUnit> i = Hangar.iterator(); i.hasNext();) {
            if (i.next().getId() == unitID) {
                i.remove();
                return (true);
            }
        }
        return (false);
    }

    /**
     * @return Returns the armies.
     */
    public Vector<CArmy> getArmies() {
        return Armies;
    }

    public void setExp(int texp) {
        Exp = texp;
    }

    public void setMoney(int tmoney) {
        Money = tmoney;
    }

    public void setRewardPoints(int rewards) {
        RewardPoints = rewards;
    }

    public void setBays(int tbays) {
        Bays = tbays;
    }

    public void setFreeBays(int tfreebays) {
        FreeBays = tfreebays;
    }

    public void setInfluence(int tinfluence) {
        Influence = tinfluence;
    }

    public void setRating(double trating) {
        Rating = trating;
    }

    public void setHouse(String faction) {
        myHouse = mwclient.getData().getHouseByName(faction);
        House = faction;

        /*
         * Get the faction configs before starting anything else. I could pause
         * the client and wait for the configs but I'll let it go. --Torren
         */
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c getfactionconfigs#0" + mwclient.getserverConfigs("TIMESTAMP"));

        /*
         * Now that we have a house set, we can check for BM access properly. Do
         * the BM buy and sell button checks.
         */
        if (mwclient.getMainFrame().getMainPanel().getBMPanel() != null) {
            mwclient.getMainFrame().getMainPanel().getBMPanel().checkFactionAccess();
        }

        /*
         * Same thing for the HQ. We have a house, so we can rebuild the button
         * bar w/ or w/o a reset button, as appropriate.
         */
        if (mwclient.getMainFrame().getMainPanel().getHQPanel() != null) {
            mwclient.getMainFrame().getMainPanel().getHQPanel().reinitialize();
        }
    }

    public String getHouse() {
        return House;
    }

    public void setHouseFightingFor(String faction) {
        houseFightingFor = mwclient.getData().getHouseByName(faction);
    }

    public House getHouseFightingFor() {
        return houseFightingFor;
    }

    public void setLogo(String logo) {
        myLogo = logo;
    }

    public String getLogo() {
        return "<img height=\"140\" width=\"130\" src =\"" + myLogo + "\">";
    }

    public String getMyLogo() {
        return myLogo;
    }

    public String getName() {
        return Name;
    }

    public int getExp() {
        return Exp;
    }

    public double getRating() {
        return Rating;
    }

    public int getRewardPoints() {
        return RewardPoints;
    }

    public int getMoney() {
        return Money;
    }

    public int getBays() {
        return Bays;
    }

    public int getFreeBays() {
        return FreeBays;
    }

    public int getInfluence() {
        return Influence;
    }

    public int getTechs() {
        return Techs;
    }

    @Override
    public void setTechnicians(int tech) {
        Techs = tech;
        doPayTechniciansMath();
    }

    public int getTechCost() {
        if (TechCost < 0) {
            return 0;
        }
        // else
        return TechCost + getHangarPenalty();  // If not using sliding hangar costs, hangarPenalty will be 0, so will still return the same.
    }

    public Vector<CUnit> getHangar() {
        return Hangar;
    }

    /**
     * Calculate the the ID that would be assined to a newly created army. This
     * is used by the army builder to construct /c exm# commands for an as-yet
     * non-existant army.
     */
    public int getNextNewArmyID() {
        int newID = -1;
        int possibleNewID = 0;
        while (newID == -1) {
            for (int i = 0; i < Armies.size(); i++) {
                if ((Armies.get(i)).getID() == possibleNewID) {
                    newID = i;
                }
            }
            if (newID == -1) {
                newID = possibleNewID;
            } else {
                possibleNewID++;
                newID = -1;
            }
        }
        return newID;
    }

    /**
     * Method which greates an autoarmy. takes in a string with weight classes,
     * and uses server configs (path, filenames) to construct units of those
     * weights.
     *
     * Units are added to servers when a player joins a game, same as units from
     * locked armies.
     */
    public void setAutoArmy(StringTokenizer st) {

        /*
         * clear the previous autoarmy. Auto army is always called first, and is
         * cleared correctly even if only gun emplacements are sent.
         */
        AutoArmy = new ArrayList<CUnit>();

        // if its a null, this was just a clearing call.
        if (st == null) {
            return;
        }

        while (st.hasMoreTokens()) {
            String filename = TokenReader.readString(st);
            if (filename.equals("CLEAR")) {
                return;
            }

            // get the distance
            int distInBoards = Integer.parseInt(mwclient.getserverConfigs("DistanceFromMap"));
            int distInHexes = distInBoards * 17;// 17 hexes per board.

            CUnit currUnit = new CUnit(mwclient);

            /*
             * This is needed to set the edge for auto arty when auto edge is
             * set for players. Else, arty edge is set in MM when the players
             * click on the edge they want.
             */
            OffBoardDirection direction = OffBoardDirection.NORTH;
            switch (mwclient.getPlayerStartingEdge()) {
                case 0:
                    break;
                case 1:
                case 2:
                case 3:
                    direction = OffBoardDirection.NORTH;
                    break;
                case 4:
                    direction = OffBoardDirection.EAST;
                    break;
                case 5:
                case 6:
                case 7:
                    direction = OffBoardDirection.SOUTH;
                    break;
                case 8:
                    direction = OffBoardDirection.WEST;
                    break;
            }

            currUnit.setAutoUnitData(filename, distInHexes, direction);
            AutoArmy.add(currUnit);
        }// end while(tokens)
    }// end setAutoArmy()

    /**
     * Method which greates an autoarmy gun emplacements. takes in a string with
     * weight classes, and uses server configs (path, filenames) to construct
     * units of those weights.
     *
     * Units are added to servers when a player joins a game, same as units from
     * locked armies.
     */
    public void setAutoGunEmplacements(StringTokenizer st) {

        // if its a null, this was just a clearing call.
        if (st == null) {
            return;
        }

        while (st.hasMoreTokens()) {
            String filename = TokenReader.readString(st);
            if (filename.equals("CLEAR")) {
                return;
            }

            CUnit currUnit = new CUnit(mwclient);
            currUnit.setAutoUnitData(filename, 0, OffBoardDirection.NORTH);
            AutoArmy.add(currUnit);
        }// end while(tokens)
    }// end setAutoArmy()

    public void setMULCreatedArmy(StringTokenizer st) {

        while (st.hasMoreElements()) {
            String data = TokenReader.readString(st);
            if (data.equalsIgnoreCase("CLEAR")) {
                return;
            }

            CUnit cm = new CUnit();
            cm.setData(data);
            AutoArmy.add(cm);
        }
    }

    /**
     * Method which returns the autoArmy arraylist.
     */
    public ArrayList<CUnit> getAutoArmy() {
        return AutoArmy;
    }

    public CUnit getUnit(int unitID) {

        for (CUnit currU : Hangar) {
            if (currU.getId() == unitID) {
                return currU;
            }
        }
        return null;
    }

    public CArmy getArmy(int id) {

        for (CArmy currA : Armies) {
            if (currA.getID() == id) {
                return currA;
            }
        }
        return null;
    }
    
    //@salient- compare client quirks with server
    // lol while this works, realized the way i'm doing things makes this check meaningless... 
    // what needs to be checked is the hosts xmls, not the client quirks
    // which are already set by the server anyway....
//    public String getAllQuirkInfoForActivation()
//    {
//    	StringJoiner quirksList = new StringJoiner("*");
//    	List<Integer> idList = new ArrayList<Integer>();
//    	
//        for (CArmy currA : Armies) 
//        {
//        	for (Unit currU : currA.getUnits())
//        	{
//        		CUnit currCU = (CUnit) currU;
//        		if(currCU.hasQuirks())
//        		{
//        			int ID = currCU.getId();
//        			if(idList.contains(ID)) //skip dupes
//        				continue;
//        			idList.add(ID);
//        			quirksList.add(String.valueOf(ID));
//        			quirksList.add(currCU.getQuirksList());        			
//        		}
//        	}
//        }
//        MWLogger.debugLog(quirksList.toString());
//        return quirksList.toString();
//    }

    public int getAmountOfTimesUnitExistsInArmies(int unitID) {
        int result = 0;
        for (CArmy currA : Armies) {
            if (currA.getUnit(unitID) != null) {
                result++;
            }
        }
        return result;
    }

    public String getArmiesUnitIsIn(int unitID) {
        StringBuilder result = new StringBuilder();
        for (CArmy currA : Armies) {
            if (currA.getUnit(unitID) != null) {
                result.append(currA.getID() + " ");
            }
        }
        return result.toString();
    }

    public synchronized ArrayList<Unit> getLockedUnits() {

        ArrayList<Unit> result = new ArrayList<Unit>();
        for (CArmy currA : Armies) {
            if (currA.isLocked()) {
                result.addAll(currA.getUnits());
            }
        }
        return result;
    }

    public synchronized CArmy getLockedArmy() {

        for (CArmy currA : Armies) {
            if (currA.isLocked()) {
                return currA;
            }
        }
        return null;
    }

    public void doPayTechniciansMath() {

        // don't even waste time on 0 cases. Just return.
        if (Techs <= 0) {
            TechCost = 0;
            return;
        }

        // starts as a double, gets cast back to an int for return.
        float amountToPay = 0;

        // load config variables needed to do the math ...
        float additive = Float.parseFloat(mwclient.getserverConfigs("AdditivePerTech"));
        float ceiling = Float.parseFloat(mwclient.getserverConfigs("AdditiveCostCeiling"));

        /*
         * divide the ceiling by the addiive. techs past this number are all
         * charged at the ceiling rate. Example: (With 1.20 and .04, the result
         * is 30. Every additional tech (31, 32, etc.) is paid at the ceiling
         * wage.
         */
        int techCeiling = (int) (ceiling / additive);
        if (Techs > techCeiling) {
            int techsPastCeiling = Techs - techCeiling;
            amountToPay += ceiling * techsPastCeiling;
        }// end if(some techs are paid @ ceiling price)

        /*
         * Add up the number of times the non-ceiling techs were incremented,
         * then figure out their total cost. In cases where the ceiling is
         * passed, the flat fee techs are handled above, so only techs up to
         * that ceiling need to have the additive math done. If the ceiling isnt
         * reached, just use the number of techToPay from the param.
         */
        int techsUsingAdditive = 0;
        if (Techs > techCeiling) {
            techsUsingAdditive = techCeiling;
        }// end if(ceiling threshold crossed)
        else {
            techsUsingAdditive = Techs;
        }// end else (just using the techstopay number)

        /*
         * Faster to just to a for loop to determine the number of times the
         * additive was made (1 + 2 + 3 + 4, and so on) with ints, and THEN
         * multiply by the double additive than do alot of floating point math
         * by for-in through and multiplying by the additive each time.
         */
        int totalAdditions = 0;
        for (int i = 1; i <= techsUsingAdditive; i++) {
            totalAdditions += i;
        }// end for(all counted techs)

        // now figure out the final amount to pay ...
        amountToPay += totalAdditions * additive;

        // Add penalty if the player is over a sliding limit

        amountToPay += hangarPenalty;

        // now return the amount in INT form since we don't support fractional
        // MU costs.
        // also, set the currentTechPayment, to avoid doing this math again if
        // possible

        TechCost = Math.round(amountToPay);
    }

    public void addArmyUnit(String data) {
        StringTokenizer ST = new StringTokenizer(data, DELIMITER);

        if (ST.hasMoreTokens()) {
            int army = TokenReader.readInt(ST);
            int unitid = TokenReader.readInt(ST);
            int bv = TokenReader.readInt(ST);
            int position = TokenReader.readInt(ST);
            if (position >= 0) {
                getArmy(army).addUnit(getUnit(unitid), position);
            } else {
                getArmy(army).addUnit(getUnit(unitid));
            }
            getArmy(army).setBV(bv);
            sortArmies();
        }
    }

    public void removeArmyUnit(String data) {
        StringTokenizer ST = new StringTokenizer(data, DELIMITER);
        if (ST.hasMoreTokens()) {
            int army = TokenReader.readInt(ST);
            int unitid = TokenReader.readInt(ST);
            int bv = TokenReader.readInt(ST);

            Iterator<Unit> i = getArmy(army).getUnits().iterator();
            while (i.hasNext()) {
                if (i.next().getId() == unitid) {
                    i.remove();
                    getArmy(army).removeCommander(unitid); //Baruk Khazad!  20151108c it is safe to removeCommander regardless of whether isCommander or not
                    break;
                }
            }

            getArmy(army).setBV(bv);
            getArmy(army).getC3Network().remove(unitid);
        }
        mwclient.refreshGUI(MWClient.REFRESH_HQPANEL);
    }

    /**
     * Method called from PL| which updates a CArmy's legalOperations tree.
     */
    public void updateOperations(String data) {

        if (data.equals("CLEAR")) {
            for (CArmy army : getArmies()) {
                army.getLegalOperations().clear();
            }
            return;
        }

        StringTokenizer tokenizer = new StringTokenizer(data, "*");
        int armyID = TokenReader.readInt(tokenizer);
        CArmy army = getArmy(armyID);

        // System.err.println(" ArmyID: "+armyID+ " Army: "+army);
        if (army == null) {
            return;
        }

        while (tokenizer.hasMoreTokens()) {
            String mode = "";
            String name = "";

            try {
                mode = TokenReader.readString(tokenizer);
                name = TokenReader.readString(tokenizer);
            } catch (NoSuchElementException e) {
                return;
            }

            if (mode.equals("a")) {
                army.getLegalOperations().add(name);
            } else if (mode.equals("r")) {
                army.getLegalOperations().remove(name);
            }
        }// end while(more tokens)

        // update the CMainFrame Attack menu
        mwclient.getMainFrame().updateAttackMenu();

    }// end updateOperations

    public void repositionArmyUnit(String data) {
        StringTokenizer ST = new StringTokenizer(data, DELIMITER);

        int army = TokenReader.readInt(ST);
        int unitid = TokenReader.readInt(ST);
        int position = TokenReader.readInt(ST);

        CArmy a = getArmy(army);

        // remove the unit
        Iterator<Unit> i = a.getUnits().iterator();
        while (i.hasNext()) {
            if (i.next().getId() == unitid) {
                i.remove();
                break;
            }
        }

        // then re-add the unit
        getArmy(army).addUnit(getUnit(unitid), position);

    }

    public void setUnitStatus(String data) {
        StringTokenizer ST = new StringTokenizer(data, DELIMITER);
        if (ST.hasMoreTokens()) {
            int unitid = TokenReader.readInt(ST);
            int status = TokenReader.readInt(ST);
            CUnit unit = getUnit(unitid);

            if (unit == null) {
                return;
            }

            if (mwclient.isUsingAdvanceRepairs() && (status == Unit.STATUS_UNMAINTAINED)) {
                unit.setStatus(Unit.STATUS_OK);
            } else {
                unit.setStatus(status);
            }
        }
    }

    public void setArmyName(String data) {
        StringTokenizer ST = new StringTokenizer(data, DELIMITER);
        if (ST.hasMoreTokens()) {
            int army = TokenReader.readInt(ST);
            String name = TokenReader.readString(ST);

            if (name == "-1") {
                name = "";
            }
            if (getArmy(army) != null) {
                getArmy(army).setName(name);
            }
        }
    }

    public void playerLockArmy(int aid) {
        if (getArmy(aid) != null) {
            getArmy(aid).playerLockArmy();
        }
    }

    public void playerUnlockArmy(int aid) {
        if (getArmy(aid) != null) {
            getArmy(aid).playerUnlockArmy();
        }
    }

    public void toggleArmyDisabled(int aid) {
        if (getArmy(aid) != null) {
            getArmy(aid).toggleArmyDisabled();
        }
    }

    public void setArmyBV(String data) {
        StringTokenizer ST = new StringTokenizer(data, DELIMITER);
        if (ST.hasMoreTokens()) {
            int army = TokenReader.readInt(ST);
            if (getArmy(army) != null) {
                getArmy(army).setBV(TokenReader.readInt(ST));
            } else {
                MWLogger.errLog("Bad Army id: " + army);
            }
        }
    }

    public void setArmyLimit(String data) {
        StringTokenizer ST = new StringTokenizer(data, DELIMITER);
        if (ST.hasMoreTokens()) {
            int army = TokenReader.readInt(ST);
            int lowerLimit = TokenReader.readInt(ST);
            int upperLimit = TokenReader.readInt(ST);

            getArmy(army).setLowerLimiter(lowerLimit);
            getArmy(army).setUpperLimiter(upperLimit);
        }
    }

    public void setArmyOpForceSize(String data) {
        StringTokenizer ST = new StringTokenizer(data, DELIMITER);
        if (ST.hasMoreTokens()) {
            int army = TokenReader.readInt(ST);
            float opForceSize = TokenReader.readFloat(ST);

            getArmy(army).setOpForceSize(opForceSize);
        }

    }

    public void setArmyLock(String data) {
        StringTokenizer ST = new StringTokenizer(data, DELIMITER);
        if (ST.hasMoreTokens()) {
            int army = TokenReader.readInt(ST);
            boolean lock = TokenReader.readBoolean(ST);
            getArmy(army).setLocked(lock);
        }
    }

    public void setPlayerPersonalPilotQueue(CPersonalPilotQueues queue) {
        personalPilotQueue = queue;
    }

    public CPersonalPilotQueues getPersonalPilotQueue() {
        return personalPilotQueue;
    }

    /**
     * Exclude method, called after receipt of PL|AEU| (Admin Exclude Update).
     * Because NP lists are expected to be small (2-5 players), the entire list
     * is sent every time.
     *
     * @urgru 4.3.05
     */
    public void setAdminExcludes(String buffer, String token) {
        adminExcludes.clear();
        StringTokenizer ST = new StringTokenizer(buffer, token);
        while (ST.hasMoreElements()) {
            String curr = TokenReader.readString(ST);
            if (!curr.equals("0")) {
                adminExcludes.add(curr);
            }
        }
        mwclient.getMainFrame().getMainPanel().getUserListPanel().repaint();
    }

    /**
     * Exclude method, called after receipt of PL|PEU| (Player Exclude Update).
     * Because NP lists are expected to be small (2-5 players), the entire list
     * is sent every time.
     */
    public void setPlayerExcludes(String buffer, String token) {
        playerExcludes.clear();
        StringTokenizer ST = new StringTokenizer(buffer, token);
        while (ST.hasMoreElements()) {
            String curr = TokenReader.readString(ST);
            if (!curr.equals("0")) {
                playerExcludes.add(curr);
            }
        }
        mwclient.getMainFrame().getMainPanel().getUserListPanel().repaint();
    }

    public ArrayList<String> getAdminExcludes() {
        return adminExcludes;
    }

    public ArrayList<String> getPlayerExcludes() {
        return playerExcludes;
    }

    /*
     * Hangar sorting mechanisms. Client and server need not order hangars in
     * the same fashion, since all transactions (after the initial data feed)
     * take place on a unit by unit basis.
     *
     * Sort options: - BV - Name - Type - Unit ID - Weight - No sort [load
     * order]
     *
     * BV is (for all intents and purposes) an exclusive sort. The others can
     * lead to significant clustering. Hence, secondary filters can be applied.
     */

    /**
     * Method which resorts every unit. Inefficient, but we hate clients.
     * Because we're evil. So there.
     *
     * @urgru 4.4.05
     */
    public void sortHangar() {

        // load configs
        String primeSortOrder = mwclient.getConfigParam("PRIMARYHQSORTORDER");
        String secondarySortOrder = mwclient.getConfigParam("SECONDARYHQSORTORDER");
        String tertiarySortOrder = mwclient.getConfigParam("TERTIARYHQSORTORDER");

        // Choices [note - this array must be duplicated in CHQPanel's
        // maybeShowPopup()]
        String[] choices =
            { "Name", "Battle Value", "Gunnery Skill", "ID Number", "MP (Jumping)", "MP (Walking)", "Pilot Kills", "Unit Type", "Weight (Class)", "Weight (Tons)", "No Sort" };

        // determine which sort will dominate
        int primarySort = CUnitComparator.HQSORT_NONE;
        for (int i = 0; i < choices.length; i++) {
            if (primeSortOrder.equals(choices[i])) {
                primarySort = i;
            }
        }

        // determine secondary sort
        int secondarySort = CUnitComparator.HQSORT_NONE;
        for (int i = 0; i < choices.length; i++) {
            if (secondarySortOrder.equals(choices[i])) {
                secondarySort = i;
            }
        }

        // determine tertiary sort
        int tertiarySort = CUnitComparator.HQSORT_NONE;
        for (int i = 0; i < choices.length; i++) {
            if (tertiarySortOrder.equals(choices[i])) {
                tertiarySort = i;
            }
        }

        // we know this holds CUnits. Can safely cast.
        Object[] unitsArray = Hangar.toArray();

        // run third sort
        if ((tertiarySort != primarySort) && (tertiarySort != secondarySort) && (tertiarySort != CUnitComparator.HQSORT_NONE)) {
            Arrays.sort(unitsArray, new CUnitComparator(tertiarySort));
        }

        // run the second sort
        if ((primarySort != secondarySort) && (secondarySort != CUnitComparator.HQSORT_NONE)) {
            Arrays.sort(unitsArray, new CUnitComparator(secondarySort));
        }

        // now the primary sort
        if (primarySort != CUnitComparator.HQSORT_NONE) {
            Arrays.sort(unitsArray, new CUnitComparator(primarySort));
        }

        // overwrite the hangar with a new arraylist constructed from the
        // unitsArray.
        Vector<CUnit> Hangar2 = new Vector<CUnit>(1, 1);
        for (Object element : unitsArray) {
            Hangar2.add((CUnit) element);
        }

        // replace the hangar and flush the array
        Hangar = Hangar2;
        unitsArray = null;
    }

    /*
     * Hangar sorting mechanisms. Client and server need not order hangars in
     * the same fashion, since all transactions (after the initial data feed)
     * take place on a unit by unit basis.
     *
     * Sort options: - BV - Name - Type - Unit ID - Weight - No sort [load
     * order]
     *
     * BV is (for all intents and purposes) an exclusive sort. The others can
     * lead to significant clustering. Hence, secondary filters can be applied.
     */

    /**
     * Method which resorts every unit. Inefficient, but we hate clients.
     * Because we're evil. So there.
     *
     * @urgru 4.4.05
     */
    public void sortArmies() {

        // load configs
        String primeSortOrder = mwclient.getConfigParam("PRIMARYARMYSORTORDER");
        String secondarySortOrder = mwclient.getConfigParam("SECONDARYARMYSORTORDER");
        String tertiarySortOrder = mwclient.getConfigParam("TERTIARYARMYSORTORDER");

        // Choices [note - this array must be duplicated in CHQPanel's
        // maybeShowPopup()]
        String[] choices =
            { "Name", "Battle Value", "ID Number", "Max Tonnage", "Avg Walk MP", "Avg Jump MP", "Number Of Units", "No Sort" };

        // determine which sort will dominate
        int primarySort = CArmyComparator.ARMYSORT_NONE;
        for (int i = 0; i < choices.length; i++) {
            if (primeSortOrder.equals(choices[i])) {
                primarySort = i;
            }
        }

        // determine secondary sort
        int secondarySort = CArmyComparator.ARMYSORT_NONE;
        for (int i = 0; i < choices.length; i++) {
            if (secondarySortOrder.equals(choices[i])) {
                secondarySort = i;
            }
        }

        // determine tertiary sort
        int tertiarySort = CArmyComparator.ARMYSORT_NONE;
        for (int i = 0; i < choices.length; i++) {
            if (tertiarySortOrder.equals(choices[i])) {
                tertiarySort = i;
            }
        }

        // we know this holds CUnits. Can safely cast.
        Object[] armiesArray = Armies.toArray();

        // run third sort
        if ((tertiarySort != primarySort) && (tertiarySort != secondarySort) && (tertiarySort != CArmyComparator.ARMYSORT_NONE)) {
            Arrays.sort(armiesArray, new CArmyComparator(tertiarySort));
        }

        // run the second sort
        if ((primarySort != secondarySort) && (secondarySort != CArmyComparator.ARMYSORT_NONE)) {
            Arrays.sort(armiesArray, new CArmyComparator(secondarySort));
        }

        // now the primary sort
        if (primarySort != CArmyComparator.ARMYSORT_NONE) {
            Arrays.sort(armiesArray, new CArmyComparator(primarySort));
        }

        // overwrite the hangar with a new arraylist constructed from the
        // unitsArray.
        Vector<CArmy> Army2 = new Vector<CArmy>(1, 1);
        for (Object element : armiesArray) {
            Army2.add((CArmy) element);
        }

        // replace the hangar and flush the array
        Armies = Army2;
        armiesArray = null;
    }

    public int getHangarSpaceRequired(int typeid, int weightclass, int baymod, String model) {

        if (typeid == Unit.PROTOMEK) {
            return 0;
        }

        if ((typeid == Unit.INFANTRY) && Boolean.parseBoolean(mwclient.getserverConfigs("FootInfTakeNoBays"))) {

            // check types
            boolean isFoot = model.startsWith("Foot");
            boolean isAMFoot = model.startsWith("Anti-Mech Foot");

            if (isFoot || isAMFoot) {
                return 0;
            }
        }

        int result = 1;
        String techAmount = "TechsFor" + Unit.getWeightClassDesc(weightclass) + Unit.getTypeClassDesc(typeid);
        result = Integer.parseInt(mwclient.getserverConfigs(techAmount));

        // Apply Pilot Mods (Astech skill)
        if (!mwclient.isUsingAdvanceRepairs()) {
            result += baymod;
        }

        // no negative techs
        if (result < 0) {
            result = 0;
        }

        return result;
    }// end getHangarSpaceRequired()

    public House getMyHouse() {
        return myHouse;
    }

    public void applyUnitRepairs(StringTokenizer data) {
        CUnit unit = getUnit(TokenReader.readInt(data));
        unit.applyRepairs(TokenReader.readString(data));
    }

    public void updateTotalTechs(String data) {
        StringTokenizer techs = new StringTokenizer(data, "%");
        int slot = 0;

        while (techs.hasMoreTokens()) {
            setTotalTechs(slot, TokenReader.readInt(techs));
            slot++;
        }
    }

    public void setTotalTechs(int slot, int techs) {
        totalTechs.set(slot, techs);
    }

    public ArrayList<Integer> getTotalTechs() {
        return totalTechs;
    }

    public void updateAvailableTechs(String data) {
        StringTokenizer techs = new StringTokenizer(data, "%");
        int slot = 0;

        while (techs.hasMoreTokens()) {
            setAvailableTechs(slot, TokenReader.readInt(techs));
            slot++;
        }
    }

    public void setAvailableTechs(int slot, int techs) {

        availableTechs.set(slot, techs);
    }

    public ArrayList<Integer> getAvailableTechs() {
        return availableTechs;
    }

    public void setRepairLocation(int loc) {
        repairLocation = loc;
    }

    public int getRepairLocation() {
        return repairLocation;
    }

    public void setRepairTechType(int type) {
        repairTechType = type;
    }

    public int getRepairTechType() {
        return repairTechType;
    }

    public void setRepairRetries(int retries) {
        repairRetries = retries;
    }

    public int getRepairRetries() {
        return repairRetries;
    }

    public void resetRepairs() {
        repairLocation = 0;
        repairTechType = 0;
        repairRetries = 0;
    }

    public void setConventionalMinesAllowed(int mines) {
        conventionalMinesAllowed = mines;
    }

    public int getConventionalMinesAllowed() {
        return conventionalMinesAllowed;
    }

    public void setVibraMinesAllowed(int mines) {
        vibraMinesAllowed = mines;
    }

    public int getVibraMinesAllowed() {
        return vibraMinesAllowed;
    }

    public void setMines(StringTokenizer st) {
        setConventionalMinesAllowed(TokenReader.readInt(st));
        setVibraMinesAllowed(TokenReader.readInt(st));
    }

    public void setFactionConfigs(String data) {

        if (data.startsWith("DONE#DONE")) {
            mwclient.setWaiting(false);
            return;
        }

        StringTokenizer ST = new StringTokenizer(data, DELIMITER);
        // mwclient.getserverConfigs().clear();
        // mwclient.getServerConfigData();
        while (ST.hasMoreTokens()) {
            String key = TokenReader.readString(ST);
            String value = TokenReader.readString(ST);

            mwclient.getserverConfigs().setProperty(key, value);
        }
        mwclient.setWaiting(false);
    }

    public UnitComponents getPartsCache() {
        return partsCache;
    }

    public void setSubFaction(String name) {
        subFactionName = name;
    }

    public SubFaction getSubFaction() {

        SubFaction mySubFaction = myHouse.getSubFactionList().get(subFactionName);
        if (mySubFaction == null) {
            return new SubFaction();
        }

        return mySubFaction;
    }

    public int getSubFactionAccess() {

        SubFaction mySubFaction = myHouse.getSubFactionList().get(subFactionName);
        if (mySubFaction == null) {
            return 0;
        }

        return Integer.parseInt(mySubFaction.getConfig("AccessLevel"));

    }

    public String getSubFactionName() {
        return subFactionName;
    }

    public int getHangarPenalty() {
    	return hangarPenalty;
    }

    public int getHangarPurchasePenalty(int type, int weight) {
    	return hangarPurchasePenalties[type][weight];
    }

    public void setHangarPenalty(int p) {
    	hangarPenalty = p;
    }

    public void setHangarPurchasePenalty(int type, int weight, int p) {
    	hangarPurchasePenalties[type][weight] = p;
    }

	public void parseHangarPenaltyString(String readString) {
		StringTokenizer st = new StringTokenizer(readString, "*");
		setHangarPenalty(Integer.parseInt(st.nextToken()));
		for (int type = Unit.MEK; type < Unit.MAXBUILD; type++) {
			for (int weight = Unit.LIGHT; weight <= Unit.ASSAULT; weight++) {
				setHangarPurchasePenalty(type, weight, Integer.parseInt(st.nextToken()));
			}
		}
		mwclient.getMainFrame().getMainPanel().getHSPanel().updateDisplay();
	}
}
