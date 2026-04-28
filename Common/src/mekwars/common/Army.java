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

/*
 * Created on 21.05.2004
 *
 */
package mekwars.common;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * A virtual army which can contain any combination of units
 *
 * @author Helge Richter
 *
 */
public class Army {

    // STATIC VARIABLES
    public static final int NO_LIMIT = -1;

    // VARIABLES
    private final Vector<Unit> units = new Vector<>(1, 1);
    private final Vector<Integer> commanders = new Vector<>(1, 1);
    private String name = " ";
    private int upperLimiter = NO_LIMIT;
    private int lowerLimiter = NO_LIMIT;
    private int bv = 0;
    private int id;
    private boolean locked = false;
    private boolean armyPlayerLocked = false; // Used by players to keep armies
    // from being cleared
    private boolean armyDisabled = false;
    private float opForceSize = NO_LIMIT;
    private Hashtable<Integer, Integer> c3Network = new Hashtable<>();

    // CONSTRUCTORS
    public Army() {
        // no content
    }

    // METHODS
    public int getAmountOfUnits() {
        return units.size();
    }

    public boolean isDisabled() {
        return armyDisabled;
    }

    public void disableArmy() {
        armyDisabled = true;
    }

    public void enableArmy() {
        armyDisabled = false;
    }

    public void toggleArmyDisabled() {
        armyDisabled = !armyDisabled;
    }

    public boolean isPlayerLocked() {
        return armyPlayerLocked;
    }

    public void playerLockArmy() {
        armyPlayerLocked = true;
    }

    public void playerUnlockArmy() {
        armyPlayerLocked = false;
    }

    /**
     * Add a unit to a specific position.
     *
     */
    public void addUnit(Unit unit, int Position) {
        units.add(Position, unit);
    }

    /**
     * add units to the army vector.
     *
     */
    public void addUnit(Unit unit) {
        units.add(unit);
    }

    /**
     * This will pull The number of unit types this army holds i.e. type = Unit.MEK all meks will be counted.
     *
     * @param type The unit type to check against. MEK VEHICLE
     *
     * @return number of unit type that exist in this army
     */
    public int getNumberOfUnitTypes(int type) {
        int count = 0;

        for (Unit unit : getUnits()) {
            if (unit.getType() == type) {
                count++;
            }
        }

        return count;
    }

    /**
     * @return Returns the units.
     */
    public Vector<Unit> getUnits() {
        return units;
    }

    /**
     * This will pull The number of unit types this army holds i.e. type = Unit.MEK all meks will be counted.
     *
     * @param type         The unit type to check against.
     * @param countSupport Whether to count Support Units.
     *
     */

