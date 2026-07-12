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

package mekwars.server.campaign.operations;

import common.Unit;
import megamek.common.IEntityRemovalConditions;
import megamek.common.Mech;
import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;


public class OperationEntity {
    private static final MMLogger LOGGER = MMLogger.create(OperationEntity.class);

    //IVARS
    private String ownerName;

    private int CTint = 1;
    private int HDint = 1;
    private int LLint = 1;
    private int RLint = 1;
    private int RAint = 1;
    private int LAint = 1;
    private int gyrohits = 0;
    private int pilothits = 0;
    private int offBoardRange = 0;
    private int cockpitType = Mech.COCKPIT_STANDARD;

    private boolean isSalvage = false;
    private boolean pilotUnconscious = false;
    private boolean isImmobile = false;//for correct forceSalvage on vehs
    private boolean crewDead = false;//for correct forceSalvage on vehs

    private int MMUnitType;
    private int ID;
    private int RemovalReason;
    private String unitFileName = "";
    private String unitDamage = "";

    private java.util.Vector<Integer> kills = new java.util.Vector<Integer>(1, 1);

    //CONSTRUCTOR

    /**
     * Primary constructor. Uses autoreport strings.
     */
    public OperationEntity(String s) {

        try {
            java.util.StringTokenizer ST = new java.util.StringTokenizer(s, "*");
            ID = Integer.parseInt(ST.nextToken());

            //get the player's name from the unit string
            java.util.StringTokenizer nameT = new java.util.StringTokenizer(ST.nextToken(), "~");
            String pname = "";
            while (nameT.hasMoreElements()) {pname = nameT.nextToken().trim();}

            ownerName = pname;
            pilothits = Integer.parseInt(ST.nextToken());
            RemovalReason = Integer.parseInt(ST.nextToken());
            MMUnitType = Integer.parseInt(ST.nextToken());

            //get the unit's kills
            java.util.StringTokenizer STR = new java.util.StringTokenizer(ST.nextToken(), "~");
            while (STR.hasMoreElements()) {
                String kill = STR.nextToken();
                if (!kill.trim().equals("")) {
                    Integer killid = Integer.parseInt(kill);
                    kills.add(killid);
                }
            }

            //if mech, load internals
            if (MMUnitType == Unit.MEK || MMUnitType == Unit.QUAD) {
                pilotUnconscious = Boolean.parseBoolean(ST.nextToken());
                CTint = Integer.parseInt(ST.nextToken());
                HDint = Integer.parseInt(ST.nextToken());
                LLint = Integer.parseInt(ST.nextToken());
                RLint = Integer.parseInt(ST.nextToken());
                LAint = Integer.parseInt(ST.nextToken());
                RAint = Integer.parseInt(ST.nextToken());
                gyrohits = Integer.parseInt(ST.nextToken());
                cockpitType = Integer.parseInt(ST.nextToken());
                if (CampaignMain.campaignMain.isUsingAdvanceRepair() && ST.hasMoreElements()) {
                    unitDamage = ST.nextToken();
                }
                unitFileName = ST.nextToken();
                server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(ownerName);
                if (player != null) {
                    server.campaign.SUnit currUnit = player.getUnit(ID);
                    if (currUnit != null) {currUnit.setLastCombatPilot(currUnit.getPilot().getPilotId());}
                }

            }

            //if vehicle, check salvage flag
            else if (MMUnitType == Unit.VEHICLE) {
                isSalvage = Boolean.parseBoolean(ST.nextToken());
                isImmobile = Boolean.parseBoolean(ST.nextToken());
                crewDead = Boolean.parseBoolean(ST.nextToken());
                server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(ownerName);
                if (CampaignMain.campaignMain.isUsingAdvanceRepair() && ST.hasMoreElements()) {
                    unitDamage = ST.nextToken();
                }
                if (player != null && ID != -1) {
                    server.campaign.SUnit currUnit = player.getUnit(ID);
                    if (currUnit != null && currUnit.getPilot() != null) //auto-assigned artillery throwing NPEs
                    {currUnit.setLastCombatPilot(currUnit.getPilot().getPilotId());}
                }
                unitFileName = ST.nextToken();
            } else if (MMUnitType == Unit.AERO) {
                isSalvage = Boolean.parseBoolean(ST.nextToken());
                isImmobile = Boolean.parseBoolean(ST.nextToken());
                crewDead = Boolean.parseBoolean(ST.nextToken());
                server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(ownerName);
                if (player != null && ID != -1) {
                    server.campaign.SUnit currUnit = player.getUnit(ID);
                    if (currUnit != null && currUnit.getPilot() != null) //auto-assigned artillery throwing NPEs
                    {currUnit.setLastCombatPilot(currUnit.getPilot().getPilotId());}
                }
                unitFileName = ST.nextToken();
            }

            /*
             * Note: don't need to do anything special for infantry.
             */
            //else if (mmUnitType == SUnit.INFANTRY)

            //check range. used to determine whether or not arty is overun.
            if (ST.hasMoreElements()) {this.setOffBoardRange(Integer.parseInt(ST.nextToken()));}
        } catch (Exception ex) {
            LOGGER.error("Error while parsing the following String: " + s);
            LOGGER.error(ex, "");
        }
    }//end OperationEntity()

