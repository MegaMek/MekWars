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
 * Created on 26.03.2004
 *
 */
package mekwars.common;

import java.util.Vector;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.AmmoType;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.campaign.targetsystems.TargetSystem;


/**
 * Protocol-agnostic base class for a single BattleTech unit ('Mek, vehicle, infantry, aerospace, etc.) tracked by
 * MekWars' campaign layer.
 * <p>
 * A {@code Unit} wraps the campaign-relevant bookkeeping around a MegaMek unit: its weight class and unit-type
 * classification (see the {@code MEK}/{@code VEHICLE}/... and {@code LIGHT}/{@code MEDIUM}/... constants below),
 * pilot/crew assignment ({@link Pilot}), maintenance level, repair-cost history, operational status (in-service,
 * unmaintained, for-sale), targeting system, C3 network level and linking logic, and various flags (locked,
 * support unit, "Christmas unit"). It intentionally does not hold a live reference to a MegaMek {@code Entity} -
 * that association is added by subclasses.
 * <p>
 * This class is extended by {@code mekwars.common.campaign.CUnit} on the client (which layers on a decoded
 * MegaMek {@code Entity}, quirks, ammo loadout, and wire-protocol decoding via {@code setData(String)}) and by
 * {@code mekwars.server.campaign.SUnit} on the server (which adds authoritative game logic: persistence, repair
 * simulation, purchase/sale, etc.). Most of the C3-network linking logic in this base class
 * ({@link #linkToC3Network}, {@link #hasBeenC3LinkedTo}, {@link #hasC3SlavesLinkedTo},
 * {@link #checkC3mNetworkHasOpen}, {@link #checkC3iNetworkHasOpen}) operates on an {@link Army} the unit belongs
 * to, since C3 links are stored per-army rather than per-unit.
 *
 * @author Helge Richter
 *
 */

public class Unit {

    //STATIC VARIABLES
    /** Weight class constant: Light 'Mek/vehicle (up to 35 tons). */
    public static final int LIGHT = 0;
    /** Weight class constant: Medium 'Mek/vehicle (40-55 tons). */
    public static final int MEDIUM = 1;
    /** Weight class constant: Heavy 'Mek/vehicle (60-75 tons). */
    public static final int HEAVY = 2;
    /** Weight class constant: Assault 'Mek/vehicle (80-100 tons). */
    public static final int ASSAULT = 3;

    /** Unit-type constant: BattleMek. */
    public static final int MEK = 0;
    /** Unit-type constant: combat vehicle (tank). */
    public static final int VEHICLE = 1;
    /** Unit-type constant: conventional infantry. */
    public static final int INFANTRY = 2;
    /** Unit-type constant: ProtoMek. */
    public static final int PROTOMEK = 3;
    /** Unit-type constant: Battle Armor. */
    public static final int BATTLEARMOR = 4;
    /** Unit-type constant: aerospace unit (fighter/DropShip/etc.). */
    public static final int AERO = 5;
    /** Unit-type constant: quad-legged 'Mek. Note: not produced by {@link #getEntityType(Entity)}, which never
     *  distinguishes quads from {@link #MEK}; used elsewhere (e.g. by build-limit logic) as its own category. */
    public static final int QUAD = 6;
    /** Unit-type constant: a standalone MekWarrior (pilot without a unit), as opposed to a piloted machine. */
    public static final int MEKWARRIOR = 7;
    /** Highest unit-type constant used when iterating "buildable" unit type slots (0..MAX_BUILD inclusive covers MEK..QUAD). */
    public static final int MAX_BUILD = 6;

    /** C3 level constant: unit has no C3 equipment. */
    public static final int C3_NONE = 0;
    /** C3 level constant: unit has a standard C3 slave unit. */
    public static final int C3_SLAVE = 1;
    /** C3 level constant: unit has a standard C3 master unit. */
    public static final int C3_MASTER = 2;
    /** C3 level constant: unit has Improved C3 (C3i). */
    public static final int C3_IMPROVED = 3;
    /** C3 level constant: unit has a C3 master capable of linking to another master ("dual master"/C3 Master Booster). */
    public static final int C3M_MASTER = 4;

    /** Operational status constant: unit is in normal service. */
    public static final int STATUS_OK = 1;
    /** Operational status constant: unit is not being maintained (maintenance neglected). */
    public static final int STATUS_UNMAINTAINED = 2;//@urgru 7/18/04
    /** Operational status constant: unit is listed for sale. */
    public static final int STATUS_FOR_SALE = 3;//@urgru 12.29.05

