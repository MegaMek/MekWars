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
 * A virtual army which can contain any combination of units.
 * <p>
 * In MekWars, an "army" is a named grouping of {@link Unit}s a player organizes for deployment into a game
 * (a "force"), together with bookkeeping shared by both sides of the network protocol: total Battle Value,
 * lock state (whether the composition can still be edited), min/max unit-count limiters, an optional operating
 * force size cap, designated commander units, and the C3 network link table (which unit is slaved to which C3
 * master within this army).
 * <p>
 * This class is the protocol-agnostic base class for the army data model: it is extended by
 * {@code mekwars.common.campaign.CArmy} on the client (adding client/session-specific bits such as legal
 * operations and a force-size-modifier cache) and by {@code mekwars.server.campaign.SArmy} on the server (adding
 * authoritative game-logic such as roster validation, persistence, and BV recalculation). Code here operates
 * purely on the fields declared in this class and the public API of {@link Unit}.
 *
 * @author Helge Richter
 *
 */
public class Army {

    // STATIC VARIABLES
    /** Sentinel value meaning "no limit is set" for {@link #upperLimiter}, {@link #lowerLimiter}, and {@link #opForceSize}. */
    public static final int NO_LIMIT = -1;

    // VARIABLES
    /** The units currently assigned to this army, in add order (index also implicitly matches {@link #addUnit(Unit, int)} position). */
    private final Vector<Unit> units = new Vector<>(1, 1);
    /** Ids of units designated as commanders of this army (there may be more than one). */
    private final Vector<Integer> commanders = new Vector<>(1, 1);
    private String name = " ";
    /** Maximum number of units allowed in this army, or {@link #NO_LIMIT}. */
    private int upperLimiter = NO_LIMIT;
    /** Minimum number of units required in this army, or {@link #NO_LIMIT}. */
    private int lowerLimiter = NO_LIMIT;
    /** Total Battle Value of the army; never reported negative (see {@link #getBV()}). */
    private int bv = 0;
    private int id;
    private boolean locked = false;
    private boolean armyPlayerLocked = false; // Used by players to keep armies
    // from being cleared
    private boolean armyDisabled = false;
    /** Cap on the "operating force size" (e.g. tonnage/BV-weighted force size rule), or {@link #NO_LIMIT}. */
    private float opForceSize = NO_LIMIT;
    /** C3 network link table: key=slave/linked unit id, value=the master/network-root unit id it is linked to. */
    private Hashtable<Integer, Integer> c3Network = new Hashtable<>();

    // CONSTRUCTORS
    /** Creates an empty, unlocked army with no units, no name, and no limits. */
    public Army() {
        // no content
    }

    // METHODS
    /** @return the number of units currently in this army. */
    public int getAmountOfUnits() {
        return units.size();
    }

    /** @return {@code true} if this army is currently disabled (e.g. excluded from deployment/selection). */
    public boolean isDisabled() {
        return armyDisabled;
    }

    /** Marks this army as disabled. */
    public void disableArmy() {
        armyDisabled = true;
    }

    /** Marks this army as enabled (not disabled). */
    public void enableArmy() {
        armyDisabled = false;
    }

    /** Flips the disabled/enabled state of this army. */
    public void toggleArmyDisabled() {
        armyDisabled = !armyDisabled;
    }

    /** @return {@code true} if the owning player has locked this army to keep it from being auto-cleared. */
    public boolean isPlayerLocked() {
        return armyPlayerLocked;
    }

    /** Locks this army against player-side auto-clearing (see {@link #armyPlayerLocked}). */
    public void playerLockArmy() {
        armyPlayerLocked = true;
    }

    /** Releases the player-side lock set by {@link #playerLockArmy()}. */
    public void playerUnlockArmy() {
        armyPlayerLocked = false;
    }

    /**
     * Add a unit to a specific position in the army's unit list, shifting subsequent units up by one index.
     *
     * @param unit     the unit to add.
     * @param Position the index to insert at; must be a valid insertion index (0..size), or an
     *                  {@link ArrayIndexOutOfBoundsException} is thrown by the underlying {@link Vector}.
     */
    public void addUnit(Unit unit, int Position) {
        units.add(Position, unit);
    }

    /**
     * Add a unit to the end of the army's unit list.
     *
     * @param unit the unit to add.
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
     * @return Returns the units. Note: this returns the live, mutable backing {@link Vector}, not a copy - callers
     *         can add/remove units through the returned reference.
     */
    public Vector<Unit> getUnits() {
        return units;
    }