    public int getNumberOfUnitTypes(int type, boolean countSupport) {
        int count = 0;

        for (Unit unit : getUnits()) {
            if (unit.getType() == type) {
                if (!unit.isSupportUnit() || (unit.isSupportUnit() && countSupport)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * This method will return the total number of support units in the army
     *
     * @return Total number of support units in the army
     */
    public int getTotalSupportUnits() {
        int count = 0;
        for (Unit unit : getUnits()) {
            if (unit.isSupportUnit()) {
                count++;
            }
        }
        return count;
    }

    public String toString(boolean toClient, String delimiter) {
        StringBuilder result = new StringBuilder();
        result.append(getID());
        result.append(delimiter);
        if (toClient) {
            result.append(getBV());
            result.append(delimiter);
            result.append(isLocked());
            result.append(delimiter);
        }
        if (!getName().isEmpty()) {
            result.append(getName());
        } else {
            result.append(" ");
        }
        result.append(delimiter);
        result.append(getLowerLimiter());
        result.append(delimiter);
        result.append(getUpperLimiter());
        result.append(delimiter);
        result.append(getUnits().size());
        result.append(delimiter);
        for (Unit unit : getUnits()) {
            result.append(unit.getId());
            result.append(delimiter);
        }
        result.append(delimiter);
        result.append(getC3Network().size());
        result.append(delimiter);
        for (Integer currI : getC3Network().keySet()) {
            result.append(currI);
            result.append(delimiter);
            result.append(getC3Network().get(currI));
            result.append(delimiter);
        }
        result.append(opForceSize);
        result.append(delimiter);

        result.append(commanders.size());
        result.append(delimiter);
        for (Integer unitId : commanders) {
            result.append(unitId);
            result.append(delimiter);
        }
        result.append(armyPlayerLocked);
        result.append(delimiter);
        result.append(armyDisabled);
        result.append(delimiter);
        return result.toString();
    }

    /**
     * @return Returns the iD.
     */
    public int getID() {
        return id;
    }

    /**
     * @return return the BV.
     */
    public int getBV() {
        return Math.max(bv, 0);
    }

    /**
     * @return Returns the locked.
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * @param locked The locked to set.
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns the lowerLimit.
     */
    public int getLowerLimiter() {
        return lowerLimiter;
    }

    /**
     * @param lowerLimit The lowerLimit to set.
     */
    public void setLowerLimiter(int lowerLimit) {
        lowerLimiter = lowerLimit;
    }

    /**
     * @return Returns the upperLimit.
     */
    public int getUpperLimiter() {
        return upperLimiter;
    }

    /**
     * @param upperLimit The upperLimit to set.
     */
    public void setUpperLimiter(int upperLimit) {
        upperLimiter = upperLimit;
    }

    /**
     * @return Returns the C3Networks.
     */
    public Hashtable<Integer, Integer> getC3Network() {
        return c3Network;
    }

    /**
     * @param c3Network The C3Networks to set.
     */
    public void setC3Network(Hashtable<Integer, Integer> c3Network) {
        this.c3Network = c3Network;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name.trim();
    }

    /**
     * @param bv The bV to set.
     */
    public void setBV(int bv) {
        this.bv = bv;
    }

    /**
     * @param id The iD to set.
     */
    public void setID(int id) {
        this.id = id;
    }

    public void removeUnitFromC3Network(int unitID) {

        if (getC3Network().get(unitID) != null) {
            getC3Network().remove(unitID);
            return;
        }

        Iterator<Integer> i = getC3Network().keySet().iterator();
        while (i.hasNext()) {
            Integer slave = i.next();
            Integer master = getC3Network().get(slave);
            if (master == unitID) {
                i.remove();
            }
        }

    }

    /**
     * Finds out if unitOne and unitTwo are in the ame c3 Network
     *
     */
    public boolean isSameC3Network(int unitOne, int unitTwo) {

        if (getC3Network().containsKey(unitOne) && getC3Network().get(unitOne) == unitTwo) {
            return true;
        }

        if (getC3Network().containsKey(unitTwo) && getC3Network().get(unitTwo) == unitOne) {
            return true;
        }

        Integer networkOne = getC3Network().get(unitOne);
        Integer networkTwo = getC3Network().get(unitTwo);

        return networkOne != null && networkOne.equals(networkTwo);
    }

    /**
     * Return the number of C3 networks in this army.
     *
     */
    public int getNumberOfNetworks() {
        int count = 0;

        for (int uid : c3Network.values()) {

            try {
                Unit master = getUnit(uid);
                if (!c3Network.containsKey(uid) && master.hasBeenC3LinkedTo(this)) {
                    count++;
                }
            } catch (Exception ex) {
            }

        }

        return Math.max(1, count);
    }

    public Unit getUnit(int unitId) {

        for (Unit currU : getUnits()) {
            if (currU.getId() == unitId) {
                return currU;
            }
        }

        return null;
    }

    public float getOpForceSize() {
        return opForceSize;
    }

    public void setOpForceSize(float force) {
        opForceSize = force;
    }

    public Vector<Integer> getCommanders() {
        return commanders;
    }

    public void removeCommander(int id) {
        commanders.removeElement(id);
        commanders.trimToSize();
    }

    public void addCommander(int id) {
        if (isCommander(id)) {
            return;
        }
        commanders.add(id);
        commanders.trimToSize();
    }

    public boolean isCommander(int id) {
        return commanders.contains(id);
    }

}