    /** Count of distinct combat unit types (used for sizing type-indexed arrays/tables); does not include {@link #MEKWARRIOR}. */
    public static final int TOTAL_TYPES = 6;
    /** Unused/scratch array; no references found elsewhere in this class - purpose unclear, left as-is. */
    public int[] test = new int[4];
    /** Precomputed "simple" repair cost estimate for this unit (as opposed to {@link #currentRepairCost}, which accrues via {@link #addRepairCost(int)}). */
    public int simpleRepairCost = 0;
    //VARIABLES
    /** This unit's campaign-assigned id, unique within its owning collection (e.g. a player's hangar). */
    protected int id;
    /** Database row id, used by server-side persistence to locate this unit's saved record. */
    protected int DBId;
    /** The targeting system fitted to this unit (defaults to a plain/no targeting-system instance). */
    protected TargetSystem targetSystem = new TargetSystem();
    private Pilot pilot;
    private int type;
    private int weightClass;
    private int status = Unit.STATUS_OK;
    private String producer;
    private String unitFilename;
    private int posId;
    private String modelName;
    /** Maintenance level as a percentage (0-100); clamped to that range by {@link #setMaintenanceLevel(int)}. */
    private int maintenanceLevel = 100;//@urgru 8/2/04
    /** This unit's C3 equipment level; one of the {@code C3_*}/{@code C3M_MASTER} constants. 0=None 1=Slave 2=Master 3=Independent */
    private int unitC3Level = 0; //@Torren 12/13/04 0=None 1=Slave 2=Master 3=Independent
    /** Repair cost accrued since the last reset/payment; can be reset to 0 via {@link #addRepairCost(int)} with a negative argument. */
    private int currentRepairCost = 0;
    /** Total repair cost ever accrued by this unit, never reset (running total for historical/statistics purposes). */
    private int lifeTimeRepairCost = 0;
    private boolean isSupportUnit = false;
    /** Marks this unit as a special "Christmas" cosmetic/event variant. */
    private boolean ChristmasUnit = false;

    /** Whether this unit is locked against modification (e.g. mid-repair, or reserved). */
    private boolean isLocked = false;

    //CONSTRUCTOR
    /** Creates a unit with default field values (id 0, status OK, no pilot, maintenance level 100%, unlocked). */
    public Unit() {
        //no content
    }

    /**
     * @param weightClass one of the {@link #LIGHT}/{@link #MEDIUM}/{@link #HEAVY}/{@link #ASSAULT} constants.
     *
     * @return a String describing the weightClass (light, medium etc), or {@code "Unknown"} if it doesn't match
     *         one of the known constants.
     */
    public static String getWeightClassDesc(int weightClass) {
        if (weightClass == LIGHT) {
            return "Light";
        }

        if (weightClass == MEDIUM) {
            return "Medium";
        }

        if (weightClass == HEAVY) {
            return "Heavy";
        }

        if (weightClass == ASSAULT) {
            return "Assault";
        }

        return "Unknown";
    }

    /**
     * Parses a weight class name back into its numeric constant (case-insensitive).
     *
     * @param name one of "LIGHT", "MEDIUM", "HEAVY", "ASSAULT".
     * @return the matching weight class constant, or {@code 0} ({@link #LIGHT}) if the name is not recognized.
     */
    public static int getWeightIDForName(String name) {
        if (name.equalsIgnoreCase("LIGHT")) {
            return LIGHT;
        }

        if (name.equalsIgnoreCase("MEDIUM")) {
            return MEDIUM;
        }

        if (name.equalsIgnoreCase("HEAVY")) {
            return HEAVY;
        }

        if (name.equalsIgnoreCase("ASSAULT")) {
            return ASSAULT;
        }

        return 0;
    }

    //STATIC METHODS
    /*
     * Unit's static methods handle generalized information
     * about unit weight classes and types, including text to
     * int conversion, and vice versa.
     */