    /**
     * This will pull The number of unit types this army holds i.e. type = Unit.MEK all meks will be counted.
     *
     * @param type         The unit type to check against.
     * @param countSupport Whether to count Support Units (see {@link Unit#isSupportUnit()}) toward the total, in
     *                      addition to combat units of the matching type.
     *
     * @return number of units of the given type in this army (support units excluded unless {@code countSupport}).
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

    /**
     * Serializes this army to a single delimited line of text for transmission over the wire protocol (the
     * server sends army data to clients this way). Fields are appended in a fixed order: id; (if
     * {@code toClient}) BV and lock state; name (or a single space if blank); lower/upper limiters; unit count
     * followed by each unit's id; C3 network entry count followed by each (slaveId, masterId) pair; op-force
     * size; commander count followed by each commander id; player-lock flag; disabled flag. Every value,
     * including the very last one, is followed by a trailing {@code delimiter}. There is no corresponding
     * "fromString" method on this base class - decoding is implemented by the client/server subclasses
     * ({@code CArmy}/{@code SArmy}), so this format must stay in sync with their parsers.
     *
     * @param toClient whether to include the client-relevant BV and lock-state fields.
     * @param delimiter the field separator to use (e.g. a control character reserved by the protocol).
     * @return the encoded line, NOT terminated by a newline.
     */
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
     * @return return the BV (Battle Value) of this army. Clamped to never report a negative number even if the
     *         stored {@link #bv} field somehow became negative.
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

    /**
     * Removes a unit from the C3 network link table, regardless of whether it was linked as a slave or acting as
     * a master/network root.
     * <p>
     * If {@code unitID} itself is a key in {@link #c3Network} (i.e. it is linked to some master), that single
     * entry is removed and the method returns immediately. Otherwise, the network is scanned for any entries
     * where {@code unitID} is the <em>value</em> (i.e. it is a master other units are linked to), and every such
     * link is removed - effectively disbanding the whole sub-network rooted at this unit.
     *
     * @param unitID the id of the unit to unlink from C3.
     */
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
     * Finds out if unitOne and unitTwo are in the same C3 Network. Considers them in the same network if either
     * is directly linked to the other, or if both are linked (as slaves) to the same master unit.
     *
     * @param unitOne id of the first unit.
     * @param unitTwo id of the second unit.
     * @return {@code true} if the two units share a C3 network connection.
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
     * Return the number of C3 networks in this army, counted by their master/root units: for every distinct
     * "master" value appearing in {@link #c3Network}, it counts as a network root if that master unit is not
     * itself linked as someone else's slave ({@code !c3Network.containsKey(uid)}) and has at least one unit
     * actually linked to it ({@link Unit#hasBeenC3LinkedTo(Army)}). Any exception while resolving a master unit
     * (e.g. {@code getUnit(uid)} returning {@code null} and NPE-ing on {@code hasBeenC3LinkedTo}) is silently
     * swallowed and simply does not count that entry - a quirk to be aware of if C3 data becomes inconsistent.
     * The result is never less than 1, even if there are zero networks, since the return is clamped with
     * {@code Math.max(1, count)}.
     *
     * @return the number of distinct C3 networks (minimum 1, regardless of actual count).
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

    /**
     * Linear search of this army's units by id.
     *
     * @param unitId the unit id to look up.
     * @return the matching {@link Unit}, or {@code null} if no unit with that id is in this army.
     */
    public Unit getUnit(int unitId) {

        for (Unit currU : getUnits()) {
            if (currU.getId() == unitId) {
                return currU;
            }
        }

        return null;
    }

    /** @return the configured operating force size cap, or {@link #NO_LIMIT} if uncapped. */
    public float getOpForceSize() {
        return opForceSize;
    }

    /** @param force the new operating force size cap (or {@link #NO_LIMIT} to remove the cap). */
    public void setOpForceSize(float force) {
        opForceSize = force;
    }

    /** @return the live list of unit ids designated as commanders of this army. */
    public Vector<Integer> getCommanders() {
        return commanders;
    }

    /**
     * Removes a unit id from the commanders list.
     * <p>
     * Quirk: {@code commanders.removeElement(id)} is called with an {@code int}, which autoboxes to
     * {@link Integer} for the {@code Vector<Integer>.removeElement(Object)} overload - this correctly removes by
     * value (not by index), so this works as intended, but is easy to misread as an index-based removal.
     *
     * @param id the unit id to remove from the commanders list.
     */
    public void removeCommander(int id) {
        commanders.removeElement(id);
        commanders.trimToSize();
    }

    /**
     * Adds a unit id to the commanders list, unless it is already present.
     *
     * @param id the unit id to designate as a commander of this army.
     */
    public void addCommander(int id) {
        if (isCommander(id)) {
            return;
        }
        commanders.add(id);
        commanders.trimToSize();
    }

    /**
     * @param id a unit id.
     * @return {@code true} if that unit is designated as a commander of this army.
     */
    public boolean isCommander(int id) {
        return commanders.contains(id);
    }

}