    /**
     * Secondary constructor. Uses information from DeathTree.
     */
    public OperationEntity(String playername, int unitID, int removalReason, int ctIS, int headIS, boolean repairable) {

        //same for all states
        ID = unitID;
        RemovalReason = removalReason;
        CTint = ctIS;
        HDint = headIS;
        isSalvage = repairable;
        ownerName = playername;

        try {
            MMUnitType = CampaignMain.campaignMain.getPlayer(ownerName).getUnit(ID).getType();
        } catch (Exception ex) {}//unit is not owned by the player. most likely auto artillary or gun emplacements.

    }//end Secondary Constructor

    /**
     * @return the entity's kills.
     */
    public java.util.Vector<Integer> getKills() {
        return kills;
    }

    /**
     * @return The IS remaning for entity's head.
     */
    public int getHDint() {
        return HDint;
    }

    /**
     * @return the number of hits taken by the pilot
     */
    public int getPilothits() {
        return pilothits;
    }

    public void setPilothits(int hits) {
        pilothits = hits;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public server.campaign.SPlayer getOwner() {
        return CampaignMain.campaignMain.getPlayer(ownerName);
    }

    /**
     * @return the entity's unit type in MM.
     */
    public int getType() {
        return MMUnitType;
    }

    /**
     * The entity's ID. Megamek External ID.
     */
    public int getID() {
        return ID;
    }

    public boolean isSalvagable() {


        if (this.getRemovalReason() == IEntityRemovalConditions.REMOVE_DEVASTATED) {return false;}

        if (CampaignMain.campaignMain.getBooleanConfig("UsePartsRepair") &&
                  (MMUnitType == Unit.MEK || MMUnitType == Unit.QUAD || MMUnitType == Unit.VEHICLE)) {return true;}

        if (MMUnitType == Unit.MEK || MMUnitType == Unit.QUAD) {return (this.getCTint() > 0);} else if (MMUnitType ==
                                                                                                              Unit.VEHICLE) {
            return isSalvage;
        }

        //Infantry can never be salvaged (unless we introduce necromancers?)
        //This includes Protos and BA.
        return false;
    }

    public int getRemovalReason() {
        return RemovalReason;
    }

    public void setRemovalReason(int reason) {
        RemovalReason = reason;
    }

    /**
     * @return the entity's CT internals
     */
    public int getCTint() {
        return CTint;
    }

    /**
     * @param new CT internals to set. used to kill offboard artillery in ShortResolver
     */
    public void setCTint(int newInternals) {
        CTint = newInternals;
    }

    public boolean isLiving() {

        if (this.getCTint() <= 0) {return false;}

        switch (this.getRemovalReason()) {
            //destroyed, devastated, ejected and salvage units aren't living
            case IEntityRemovalConditions.REMOVE_DEVASTATED:
            case IEntityRemovalConditions.REMOVE_EJECTED:
            case IEntityRemovalConditions.REMOVE_SALVAGEABLE:
                return false;
            //we can assume that any unit which makes it off map IS living.
            case IEntityRemovalConditions.REMOVE_IN_RETREAT:
            case IEntityRemovalConditions.REMOVE_PUSHED:
                return true;
        }


        return canStand();
    }

    public boolean canStand() {

        /*
         * Retreating units have already been returned as living. Anything on board
         * with no leg or a destroyed gyro is presumed salvageable. Vehicles which
         * are immobile are also returned as potential salvage.
         */
        if (CampaignMain.campaignMain.getBooleanConfig("ForceSalvage")) {
            if (MMUnitType == Unit.MEK) {
                if (this.getLLint() <= 0 || this.getRLint() <= 0 || this.getGyrohits() >= 2) {return false;}
            } else if (MMUnitType == Unit.QUAD) {
                //Quads
                int missingLegsCount = 0;
                if (getLLint() <= 0) {missingLegsCount++;}
                if (getRLint() <= 0) {missingLegsCount++;}
                if (getRAint() <= 0) {missingLegsCount++;}
                if (getLAint() <= 0) {missingLegsCount++;}
                if (missingLegsCount >= 2 || this.getGyrohits() >= 2) {return false;}
            } else if (MMUnitType == Unit.VEHICLE) {
                return !isImmobile;
            }
        }

        return true;
    }

    /**
     * @return Returns the lLint.
     */
    public int getLLint() {
        return LLint;
    }

    /**
     * @return Returns the rLint.
     */
    public int getRLint() {
        return RLint;
    }

    /**
     * @return Returns the gyrohits.
     */
    public int getGyrohits() {
        return gyrohits;
    }

    /**
     * @return Returns the rAint.
     */
    public int getRAint() {
        return RAint;
    }

    /**
     * @return Returns the lAint.
     */
    public int getLAint() {
        return LAint;
    }

    /**
     * @return true if this is a Mech with one or more missing legs
     */
    public boolean isLegged() {
        if (MMUnitType == Unit.MEK) {
            return (this.getLLint() <= 0 || this.getRLint() <= 0);
        } else if (MMUnitType == Unit.QUAD) {
            //Quads
            int missingLegsCount = 0;
            if (getLLint() <= 0) {missingLegsCount++;}
            if (getRLint() <= 0) {missingLegsCount++;}
            if (getRAint() <= 0) {missingLegsCount++;}
            if (getLAint() <= 0) {missingLegsCount++;}
            if (missingLegsCount >= 2) {return true;}
        }
        return false;
    }

    /**
     * @return true if this is a Mech with a missing gyro, or an immobile vehicle
     */
    public boolean isGyroed() {
        if (MMUnitType == Unit.MEK || MMUnitType == Unit.QUAD) {
            return (this.getGyrohits() >= 2);
        } else if (MMUnitType == Unit.VEHICLE) {
            return isImmobile;
        }
        return false;
    }

    /**
     * @return Returns the isSalvage.
     */
    public boolean isSalvage() {
        return isSalvage;
    }

    /**
     * @param boolean isSalvage. Used to set salvage to false when overrun.
     */
    public void setSalvage(boolean newSalvage) {
        isSalvage = newSalvage;
    }

    /**
     * @return Returns the crew's status. Should only use when checking veh salvage.
     */
    public boolean isCrewDead() {
        return crewDead;
    }

    /**
     * @return Returns the pilotUnconscious.
     */
    public boolean isPilotUnconscious() {
        return pilotUnconscious;
    }

    public boolean isOffBoard() {
        return offBoardRange > 0;
    }

    public int getOffBoardRange() {
        return offBoardRange;
    }

    public void setOffBoardRange(int range) {
        offBoardRange = range;
    }

    public int getCockpitType() {
        return cockpitType;
    }

    public String getUnitDamage() {
        return unitDamage;
    }

    public String getUnitFileName() {
        return unitFileName;
    }

}//end OperationEntity