    /**
     * Converts a MegaMek {@link Entity}'s weight class into the MekWars {@code Unit} weight-class constant.
     *
     * @param ent the MegaMek entity to classify.
     * @return one of {@link #LIGHT}/{@link #MEDIUM}/{@link #HEAVY}/{@link #ASSAULT}; defaults to {@link #LIGHT}
     *         if the entity's weight class doesn't match any of MegaMek's standard weight tiers (e.g. superheavy).
     */
    public static int getEntityWeight(Entity ent) {
        int weight = ent.getWeightClass();

        if (weight == EntityWeightClass.WEIGHT_LIGHT) {
            return Unit.LIGHT;
        }

        if (weight == EntityWeightClass.WEIGHT_MEDIUM) {
            return Unit.MEDIUM;
        }

        if (weight == EntityWeightClass.WEIGHT_HEAVY) {
            return Unit.HEAVY;
        }

        if (weight == EntityWeightClass.WEIGHT_ASSAULT) {
            return Unit.ASSAULT;
        }

        return Unit.LIGHT;
    }

    /**
     * Classifies a MegaMek {@link Entity} into a MekWars unit-type constant based on its Java runtime type.
     * <p>
     * Quirk: checks are ordered {@code Mek}, {@code Tank}, {@code BattleArmor}, {@code ProtoMek}, {@code Aero},
     * falling through to {@link #INFANTRY} for anything else - so any entity subtype not covered by those five
     * checks (e.g. a hypothetical new unit category) is silently classified as infantry. This method also never
     * returns {@link #QUAD} or {@link #MEKWARRIOR}; quad 'Meks are classified simply as {@link #MEK}.
     *
     * @param ent the MegaMek entity to classify.
     * @return one of {@link #MEK}/{@link #VEHICLE}/{@link #BATTLEARMOR}/{@link #PROTOMEK}/{@link #AERO}, or
     *         {@link #INFANTRY} as the fallback.
     */
    public static int getEntityType(Entity ent) {
        if (ent instanceof Mek) {
            return Unit.MEK;
        }

        if (ent instanceof Tank) {
            return Unit.VEHICLE;
        }

        if (ent instanceof BattleArmor) {
            return Unit.BATTLEARMOR;
        }

        if (ent instanceof ProtoMek) {
            return Unit.PROTOMEK;
        }

        if (ent instanceof Aero) {
            return Unit.AERO;
        }

        return Unit.INFANTRY;
    }

    /**
     * @param type one of the unit-type constants ({@link #MEK}, {@link #VEHICLE}, etc.).
     * @return a human-readable description of the unit type, or {@code "Unknown"} if {@code type} doesn't match
     *         any of the checked constants (note {@link #QUAD} and {@link #MEKWARRIOR} are not checked here and
     *         so would map to "Unknown").
     */
    public static String getTypeClassDesc(int type) {
        if (type == Unit.MEK) {return "Mek";}
        if (type == Unit.VEHICLE) {return "Vehicle";}
        if (type == Unit.INFANTRY) {return "Infantry";}
        if (type == Unit.BATTLEARMOR) {return "BattleArmor";}
        if (type == Unit.PROTOMEK) {return "ProtoMek";}
        if (type == Unit.AERO) {return "Aero";}

        return "Unknown";
    }

    /**
     * Guesses a unit type from the leading letter of a (unit filename/model) name, following MekWars' file
     * naming convention: {@code V}=Vehicle, {@code I}=Infantry, {@code P}=ProtoMek, {@code B}=BattleArmor,
     * {@code A}=Aero, anything else defaults to {@link #MEK}. Matching is case-insensitive and only looks at the
     * first character.
     *
     * @param name the name/filename to inspect.
     * @return the guessed unit-type constant.
     */
    public static int getTypeIDForName(String name) {

        //If the string contains V, it's supposely a Vehicle
        if (name.toLowerCase().startsWith("v")) {return VEHICLE;}

        //I = Infantry. the I in Vehicle is not affected, because it's caught above
        if (name.toLowerCase().startsWith("i")) {return INFANTRY;}

        //P = ProtoMek
        if (name.toLowerCase().startsWith("p")) {return PROTOMEK;}

        //B = BattleArmor
        if (name.toLowerCase().startsWith("b")) {return BATTLEARMOR;}

        //A = Aero
        if (name.toLowerCase().startsWith("a")) {return AERO;}

        //Default = Mek
        return MEK;
    }

    /** @return {@code true} if this unit is locked against modification. */
    public boolean isLocked() {
        return isLocked;
    }

