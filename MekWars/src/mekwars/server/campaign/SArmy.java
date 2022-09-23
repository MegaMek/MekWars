/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

/*
 * Created on 21.05.2004
 */
package server.campaign;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import common.Army;
import common.Unit;
import common.campaign.operations.Operation;
import common.util.MWLogger;
import common.util.TokenReader;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.VTOL;
import megamek.common.battlevalue.BvMultiplier;

/**
 * @author Helge Richter
 *
 */

public class SArmy extends Army {

    // VARIABLES
    private float rawForceSize = 0;
    private Vector<SArmy> opponents = new Vector<SArmy>(1, 1);
    private TreeMap<String, String> legalOperations = new TreeMap<String, String>();
    private String playerName = "";

    // CONSTRUCTORS
    public SArmy(String ownerName) {
        super();
        opponents = new Vector<SArmy>(1, 1);
        playerName = ownerName;
    }

    public SArmy(int id, String ownerName) {
        super();
        setID(id);
        opponents = new Vector<SArmy>(1, 1);
        playerName = ownerName;
    }

    // METHODS
    public void addUnit(SUnit u) {
        super.addUnit(u);
        super.setBV(0);
        setRawForceSize(0);
    }

    public void addUnit(SUnit u, int position) {
        super.addUnit(u, position);
        super.setBV(0);
        setRawForceSize(0);
    }

    public void removeUnit(int id) {

        Iterator<Unit> i = getUnits().iterator();
        while (i.hasNext()) {
            if (i.next().getId() == id) {
                i.remove();
                break;
            }
        }

        removeUnitFromC3Network(id);
        super.setBV(0);
        setRawForceSize(0);
        removeCommander(id);
    }

