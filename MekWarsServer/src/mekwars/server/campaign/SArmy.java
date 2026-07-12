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
package mekwars.server.campaign;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.battleValue.BVCalculator;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import megamek.logging.MMLogger;
import mekwars.common.Army;
import mekwars.common.Unit;
import mekwars.common.campaign.operations.Operation;
import mekwars.common.util.TokenReader;

/**
 * @author Helge Richter
 *
 */

public class SArmy extends Army {
    private final static MMLogger LOGGER = MMLogger.create(SArmy.class);

    private final TreeMap<String, String> legalOperations = new TreeMap<>();
    private final String playerName;
    private float rawForceSize = 0;
    private Vector<SArmy> opponents;

    // CONSTRUCTORS
    public SArmy(String ownerName) {
        super();
        opponents = new Vector<>(1, 1);
        playerName = ownerName;
    }

    public SArmy(int id, String ownerName) {
        super();
        setID(id);
        opponents = new Vector<>(1, 1);
        playerName = ownerName;
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
        Vector<Unit> units = getUnits();
        for (int i = 0; i < units.size(); i++) {
            SUnit unit = (SUnit) units.elementAt(i);
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

        // dont recalculate if it isn't necessary
        if (rawForceSize != 0) {
            return rawForceSize;
        }

        // no break, generate a raw force size
        for (Unit unit : getUnits()) {
            if (unit.getType() == Unit.INFANTRY) {
                rawForceSize += CampaignMain.campaignMain.getFloatConfig("InfantryOperationsBVMod");
            } else if (unit.getType() == Unit.VEHICLE) {
                rawForceSize += CampaignMain.campaignMain.getFloatConfig("VehicleOperationsBVMod");
            } else if (unit.getType() == Unit.BATTLEARMOR) {
                rawForceSize += CampaignMain.campaignMain.getFloatConfig("BAOperationsBVMod");
            } else if (unit.getType() == Unit.PROTOMEK) {
                rawForceSize += CampaignMain.campaignMain.getFloatConfig("ProtoOperationsBVMod");
            } else if (unit.getType() == Unit.AERO) {
                rawForceSize += CampaignMain.campaignMain.getFloatConfig("AeroOperationsBVMod");
            } else {
                // all other allowed types have a 1.0 weight
                rawForceSize += CampaignMain.campaignMain.getFloatConfig("MekOperationsBVMod");
            }
        }

        return rawForceSize;
    }// end getRawForceSize()

    /**
     * @param rawForceSize - the forcesize to set (Operations Rule)
     */
    public void setRawForceSize(float rawForceSize) {
        this.rawForceSize = rawForceSize;
    }

    /**
     * @author Torren 2/23/2007 New Tech Manual rules on force Size. This returns the new <code>BV</code> of the
     *       <code>this</code> army which is considerd the larger force
     */
    public int getOperationsBV(SArmy OpposingForce) {

        // if not using the operations rules, return a normal BV.
        boolean usingOpRules = CampaignMain.campaignMain.getBooleanConfig("UseOperationsRule");

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

                if (hasTAG && hasHoming) {
                    return true;
                }
            }
        } catch (Exception ex) {
            LOGGER.debug("Bad unit in army for TAGandHomingCombo. Returning false.");
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

                if (hasTAG && hasSemiGuided) {
                    return true;
                }
            }
        } catch (Exception ex) {
            LOGGER.debug("Bad unit in army for hasTAGAndSemiGuidedCombo. Returning false.");
            return false;
        }

        return false;
    }

    public int getSemiGuidedBV() {
        double bv = 0;

        for (Unit currU : getUnits()) {
            SUnit unit = (SUnit) currU;
            for (AmmoMounted ammo : unit.getEntity().getAmmo()) {
                if (ammo.getType().getMunitionType().contains(AmmoType.Munitions.M_SEMIGUIDED)) {
                    bv += ammo.getType().getBV(unit.getEntity());
                }
            }
        }

        return (int) Math.round(bv);
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
        int subTotal;
        double c3BV;

        boolean hasTAGHomingCombo = hasTAGAndHomingCombo();
        boolean hasSemiGuided = hasTAGAndSemiGuidedCombo();

        for (Unit currU : getUnits()) {

            // Bad units in the queue (possible issues with the rest. best to protect now.
            if (currU == null) {
                continue;
            }

            SUnit sUnit = (SUnit) currU;

            c3BV = sUnit.getBVForMatch();

            if (sUnit.hasBeenC3LinkedTo(this) || getC3Network().get(sUnit.getId()) != null) {
                int totalForceBV = 0;
                totalForceBV += sUnit.getEntity().calculateBattleValue(true, true);

                for (Unit c3Unit : getUnits()) {
                    if (c3Unit == null) {
                        continue;
                    }

                    SUnit subUnit = (SUnit) c3Unit;

                    if (!sUnit.equals(subUnit) && isSameC3Network(sUnit.getId(), subUnit.getId())) {
                        totalForceBV += subUnit.getEntity().calculateBattleValue(true, true);
                    }
                }
                c3BV += totalForceBV * 0.05;
            }

            subTotal = (int) Math.round(c3BV);

            // Arrow IV adjustments
            if (hasTAGHomingCombo) {
                double temp = subTotal /
                                    BVCalculator.bvSkillMultiplier(sUnit.getEntity().getCrew().getGunnery(),
                                          sUnit.getEntity().getCrew().getPiloting());
                if (sUnit.hasTAG()) {
                    temp += 200;
                }

                if (sUnit.hasHoming()) {
                    temp += 200;
                }

                temp *= BVCalculator.bvSkillMultiplier(sUnit.getEntity().getCrew().getGunnery(),
                      sUnit.getEntity().getCrew().getPiloting());

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
     * Method which compares two armies and returns a boolean which indicates whether they fall within each others' unit
     * limits and have a generic BV match.
     */
    public boolean matches(SArmy enemy, Operation operation) {
        int flatCap = operation.getIntValue("MaxBVDifference");
        double percentCap = operation.getDoubleValue("MaxBVPercent");

        // catch a 0 BV, just in case getBV(false) calls lead here
        if (enemy.getBV() == 0 && !operation.getBooleanValue("MULArmiesOnly")) {
            return false;
        }

        // determine BV difference between the two armies
        int enemyOpBV = enemy.getOperationsBV(this);
        int myOpBV = getOperationsBV(enemy);
        int bvDiff = Math.abs(enemyOpBV - myOpBV);

        // percentage caps aren't being used, only check the straight cap from
        // the params
        if (percentCap == 0) {
            return bvDiff <= flatCap;
        } else {
            double percentDiff;

            // use smaller army to determine percentage; gives narrowest legal
            // range possible
            double smallestDiff = Math.min(enemyOpBV, myOpBV);
            double smallestBV = Math.min(enemyOpBV, myOpBV);
            double precentTotal = percentCap * smallestBV;

            percentDiff = bvDiff / smallestDiff;

            if (precentTotal < flatCap) {
                return bvDiff <= flatCap;
            } else {// percent cap is greater than flat
                return !(percentDiff > percentCap);
            }
        }
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
        if (CampaignMain.campaignMain.getBooleanConfig("ShowUnitTypeCounts")) {
            StringBuilder toReturn = new StringBuilder("(Units: ");
            int numMechs = 0, numVees = 0, numVTOLs = 0, numInf = 0, numProtos = 0, numBA = 0, numAero = 0;

            for (Unit unit : getUnits()) {
                SUnit sUnit = CampaignMain.campaignMain.getPlayer(playerName).getUnit(unit.getId());

                if (sUnit != null) {
                    Entity entity = sUnit.getEntity();

                    if (entity instanceof Mek) {
                        numMechs++;
                    } else if (entity instanceof VTOL) {
                        numVTOLs++;
                    } else if (entity instanceof Tank) {
                        numVees++;
                    } else if (entity instanceof Infantry) {
                        numInf++;
                    } else if (entity instanceof Aero) {
                        numAero++;
                    } else if (entity instanceof BattleArmor) {
                        numBA++;
                    } else if (entity instanceof ProtoMek) {
                        numProtos++;
                    }
                }
            }

            int items = 0;

            if (numMechs > 0) {
                toReturn.append(numMechs).append(" Meks");
                items++;
            }

            if (numVees > 0) {
                if (items > 0) {
                    toReturn.append(", ");
                }

                toReturn.append(numVees).append(" Vees");
                items++;
            }

            if (numVTOLs > 0) {
                if (items > 0) {
                    toReturn.append(", ");
                }

                toReturn.append(numVTOLs).append(" VTOLs");
                items++;
            }

            if (numInf > 0) {
                if (items > 0) {
                    toReturn.append(", ");
                }
                toReturn.append(numInf).append(" Inf");
                items++;
            }

            if (numBA > 0) {
                if (items > 0) {
                    toReturn.append(", ");
                }
                toReturn.append(numBA).append(" BA");
                items++;
            }

            if (numProtos > 0) {
                if (items > 0) {
                    toReturn.append(", ");
                }

                toReturn.append(numProtos).append(" ProtoMeks");
                items++;
            }

            if (numAero > 0) {
                if (items > 0) {
                    toReturn.append(", ");
                }

                toReturn.append(numAero).append(" Aero");
            }

            toReturn.append(" / BV: ").append(getBV()).append(")");
            return toReturn.toString();
        } else {
            return String.format("(Units: %s / BV: %s)", getAmountOfUnits(), getBV());
        }
    }

    /**
     * Special getDescription() which also shows an ID number. Used by SPlayer's getStatus and the ShowToHouseCommand.
     */
    public String getDescription(boolean accurate, boolean showID, boolean idShouldLink) {
        String toReturn = "";

        if (accurate) {
            if (showID && !idShouldLink) {
                toReturn += String.format("#%s", getID());
            } else if (showID) {
                toReturn += String.format("<a href=\"MEKWARS/c sth#a#%s\">#%s</a>", getID(), getID());
            }

            if (isDisabled()) {
                toReturn += " (disabled)";
            }

            toReturn += " - ";
        }

        toReturn += this.getDescription(accurate);

        return toReturn;
    }

    public String getDescription(boolean accurate) {
        return getDescription(accurate, null);
    }

    public String getDescription(boolean accurate, SArmy opposingArmy) {
        if (accurate) {
            StringBuilder result = new StringBuilder();

            // only show a name if one is set
            if (!getName().trim().isEmpty()) {
                result.append("\"").append(getName()).append("\" - ");
            }

            Iterator<Unit> iterator = getUnits().iterator();
            while (iterator.hasNext()) {
                SUnit next = (SUnit) iterator.next();

                if (isCommander(next.getId())) {
                    result.append("<iterator>");
                }

                result.append(next.getSmallDescription());

                if (isCommander(next.getId())) {
                    result.append("</iterator>");
                }

                if (iterator.hasNext()) {
                    result.append(", ");
                }
            }

            result.append("; BV: ").append(getBV());

            if (opposingArmy != null && getBV() != getOperationsBV(opposingArmy)) {
                result.append(" (BV vs ")
                      .append(opposingArmy.getRawForceSize())
                      .append(" units : ")
                      .append(getOperationsBV(opposingArmy))
                      .append(")");
            }

            return result.toString();
        }

        // else
        return getInaccurateDescription();
    }

    /**
     * Used by Operations to determine how many mines to assign to attacker/defender, in lieu of BV.
     */
    public int getTotalTonnage() {
        double tonnage = 0;

        for (Unit currU : getUnits()) {
            tonnage += ((SUnit) currU).getEntity().getWeight();
        }

        return (int) Math.round(tonnage);
    }

    public void fromString(String string, String delimiter, SPlayer sPlayer) {
        StringTokenizer stringTokenizer = new StringTokenizer(string, delimiter);
        setID(TokenReader.readInt(stringTokenizer));
        setName(TokenReader.readString(stringTokenizer));
        setLowerLimiter(TokenReader.readInt(stringTokenizer));
        setUpperLimiter(TokenReader.readInt(stringTokenizer));
        int count = TokenReader.readInt(stringTokenizer);

        for (int i = 0; i < count; i++) {
            int id = TokenReader.readInt(stringTokenizer);

            if (id != 0) {
                addUnit(sPlayer.getUnit(id));
            }
        }

        count = TokenReader.readInt(stringTokenizer);
        for (int i = 0; i < count; i++) {
            int key = TokenReader.readInt(stringTokenizer);
            int unit = TokenReader.readInt(stringTokenizer);
            getC3Network().put(key, unit);
        }

        setOpForceSize(TokenReader.readFloat(stringTokenizer));

        count = TokenReader.readInt(stringTokenizer);

        for (int i = 0; i < count; i++) {
            int unit = TokenReader.readInt(stringTokenizer);
            addCommander(unit);
        }

        boolean lock = TokenReader.readBoolean(stringTokenizer);

        if (lock) {
            playerLockArmy();
        } else {
            playerUnlockArmy();
        }

        boolean disabled = TokenReader.readBoolean(stringTokenizer);

        if (disabled) {
            disableArmy();
        } else {
            enableArmy();
        }
    }

    @Override
    public void setName(String name) {
        super.setName(name);

        CampaignMain.campaignMain.toUser(String.format("PL|RNA|%s#%s", getID(), name), getPlayerName(), false);
    }

    @Override
    public void setLowerLimiter(int lowerLimit) {
        int buffer = CampaignMain.campaignMain.getIntegerConfig("LowerLimitBuffer");
        if (lowerLimit < buffer && lowerLimit != Army.NO_LIMIT) {
            lowerLimit = buffer;
            CampaignMain.campaignMain.toUser(String.format("Army %s's lower limit set to %s.", getID(), buffer),
                  getPlayerName(),
                  true);
            CampaignMain.campaignMain.toUser(String.format("PL|SAB|%s#%s#%s", getID(), getLowerLimiter(), getUpperLimiter()),
                  getPlayerName(),
                  false);
        }

        super.setLowerLimiter(lowerLimit);
    }

    @Override
    public void setUpperLimiter(int upperLimit) {
        int buffer = CampaignMain.campaignMain.getIntegerConfig("UpperLimitBuffer");
        if (upperLimit < buffer && upperLimit != Army.NO_LIMIT) {
            upperLimit = buffer;
            CampaignMain.campaignMain.toUser(String.format("Army %s's upper limit set to %s.", getID(), buffer),
                  getPlayerName(),
                  true);
            CampaignMain.campaignMain.toUser(String.format("PL|SAB|%s#%s#%s", getID(), getLowerLimiter(), getUpperLimiter()),
                  getPlayerName(),
                  false);

        }

        super.setUpperLimiter(upperLimit);
    }

    // METHODS
    public void addUnit(SUnit u) {
        super.addUnit(u);
        super.setBV(0);
        setRawForceSize(0);
    }

    /*
     * Player name is stored by construction in lieu of a complete backreference to the SPlayer who owns the army
     * (and, in turn, back to his faction). It is used by check attack to generate readable output like "Liao(4),
     * Davion(3)."
     *
     * Also used to generate lists of players who should receive notification when SArmy's owner deactivates or joins
     *  a game and moves to STATUS_FIGHTING.
     */
    public String getPlayerName() {
        return playerName;
    }

    public String getMinimalInfo() {
        return getDescription(true);
    }

    public String getInfo() {
        return getDescription(true);
    }

    /**
     * Conduit that returns legal operations from the SArmyData. Note the lack of a corresponding set().
     *
     * @return legalOperations
     */
    public TreeMap<String, String> getLegalOperations() {
        return legalOperations;
    }

    public Vector<SArmy> getOpponents() {
        return opponents;
    }

    /*
     * Opponent Methods. Used to get, set, add and remove opposing forces.
     */
    public void setOpponents(Vector<SArmy> sArmies) {
        opponents = sArmies;
    }

    public void addOpponent(SArmy sArmy) {
        // for now, just tack it on to the list.
        // TODO: Sort by faction.
        try {
            opponents.add(sArmy);
            opponents.trimToSize();
        } catch (Exception e) {
            LOGGER.debug("Error adding army to opponentList. Trace follows.");
        }
    }// end addOpponent

    public void removeOpponent(SArmy sArmy) {
        try {
            opponents.remove(sArmy);
            opponents.trimToSize();
        } catch (Exception e) {
            LOGGER.debug("Error removing army from opponentList. Trace follows.");
        }
    }// end removeOpponent()

    public void setPlayerLock(int aid, boolean lock) {
        if (lock) {
            super.playerLockArmy();
            CampaignMain.campaignMain.toUser(String.format("PL|LA|%s", getID()), getPlayerName(), false);
        } else {
            super.playerUnlockArmy();
            CampaignMain.campaignMain.toUser(String.format("PL|ULA|%s", getID()), getPlayerName(), false);
        }
    }

    @Override
    public void toggleArmyDisabled() {
        super.toggleArmyDisabled();
        CampaignMain.campaignMain.toUser(String.format("PL|TAD|%s", getID()), getPlayerName(), false);
    }

    /**
     * Override object's .equals().
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof SArmy incoming) {
            if (!incoming.getPlayerName().equals(getPlayerName())) {
                return false;
            }

            return incoming.getID() == getID();
        }

        return false;
    }

    public void checkLegalRatio(String Username) {
        if (CampaignMain.campaignMain.getBooleanConfig("AllowRatios")) {
            if (!isLegalMekToInfantryRatio()) {
                CampaignMain.campaignMain.toUser(
                      "This army has an Illegal Mek to Infantry ratio and will not be allowed to participate in games.",
                      Username,
                      true);
            } else if (!isLegalMekToVehicleRatio()) {
                CampaignMain.campaignMain.toUser(
                      "This army has an Illegal Mek to Vehicle ratio and will not be allowed to participate in games.",
                      Username,
                      true);
            } else {
                CampaignMain.campaignMain.toUser("Army Ratio Checks", Username, true);
            }
        }
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

        return ratio <= CampaignMain.campaignMain.getIntegerConfig("MekToInfantryRatio");
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

        return ratio <= CampaignMain.campaignMain.getIntegerConfig("MekToVehicleRatio");
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
        if (!CampaignMain.campaignMain.getBooleanConfig("PlayersCanBuyPilotUpgrades")) {
            return false;
        }

        int maxPilotSkills = CampaignMain.campaignMain.getIntegerConfig("MaxPilotUpgrades");

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
