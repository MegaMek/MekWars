/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

package mekwars.server.campaign;

import common.Unit;


public class NewbieHouse extends NonConqHouse {

    /**
     *
     */
    private static final long serialVersionUID = -6530126180469336846L;
    //VARIABLES
    java.util.TreeMap<String, Integer> resetPlayers;//<lowerName,numResetsRemaning>

    //CONSTRUCTORS

    /**
     * Used for serialization
     */
    public NewbieHouse() {
        super();
        resetPlayers = new java.util.TreeMap<String, Integer>();
    }

    public NewbieHouse(int id, String name, String HouseColor, int BaseGunner, int BasePilot, String abbreviation) {
        super(id, name, HouseColor, BaseGunner, BasePilot, abbreviation);
        resetPlayers = new java.util.TreeMap<String, Integer>();
    }

    public NewbieHouse(int id) {
        super(id);
        resetPlayers = new java.util.TreeMap<String, Integer>();
    }

    //METHODS
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("[N]");
        result.append(super.toString());
        return result.toString();
    }

    /**
     * @return int number of bays given to SOLies
     *
     * @urgru 8/18/04
     *       <p>
     *       Override the getBaysProvided from SHouse and return NewbieHouseBays from the campaignconfig.txt
     */
    @Override
    public int getBaysProvided() {
        int newbBays = CampaignMain.campaignMain.getIntegerConfig("NewbieHouseBays");
        return newbBays;
    }

    @Override
    public boolean isNewbieHouse() {
        return true;
    }

    @Override
    public int getPP(int weight, int type_id) {
        //Always enough components to get raided..
        return 1 * this.getPPCost(weight, type_id);
    }

    @Override
    public SUnit getEntity(int weightclass, int type_id) {
        SUnit m = super.getEntity(weightclass, type_id);
        if (m == null) {m = this.getRandomUnit(type_id, weightclass, null).firstElement();}
        return m;
    }

    public java.util.Vector<mekwars.server.campaign.SUnit> getRandomUnit(int unitType, int weightClass,
          String houseName) {

        String factionName;
        if (houseName == null) {factionName = CampaignMain.campaignMain.getConfig("NewbieHouseName");} else {
            factionName = houseName;
        }

        String unitFilename = BuildTable.getUnitFilename(factionName,
              Unit.getWeightClassDesc(weightClass),
              unitType,
              BuildTable.STANDARD);

        java.util.Vector<mekwars.server.campaign.SUnit> newbieUnits = new java.util.Vector<mekwars.server.campaign.SUnit>(
              1,
              1);
        if (unitFilename.toLowerCase().trim().endsWith(".mul")) {
            newbieUnits.addAll(SUnit.createMULUnits(unitFilename, "Training Unit"));
        } else {
            //build the new unit
            SUnit newbieUnit = new SUnit(factionName, unitFilename, weightClass);
            newbieUnit.setProducer("Training Unit");
            newbieUnits.add(newbieUnit);
        }
        return newbieUnits;
    }

    /**
     * Override SHouse.removePlayer() in order to add a reset removal. Keeps SPlayers who defect out of the reset list.
     */
    @Override
    public void removePlayer(SPlayer p, boolean donateMechs) {
        super.removePlayer(p, donateMechs);
        this.removeResetPlayer(p);
    }

    @Override
    public int getMoney() {
        return 0;
    }

    public void removeResetPlayer(SPlayer p) {
        this.resetPlayers.remove(p.getName().toLowerCase());
    }

    public String cleanupHangarAndPP() {
        //Should never be anything in training faction bay.
        for (int type = 0; type < 3; type++) {
            for (int size = 0; size < 4; size++) {
                java.util.Vector<mekwars.server.campaign.SUnit> Weightclass = getHangar(type).elementAt(size);
                Weightclass.clear();
            }
        }
        return "";
    }//end cleanup

    public void setPP(int weight, int val) {
        //Newbiefaction can never get PP
        //currentPP.setElementAt(new Integer(0),weight-1);
    }

    public String potentialHouseProduction() {
        return "";
    }

    /**
     * Method that checks a player's eligibility for a unit reset and assigns new units if the player is under-armed.
     * Also used by DefectCommand to strip and replace a player's SOL units if so configured.
     *
     * @param p          - player requesting new units
     * @param forceReset - if true, ignore player's unit count. used by DefectCommand to always pass reset check.
     *
     * @return - Human readable outcome of request.
     */
    public String requestNewMech(SPlayer p, boolean forceReset, String houseName) {


        //don't let fighting player's change their units.
        if (p.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
            return "You may not request new units while playing a game.";
        }

        if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {return "You may not request new units while on active duty.";}

        String lowerName = p.getName().toLowerCase();
        String toSend = "Your units were reset";

        /*
         * see if the player has enough units already. if this is a defection or the
         * player is immune and has resets remaining, ignore the number of units in
         * his hangar and generate a new set.
         */
        int replace = CampaignMain.campaignMain.getIntegerConfig("NumUnitsToQualifyForNew");
        if (forceReset) {replace = 999999;}

        if (resetPlayers.containsKey(lowerName) && resetPlayers.get(lowerName) > 0) {
            replace = 999999;

            // decrement the reset counter & tell player
            int remainingResets = resetPlayers.get(lowerName) - 1;
            toSend += " (" + remainingResets + " post-game resets remaining).";
            resetPlayers.put(lowerName, remainingResets);

        } else {
            toSend += ".";
        }

        if (p.getUnits().size() > replace) {return "You already have enough units.";}

        //get new units, replace PPQ, then resend the player's data
        p.stripOfAllUnits(false);
        if (CampaignMain.campaignMain.getBooleanConfig("AllowPersonalPilotQueues")) {
            p.getPersonalPilotQueue().flushQueue();
            CampaignMain.campaignMain.toUser("PL|PPQ|" + p.getPersonalPilotQueue().toString(true), p.getName(), false);
        }

        getNewSOLUnits(p, houseName);

        /*
         * send complete army/unit/tech update if this is a normal reset, but
         * refrain if this is a defection (forceReset), in which case the status
         * will be completely reset during login to newHouse.
         */
        if (!forceReset) {CampaignMain.campaignMain.toUser("PS|" + p.toString(true), p.getName(), false);}

        //inform him of the positive outcome
        return toSend;
    }

    /**
     * A method which gets a new SOL force and assigns it to a player. Top heavy b/c of the number of server
     * configurables involved.
     * <p>
     * Note that the player is *not* assured 1 elite pilot and 1 green pilot (this differs from the old MMNET
     * implementation). Pilots may still be elite or green randomly.
     * <p>
     * Retuns a string which is (sometimes) added to the enroll/reset messages sent to the player.
     *
     * @urgru 12/29/04
     */
    public String getNewSOLUnits(SPlayer p, String houseName) {

        java.util.Vector<mekwars.server.campaign.SUnit> units = new java.util.Vector<mekwars.server.campaign.SUnit>(1,
              1);

        //meks
        int numLMeks = CampaignMain.campaignMain.getIntegerConfig("SOLLightMeks");
        int numMMeks = CampaignMain.campaignMain.getIntegerConfig("SOLMediumMeks");
        int numHMeks = CampaignMain.campaignMain.getIntegerConfig("SOLHeavyMeks");
        int numAMeks = CampaignMain.campaignMain.getIntegerConfig("SOLAssaultMeks");

        //vehicles
        int numLVehs = CampaignMain.campaignMain.getIntegerConfig("SOLLightVehs");
        int numMVehs = CampaignMain.campaignMain.getIntegerConfig("SOLMediumVehs");
        int numHVehs = CampaignMain.campaignMain.getIntegerConfig("SOLHeavyVehs");
        int numAVehs = CampaignMain.campaignMain.getIntegerConfig("SOLAssaultVehs");

        //infantry
        int numLInf = CampaignMain.campaignMain.getIntegerConfig("SOLLightInf");
        int numMInf = CampaignMain.campaignMain.getIntegerConfig("SOLMediumInf");
        int numHInf = CampaignMain.campaignMain.getIntegerConfig("SOLHeavyInf");
        int numAInf = CampaignMain.campaignMain.getIntegerConfig("SOLAssaultInf");

        //protomechs
        int numLPM = CampaignMain.campaignMain.getIntegerConfig("SOLLightProtoMek");
        int numMPM = CampaignMain.campaignMain.getIntegerConfig("SOLMediumProtoMek");
        int numHPM = CampaignMain.campaignMain.getIntegerConfig("SOLHeavyProtoMek");
        int numAPM = CampaignMain.campaignMain.getIntegerConfig("SOLAssaultProtoMek");

        //BattleArmor
        int numLBA = CampaignMain.campaignMain.getIntegerConfig("SOLLightBattleArmor");
        int numMBA = CampaignMain.campaignMain.getIntegerConfig("SOLMediumBattleArmor");
        int numHBA = CampaignMain.campaignMain.getIntegerConfig("SOLHeavyBattleArmor");
        int numABA = CampaignMain.campaignMain.getIntegerConfig("SOLAssaultBattleArmor");

        //Aero
        int numLAero = CampaignMain.campaignMain.getIntegerConfig("SOLLightAero");
        int numMAero = CampaignMain.campaignMain.getIntegerConfig("SOLMediumAero");
        int numHAero = CampaignMain.campaignMain.getIntegerConfig("SOLHeavyAero");
        int numAAero = CampaignMain.campaignMain.getIntegerConfig("SOLAssaultAero");

        //for loops.
        for (int i = 0; i < numLMeks; i++) {
            units.addAll(this.getRandomUnit(Unit.MEK, Unit.LIGHT, houseName));
        }

        for (int i = 0; i < numMMeks; i++) {
            units.addAll(this.getRandomUnit(Unit.MEK, Unit.MEDIUM, houseName));
        }

        for (int i = 0; i < numHMeks; i++) {
            units.addAll(this.getRandomUnit(Unit.MEK, Unit.HEAVY, houseName));
        }

        for (int i = 0; i < numAMeks; i++) {
            units.addAll(this.getRandomUnit(Unit.MEK, Unit.ASSAULT, houseName));
        }

        for (int i = 0; i < numLVehs; i++) {
            units.addAll(this.getRandomUnit(Unit.VEHICLE, Unit.LIGHT, houseName));
        }

        for (int i = 0; i < numMVehs; i++) {
            units.addAll(this.getRandomUnit(Unit.VEHICLE, Unit.MEDIUM, houseName));
        }

        for (int i = 0; i < numHVehs; i++) {
            units.addAll(this.getRandomUnit(Unit.VEHICLE, Unit.HEAVY, houseName));
        }

        for (int i = 0; i < numAVehs; i++) {
            units.addAll(this.getRandomUnit(Unit.VEHICLE, Unit.ASSAULT, houseName));
        }

        for (int i = 0; i < numLInf; i++) {
            units.addAll(this.getRandomUnit(Unit.INFANTRY, Unit.LIGHT, houseName));
        }

        for (int i = 0; i < numMInf; i++) {
            units.addAll(this.getRandomUnit(Unit.INFANTRY, Unit.MEDIUM, houseName));
        }

        for (int i = 0; i < numHInf; i++) {
            units.addAll(this.getRandomUnit(Unit.INFANTRY, Unit.HEAVY, houseName));
        }

        for (int i = 0; i < numAInf; i++) {
            units.addAll(this.getRandomUnit(Unit.INFANTRY, Unit.ASSAULT, houseName));
        }

        for (int i = 0; i < numLPM; i++) {
            units.addAll(this.getRandomUnit(Unit.PROTOMEK, Unit.LIGHT, houseName));
        }

        for (int i = 0; i < numMPM; i++) {
            units.addAll(this.getRandomUnit(Unit.PROTOMEK, Unit.MEDIUM, houseName));
        }

        for (int i = 0; i < numHPM; i++) {
            units.addAll(this.getRandomUnit(Unit.PROTOMEK, Unit.HEAVY, houseName));
        }

        for (int i = 0; i < numAPM; i++) {
            units.addAll(this.getRandomUnit(Unit.PROTOMEK, Unit.ASSAULT, houseName));
        }

        for (int i = 0; i < numLBA; i++) {
            units.addAll(this.getRandomUnit(Unit.BATTLEARMOR, Unit.LIGHT, houseName));
        }

        for (int i = 0; i < numMBA; i++) {
            units.addAll(this.getRandomUnit(Unit.BATTLEARMOR, Unit.MEDIUM, houseName));
        }

        for (int i = 0; i < numHBA; i++) {
            units.addAll(this.getRandomUnit(Unit.BATTLEARMOR, Unit.HEAVY, houseName));
        }

        for (int i = 0; i < numABA; i++) {
            units.addAll(this.getRandomUnit(Unit.BATTLEARMOR, Unit.ASSAULT, houseName));
        }

        for (int i = 0; i < numLAero; i++) {
            units.addAll(this.getRandomUnit(Unit.AERO, Unit.LIGHT, houseName));
        }

        for (int i = 0; i < numMAero; i++) {
            units.addAll(this.getRandomUnit(Unit.AERO, Unit.MEDIUM, houseName));
        }

        for (int i = 0; i < numHAero; i++) {
            units.addAll(this.getRandomUnit(Unit.AERO, Unit.HEAVY, houseName));
        }

        for (int i = 0; i < numAAero; i++) {
            units.addAll(this.getRandomUnit(Unit.AERO, Unit.ASSAULT, houseName));
        }

        //now add the units to player and get a return string
        StringBuilder toReturn = new StringBuilder();
        for (SUnit currUnit : units) {

            //add the unit to the player and the tracker
            p.addUnit(currUnit, true, false);

            //construct the info string
            toReturn.append(currUnit.getVerboseModelName());

            toReturn.append(", ");
        }
        //remove last 2 chars (", ")
        if (units.size() > 0) {
            toReturn.delete(toReturn.length() - 2, toReturn.length());
        }

        return toReturn.toString();
    }//end getNewSOLUnits

    public void addResetPlayer(SPlayer p, Integer numResets) {
        this.resetPlayers.put(p.getName().toLowerCase(), numResets);
    }

    public int getResetsRemaining(SPlayer p) {
        return this.resetPlayers.get(p.getName().toLowerCase());
    }


}