    public int getUnitPosition(int id) {
        Vector<Unit> v = getUnits();
        for (int i = 0; i < v.size(); i++) {
            SUnit unit = (SUnit) v.elementAt(i);
            if (unit.getId() == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return returns the raw force size (Force Mod Rule)
     */
    public float getRawForceSize() {

        // dont recalculate if it isnt necessary
        if (rawForceSize != 0) {
            return rawForceSize;
        }

        // no break, generate a raw force size
        for (Unit u : getUnits()) {
            if (u.getType() == Unit.INFANTRY) {
                rawForceSize += CampaignMain.cm.getFloatConfig("InfantryOperationsBVMod");
            } else if (u.getType() == Unit.VEHICLE) {
                rawForceSize += CampaignMain.cm.getFloatConfig("VehicleOperationsBVMod");
            } else if (u.getType() == Unit.BATTLEARMOR) {
                rawForceSize += CampaignMain.cm.getFloatConfig("BAOperationsBVMod");
            } else if (u.getType() == Unit.PROTOMEK) {
                rawForceSize += CampaignMain.cm.getFloatConfig("ProtoOperationsBVMod");
            } else if (u.getType() == Unit.AERO) {
                rawForceSize += CampaignMain.cm.getFloatConfig("AeroOperationsBVMod");
            } else {
                // all other allowed types have a 1.0 weight
                rawForceSize += CampaignMain.cm.getFloatConfig("MekOperationsBVMod");
            }
        }

        return rawForceSize;
    }// end getRawForceSize()

    /**
     * @param rfs
     *            - the forcesize to set (Operations Rule)
     */
    public void setRawForceSize(float rfs) {
        rawForceSize = rfs;
    }

    /**
     * @author Torren 2/23/2007 New Tech Manual rules on force Size. This
     *         returns the new <code>BV</code> of the <code>this</code> army
     *         which is considerd the larger force
     */
    public int getOperationsBV(SArmy OpposingForce) {

        // if not using the operations rules, return a normal BV.
        boolean usingOpRules = CampaignMain.cm.getBooleanConfig("UseOperationsRule");
        if (!usingOpRules) {
            return getBV();
        }

        if (OpposingForce == null) {
            return getBV();
        }

        double finalMultiplier = forceSizeModifier(OpposingForce);
        return (int) Math.round(getBV() * finalMultiplier);

    }// end getOperationsBV

    public boolean hasTAGAndHomingCombo() {

        boolean hasTAG = false;
        boolean hasHoming = false;

        try {
            for (Unit currU : getUnits()) {
                SUnit u = (SUnit) currU;
                if (u.hasTAG()) {
                    hasTAG = true;
                }
                if (u.hasHoming()) {
                    hasHoming = true;
                }

                // MWLogger.errLog(" Unit: "+u.getModelName()+" TAG:
                // "+hasTAG+" Homing: "+hasHoming);
                if (hasTAG && hasHoming) {
                    return true;
                }
            }
        } catch (Exception ex) {
            MWLogger.errLog("Bad unit in army for TAGandHomingCombo. Returning false.");
            MWLogger.errLog(ex);
            return false;
        }
        return false;
    }

    public boolean hasTAGAndSemiGuidedCombo() {

        boolean hasTAG = false;
        boolean hasSemiGuided = false;

        try {
            for (Unit currU : getUnits()) {
                SUnit u = (SUnit) currU;
                if (u.hasTAG()) {
                    hasTAG = true;
                }
                if (u.hasSemiGuided()) {
                    hasSemiGuided = true;
                }

                // MWLogger.errLog(" Unit: "+u.getModelName()+" TAG:
                // "+hasTAG+" SemiGuided: "+hasSemiGuided);
                if (hasTAG && hasSemiGuided) {
                    return true;
                }
            }
        } catch (Exception ex) {
            MWLogger.errLog("Bad unit in army for hasTAGAndSemiGuidedCombo. Returning false.");
            MWLogger.errLog(ex);
            return false;
        }
        return false;
    }

    public int getSemiGuidedBV() {
        int bv = 0;

        for (Unit currU : getUnits()) {
            SUnit unit = (SUnit) currU;
            for (Mounted ammo : unit.getEntity().getAmmo()) {
                if (((AmmoType) ammo.getType()).getMunitionType() == AmmoType.M_SEMIGUIDED) {
                    bv += ((AmmoType) ammo.getType()).getBV(unit.getEntity());
                }
            }
        }

        return bv;
    }

    @Override
    public int getBV() {
        if (super.getBV() == 0) {
            calcBV();
        }
        return super.getBV();
    }

    public void calcBV() {

        int total = 0;
        int subTotal = 0;
        double c3BV = 0;

        boolean hasTAGHomingCombo = hasTAGAndHomingCombo();
        boolean hasSemiGuided = hasTAGAndSemiGuidedCombo();

        for (Unit currU : getUnits()) {

            // Bad units in the queue(possible issues with rest. best to protect
            // now.
            if (currU == null) {
                continue;
            }

            SUnit u = (SUnit) currU;

            //c3BV = u.calcBV();
            
            c3BV = u.getBVForMatch();

            if (u.hasBeenC3LinkedTo(this) || getC3Network().get(u.getId()) != null) {
                int totalForceBV = 0;
                totalForceBV += u.getEntity().calculateBattleValue(true, true);
                for (Unit c3Unit : getUnits()) {
                    if (c3Unit == null) {
                        continue;
                    }

                    SUnit subUnit = (SUnit) c3Unit;

                    if (!u.equals(subUnit) && isSameC3Network(u.getId(), subUnit.getId())) {
                        totalForceBV += subUnit.getEntity().calculateBattleValue(true, true);
                    }
                }
                c3BV += totalForceBV *= 0.05;
            }

            subTotal = (int) Math.round(c3BV);

            // Arrow IV adjustments
            if (hasTAGHomingCombo) {
                double temp = subTotal / BvMultiplier.bvSkillMultiplier(u.getEntity().getCrew().getGunnery(), u.getEntity().getCrew().getPiloting());
                if (u.hasTAG()) {
                    temp += 200;
                }
                if (u.hasHoming()) {
                    temp += 200;
                }
                temp *= BvMultiplier.bvSkillMultiplier(u.getEntity().getCrew().getGunnery(), u.getEntity().getCrew().getPiloting());
                subTotal = (int) temp;
            }
            total += subTotal;
        }

        if (hasSemiGuided) {
            total += getSemiGuidedBV();
        }

        super.setBV(total);
    }

    /**
     * Method which compares two armies and returns a boolean which indicates
     * whether they fall within each others' unit limits and have a generic BV
     * match.
     */
    public boolean matches(SArmy enemy, Operation o) {
        int flatCap = o.getIntValue("MaxBVDifference");
        double percentCap = o.getDoubleValue("MaxBVPercent");

        // catch a 0 BV, just in case getBV(false) calls lead here
        if (enemy.getBV() == 0 && !o.getBooleanValue("MULArmiesOnly")) {
            return false;
        }

        // determine BV difference between the two armies
        int enemyOpBV = enemy.getOperationsBV(this);
        int myOpBV = getOperationsBV(enemy);
        int bvDiff = Math.abs(enemyOpBV - myOpBV);

        // percentage caps arent being used, only check the straight cap from
        // the params
        if (percentCap == 0) {
            if (bvDiff > flatCap) {
                return false;
            }
        }

        // percentage caps are being used. see which is larger
        // (percent or straight) and check as appropriate.
        else {

            double percentDiff = 0;

            // use smaller army to determine percentage; gives narrowest legal
            // range possible
            double smallestDiff = Math.min(enemyOpBV, myOpBV);
            double smallestBV = Math.min(enemyOpBV, myOpBV);
            double precentTotal = percentCap * smallestBV;

            percentDiff = bvDiff / smallestDiff;

            if (precentTotal < flatCap) {
                if (bvDiff > flatCap) {
                    return false;
                }
            } else {// percent cap is greater than flat
                if (percentDiff > percentCap) {
                    return false;
                }
            }
        }

        // BVs match - check limits of THIS army
        /*
         * boolean infCounts =
         * CampaignMain.cm.getBooleanConfig("CountInfForLimiters"); boolean
         * allowLimiters = CampaignMain.cm.getBooleanConfig("AllowLimiters"); if
         * (getLowerLimiter() != Army.NO_LIMIT && allowLimiters){
         *
         * int smallest = -1; int enemyNum = -1; if (infCounts) { smallest =
         * getAmountOfUnits() - Math.abs(getLowerLimiter()); enemyNum =
         * enemy.getAmountOfUnits(); } else { smallest =
         * getAmountOfUnitsWithoutInfantry() - Math.abs(getLowerLimiter());
         * enemyNum = enemy.getAmountOfUnitsWithoutInfantry(); }
         *
         * //check for 0 and negatives. if (smallest < 1) smallest = 1;
         *
         * if (enemyNum < smallest) return false; } if (getUpperLimiter() !=
         * Army.NO_LIMIT && allowLimiters){
         *
         * int highest = -1; int enemyNum = -1; if (infCounts) { highest =
         * getAmountOfUnits() + getUpperLimiter(); enemyNum =
         * enemy.getAmountOfUnits(); } else { highest =
         * getAmountOfUnitsWithoutInfantry() + getUpperLimiter(); enemyNum =
         * enemy.getAmountOfUnitsWithoutInfantry(); }
         *
         * if (enemyNum > highest) return false; }
         *
         * //Within Limits of the OTHER army? if (enemy.getLowerLimiter() !=
         * Army.NO_LIMIT && allowLimiters){
         *
         * int smallest = -1; int ownNum = -1; if (infCounts) { smallest =
         * enemy.getAmountOfUnits() - Math.abs(enemy.getLowerLimiter()); ownNum
         * = getAmountOfUnits(); } else { smallest =
         * enemy.getAmountOfUnitsWithoutInfantry() -
         * Math.abs(getLowerLimiter()); ownNum =
         * getAmountOfUnitsWithoutInfantry(); }
         *
         * if (smallest < 1) smallest = 1;
         *
         * if (ownNum < smallest) return false; } if (enemy.getUpperLimiter() !=
         * Army.NO_LIMIT && allowLimiters){
         *
         * int highest = -1; int ownNum = -1; if (infCounts) { highest =
         * enemy.getAmountOfUnits() + enemy.getUpperLimiter(); ownNum =
         * getAmountOfUnits(); } else { highest =
         * enemy.getAmountOfUnitsWithoutInfantry() + enemy.getUpperLimiter();
         * ownNum = getAmountOfUnitsWithoutInfantry(); }
         *
         * if (ownNum > highest) return false; }
         */
        return true;
    }// end matches()

    public int getAmountOfUnitsWithoutInfantry() {
        int total = 0;
        for (Unit unit : getUnits()) {
            if (unit.getType() != Unit.INFANTRY) {
                total++;
            }
        }
        return total;
    }

    public String getInaccurateDescription() {
    	if (CampaignMain.cm.getBooleanConfig("ShowUnitTypeCounts")) {
    		StringBuilder toReturn = new StringBuilder("(Units: ");
    		int numMechs = 0, numVees=0, numVTOLs=0, numInf=0, numProtos=0, numBA=0, numAero=0;
    		for (Unit unit : getUnits()) {
    			Entity e = CampaignMain.cm.getPlayer(playerName).getUnit(unit.getId()).getEntity();
    			if (e instanceof Mech) {
    				numMechs++;
    			} else if (e instanceof VTOL) {
    				numVTOLs++;
    			} else if (e instanceof Tank) {
    				numVees++;
    			} else if (e instanceof Infantry) {
    				numInf++;
    			} else if (e instanceof Aero) {
    				numAero++;
    			} else if (e instanceof BattleArmor) {
    				numBA++;
    			} else if (e instanceof Protomech) {
    				numProtos++;
    			}
    		}
    		int items = 0;
    		if (numMechs > 0) {
    			toReturn.append(numMechs + " Meks");
    			items++;
    		}
    		if (numVees > 0) {
    			if (items > 0) {
    				toReturn.append(", ");
    			}
    			toReturn.append(numVees + " Vees");
    			items++;
    		}
    		if (numVTOLs > 0) {
    			if (items > 0) {
    				toReturn.append(", ");
    			}
    			toReturn.append(numVTOLs + " VTOLs");
    			items++;
    		}
    		if (numInf > 0) {
    			if (items > 0) {
    				toReturn.append(", ");
    			}
    			toReturn.append(numInf + " Inf");
    			items++;
    		}
    		if (numBA > 0) {
    			if (items > 0) {
    				toReturn.append(", ");
    			}
    			toReturn.append(numBA + " BA");
    			items++;
    		}
    		if (numProtos > 0) {
    			if (items > 0) {
    				toReturn.append(", ");
    			}
    			toReturn.append(numProtos + " Protomechs");
    			items++;
    		}
    		if (numAero > 0) {
    			if (items > 0) {
    				toReturn.append(", ");
    			}
    			toReturn.append(numAero + " Aero");
    			items++;
    		}
    		toReturn.append(" / BV: " + getBV() + ")");
    		return toReturn.toString();
    	} else {
    		return "(Units: " + getAmountOfUnits() + " / BV: " + getBV() + ")";
    	}
    }

    /**
     * Special getDescription() which also shows an ID number. Used by SPlayer's
     * getStatus and the ShowToHouseCommand.
     */
    public String getDescription(boolean accurate, boolean showID, boolean idShouldLink) {

        String toReturn = "";
        if (accurate) {
            if (showID && !idShouldLink) {
                toReturn += "#" + getID();
            } else if (showID && idShouldLink) {
                toReturn += "<a href=\"MEKWARS/c sth#a#" + getID() + "\">#" + getID() + "</a>";
            }

            if (isDisabled()) {
                toReturn += " (disabled)";
            }
            toReturn += " - ";

            toReturn += this.getDescription(accurate);
        } else {
            toReturn += this.getDescription(accurate);
        }

        return toReturn;
    }

    public String getDescription(boolean accurate) {

        return getDescription(accurate, null);

    }

    public String getDescription(boolean accurate, SArmy opposingArmy) {
        if (accurate) {

            StringBuilder result = new StringBuilder();

            // only show a name if one is set
            if (getName().trim().length() != 0) {
                result.append("\"" + getName() + "\" - ");
            }

            Iterator<Unit> i = getUnits().iterator();
            while (i.hasNext()) {
                SUnit u = (SUnit) i.next();
                if (isCommander(u.getId())) {
                    result.append("<i>");
                }
                result.append(u.getSmallDescription());
                if (isCommander(u.getId())) {
                    result.append("</i>");
                }
                if (i.hasNext()) {
                    result.append(", ");
                }
            }
            result.append("; BV: " + getBV());

            if (opposingArmy != null && getBV() != getOperationsBV(opposingArmy)) {
                result.append(" (BV vs " + opposingArmy.getRawForceSize() + " units : " + getOperationsBV(opposingArmy) + ")");
            }

            return result.toString();
        }

        // else
        return getInaccurateDescription();
    }

    /**
     * Used by Operations to determine how many mines to assign to
     * attacker/defender, in lieu of BV.
     */
    public int getTotalTonnage() {
        int tonnage = 0;
        for (Unit currU : getUnits()) {
            tonnage += ((SUnit) currU).getEntity().getWeight();
        }
        return tonnage;
    }

    public void fromString(String s, String delimiter, SPlayer p) {
        StringTokenizer ST = new StringTokenizer(s, delimiter);
        setID(TokenReader.readInt(ST));
        setName(TokenReader.readString(ST));
        setLowerLimiter(TokenReader.readInt(ST));
        setUpperLimiter(TokenReader.readInt(ST));
        int count = TokenReader.readInt(ST);
        for (int i = 0; i < count; i++) {
            int id = TokenReader.readInt(ST);

            if (id != 0) {
                // already been replaced --Torren
                addUnit(p.getUnit(id));
            }
        }
        count = TokenReader.readInt(ST);
        for (int i = 0; i < count; i++) {
            int key = TokenReader.readInt(ST);
            int unit = TokenReader.readInt(ST);
            getC3Network().put(key, unit);
        }
        setOpForceSize(TokenReader.readFloat(ST));

        count = TokenReader.readInt(ST);
        for (int i = 0; i < count; i++) {
            int unit = TokenReader.readInt(ST);
            addCommander(unit);
        }
        boolean lock = TokenReader.readBoolean(ST);
        if (lock) {
            playerLockArmy();
        } else {
            playerUnlockArmy();
        }
        boolean disabled = TokenReader.readBoolean(ST);
        if (disabled) {
            disableArmy();
        } else {
            enableArmy();
        }
    }

    public String getMinimalInfo() {
        return getDescription(true);
    }

    public String getInfo() {
        return getDescription(true);
    }

    /**
     * Conduit which returns legal operations from the SArmyData. Note the lack
     * of a corresponding set().
     *
     * @return legalOperations
     */
    public TreeMap<String, String> getLegalOperations() {
        return legalOperations;
    }

    /*
     * Opponent Methods. Used to get, set, add and remove opposing forces.
     */
    public void setOpponents(Vector<SArmy> v) {
        opponents = v;
    }

    public Vector<SArmy> getOpponents() {
        return opponents;
    }

    public void addOpponent(SArmy a) {
        // for now, just tack it on to the list.
        // TODO: Sort by faction.
        try {
            opponents.add(a);
            opponents.trimToSize();
        } catch (Exception e) {
            MWLogger.errLog("Error adding army to opponentList. Trace follows.");
            MWLogger.errLog(e);
        }
    }// end addOpponent

    public void removeOpponent(SArmy a) {
        try {
            opponents.remove(a);
            opponents.trimToSize();
        } catch (Exception e) {
            MWLogger.errLog("Error removing army from opponentList. Trace follows.");
            MWLogger.errLog(e);
        }
    }// end removeOpponent()

    @Override
    public void setName(String name) {
        super.setName(name);

        if (name.trim().length() >= 0) {
            CampaignMain.cm.toUser("PL|RNA|" + getID() + "#" + name, getPlayerName(), false);
        }
    }

    public void setPlayerLock(int aid, boolean lock) {
        if (lock) {
            super.playerLockArmy();
            CampaignMain.cm.toUser("PL|LA|" + getID(), getPlayerName(), false);
        } else {
            super.playerUnlockArmy();
            CampaignMain.cm.toUser("PL|ULA|" + getID(), getPlayerName(), false);
        }
    }

    @Override
    public void toggleArmyDisabled() {
        super.toggleArmyDisabled();
        CampaignMain.cm.toUser("PL|TAD|" + getID(), getPlayerName(), false);
    }

    /*
     * Playername is stored by contructor in leiu of a complete backreference to
     * the SPlayer who owns the army (and, in turn, back to his faction). It is
     * used by checkattack to generate readible output like "Liao(4),
     * Davion(3)."
     *
     * Also used to generate lists of players who should receive notification
     * when SArmy's owner deactivates or joins a game and moves to
     * STATUS_FIGHTING.
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Override object's .equals().
     */
    @Override
    public boolean equals(Object o) {

        SArmy a = null;
        try {
            a = (SArmy) o;
        } catch (ClassCastException e) {
            return false;
        }

        if (a == null) {
            return false;
        }

        if (!a.getPlayerName().equals(getPlayerName())) {
            return false;
        }

        if (a.getID() != getID()) {
            return false;
        }

        // same owner and ID number, so same army.
        return true;
    }

    private boolean isLegalMekToInfantryRatio() {
        int infcount = 0;
        int mekcount = 0;
        for (Unit unit : getUnits()) {
            if (unit.getType() == Unit.INFANTRY) {
                infcount++;
            } else if (unit.getType() == Unit.MEK) {
                mekcount++;
            }
        }

        if (infcount == 0) {
            return true;
        }

        if (mekcount == 0) {
            return false;
        }

        int ratio = (infcount * 100) / mekcount;

        if (ratio > CampaignMain.cm.getIntegerConfig("MekToInfantryRatio")) {
            return false;
        }
        return true;
    }

    private boolean isLegalMekToVehicleRatio() {
        int veecount = 0;
        int mekcount = 0;
        for (Unit unit : getUnits()) {
            if (unit.getType() == Unit.VEHICLE) {
                veecount++;
            } else if (unit.getType() == Unit.MEK) {
                mekcount++;
            }
        }

        if (veecount == 0) {
            return true;
        }

        if (mekcount == 0) {
            return false;
        }

        int ratio = (veecount * 100) / mekcount;

        if (ratio > CampaignMain.cm.getIntegerConfig("MekToVehicleRatio")) {
            return false;
        }
        return true;
    }

    public void checkLegalRatio(String Username) {

        if (CampaignMain.cm.getBooleanConfig("AllowRatios")) {
            if (!isLegalMekToInfantryRatio()) {
                CampaignMain.cm.toUser("This army has an Illegal Mek to Infantry ratio and will not be allowed to participate in games.", Username, true);
            } else if (!isLegalMekToVehicleRatio()) {
                CampaignMain.cm.toUser("This army has an Illegal Mek to Vehicle ratio and will not be allowed to participate in games.", Username, true);
            } else {
                CampaignMain.cm.toUser("Army Ratio Checks", Username, true);
            }
        }
    }

    @Override
    public void setLowerLimiter(int lowerLimit) {

        int buffer = CampaignMain.cm.getIntegerConfig("LowerLimitBuffer");
        if (lowerLimit < buffer && lowerLimit != Army.NO_LIMIT) {
            lowerLimit = buffer;
            CampaignMain.cm.toUser("Army " + getID() + "'s lower limit set to " + buffer + ".", getPlayerName(), true);
            CampaignMain.cm.toUser("PL|SAB|" + getID() + "#" + getLowerLimiter() + "#" + getUpperLimiter(), getPlayerName(), false);
        }

        super.setLowerLimiter(lowerLimit);
    }

    @Override
    public void setUpperLimiter(int upperLimit) {

        int buffer = CampaignMain.cm.getIntegerConfig("UpperLimitBuffer");
        if (upperLimit < buffer && upperLimit != Army.NO_LIMIT) {
            upperLimit = buffer;
            CampaignMain.cm.toUser("Army " + getID() + "'s upper limit set to " + buffer + ".", getPlayerName(), true);
            CampaignMain.cm.toUser("PL|SAB|" + getID() + "#" + getLowerLimiter() + "#" + getUpperLimiter(), getPlayerName(), false);

        }

        super.setUpperLimiter(upperLimit);
    }

    public boolean isUnitInArmy(SUnit unit) {
        if (unit == null) {
            return false;
        }
        Vector<Unit> v = getUnits();
        for (int i = 0; i < v.size(); i++) {
            SUnit newUnit = (SUnit) v.elementAt(i);
            if (newUnit.equals(unit)) {
                return true;
            }
        }
        return false;
    }

    public double forceSizeModifier(SArmy opposingForce) {

        double myForceSize = 0;
        double opposingForceSize = 0;

        setRawForceSize(0);
        myForceSize = getRawForceSize();

        opposingForce.setRawForceSize(0);
        opposingForceSize = opposingForce.getRawForceSize();

        if (myForceSize > opposingForceSize) {
            return ((opposingForceSize / myForceSize) + (myForceSize / opposingForceSize)) - 1;
        }
        return 1.0;
    }

    public boolean hasPilotWithTooManySkills() {

        if (!CampaignMain.cm.getBooleanConfig("PlayersCanBuyPilotUpgrades")) {
            return false;
        }
        int maxPilotSkills = CampaignMain.cm.getIntegerConfig("MaxPilotUpgrades");

        if (maxPilotSkills == -1) {
            return false;
        }

        for (Unit unit : getUnits()) {
            if (unit.getPilot().getSkills().size() > maxPilotSkills) {
                return true;
            }
        }
        return false;
    }

}