    /** @param isLocked the new locked state for this unit. */
    public void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }

    //METHODS
    /*
     * Nearly all gets and sets.
     */

    /**
     * Method that returns a model name. Name is "checkModel" to avoid confusion with the SUnit.getModelName() method,
     * which should be used instead of this method wherever possible.
     *
     * @return Returns the modelname.
     */
    public String checkModelName() {
        return modelName;
    }

    /**
     * @param modelName The modelName to set.
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * @return Returns the posId.
     */
    public int getPosId() {
        return posId;
    }

    /**
     * @param posId The posId to set.
     */
    public void setPosId(int posId) {
        this.posId = posId;
    }

    /**
     * @return Returns the producer.
     */
    public String getProducer() {
        return producer;
    }

    /**
     * @param producer The producer to set.
     */
    public void setProducer(String producer) {
        this.producer = producer;
    }

    /**
     * @return Returns the unitFilename. Note: the trailing {@code //.trim()} comment is dead/commented-out code -
     *         the returned value is NOT trimmed despite the comment suggesting it once was (or was intended to be).
     */
    public String getUnitFilename() {
        return unitFilename;//.trim();
    }

    /**
     * @param unitFilename The unitFilename to set.
     */
    public void setUnitFilename(String unitFilename) {
        this.unitFilename = unitFilename;
    }

    /**
     * @return Returns the weightClass.
     */
    public int getWeightClass() {
        return weightClass;
    }

    /**
     * @param weightClass The weightClass to set.
     */
    public void setWeightClass(int weightClass) {
        this.weightClass = weightClass;
    }

    /**
     * @return Returns the status.
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status The status to set.
     */
    public void setStatus(int status) {
        this.status = status;
    }

    public int getDBId() {
        return DBId;
    }

    public void setDBId(int i) {
        DBId = i;
    }

    /**
     * @return return the maintenance status
     */
    public int getMaintenanceLevel() {
        return maintenanceLevel;
    }

    /**
     * Sets the maintenance level, clamping to the valid 0-100 percentage range.
     *
     * @param maintenanceLevel maintenance level to set.
     *                         <p>
     *                         since maintenance is expressed as a percentage, dont let this this exceed 100 or drop
     *                         below 0. Values below 0 are clamped to 0, values above 100 are clamped to 100.
     */
    public void setMaintenanceLevel(int maintenanceLevel) {
        if (maintenanceLevel < 0) {
            maintenanceLevel = 0;
        }

        if (maintenanceLevel > 100) {
            maintenanceLevel = 100;
        }

        this.maintenanceLevel = maintenanceLevel;
    }

    /**
     * @param i amount of maintenance to add.
     */
    public void addToMaintenanceLevel(int i) {
        setMaintenanceLevel(maintenanceLevel + i);
    }

    /**
     * Attempts to link this unit into a C3 network as a slave/subordinate of {@code master}, recording the link
     * in {@code army}'s C3 network table ({@link Army#getC3Network()}) on success. The rules applied depend on
     * this unit's C3 equipment level ({@link #getC3Level()}):
     * <ul>
     *   <li>{@link #C3_NONE}: never links; always fails.</li>
     *   <li>{@link #C3_SLAVE}: master must be a {@link #C3_MASTER} or {@link #C3M_MASTER}; the master must not
     *       already be a slave in some other unit's network unless it also has slaves of its own linked to it;
     *       and the master's C3 Master (C3M) network must have room (see
     *       {@link #checkC3mNetworkHasOpen(Army, int)}).</li>
     *   <li>{@link #C3_MASTER} or {@link #C3M_MASTER}: master must also be a {@link #C3_MASTER}/{@link #C3M_MASTER};
     *       this unit must not already be linked as someone else's slave unless it has its own slaves; a plain
     *       {@link #C3_MASTER} cannot serve as master if it is itself linked as a slave elsewhere while also
     *       having slaves; and the master's C3M network must have room.</li>
     *   <li>{@link #C3_IMPROVED} (C3i): master must also be {@link #C3_IMPROVED}; this unit must not already be
     *       linked to anything; and the master's C3i network must have room (see
     *       {@link #checkC3iNetworkHasOpen(Army)}).</li>
     * </ul>
     * On success, {@code army.getC3Network().put(thisUnitId, masterUnitId)} is performed and the master's id is
     * returned. On any rule violation (including {@code army} or {@code master} being {@code null}, or this unit
     * not being present in {@code army}), no change is made and {@code -1} is returned.
     *
     * @param army   the army whose C3 network table this link is recorded in; this unit must already be a member.
     * @param master the unit to link to as the C3 master/network node.
     * @return the master's unit id on success, or {@code -1} if the link could not be established.
     */
    public int linkToC3Network(Army army, Unit master) {

        if (army == null || master == null) {
            return -1;
        }

        if (army.getUnit(this.getId()) == null) {
            return -1;
        }

        if (this.getC3Level() == C3_NONE) {
            return -1;
        }

        if (this.getC3Level() == C3_SLAVE) {

            if (master.getC3Level() != C3_MASTER &&
                      master.getC3Level() != C3M_MASTER) {
                return -1;
            }

            if (master.hasBeenC3LinkedTo(army) && !master.hasC3SlavesLinkedTo(army)) {
                return -1;
            }

            if (!master.checkC3mNetworkHasOpen(army, this.getC3Level())) {
                return -1;
            }

            army.getC3Network().put(this.getId(), master.getId());
            return master.getId();
        } else if (this.getC3Level() == C3_MASTER || this.getC3Level() == C3M_MASTER) {

            if (master.getC3Level() != C3_MASTER &&
                      master.getC3Level() != C3M_MASTER)//master is really a slave or doesn't have C3
            {return -1;}

            if (this.hasBeenC3LinkedTo(army) &&
                      !this.hasC3SlavesLinkedTo(army)) //other units have linked to this unit so it cannot link to other units
            {return -1;}

            if (master.getC3Level() != Unit.C3M_MASTER &&
                      master.hasBeenC3LinkedTo(army) &&
                      master.hasC3SlavesLinkedTo(army)) {return -1;}

            if (!master.checkC3mNetworkHasOpen(army, this.getC3Level())) {return -1;}

            army.getC3Network().put(this.getId(), master.getId());
            return master.getId();
        } else if (this.getC3Level() == C3_IMPROVED) {
            if (master.getC3Level() != C3_IMPROVED) {return -1;}

            if (this.hasBeenC3LinkedTo(army)) {return -1;}

            if (!master.checkC3iNetworkHasOpen(army)) {return -1;}

            army.getC3Network().put(this.getId(), master.getId());
            return master.getId();
        }

        return -1;
    }

    /**
     * @return Returns the id.
     */
    public int getId() {
        return id;
    }

    /**
     * @param id The id to set.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the units current C3 Level 0=None 1=Slave 2=Master 3=Independent 4=Dual Masters
     */
    public int getC3Level() {
        return unitC3Level;
    }

    /**
     * Checks whether some other unit in {@code army} is linked to this unit as its C3 master (i.e. this unit is
     * acting as a network node that others point to). Returns {@code false} if this unit is not a member of
     * {@code army} (will throw NPE if {@code army} itself is {@code null} - not null-checked here, unlike the
     * {@code checkC3*NetworkHasOpen} methods).
     *
     * @param army the army whose C3 network table to search.
     * @return {@code true} if at least one entry in {@code army}'s C3 network table points at this unit's id.
     */
    public boolean hasBeenC3LinkedTo(Army army) {

        if (army.getUnit(this.getId()) == null) {return false;}

        for (int c3U : army.getC3Network().values()) {
            if (c3U == this.getId()) {return true;}
        }

        return false;
    }

    /**
     * Checks that every unit linked to this unit (as this unit's C3 slave, i.e. entries in {@code army}'s C3
     * network table whose value is this unit's id) is itself a plain {@link #C3_SLAVE} - i.e. this unit is acting
     * as a "pure" master with only slaves attached, not as an intermediate node linked to by another master.
     * Returns {@code true} vacuously if nothing is linked to this unit, or if this unit is not a member of
     * {@code army}.
     *
     * @param army the army whose C3 network table to search.
     * @return {@code true} if this unit has only slave-level units (or nothing) linked to it as master.
     */
    public boolean hasC3SlavesLinkedTo(Army army) {

        if (army.getUnit(this.getId()) == null) {return false;}

        for (Integer c3Slave : army.getC3Network().keySet()) {
            Integer c3Master = army.getC3Network().get(c3Slave);
            if (c3Master == this.getId()) {
                if (army.getUnit(c3Slave).getC3Level() != Unit.C3_SLAVE) {return false;}
            }
        }
        return true;
    }

    /**
     * Checks whether this unit (acting as a C3 master candidate) has room in its C3 Master ("C3M") network to
     * accept one more link of the given type, per BattleTech C3 network size limits.
     * <p>
     * If this unit is a {@link #C3M_MASTER} ("dual master"), it enforces the split limits separately: at most 2
     * other masters and at most 3 slaves may be linked to it (the comment in the loop notes it assumes no C3i
     * units are mixed into a C3M/S network). For any other C3 level (plain {@link #C3_MASTER}), it enforces a
     * flat cap of 4 total units (including itself) in the network.
     * <p>
     * Returns {@code false} immediately if {@code army} is {@code null}, this unit is not a member of
     * {@code army}, or this unit is already linked to some other unit as its own slave.
     *
     * @param army   the army whose C3 network table to check against.
     * @param c3Type the C3 level of the unit that wants to join (one of the {@code C3_*} constants); only used to
     *               distinguish slave vs. master joins when this unit is a {@link #C3M_MASTER}.
     * @return {@code true} if there is room for one more link of the given type.
     */
    public boolean checkC3mNetworkHasOpen(Army army, int c3Type) {

        int MAX_UNITS = 4;
        int unitCount = 1;//this unit is already in the network :)

        if (army == null) {return false;}

        if (army.getUnit(this.getId()) == null) {return false;}

        if (army.getC3Network().get(this.getId()) != null) //meaning hes already linked to someone
        {return false;}

        if (this.getC3Level() == C3M_MASTER) {
            int slaveCount = 0, masterCount = 0;
            int maxMasters = 2;
            int maxSlaves = 3;

            for (Integer c3Slave : army.getC3Network().keySet()) {
                Integer c3Master = army.getC3Network().get(c3Slave);
                if (c3Master == this.getId()) {
                    Unit tempUnit = army.getUnit(c3Slave);
                    if (tempUnit.getC3Level() == C3_SLAVE) {slaveCount++;} else {
                        masterCount++; // we are going to assume that no C3I's are in a C3M/S network
                    }
                }
            }

            if (c3Type != Unit.C3_SLAVE && masterCount >= maxMasters) {return false;}

            return c3Type != Unit.C3_SLAVE || slaveCount < maxSlaves;
        }

        for (Integer c3Unit : army.getC3Network().values()) {
            if (c3Unit == this.getId()) {unitCount++;}
        }

        return unitCount < MAX_UNITS;
    }

    /**
     * Checks whether this unit (acting as a C3i "master"/network node candidate) has room in its Improved C3
     * network for one more link, per the BattleTech C3i limit of 6 units total per network. Returns {@code false}
     * immediately if {@code army} is {@code null}, this unit is not a member of {@code army}, or this unit is
     * already linked to some other unit as its own slave.
     *
     * @param army the army whose C3 network table to check against.
     * @return {@code true} if there is room for one more unit in this unit's C3i network.
     */
    public boolean checkC3iNetworkHasOpen(Army army) {
        int MAX_UNITS = 6;
        int unitCount = 1; //this unit is already in the network :)

        if (army == null) {return false;}

        if (army.getUnit(this.getId()) == null) {return false;}

        if (army.getC3Network().get(this.getId()) != null) {return false;}

        for (Integer c3U : army.getC3Network().values()) {
            if (c3U == this.getId()) {unitCount++;}
        }

        return unitCount < MAX_UNITS;
    }

    /**
     * @param level Sets the units current C3 Level
     */
    public void setC3Level(int level) {
        unitC3Level = level;
    }

    /**
     * Despite the "get" name, this does not return a value - it inspects the given MegaMek {@link Entity}'s
     * installed C3 equipment and updates this unit's {@link #unitC3Level} (via {@link #setC3Level(int)}) to
     * match: C3 Slave, C3 Master, C3 Master Booster ("dual master"), or C3i, checked in that order, defaulting to
     * {@link #C3_NONE} if the entity has none of them.
     * <p>
     * Side effect worth flagging: this method unconditionally calls {@code unit.setShutDown(false)} on the
     * passed-in entity before inspecting its C3 gear, which un-shuts-down the entity as a side effect of what
     * looks like a pure "read C3 type" query. This looks unintentional/left over from other code but is left
     * as-is per this documentation pass.
     *
     * @param unit the MegaMek entity whose C3 equipment to inspect.
     */
    public void getC3Type(Entity unit) {
        unit.setShutDown(false);
        if (unit.hasC3S()) {
            this.setC3Level(C3_SLAVE); //Slave
        } else if (unit.hasC3MM()) {
            this.setC3Level(C3M_MASTER); //Dual Master
        } else if (unit.hasC3M()) {
            this.setC3Level(C3_MASTER); //Master
        } else if (unit.hasC3i()) {
            this.setC3Level(C3_IMPROVED); //Improved
        } else {this.setC3Level(C3_NONE);}
    }

    /**
     * Finds a specific {@link AmmoType} (by MegaMek internal name) among all ammo types available for a given
     * weapon type.
     *
     * @param weaponType the weapon type to enumerate compatible munitions for.
     * @param ammoName   the MegaMek internal name of the desired munition (case-insensitive match).
     * @return the matching {@link AmmoType}, or {@code null} if not found (callers should then fall back to the
     *         entity's standard ammo).
     */
    public AmmoType getEntityAmmo(AmmoType.AmmoTypeEnum weaponType, String ammoName) {
        Vector<AmmoType> v_Ammo = AmmoType.getMunitionsFor(weaponType);
        AmmoType at;

        for (int count = 0; count < v_Ammo.size(); count++) {
            at = v_Ammo.elementAt(count);
            if (at.getInternalName().equalsIgnoreCase(ammoName)) {
                return at;
            }
        }

        //couldn't find the ammo return null and just use the entities standard ammo.
        return null;
    }

    /**
     * @return {@code true} if this unit has no assigned {@link Pilot}, or the assigned pilot's name is
     *         literally "Vacant" (the sentinel name MekWars uses for an unfilled pilot slot).
     */
    public boolean hasVacantPilot() {
        return this.getPilot() == null || this.getPilot().getName().equalsIgnoreCase("Vacant");
    }

    /**
     * @return Returns the pilot.
     */
    public Pilot getPilot() {
        return pilot;
    }

    /**
     * @param pilot The pilot to set.
     */
    public void setPilot(Pilot pilot) {
        this.pilot = pilot;
    }

    /**
     * Directly overwrites both repair-cost counters (e.g. when loading a persisted unit).
     *
     * @param current the new "current" (since-last-reset) repair cost.
     * @param life    the new lifetime (all-time total) repair cost.
     */
    public void setRepairCosts(int current, int life) {
        currentRepairCost = current;
        lifeTimeRepairCost = life;
    }

    /**
     * Adds to this unit's accrued repair cost.
     * <p>
     * Quirk: a negative {@code cost} does NOT subtract from the running total - it instead resets
     * {@link #currentRepairCost} to 0 entirely (and leaves {@link #lifeTimeRepairCost} untouched), which is how
     * this method is used to "clear" the current repair cost (e.g. after the player pays it off) rather than to
     * apply a refund.
     *
     * @param cost the repair cost to add; a negative value resets the current repair cost to 0 instead of adding.
     */
    public void addRepairCost(int cost) {

        if (cost < 0) {currentRepairCost = 0;} else {
            currentRepairCost += cost;
            lifeTimeRepairCost += cost;
        }
    }

    /** @return the repair cost accrued since the last reset (see {@link #addRepairCost(int)}). */
    public int getCurrentRepairCost() {
        return currentRepairCost;
    }

    /** @return the total repair cost ever accrued by this unit (never reset). */
    public int getLifeTimeRepairCost() {
        return lifeTimeRepairCost;
    }

    /**
     * @return {@code true} if this unit's type is crewed by exactly one pilot ({@link #MEK}, {@link #PROTOMEK},
     *         {@link #QUAD}, or {@link #AERO}), as opposed to multi-crew types like vehicles/infantry/BattleArmor.
     */
    public boolean isSinglePilotUnit() {
        return this.getType() == Unit.MEK ||
                     this.getType() == Unit.PROTOMEK ||
                     this.getType() == Unit.QUAD ||
                     this.getType() == Unit.AERO;
    }

    /**
     * @return Returns the type.
     */
    public int getType() {
        return type;
    }

    /**
     * @param type The type to set.
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * @return the isSupportUnit
     */
    public boolean isSupportUnit() {
        return isSupportUnit;
    }

    /**
     * @param isSupportUnit the isSupportUnit to set
     */
    public void setSupportUnit(boolean isSupportUnit) {
        this.isSupportUnit = isSupportUnit;
    }

    /**
     * @return the christmasUnit
     */
    public boolean isChristmasUnit() {
        return ChristmasUnit;
    }

    /**
     * @param christmasUnit the christmasUnit to set
     */
    public void setChristmasUnit(boolean christmasUnit) {
        ChristmasUnit = christmasUnit;
    }
}
