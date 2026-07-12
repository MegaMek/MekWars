/*
 * Copyright (C) 2005 - Torren (torren@users.sourceforge.net)
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
package mekwars.common.util;

import jakarta.annotation.Nonnull;
import megamek.common.CriticalSlot;
import megamek.common.TechConstants;
import megamek.common.enums.Gender;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekwars.common.I18N.I18NMessages;
import mekwars.common.MegaMekPilotOption;
import mekwars.common.Unit;
import mekwars.common.campaign.pilot.skills.PilotSkill;
import mekwars.common.util.unitdamage.UnitDamageHandlerFactory;

/**
 * Static grab-bag of BattleTech/MegaMek {@link Entity} helper methods used throughout MekWars for the "Advanced
 * Repair" subsystem: inspecting and mutating critical-slot damage state (destroyed/breached/missing/repairing),
 * computing tech-level compatibility and repair difficulty rolls, pricing damaged parts for repair-bay costs,
 * building human-readable repair/salvage status messages, looking up armor/internal-structure/engine type names,
 * and locating or synthesizing a {@link megamek.common.units.Entity} from the MegaMek unit cache.
 * <p>
 * This class has no state of its own (aside from shared logging/i18n helpers) and is never instantiated; every
 * member is {@code static}. Callers are typically the repair-bay UI/servlets and campaign/unit persistence code
 * that need to reason about a unit's damage without duplicating MegaMek's lower-level {@code Entity}/
 * {@code CriticalSlot} bookkeeping. Several methods here re-implement or work around bugs found in MegaMek's own
 * equivalents (see {@link #hasTargetingComputer(Entity)}) and use a MekWars-specific numbering of unit locations
 * that is distinct from MegaMek's own {@code Mek.LOC_*}/{@code Tank.LOC_*} constants (see the {@code LOC_*} fields
 * below).
 */
public class UnitUtils {
    // Engines
    /** Engine-type code: standard (non-XL/XXL) fusion engine. */
    public static final int STANDARD_ENGINE = 0;
    /** Engine-type code: Inner Sphere Light engine. */
    public static final int IS_LIGHT_ENGINE = 1;
    /** Engine-type code: Inner Sphere XL engine. */
    public static final int IS_XL_ENGINE = 2;
    /** Engine-type code: Inner Sphere XXL engine. */
    public static final int IS_XXL_ENGINE = 3;
    /** Engine-type code: Clan XL engine. */
    public static final int CLAN_XL_ENGINE = 4;
    /** Engine-type code: Clan XXL engine. */
    public static final int CLAN_XXL_ENGINE = 5;
    /**
     * Short display names for each engine-type code above, indexed by that code. Note IS and Clan XL/XXL share the
     * same short label here ("XL Engine"/"XXL Engine") since this array does not distinguish faction.
     */
    public static final String[] ENGINE_SHORT_STRING = { "Standard Engine", "Light Engine", "XL Engine", "XXL Engine",
                                                         "XL Engine", "XXL Engine" };
    /** Full tech-qualified display names for each engine-type code above, indexed by that code (IS vs. Clan shown). */
    public static final String[] ENGINE_TECH_STRING = { "Standard Engine", "IS Light Engine", "IS XL Engine",
                                                        "IS XXL Engine", "Clan XL Engine", "Clan XXL Engine" };
    // Locations for Advanced Repair.
    /*
     * These LOC_* codes are MekWars' own location numbering used by the Advanced Repair screens, and only loosely
     * mirror MegaMek's Mek.LOC_* constants (which is why several methods below adjust by subtracting 7 to map a
     * *R rear-torso pseudo-location back onto the corresponding front torso location before calling into MegaMek).
     */
    /** Location code: head. */
    public static final int LOC_HEAD = 0;
    /** Location code: center torso (front). */
    public static final int LOC_CENTER_TORSO = 1;
    /** Location code: right torso (front). */
    public static final int LOC_RT = 2;
    /** Location code: left torso (front). */
    public static final int LOC_LT = 3;
    /** Location code: right arm. */
    public static final int LOC_RIGHT_ARM = 4;
    /** Location code: left arm. */
    public static final int LOC_LEFT_ARM = 5;
    /** Location code: right leg. */
    public static final int LOC_RIGHT_LEG = 6;
    /** Location code: left leg. */
    public static final int LOC_LEFT_LEG = 7;
    /** Pseudo-location code: center torso, used to select the rear-armor facing of the center torso. */
    public static final int LOC_CENTER_TORSOR = 8;
    /** Pseudo-location code: right torso, used to select the rear-armor facing of the right torso. */
    public static final int LOC_RTR = 9;
    /** Pseudo-location code: left torso, used to select the rear-armor facing of the left torso. */
    public static final int LOC_LTR = 10;
    /** Repair-target code: front (external) armor of a location, as opposed to internal structure. */
    public static final int LOC_FRONT_ARMOR = 13;
    /** Repair-target code: rear armor of a location (only meaningful for torsos, which have rear armor). */
    public static final int LOC_REAR_ARMOR = 14;
    /** Repair-target code: internal structure (IS) of a location, as opposed to armor. */
    public static final int LOC_INTERNAL_ARMOR = 15;
    // Tech levels
    /** Crew/tech skill tier: Green. Also used as the fallback/"unrecognized" tech-level result. */
    public static final int TECH_GREEN = 0;
    /** Crew/tech skill tier: Regular. */
    public static final int TECH_REG = 1;
    /** Crew/tech skill tier: Veteran. */
    public static final int TECH_VET = 2;
    /** Crew/tech skill tier: Elite. */
    public static final int TECH_ELITE = 3;
    /** Special tech tier meaning "use the unit's assigned pilot/tech" rather than a fixed skill tier. */
    public static final int TECH_PILOT = 4;
    /** Special tech tier meaning the repair is being paid for with reward points rather than rolled. */
    public static final int TECH_REWARD_POINTS = 5;
    // Used for simple repairs
    /** Simple-repair category: armor. */
    public static final int ARMOR = 1;
    /** Simple-repair category: internal structure. */
    public static final int INTERNAL = 2;
    /** Simple-repair category: weapons. */
    public static final int WEAPONS = 3;
    /** Simple-repair category: non-weapon equipment. */
    public static final int EQUIPMENT = 4;
    /** Simple-repair category: Mek systems (gyro, cockpit, sensors, life support, actuators). */
    public static final int SYSTEMS = 5;
    /** Simple-repair category: engine. */
    public static final int ENGINES = 6;
    /** Localized message bundle for this class's user-facing repair/salvage status strings. */
    private final static I18NMessages MESSAGES = new I18NMessages(UnitUtils.class);
    /** Logger for this class. */
    private static final MMLogger LOGGER = MMLogger.create(UnitUtils.class);

    /**
     * Checks whether a unit has taken any armor or internal-structure damage. Infantry is always reported as
     * undamaged here since infantry does not track armor/internal the same way as Meks/Tanks.
     *
     * @param unit the unit to check
     *
     * @return {@code true} if current armor or internal totals differ from the unit's original ("O") totals
     */
    public static boolean hasArmorDamage(Entity unit) {
        if (unit instanceof Infantry) {
            return false;
        }

        return (unit.getTotalArmor() != unit.getTotalOArmor()) || (unit.getTotalInternal() != unit.getTotalOInternal());
    }

    /**
     * Checks whether a unit's internal structure (IS) specifically has taken damage. Infantry always reports
     * {@code false}.
     *
     * @param unit the unit to check
     *
     * @return {@code true} if current total internal structure is less than the original total
     */
    public static boolean hasISDamage(Entity unit) {
        if (unit instanceof Infantry) {
            return false;
        }

        return unit.getTotalInternal() != unit.getTotalOInternal();
    }

    /**
     * Scans every critical slot of a Mek or Tank for damage or breach flags. Other unit types (e.g. Infantry) are
     * never considered to have critical damage by this method.
     *
     * @param unit the unit to check
     *
     * @return {@code true} if any critical slot is damaged or breached
     */
    public static boolean hasCriticalDamage(Entity unit) {
        if ((unit instanceof Mek) || (unit instanceof Tank)) {
            for (int x = 0; x < unit.locations(); x++) {
                for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
                    CriticalSlot criticalSlot = unit.getCritical(x, y);

                    if ((criticalSlot != null) && (criticalSlot.isDamaged() || criticalSlot.isBreached())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks whether any part of a Mek or Tank is currently flagged as "under repair" — i.e. its current
     * armor/internal value has already been bumped above the original value (see {@link #setArmorRepair}) or one
     * of its critical slots has the repairing flag set. Other unit types always report {@code false}.
     *
     * @param unit the unit to check
     *
     * @return {@code true} if any location or critical slot is mid-repair
     */
    public static boolean isRepairing(Entity unit) {
        if ((unit instanceof Mek) || (unit instanceof Tank)) {
            for (int x = 0; x < unit.locations(); x++) {

                // check for armor repairs first, then move to crits.
                if (unit.getArmor(x) > unit.getOArmor(x)) {
                    return true;
                }

                if (unit.hasRearArmor(x)) {
                    if (unit.getArmor(x, true) > unit.getOArmor(x, true)) {
                        return true;
                    }
                }

                if (unit.getInternal(x) > unit.getOInternal(x)) {
                    return true;
                }

                for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
                    CriticalSlot criticalSlot = unit.getCritical(x, y);

                    if ((criticalSlot != null) && criticalSlot.isRepairing()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Destroys every engine critical slot in a Mek's torso locations (center/right/left torso), marking each as
     * salvaged. Used to represent a botched salvage attempt or a fully cored-out unit whose fusion engine is
     * beyond repair. No-op for non-Mek units.
     *
     * @param unit the unit whose engine is to be destroyed
     */
    public static void destroyAllEngineCrits(Entity unit) {
        if (unit instanceof Mek) {
            // no reason to check for engines anywhere other than the torso
            for (int x = LOC_CENTER_TORSO; x <= LOC_LT; x++) {
                for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
                    CriticalSlot criticalSlot = unit.getCritical(x, y);
                    if (UnitUtils.isEngineCrit(criticalSlot)) {
                        UnitUtils.salvageCriticalSlot(criticalSlot);
                    }
                }
            }
        }
    }

    /**
     * Tests whether a critical slot is one of the Mek's engine system slots.
     *
     * @param criticalSlot the slot to check, may be {@code null}
     *
     * @return {@code true} if non-null, of type {@code TYPE_SYSTEM}, and indexed as {@code Mek.SYSTEM_ENGINE}
     */
    public static boolean isEngineCrit(CriticalSlot criticalSlot) {
        return (criticalSlot != null) &&
                     (criticalSlot.getType() == CriticalSlot.TYPE_SYSTEM) &&
                     (criticalSlot.getIndex() == Mek.SYSTEM_ENGINE);
    }

    /**
     * Marks a single critical slot (and, if it holds equipment, the mounted equipment itself) as permanently
     * salvaged: destroyed, missing, hit, and not breached/repairing. This represents the part being physically
     * removed rather than merely damaged.
     *
     * @param criticalSlot the slot to salvage
     */
    public static void salvageCriticalSlot(CriticalSlot criticalSlot) {
        if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
            Mounted<?> mounted = criticalSlot.getMount();
            mounted.setDestroyed(true);
            mounted.setMissing(true);
            mounted.setHit(true);
            mounted.setBreached(false);
        }

        criticalSlot.setDestroyed(true);
        criticalSlot.setHit(true);
        criticalSlot.setMissing(true);
        criticalSlot.setRepairing(false);
        criticalSlot.setBreached(false);
    }

    /**
     * Repairs (un-damages) every critical slot backing a piece of equipment. Since some equipment occupies
     * multiple critical slots, all matching slots in the given location are fixed; split equipment (arm/torso
     * mounted weapons) is delegated to {@link #repairSplitEquipment}.
     *
     * @param equipment the mounted equipment to repair
     * @param unit      the unit that owns it
     * @param location  the location to scan for this equipment's critical slots
     */
    public static void repairEquipment(Mounted<?> equipment, Entity unit, int location) {
        if (equipment.isSplit()) {
            UnitUtils.repairSplitEquipment(equipment, unit);
            return;
        }

        setUnitCriticalSlots(equipment, unit, location);
    }

    /**
     * Repairs a weapon/equipment mount that is split across multiple Mek locations (e.g. an arm-and-torso mounted
     * weapon). Only Meks can have split equipment, so this is a no-op for any other unit type. Scans center torso
     * through left arm since those are the only locations that can host split equipment.
     *
     * @param equipment the split mount to repair
     * @param unit      the Mek that owns it
     */
    public static void repairSplitEquipment(Mounted<?> equipment, Entity unit) {
        // Only Meks should have split weapons crits.
        if (!(unit instanceof Mek)) {
            return;
        }

        // can only split weapons in toros and arms.
        for (int x = LOC_CENTER_TORSO; x <= LOC_LEFT_ARM; x++) {
            setUnitCriticalSlots(equipment, unit, x);
        }
    }

    /** Repairs (fixes) every critical slot in {@code x} that holds the given equipment mount. */
    private static void setUnitCriticalSlots(Mounted<?> equipment, Entity unit, int x) {
        for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
            CriticalSlot criticalSlot = unit.getCritical(x, y);

            if (criticalSlot == null) {
                continue;
            }

            if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                Mounted<?> mounted = criticalSlot.getMount();

                if (equipment.equals(mounted)) {
                    UnitUtils.fixCriticalSlot(criticalSlot, criticalSlot.isBreached());
                    unit.setCritical(x, y, criticalSlot);
                }
            }// end getType() if

        }
    }

    /**
     * Salvages (permanently removes) every critical slot backing a piece of equipment, delegating to
     * {@link #salvageSplitEquipment} if the equipment is split across locations.
     *
     * @param equipment the mounted equipment to salvage
     * @param unit      the unit that owns it
     * @param location  the location to scan for this equipment's critical slots
     */
    public static void salvageEquipment(Mounted<?> equipment, Entity unit, int location) {
        if (equipment.isSplit()) {
            UnitUtils.salvageSplitEquipment(equipment, unit);
            return;
        }

        setSalvageCriticalSlots(equipment, unit, location);
    }

    /** Salvages every critical slot in {@code location} that holds the given equipment mount. */
    private static void setSalvageCriticalSlots(Mounted<?> equipment, Entity unit, int location) {
        for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot criticalSlot = unit.getCritical(location, slot);

            if (criticalSlot == null) {
                continue;
            }

            if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                Mounted<?> mounted = criticalSlot.getMount();

                if (equipment.equals(mounted)) {
                    UnitUtils.salvageCriticalSlot(criticalSlot);
                    unit.setCritical(location, slot, criticalSlot);
                }
            }// end getType() if
        }// end for
    }

    /**
     * Salvages a weapon/equipment mount split across multiple Mek locations. No-op for non-Mek units.
     *
     * @param equipment the split mount to salvage
     * @param unit      the Mek that owns it
     */
    public static void salvageSplitEquipment(Mounted<?> equipment, Entity unit) {
        // Only Meks should have split weapons crits.
        if (!(unit instanceof Mek)) {
            return;
        }

        // can only split weapons in toros and arms.
        for (int x = LOC_CENTER_TORSO; x <= LOC_LEFT_ARM; x++) {
            setSalvageCriticalSlots(equipment, unit, x);
        }
    }

    /**
     * Salvages a system critical slot (gyro, life support, sensors, cockpit, actuators). Gyro and life
     * support/sensors occupy multiple slots per {@link #getNumberOfSystemCriticalSlots}, so for those indices every
     * matching system slot in the location is salvaged together; other system slots (actuators, cockpit) are
     * salvaged individually.
     *
     * @param location     the location containing the slot
     * @param criticalSlot the system critical slot to salvage
     * @param unit         the unit that owns it
     */
    public static void salvageSystemCrit(int location, CriticalSlot criticalSlot, Entity unit) {
        if ((criticalSlot.getIndex() >= Mek.SYSTEM_LIFE_SUPPORT) && (criticalSlot.getIndex() <= Mek.SYSTEM_GYRO)) {
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = unit.getCritical(location, slot);

                if (crit == null) {
                    continue;
                }

                if (crit.getIndex() != criticalSlot.getIndex()) {
                    continue;
                }

                UnitUtils.salvageCriticalSlot(crit);
            }
        } else {
            UnitUtils.salvageCriticalSlot(criticalSlot);
        }
    }

    /**
     * Repairs every damaged/breached engine critical slot on a unit (all locations are scanned, though in
     * practice engine slots only ever live in the torso).
     *
     * @param unit the unit whose engine is to be repaired
     */
    public static void repairDamagedEngine(Entity unit) {
        for (int x = 0; x < unit.locations(); x++) {
            for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
                CriticalSlot criticalSlot = unit.getCritical(x, y);

                if (!UnitUtils.isEngineCrit(criticalSlot)) {
                    continue;
                }

                UnitUtils.fixCriticalSlot(criticalSlot, criticalSlot.isBreached());
                unit.setCritical(x, y, criticalSlot);
            }
        }
    }

    /**
     * Clears damage/breach flags on a single critical slot (and its mounted equipment, if any). When
     * {@code breach} is {@code true} only the breached/repairing flags are cleared (a breach can be fixed without
     * a full repair); otherwise the slot is fully repaired: destroyed/hit/missing/repairing are all cleared.
     *
     * @param criticalSlot the slot to fix
     * @param breach       {@code true} to clear only the breach state, {@code false} to fully repair the slot
     */
    public static void fixCriticalSlot(CriticalSlot criticalSlot, boolean breach) {
        if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
            Mounted<?> mounted = criticalSlot.getMount();
            if (breach) {
                mounted.setBreached(false);
            } else {
                mounted.setDestroyed(false);
                mounted.setMissing(false);
                mounted.setHit(false);
            }
        }

        if (breach) {
            criticalSlot.setBreached(false);
            criticalSlot.setRepairing(false);
        } else {
            criticalSlot.setDestroyed(false);
            criticalSlot.setHit(false);
            criticalSlot.setRepairing(false);
            criticalSlot.setMissing(false);
        }
    }

    /**
     * Repairs a system critical slot, mirroring {@link #salvageSystemCrit} but fixing rather than salvaging: gyro
     * and life-support/sensor slots (which occupy multiple critical slots) have every matching slot in the
     * location fixed, other system slots are fixed individually.
     *
     * @param location     the location containing the slot
     * @param criticalSlot the system critical slot to repair
     * @param unit         the unit that owns it
     */
    public static void repairSystemCrit(int location, CriticalSlot criticalSlot, Entity unit) {
        if ((criticalSlot.getIndex() >= Mek.SYSTEM_LIFE_SUPPORT) && (criticalSlot.getIndex() <= Mek.SYSTEM_GYRO)) {
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = unit.getCritical(location, slot);

                if (crit == null) {
                    continue;
                }

                if (crit.getIndex() != criticalSlot.getIndex()) {
                    continue;
                }

                UnitUtils.fixCriticalSlot(crit, crit.isBreached());
            }
        } else {
            UnitUtils.fixCriticalSlot(criticalSlot, criticalSlot.isBreached());
        }
    }

    /**
     * Returns the "amount" backing a repair/salvage target: for the special armor/internal slot codes this is the
     * current point total (front armor, rear armor, or internal structure points); for a real critical slot it
     * delegates to {@link #getNumberOfCrits(Entity, CriticalSlot)}.
     *
     * @param unit     the unit being inspected
     * @param slot     either a critical-slot index, or one of {@link #LOC_FRONT_ARMOR}, {@link #LOC_REAR_ARMOR},
     *                 {@link #LOC_INTERNAL_ARMOR}
     * @param location the location to inspect
     *
     * @return the armor/internal point count, or the number of critical slots occupied by the equipment in that slot
     */
    public static int getNumberOfCrits(Entity unit, int slot, int location) {
        if (slot == UnitUtils.LOC_FRONT_ARMOR) {
            return unit.getArmor(location, false);
        }

        if (slot == UnitUtils.LOC_REAR_ARMOR) {
            return unit.getArmor(location, true);
        }

        if (slot == UnitUtils.LOC_INTERNAL_ARMOR) {
            return unit.getInternal(location);
        }

        CriticalSlot criticalSlot = unit.getCritical(location, slot);

        return UnitUtils.getNumberOfCrits(unit, criticalSlot);
    }

    /**
     * Returns how many critical slots a given critical slot's contents occupy in total: all engine crits for an
     * engine slot, the equipment type's declared slot count for equipment, or the system's slot count (see
     * {@link #getNumberOfSystemCriticalSlots}) otherwise. Always returns at least 1 (and 0 only for a {@code null}
     * slot).
     *
     * @param unit         the unit being inspected
     * @param criticalSlot the critical slot to inspect, may be {@code null}
     *
     * @return the number of critical slots occupied, or 0 if {@code criticalSlot} is {@code null}
     */
    public static int getNumberOfCrits(Entity unit, CriticalSlot criticalSlot) {
        if (criticalSlot == null) {
            return 0;
        }

        int numberOfCrits = 1;
        // Engine return all engine crits
        if (UnitUtils.isEngineCrit(criticalSlot)) {
            numberOfCrits = UnitUtils.getNumberOfEngineCrits(unit);
        } else if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
            Mounted<?> mounted = criticalSlot.getMount();
            numberOfCrits = mounted.getType().getNumCriticalSlots(unit);
        } else {
            numberOfCrits = UnitUtils.getNumberOfSystemCriticalSlots(unit, criticalSlot);
        }

        // always return at least 1 crit.
        return Math.max(1, numberOfCrits);
    }

    /**
     * Counts how many engine critical slots a unit's engine occupies. For Meks this is a live count of
     * {@code SYSTEM_ENGINE} slots in the torso locations (standard engines occupy 6, XL/Light/XXL occupy more, per
     * BattleTech construction rules). Non-Mek units (Tanks, etc.) don't track individual engine crit slots in
     * MegaMek, so this returns a fixed value of 6 (equivalent to a standard engine) for them.
     *
     * @param unit An @Entity unit
     *
     * @return the number of engine crits an Entity has.
     *
     * @author Torren (Jason Tighe)
     */
    public static int getNumberOfEngineCrits(Entity unit) {
        int engines = 0;

        if (unit instanceof Mek) {
            // no reason to check for engines anywhere other then the torso
            for (int x = LOC_CENTER_TORSO; x <= LOC_LT; x++) {
                for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
                    CriticalSlot criticalSlot = unit.getCritical(x, y);
                    if (UnitUtils.isEngineCrit(criticalSlot)) {
                        engines++;
                    }
                }
            }
        } else {
            engines = 6;
        }
        return engines;
    }

    /**
     * Counts how many critical slots a Mek "system" (gyro, life support, or sensors) actually occupies, since
     * these can be duplicated across locations (e.g. a torso-mounted cockpit spreads sensor/life-support slots
     * across both side torsos). Actuators and other system indices above {@code SYSTEM_GYRO} always occupy exactly
     * 1 slot. Gyro slots are counted in the center torso; sensor/life-support slots are counted in the head, or
     * across center+side torsos if the Mek has a torso-mounted cockpit.
     * <p>
     * Despite the method name, this does not itself set anything to "repairing" — the misleading comment below
     * appears to have been copy-pasted from a sibling method.
     *
     * @param unit         the unit being inspected
     * @param criticalSlot a representative system critical slot (its index determines which system is counted)
     *
     * @return the number of critical slots this system occupies
     */
    // Sets multiple system crits to repairing.
    // Gyro Life support and Sensors.
    public static int getNumberOfSystemCriticalSlots(Entity unit, CriticalSlot criticalSlot) {
        int count = 0;

        // actuators are always 1.
        if (criticalSlot.getIndex() > Mek.SYSTEM_GYRO) {
            return 1;
        }

        if (criticalSlot.getIndex() == Mek.SYSTEM_GYRO) {
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(Mek.LOC_CENTER_TORSO); slot++) {
                CriticalSlot crit = unit.getCritical(Mek.LOC_CENTER_TORSO, slot);

                if ((crit == null) || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }

                if (crit.getIndex() == criticalSlot.getIndex()) {
                    count++;
                }
            }
        } else {
            if (((Mek) unit).getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
                for (int location = LOC_CENTER_TORSO; location <= LOC_LT; location++) {
                    count = getCriticalSlotCount(unit, criticalSlot, count, location);
                }
            } else {
                count = getCriticalSlotCount(unit, criticalSlot, count, LOC_HEAD);
            }
        }
        return count;
    }

    /** Adds to {@code count} the number of system critical slots in {@code location} matching {@code criticalSlot}'s index. */
    private static int getCriticalSlotCount(Entity unit, CriticalSlot criticalSlot, int count, int location) {
        for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot crit = unit.getCritical(location, slot);

            if ((crit == null) || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                continue;
            }

            if (crit.getIndex() == criticalSlot.getIndex()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Flags a critical slot (or its equivalents/duplicates) as under repair, dispatching to the appropriate
     * specialized handler: engine slots repair every engine slot, low-index system slots (gyro/life
     * support/sensors) repair every duplicate of that system, other system slots (actuators) repair just
     * themselves, and equipment slots repair either a single location or, for split equipment, every location the
     * equipment spans.
     *
     * @param unit         the unit being repaired
     * @param criticalSlot the slot the player selected to repair
     */
    public static void setRepairing(Entity unit, CriticalSlot criticalSlot) {
        if (UnitUtils.isEngineCrit(criticalSlot)) {
            UnitUtils.setRepairingEngines(unit);
        } else if (criticalSlot.getType() == CriticalSlot.TYPE_SYSTEM) {
            if (criticalSlot.getIndex() <= Mek.SYSTEM_GYRO) {
                UnitUtils.setRepairingSystems(unit, criticalSlot);
            } else {
                criticalSlot.setRepairing(true);
            }
        } else {
            Mounted<?> criticalSlotMount = criticalSlot.getMount();
            int location = criticalSlotMount.getLocation();

            if (criticalSlotMount.isSplit()) {
                UnitUtils.setRepairingSplit(criticalSlotMount, unit);
                return;
            }

            setRepairingForCriticalEquipmentSlot(unit, criticalSlotMount, location);
        }
    }

    /**
     * Flags every engine critical slot on a unit as under repair.
     *
     * @param unit the unit whose engine is being repaired
     */
    public static void setRepairingEngines(Entity unit) {
        for (int x = 0; x < unit.locations(); x++) {
            for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
                CriticalSlot criticalSlot = unit.getCritical(x, y);
                if (!UnitUtils.isEngineCrit(criticalSlot)) {
                    continue;
                }
                criticalSlot.setRepairing(true);
                unit.setCritical(x, y, criticalSlot);
            }
        }
    }

    /**
     * Flags every critical slot belonging to the same Mek system (gyro, or life support/sensors) as under repair,
     * following the same location logic as {@link #getNumberOfSystemCriticalSlots}.
     *
     * @param unit         the unit being repaired
     * @param criticalSlot a representative slot for the system being repaired (its index determines which)
     */
    // Sets multiple system crits to repairing.
    // Gyro Life support and Sensors.
    public static void setRepairingSystems(Entity unit, CriticalSlot criticalSlot) {
        if (criticalSlot.getIndex() == Mek.SYSTEM_GYRO) {
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(Mek.LOC_CENTER_TORSO); slot++) {
                CriticalSlot crit = unit.getCritical(Mek.LOC_CENTER_TORSO, slot);
                if ((crit == null) || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }

                if (crit.getIndex() == criticalSlot.getIndex()) {
                    crit.setRepairing(true);
                }
            }
        }// if its not a GYRO then its sensors or life support
        // as engines have already been filtered
        else {
            if (((Mek) unit).getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
                for (int location = LOC_CENTER_TORSO; location <= LOC_LT; location++) {
                    setRepairingSystemCriticalSlots(unit, criticalSlot, location);
                }
            }// Normal cockpit in the head.
            else {
                setRepairingSystemCriticalSlots(unit, criticalSlot, LOC_HEAD);
            }
        }
    }

    /**
     * Flags every critical slot of a split (arm/torso spanning) equipment mount as under repair. No-op for
     * non-Mek units.
     *
     * @param equipment the split mount being repaired
     * @param unit      the Mek that owns it
     */
    public static void setRepairingSplit(Mounted<?> equipment, Entity unit) {
        // Only Meks should have split weapons crits.
        if (!(unit instanceof Mek)) {
            return;
        }

        // can only split weapons in toros and arms.
        for (int x = LOC_CENTER_TORSO; x <= LOC_LEFT_ARM; x++) {
            setRepairingForCriticalEquipmentSlot(unit, equipment, x);
        }
    }

    /** Flags every critical slot in {@code location} holding {@code criticalSlotMount} as under repair. */
    private static void setRepairingForCriticalEquipmentSlot(Entity unit, Mounted<?> criticalSlotMount, int location) {
        for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot crit = unit.getCritical(location, slot);
            if (crit == null) {
                continue;
            }

            if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                Mounted<?> mounted = crit.getMount();

                if (criticalSlotMount.equals(mounted)) {
                    crit.setRepairing(true);
                }
            }// end getType() if
        }// end for
    }

    /** Flags every system critical slot in {@code location} matching {@code criticalSlot}'s index as under repair. */
    private static void setRepairingSystemCriticalSlots(Entity unit, CriticalSlot criticalSlot, int location) {
        for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot crit = unit.getCritical(location, slot);
            if ((crit == null) || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                continue;
            }

            if (crit.getIndex() == criticalSlot.getIndex()) {
                crit.setRepairing(true);
            }
        }
    }

    /**
     * Looks up the localized display name for one of this class's {@code TECH_*} skill-tier constants.
     *
     * @param tech one of {@link #TECH_GREEN}, {@link #TECH_REG}, {@link #TECH_VET}, {@link #TECH_ELITE},
     *             {@link #TECH_PILOT}, or {@link #TECH_REWARD_POINTS}
     *
     * @return the localized name, or an empty string if {@code tech} does not match a known constant
     */
    public static String techDescription(int tech) {
        return switch (tech) {
            case TECH_GREEN -> MESSAGES.getString("TECH_GREEN");
            case TECH_REG -> MESSAGES.getString("TECH_REGULAR");
            case TECH_VET -> MESSAGES.getString("TECH_VETERAN");
            case TECH_ELITE -> MESSAGES.getString("TECH_ELITE");
            case TECH_PILOT -> MESSAGES.getString("TECH_PILOT");
            case TECH_REWARD_POINTS -> MESSAGES.getString("TECH_REWARD_POINTS");
            default -> "";
        };
    }

    /**
     * Parses a free-form (case-insensitive) tech-tier name into the corresponding {@code TECH_*} constant.
     *
     * @param tech the tech-tier name, e.g. "veteran", "reg", "reward points"
     *
     * @return the matching {@code TECH_*} constant, defaulting to {@link #TECH_GREEN} if unrecognized
     */
    public static int techType(String tech) {
        String techString = tech.toLowerCase();

        return switch (techString) {
            case "regular", "reg" -> TECH_REG;
            case "vet", "veteran" -> TECH_VET;
            case "elite" -> TECH_ELITE;
            case "pilot" -> TECH_PILOT;
            case "reward points" -> TECH_REWARD_POINTS;
            default -> TECH_GREEN;
        };
    }

    /**
     * Convenience overload of {@link #getTechRoll(Entity, int, int, int, boolean, int, int, boolean)} that assumes
     * the repair is not being performed as a salvage-job (see that method for the full rules).
     */
    public static int getTechRoll(Entity unit, int location, int slot, int techType, boolean armor, int techLevel) {
        return UnitUtils.getTechRoll(unit, location, slot, techType, armor, techLevel, false);
    }

    /**
     * Computes the target number a tech must roll (on presumably a 2d6, given the modifiers below) to successfully
     * repair a specific armor point, internal-structure point, or critical slot. Starts from
     * {@link #techBaseRoll(int)} for the tech's skill tier, then applies BattleTech-flavored modifiers:
     * <ul>
     * <li>External armor is slightly easier (-1); repairing internal structure gets harder the more of the
     * location's IS is destroyed, and requires replacing the whole location (with an extra penalty) once internal
     * reaches 0.</li>
     * <li>A breached (but not being-salvaged) critical slot is a flat, easy target number of 2.</li>
     * <li>Equipment repairs are easier the fewer critical slots the item occupies were damaged, with special
     * cases for Heat Sinks (easier) and Jump Jets (harder); Omni-mech pods repaired with compatible tech get an
     * extra bonus.</li>
     * <li>Engine, sensor, gyro, life-support, and actuator repairs each have their own bespoke modifier curves
     * based on how many matching critical slots are damaged.</li>
     * <li>Repairing with tech whose {@code techLevel} isn't compatible with the unit (see
     * {@link #isCompatibleTech}) adds a further +4 penalty.</li>
     * </ul>
     * The final roll is clamped to a minimum of 3 (2 is treated as a special always-succeeds-ish breach case
     * handled separately above).
     *
     * @param unit     the unit being repaired
     * @param location the location containing the target
     * @param slot     a critical-slot index, or one of the {@code LOC_*_ARMOR} pseudo-slot codes for armor/IS
     * @param techType the tech's skill tier, one of the {@code TECH_*} constants
     * @param armor    {@code true} if repairing armor/internal structure rather than a critical slot
     * @param techLevel the MegaMek {@link TechConstants} tech level of the part/tech performing the repair
     * @param salvage  {@code true} if this roll is for a salvage attempt rather than a repair attempt (suppresses
     *                 the breached-slot shortcut target number of 2)
     *
     * @return the target number to roll to succeed, never lower than 3
     */
    public static int getTechRoll(Entity unit, int location, int slot, int techType, boolean armor, int techLevel,
          boolean salvage) {
        int roll = UnitUtils.techBaseRoll(techType);

        if (techType == TECH_REWARD_POINTS) {
            return 1;
        }

        if ((location < 0) || (slot < 0)) {
            return roll;
        }

        if (armor) {
            // External armor
            if (slot != LOC_INTERNAL_ARMOR) {
                roll--;
            }// Internal armor
            else {
                int armorToRepair = 0;
                if (unit.getInternal(location) > unit.getOInternal(location)) {
                    UnitUtils.removeArmorRepair(unit, LOC_INTERNAL_ARMOR, location);
                    armorToRepair = unit.getOInternal(location) - unit.getInternal(location);
                    UnitUtils.setArmorRepair(unit, LOC_INTERNAL_ARMOR, location);
                } else {
                    armorToRepair = unit.getOInternal(location) - unit.getInternal(location);
                }

                // has to replace the whole location.
                if (unit.getInternal(location) <= 0) {
                    if ((location == Mek.LOC_LEFT_ARM)
                              || (location == Mek.LOC_RIGHT_ARM)
                              || (location == Mek.LOC_RIGHT_LEG)
                              || (location == Mek.LOC_LEFT_LEG)) {
                        roll += 2;
                    } else if (location == Mek.LOC_HEAD) {
                        roll += 3;
                    } else {
                        roll += 4;
                    }
                } else if (armorToRepair <= (unit.getOInternal(location) / 4)) {
                    roll = UnitUtils.techBaseRoll(techType);
                } else if (armorToRepair <= (unit.getOInternal(location) / 2)) {
                    roll++;
                } else if (armorToRepair <= ((unit.getOInternal(location) * 3) / 4)) {
                    roll += 2;
                } else {
                    roll += 3;
                }
            }

        } else {
            CriticalSlot criticalSlot = unit.getCritical(location, slot);

            if (criticalSlot == null) {
                return roll;
            }

            if (criticalSlot.isBreached() && !salvage) {
                return 2;
            }

            if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                Mounted<?> m = criticalSlot.getMount();

                if (m != null) {
                    if (!m.isDestroyed() && !m.isBreached()) {
                        return roll;
                    } else if (m.getDesc().contains("Heat Sink")) {
                        roll--;
                    } else if (m.getDesc().contains("Jump Jet")) {
                        roll++;
                    }
                } else {
                    if (!criticalSlot.isMissing()) {
                        int crits = UnitUtils.getNumberOfCrits(unit, criticalSlot);

                        switch (crits) {
                            case 0:
                                roll++;
                                break;
                            case 1:
                                roll -= 2;
                                break;
                            case 2:
                                roll -= 1;
                                break;
                            case 3:
                                roll += 1;
                                break;
                            default:
                                roll += 3;
                                break;
                        }
                    } else {
                        roll++;
                    }
                }
                if (unit.isOmni() && UnitUtils.isCompatibleTech(unit, techLevel)) {
                    roll -= 4;
                }
            }// end CS type if
            else {
                if (UnitUtils.isEngineCrit(criticalSlot)) {
                    int crits = UnitUtils.getNumberOfDamagedEngineCrits(unit);
                    switch (crits) {
                        case 1:
                            break;
                        case 2:
                            roll++;
                            break;
                        default:
                            roll += 3;
                            break;
                    }
                } else {
                    if (criticalSlot.getIndex() == Mek.SYSTEM_SENSORS) {
                        int crits = unit.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                              Mek.SYSTEM_SENSORS,
                              Mek.LOC_HEAD);
                        if ((crits >= 2) && !criticalSlot.isMissing()) {
                            roll += 4;
                        } else if (crits > 0) {
                            roll++;
                        }
                    } else if (criticalSlot.getIndex() == Mek.SYSTEM_GYRO) {
                        if (criticalSlot.isMissing()) {
                            roll++;
                        } else {
                            int crits = unit.getBadCriticalSlots(
                                  CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO,
                                  Mek.LOC_CENTER_TORSO);
                            if (crits == 0) {
                                roll++;
                            } else if (crits == 1) {
                                roll += 2;
                            } else {
                                roll += 5;
                            }
                        }
                    } else if (criticalSlot.getIndex() == Mek.SYSTEM_LIFE_SUPPORT) {
                        if (!criticalSlot.isMissing()) {
                            int crits = unit.getBadCriticalSlots(
                                  CriticalSlot.TYPE_SYSTEM,
                                  Mek.SYSTEM_LIFE_SUPPORT, Mek.LOC_HEAD);
                            if (crits == 2) {
                                roll += 2;
                            }
                        }

                    } else if (UnitUtils.isActuator(criticalSlot)) {
                        if (criticalSlot.isMissing()) {
                            roll -= 2;
                        } else {
                            roll--;
                        }
                    }
                }
            }// end CS type else
            if (!UnitUtils.isCompatibleTech(unit, techLevel)) {
                roll += 4;
            }
        }

        // Had problems with repairing internals where the roll can change after
        // each retry.
        return Math.max(roll, 3);
    }

    /**
     * Computes the baseline (unmodified) target roll for a given tech skill tier: Green is a flat 9, and each
     * tier above that lowers the target by 1 (e.g. Regular=7 by this formula's {@code 8 - techType} with
     * {@code techType=1}... note Regular actually yields 7, Veteran 6, Elite 5).
     *
     * @param techType one of the {@code TECH_*} constants
     *
     * @return the base target roll before any situational modifiers from {@link #getTechRoll}
     */
    public static int techBaseRoll(int techType) {
        int roll = 9;

        if (techType != TECH_GREEN) {
            roll = 8 - techType;
        }

        return roll;
    }

    /**
     * Undoes an in-progress armor or internal-structure repair by walking the current value back down toward the
     * original ("O") value, in steps of 99 points at a time (a large step used simply to guarantee convergence in
     * a small number of loop iterations, since armor/IS point totals are always far below 99). This is the
     * counterpart to {@link #setArmorRepair}.
     *
     * @param unit     the unit being un-repaired
     * @param slot     one of the {@code LOC_*_ARMOR} pseudo-slot codes selecting front armor, rear armor, or
     *                 internal structure
     * @param location the location to adjust; a rear-facing pseudo-location ({@code LOC_CENTER_TORSOR} etc.) is
     *                 normalized back to its front-facing equivalent by subtracting 7
     */
    public static void removeArmorRepair(Entity unit, int slot, int location) {

        if (slot < UnitUtils.LOC_INTERNAL_ARMOR) {
            // in case something was fubared.
            if (location >= UnitUtils.LOC_CENTER_TORSOR) {
                location -= 7;
            }
            while (unit.getArmor(location, slot == UnitUtils.LOC_REAR_ARMOR) >
                         unit.getOArmor(location, slot == UnitUtils.LOC_REAR_ARMOR)) {
                int currArmor = unit.getArmor(location, slot == UnitUtils.LOC_REAR_ARMOR);
                currArmor -= 99;
                unit.setArmor(currArmor, location, slot == UnitUtils.LOC_REAR_ARMOR);
            }
        }// internal
        else {
            while (unit.getInternal(location) > unit.getOInternal(location)) {
                int currArmor = unit.getInternal(location);
                currArmor -= 99;
                unit.setInternal(currArmor, location);
            }
        }
    }

    /**
     * Marks an armor or internal-structure repair as in-progress by walking the current value up toward the
     * original ("O") value, in steps of 99 points at a time (see {@link #removeArmorRepair} for why 99). Once the
     * current value reaches the original, the loop stops — so this is idempotent and safe to call repeatedly.
     *
     * @param unit     the unit being repaired
     * @param slot     one of the {@code LOC_*_ARMOR} pseudo-slot codes selecting front armor, rear armor, or
     *                 internal structure
     * @param location the location to adjust; a rear-facing pseudo-location is normalized to front-facing first
     */
    public static void setArmorRepair(Entity unit, int slot, int location) {
        if (slot < LOC_INTERNAL_ARMOR) {
            if (location >= UnitUtils.LOC_CENTER_TORSOR) {
                location -= 7;
            }

            while (unit.getArmor(location, slot == UnitUtils.LOC_REAR_ARMOR) <
                         unit.getOArmor(location, slot == UnitUtils.LOC_REAR_ARMOR)) {
                int currArmor = unit.getArmor(location, slot == UnitUtils.LOC_REAR_ARMOR);
                currArmor += 99;
                unit.setArmor(currArmor, location, slot == UnitUtils.LOC_REAR_ARMOR);
            }
        }
        // internal
        else {
            while (unit.getInternal(location) < unit.getOInternal(location)) {
                int currArmor = unit.getInternal(location);
                currArmor += 99;
                unit.setInternal(currArmor, location);
            }
        }
    }

    /**
     * Determines whether a given repair tech level (from {@link TechConstants}) is compatible with a unit's own
     * tech level, i.e. whether the tech is qualified enough to work on this unit without a repair-roll penalty
     * (armor and internal structure are treated as universal elsewhere and never call this). Any "ALL"/"unknown"
     * placeholder tech level is always considered compatible. Otherwise each Clan/IS tech tier is compatible with
     * itself and any less-advanced tier below it (e.g. Clan Advanced tech can also work on Clan Experimental or
     * Unofficial units, but not vice versa).
     * <p>
     * <b>Known bug:</b> several {@code switch} cases below ({@code T_IS_ADVANCED}, {@code T_IS_TW_ALL},
     * {@code T_IS_TW_NON_BOX}) are missing {@code break} statements and fall through into the next case's checks.
     * In practice this makes those tiers stricter than intended — for example tech qualified for
     * {@code T_IS_ADVANCED} will also be run through (and potentially rejected by) the {@code T_IS_TW_ALL},
     * {@code T_IS_TW_NON_BOX}, and {@code T_INTRO_BOX_SET} checks that follow it, rather than returning after the
     * first matching case. This has not been fixed here since this pass is documentation-only.
     *
     * @param unit      the unit needing repair
     * @param techLevel the tech level of the tech/part attempting the repair
     *
     * @return {@code true} if the tech level is considered compatible (no roll penalty), {@code false} otherwise
     */
    public static boolean isCompatibleTech(Entity unit, int techLevel) {
        // armor and IS are universal everything else gets a +4 to the roll if the tech levels are not compatible.
        if ((techLevel != TechConstants.T_ALL)
                  && (techLevel != TechConstants.T_ALLOWED_ALL)
                  && (techLevel != TechConstants.T_TECH_UNKNOWN)) {
            if (unit.getTechLevel() != techLevel) {
                switch (unit.getTechLevel()) {
                    case TechConstants.T_CLAN_UNOFFICIAL:
                        if (techLevel != TechConstants.T_CLAN_UNOFFICIAL) {
                            return false;
                        }
                        break;
                    case TechConstants.T_CLAN_EXPERIMENTAL:
                        if ((techLevel != TechConstants.T_CLAN_EXPERIMENTAL)
                                  && (techLevel != TechConstants.T_CLAN_UNOFFICIAL)) {
                            return false;
                        }
                        break;
                    case TechConstants.T_CLAN_ADVANCED:
                        if ((techLevel != TechConstants.T_CLAN_ADVANCED)
                                  && (techLevel != TechConstants.T_CLAN_EXPERIMENTAL)
                                  && (techLevel != TechConstants.T_CLAN_UNOFFICIAL)) {
                            return false;
                        }
                        break;
                    case TechConstants.T_CLAN_TW:
                        if ((techLevel != TechConstants.T_CLAN_TW)
                                  && (techLevel != TechConstants.T_CLAN_ADVANCED)
                                  && (techLevel != TechConstants.T_CLAN_EXPERIMENTAL)
                                  && (techLevel != TechConstants.T_CLAN_UNOFFICIAL)) {
                            return false;
                        }
                        break;
                    case TechConstants.T_IS_UNOFFICIAL:
                        if (techLevel != TechConstants.T_IS_UNOFFICIAL) {
                            return false;
                        }
                        break;
                    case TechConstants.T_IS_EXPERIMENTAL:
                        if ((techLevel != TechConstants.T_IS_UNOFFICIAL)
                                  && (techLevel != TechConstants.T_IS_EXPERIMENTAL)) {
                            return false;
                        }
                        break;
                    case TechConstants.T_IS_ADVANCED:
                        if ((techLevel != TechConstants.T_IS_ADVANCED)
                                  && (techLevel != TechConstants.T_IS_UNOFFICIAL)
                                  && (techLevel != TechConstants.T_IS_EXPERIMENTAL)) {
                            return false;
                        }
                    case TechConstants.T_IS_TW_ALL:
                        if ((techLevel != TechConstants.T_IS_TW_ALL)
                                  && (techLevel != TechConstants.T_IS_ADVANCED)
                                  && (techLevel != TechConstants.T_IS_UNOFFICIAL)
                                  && (techLevel != TechConstants.T_IS_EXPERIMENTAL)) {
                            return false;
                        }
                    case TechConstants.T_IS_TW_NON_BOX:
                        if ((techLevel != TechConstants.T_IS_TW_NON_BOX)
                                  && (techLevel != TechConstants.T_IS_TW_ALL)
                                  && (techLevel != TechConstants.T_IS_ADVANCED)
                                  && (techLevel != TechConstants.T_IS_UNOFFICIAL)
                                  && (techLevel != TechConstants.T_IS_EXPERIMENTAL)) {
                            return false;
                        }
                    case TechConstants.T_INTRO_BOX_SET:
                        if ((techLevel != TechConstants.T_INTRO_BOX_SET)
                                  && (techLevel != TechConstants.T_IS_TW_NON_BOX)
                                  && (techLevel != TechConstants.T_IS_TW_ALL)
                                  && (techLevel != TechConstants.T_IS_ADVANCED)
                                  && (techLevel != TechConstants.T_IS_UNOFFICIAL)
                                  && (techLevel != TechConstants.T_IS_EXPERIMENTAL)) {
                            return false;
                        }
                }
            }
        }

        return true;
    }

    /**
     * Counts how many of a unit's engine critical slots are currently damaged (breached or damaged), scanning the
     * torso locations.
     *
     * @param unit the unit to inspect
     *
     * @return the number of damaged/breached engine critical slots
     */
    public static int getNumberOfDamagedEngineCrits(Entity unit) {
        int engineHits = 0;
        // no reason to check for engines anywhere other then the torso
        for (int x = LOC_CENTER_TORSO; x <= LOC_LT; x++) {
            for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
                CriticalSlot criticalSlot = unit.getCritical(x, y);
                if (UnitUtils.isEngineCrit(criticalSlot) && (criticalSlot.isBreached() || criticalSlot.isDamaged())) {
                    engineHits++;
                }
            }
        }
        return engineHits;
    }

    /**
     * Tests whether a critical slot is one of the Mek limb actuator system slots (hand, lower/upper arm, shoulder,
     * foot, lower/upper leg, hip).
     *
     * @param criticalSlot the slot to check
     *
     * @return {@code true} if it is a system slot for one of the actuator types
     */
    public static boolean isActuator(CriticalSlot criticalSlot) {
        if (criticalSlot.getType() != CriticalSlot.TYPE_SYSTEM) {
            return false;
        }

        return (criticalSlot.getIndex() == Mek.ACTUATOR_FOOT)
                     || (criticalSlot.getIndex() == Mek.ACTUATOR_HAND)
                     || (criticalSlot.getIndex() == Mek.ACTUATOR_HIP)
                     || (criticalSlot.getIndex() == Mek.ACTUATOR_LOWER_ARM)
                     || (criticalSlot.getIndex() == Mek.ACTUATOR_LOWER_LEG)
                     || (criticalSlot.getIndex() == Mek.ACTUATOR_SHOULDER)
                     || (criticalSlot.getIndex() == Mek.ACTUATOR_UPPER_ARM)
                     || (criticalSlot.getIndex() == Mek.ACTUATOR_UPPER_LEG);
    }

    /**
     * The inverse of {@link #setRepairing}: clears the "under repair" flag from a critical slot, its system
     * duplicates, or every location a split equipment mount spans, as appropriate for the slot type.
     *
     * @param unit         the unit whose repair is being cancelled
     * @param criticalSlot the slot the player deselected/cancelled
     */
    public static void removeRepairing(Entity unit, CriticalSlot criticalSlot) {
        if (UnitUtils.isEngineCrit(criticalSlot)) {
            UnitUtils.removeRepairDamagedEngine(unit);
        } else if (criticalSlot.getType() == CriticalSlot.TYPE_SYSTEM) {
            if (criticalSlot.getIndex() <= Mek.SYSTEM_GYRO) {
                UnitUtils.removeRepairingSystems(unit, criticalSlot);
            } else {
                criticalSlot.setRepairing(false);
            }
        } else {
            Mounted<?> eq = criticalSlot.getMount();
            int location = eq.getLocation();

            UnitUtils.removeRepairEquipment(eq, unit, location);
        }// end else
    }// end removeRepairing

    /**
     * Clears the "under repair" flag from every engine critical slot that has it set. Despite the misleading
     * inherited javadoc summary ("Repairs all the engines"), this does not repair anything — it only cancels a
     * pending repair-in-progress marker; see {@link #repairDamagedEngine} for the method that actually fixes
     * engine damage.
     *
     * @param unit the unit whose engine repair is being cancelled
     */
    public static void removeRepairDamagedEngine(Entity unit) {
        for (int x = 0; x < unit.locations(); x++) {
            for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
                CriticalSlot criticalSlot = unit.getCritical(x, y);
                if (!UnitUtils.isEngineCrit(criticalSlot)) {
                    continue;
                }

                if (criticalSlot.isRepairing()) {
                    criticalSlot.setRepairing(false);
                    unit.setCritical(x, y, criticalSlot);
                }
            }
        }
    }

    /**
     * Clears the "under repair" flag from every critical slot belonging to the same Mek system (gyro, or life
     * support/sensors), mirroring {@link #setRepairingSystems}.
     *
     * @param unit         the unit whose repair is being cancelled
     * @param criticalSlot a representative slot for the system (its index determines which)
     */
    // Sets multiple system crits to repairing.
    // Gyro Life support and Sensors.
    public static void removeRepairingSystems(Entity unit, CriticalSlot criticalSlot) {
        if (criticalSlot.getIndex() == Mek.SYSTEM_GYRO) {
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(Mek.LOC_CENTER_TORSO); slot++) {
                CriticalSlot crit = unit.getCritical(Mek.LOC_CENTER_TORSO, slot);

                if ((crit == null) || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }

                if (crit.getIndex() == criticalSlot.getIndex()) {
                    crit.setRepairing(false);
                }
            }
        }// if it's not a GYRO then its sensors or life support
        // as engines have already been filtered
        else {
            if (((Mek) unit).getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
                for (int location = LOC_CENTER_TORSO; location <= LOC_LT; location++) {
                    removeRepairingCriticalSlotSystems(unit, criticalSlot, location);
                }
            } else {
                removeRepairingCriticalSlotSystems(unit, criticalSlot, LOC_HEAD);
            }
        }
    }

    /**
     * Some EQ can take up multiple slots this will track them down and repair them.
     *
     */
    /**
     * Clears the "under repair" flag from every critical slot backing a piece of equipment, delegating to
     * {@link #removeRepairSplitEquipment} for split equipment.
     *
     * @param equipment the mounted equipment whose repair is being cancelled
     * @param unit      the unit that owns it
     * @param location  the location to scan for this equipment's critical slots
     */
    public static void removeRepairEquipment(Mounted<?> equipment, Entity unit, int location) {

        if (equipment.isSplit()) {
            UnitUtils.removeRepairSplitEquipment(equipment, unit);
            return;
        }

        removeRepairingCriticalSlotEquipment(equipment, unit, location);
    }

    /** Clears the "under repair" flag from every system critical slot in {@code location} matching {@code criticalSlot}'s index. */
    private static void removeRepairingCriticalSlotSystems(Entity unit, CriticalSlot criticalSlot, int location) {
        for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot crit = unit.getCritical(location, slot);

            if ((crit == null) || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                continue;
            }

            if (crit.getIndex() == criticalSlot.getIndex()) {
                crit.setRepairing(false);
            }
        }
    }

    /**
     * Clears the "under repair" flag from every critical slot of a split (arm/torso spanning) equipment mount.
     * No-op for non-Mek units.
     *
     * @param equipment the split mount whose repair is being cancelled
     * @param unit      the Mek that owns it
     */
    public static void removeRepairSplitEquipment(Mounted<?> equipment, Entity unit) {

        // Only Meks should have split weapons crits.
        if (!(unit instanceof Mek)) {
            return;
        }

        // can only split weapons in toros and arms.
        for (int x = LOC_CENTER_TORSO; x <= LOC_LEFT_ARM; x++) {
            removeRepairingCriticalSlotEquipment(equipment, unit, x);
        }
    }

    /** Clears the "under repair" flag from every critical slot in {@code location} holding {@code equipment}. */
    private static void removeRepairingCriticalSlotEquipment(Mounted<?> equipment, Entity unit, int location) {
        for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot crit = unit.getCritical(location, slot);

            if (crit == null) {
                continue;
            }

            if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                Mounted<?> mounted = crit.getMount();

                if (equipment.equals(mounted) && crit.isRepairing()) {
                    crit.setRepairing(false);
                    unit.setCritical(location, slot, crit);
                }
            }// end getType() if
        }// end for
    }

    /**
     * Sums the C-bill cost of repairing every damaged part on a unit: front (and rear, for torsos) armor, internal
     * structure, and every critical slot in every location, via repeated calls to {@link #getPartCost}.
     *
     * @param unit the unit to price out
     * @param year the game year, used to select era-appropriate costs for some equipment
     *
     * @return the total repair cost in C-bills, rounded up
     */
    public static int getTotalDamagedPartCost(Entity unit, int year) {
        double totalCost = 0;

        for (int location = 0; location < unit.locations(); location++) {
            if ((location == LOC_CENTER_TORSO) || (location == LOC_RT) || (location == LOC_LT)) {
                totalCost += UnitUtils.getPartCost(unit, location, LOC_FRONT_ARMOR, true, year);
                totalCost += UnitUtils.getPartCost(unit, location, LOC_REAR_ARMOR, true, year);
                totalCost += UnitUtils.getPartCost(unit, location, LOC_INTERNAL_ARMOR, true, year);
            } else {
                totalCost += UnitUtils.getPartCost(unit, location, LOC_FRONT_ARMOR, true, year);
                totalCost += UnitUtils.getPartCost(unit, location, LOC_INTERNAL_ARMOR, true, year);
            }
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
                totalCost += UnitUtils.getPartCost(unit, location, slot, false, year);
            }
        }

        return (int) Math.ceil(totalCost);
    }

    /**
     * Computes the C-bill cost to repair a single damaged part of a unit: an armor/internal-structure point range,
     * or one critical slot's contents. Currently only implemented for {@link Mek}s — any other unit type always
     * returns 0.
     * <p>
     * For armor, cost is derived from the armor type's per-ton cost and points-per-ton multiplier; for internal
     * structure, from the structure type's per-ton cost divided into 8 points/ton. For critical slots: heat sinks
     * have flat costs by tech era (single vs. double/compact), other equipment uses its own {@code getCost}, the
     * engine's cost is prorated by the fraction of its critical slots that are damaged, and system slots (sensors,
     * gyro, life support, cockpit, actuators) use hardcoded BattleTech-construction-rule formulas keyed off the
     * unit's tonnage, walk MP, gyro type, and cockpit type. Undamaged, breached, or {@code null} slots cost
     * nothing (breached parts are assumed free to un-breach, or simply not resolved yet).
     *
     * @param unit     the unit being priced (must be a {@link Mek} to get a non-zero result)
     * @param location the location containing the part
     * @param slot     a critical-slot index, or one of the {@code LOC_*_ARMOR} pseudo-slot codes when {@code armor}
     *                 is {@code true}
     * @param armor    {@code true} to price armor/internal structure, {@code false} to price a critical slot
     * @param year     the game year, used to select era-appropriate heat sink costs
     *
     * @return the repair cost in C-bills, rounded up, or 0 if not a Mek or nothing needs repair
     */
    public static int getPartCost(Entity unit, int location, int slot, boolean armor, int year) {
        double cost = 0;

        if (!(unit instanceof Mek mek)) {
            return 0;
        }

        if (armor) {
            // External Armor
            if (slot < LOC_INTERNAL_ARMOR) {
                ArmorType armorType = ArmorType.forEntity(unit);
                double points = 16.0 * armorType.getArmorPointsMultiplier();
                double costPerTon = armorType.getCost();

                // just in case
                if (points == 0) {
                    points = 16;
                }

                cost = costPerTon / points;
                boolean rear = slot == LOC_REAR_ARMOR;

                cost = (mek.getOArmor(location, rear) - mek.getArmor(location, rear)) * cost;
            }// IS Armor
            else {
                double structureCost = EquipmentType.getStructureCost(mek.getStructureType());// IS

                cost = structureCost / 8;
                cost = (mek.getOInternal(location) - mek.getInternal(location)) * cost;
            }
        } else {// Crit
            CriticalSlot criticalSlot = unit.getCritical(location, slot);

            if (criticalSlot == null) {
                return 0;
            }

            if (criticalSlot.isBreached()) {
                return 0;
            }

            if (!criticalSlot.isDamaged()) {
                return 0;
            }

            if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                Mounted<?> mounted = criticalSlot.getMount();

                if (mounted.getDesc().contains("Heat Sink")) {
                    if (mounted.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                        if ((mounted.getType().getTechLevel(year) == TechConstants.T_IS_ADVANCED) ||
                                  (mounted.getType().getTechLevel(year) == TechConstants.T_IS_EXPERIMENTAL)) {
                            cost = 3000;
                        } else {
                            cost = 2000;
                        }
                    } else {
                        // Double heat sinks or 2 compact heat sinks in one slot
                        // both cost the same.
                        cost = 6000;
                    }
                } else {
                    cost = (int) mounted.getType().getCost(mek, mounted.isArmored(), mounted.getLocation());
                }

            } else {
                if (UnitUtils.isEngineCrit(criticalSlot)) {
                    Engine engine = mek.getEngine();
                    cost = (engine.getBaseCost() * engine.getRating() * mek.getWeight()) / 75.0;
                    double totalEngineCrits = UnitUtils.getNumberOfEngineCrits(unit);
                    double damagedEngineCrits = UnitUtils.getNumberOfDamagedEngineCrits(unit);
                    cost = cost * (damagedEngineCrits / totalEngineCrits);
                } else {
                    if (criticalSlot.getIndex() == Mek.SYSTEM_SENSORS) {
                        cost = mek.getWeight() * 2000;// sensors
                    } else if (criticalSlot.getIndex() == Mek.SYSTEM_GYRO) {
                        if (mek.getGyroType() == Mek.GYRO_XL) {
                            cost = 750000 * (int) Math.ceil((mek.getOriginalWalkMP() * mek.getWeight()) / 100f) * 0.5;
                        } else if (mek.getGyroType() == Mek.GYRO_COMPACT) {
                            cost = 400000 * (int) Math.ceil((mek.getOriginalWalkMP() * mek.getWeight()) / 100f) * 1.5;
                        } else if (mek.getGyroType() == Mek.GYRO_HEAVY_DUTY) {
                            cost = 500000 * (int) Math.ceil((mek.getOriginalWalkMP() * mek.getWeight()) / 100f) * 2;
                        } else {
                            cost = 300000 * (int) Math.ceil((mek.getOriginalWalkMP() * mek.getWeight()) / 100f);
                        }
                    } else if (criticalSlot.getIndex() == Mek.SYSTEM_LIFE_SUPPORT) {
                        cost = 50000;// life support
                    } else if (criticalSlot.getIndex() == Mek.SYSTEM_COCKPIT) {
                        if (mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
                            cost = 750000;
                        } else if (mek.getCockpitType() == Mek.COCKPIT_SMALL) {
                            cost = 175000;
                        } else {
                            cost = 200000;
                        }
                    } else if (UnitUtils.isActuator(criticalSlot)) {
                        if (criticalSlot.getIndex() == Mek.ACTUATOR_HAND) {
                            cost = mek.getWeight() * 80;
                        } else if (criticalSlot.getIndex() == Mek.ACTUATOR_LOWER_ARM) {
                            cost = mek.getWeight() * 50;
                        } else if ((criticalSlot.getIndex() == Mek.ACTUATOR_UPPER_ARM) ||
                                         (criticalSlot.getIndex() == Mek.ACTUATOR_SHOULDER)) {
                            cost = mek.getWeight() * 100;
                        } else if (criticalSlot.getIndex() == Mek.ACTUATOR_FOOT) {
                            cost = mek.getWeight() * 120;
                        } else if (criticalSlot.getIndex() == Mek.ACTUATOR_LOWER_LEG) {
                            cost = mek.getWeight() * 80;
                        } else if ((criticalSlot.getIndex() == Mek.ACTUATOR_UPPER_LEG) ||
                                         (criticalSlot.getIndex() == Mek.ACTUATOR_HIP)) {
                            cost = mek.getWeight() * 150;
                        }
                    }

                }
            }
        }

        return (int) Math.ceil(cost);
    }

    /**
     * Builds a localized, human-readable status message explaining why a given armor/internal/critical-slot
     * repair target either cannot be repaired or is already fully repaired — intended for display in the repair
     * bay UI. Returns an empty string when the target genuinely needs (and can have) repair work done, in which
     * case the UI presumably shows the normal repair controls instead of this message.
     * <p>
     * Order of checks: a cored Mek (destroyed center torso) or Tank (any front hull location destroyed) reports a
     * single "cored" message and skips everything else; arms/side-torsos needing adjacent internal structure
     * repaired first are flagged; then, per BattleTech repair sequencing rules (armor before internal structure,
     * outer parts before what they protect), the specific armor/internal/critical-slot completion state is
     * reported.
     *
     * @param unit     the unit being inspected
     * @param location the location containing the target (a rear pseudo-location is normalized to front first)
     * @param slot     a critical-slot index, or one of the {@code LOC_*_ARMOR} pseudo-slot codes when {@code armor}
     *                 is {@code true}
     * @param armor    {@code true} to check armor/internal structure, {@code false} to check a critical slot
     *
     * @return a localized status message, or an empty string if the target still needs (and can have) repair
     */
    public static String getRepairMessage(Entity unit, int location, int slot, boolean armor) {
        String repairMessage = "";

        if ((unit instanceof Mek) && (unit.getInternal(UnitUtils.LOC_CENTER_TORSO) < 1)) {
            return MESSAGES.getString("getRepairMessage.Cored");
        }

        if (unit instanceof Tank) {
            // Turrets can be blown off, and you can still repair the unit.
            for (int loc = Tank.LOC_FRONT; loc < Tank.LOC_TURRET; loc++) {
                if (unit.getInternal(loc) < 1) {
                    return MESSAGES.getString("getRepairMessage.Cored");
                }
            }
        }

        if (((location == UnitUtils.LOC_RIGHT_ARM) &&
                   (unit.getInternal(UnitUtils.LOC_RT) != unit.getOInternal(UnitUtils.LOC_RT)))
                  ||
                  ((location == UnitUtils.LOC_LEFT_ARM) &&
                         (unit.getInternal(UnitUtils.LOC_LT) != unit.getOInternal(UnitUtils.LOC_LT)))) {
            return MESSAGES.getString("getRepairMessage.MustRepairAdjacentInternalStructure",
                  unit.getShortNameRaw(),
                  unit.getLocationName(location));
        }

        if (location >= UnitUtils.LOC_CENTER_TORSOR) {
            location -= 7;
        }

        if (armor) {
            int armorRepaired = 0;
            boolean rear = (slot == UnitUtils.LOC_REAR_ARMOR);
            if (slot < UnitUtils.LOC_INTERNAL_ARMOR) {
                armorRepaired = unit.getOArmor(location, rear) - unit.getArmor(location, rear);

                if (armorRepaired == 0) {
                    if (rear) {
                        repairMessage = MESSAGES.getString("getRepairMessage.ExternalArmorRepairedRear",
                              unit.getLocationAbbr(location));
                    } else {
                        repairMessage = MESSAGES.getString("getRepairMessage.ExternalArmorRepaired",
                              unit.getLocationAbbr(location));
                    }
                }
            } else {
                armorRepaired = unit.getOInternal(location) - unit.getInternal(location);

                if (armorRepaired == 0) {
                    repairMessage = MESSAGES.getString("getRepairMessage.InternalStructureRepaired",
                          unit.getLocationAbbr(location));
                }

            }

        } else {// crits
            if (unit.getInternal(location) != unit.getOInternal(location)) {
                repairMessage = MESSAGES.getString("getRepairMessage.InternalStructureRepairRequired",
                      unit.getLocationAbbr(location));
            }

            CriticalSlot criticalSlot = unit.getCritical(location, slot);
            Mounted<?> mount = null;

            if (criticalSlot == null) {
                repairMessage = MESSAGES.getString("getRepairMessage.NoCriticalSlotInThatLocationNeedingRepair",
                      unit.getLocationAbbr(location));
            } else {
                if (!UnitUtils.isActuator(criticalSlot)) {
                    mount = criticalSlot.getMount();
                }

                if (mount != null) {
                    if (!mount.isDestroyed() && !mount.isBreached()
                              && !mount.isMissing() && !criticalSlot.isDamaged()
                              && !criticalSlot.isBreached()) {
                        repairMessage = MESSAGES.getString("getRepairMessage.CriticalSlotIsNotDamaged",
                              unit.getLocationAbbr(location));
                    }
                } else if (!criticalSlot.isDamaged() && !criticalSlot.isBreached()) {
                    repairMessage = MESSAGES.getString("getRepairMessage.CriticalSlotIsNotDamaged",
                          unit.getLocationAbbr(location));
                }
            }
        }

        return repairMessage;
    }

    /**
     * The salvage-side counterpart to {@link #getRepairMessage}: builds a localized status message explaining why
     * a given armor/internal/critical-slot target either cannot be salvaged right now or has already been fully
     * stripped, for display in the salvage bay UI. Returns an empty string when the target can still be salvaged.
     * <p>
     * Internal structure salvage is blocked while an adjacent arm still has internal structure (torso internal
     * structure protects the arm's shoulder actuator) or while the location still has undamaged or in-progress
     * critical slots (per BattleTech disassembly order: equipment must be pulled before structure, structure
     * before armor's "innermost" layer conceptually, though this method addresses IS directly).
     *
     * @param unit     the unit being inspected
     * @param location the location containing the target (a rear pseudo-location is normalized to front first)
     * @param slot     a critical-slot index, or one of the {@code LOC_*_ARMOR} pseudo-slot codes when {@code armor}
     *                 is {@code true}
     * @param armor    {@code true} to check armor/internal structure, {@code false} to check a critical slot
     *
     * @return a localized status message, or an empty string if the target can still be salvaged
     */
    public static String getSalvageMessage(Entity unit, int location, int slot, boolean armor) {
        String salvageMessage = "";

        if ((armor && (slot == UnitUtils.LOC_INTERNAL_ARMOR))) {
            if (((location == UnitUtils.LOC_RT) && (unit.getInternal(Mek.LOC_RIGHT_ARM) > 0))
                      || ((location == UnitUtils.LOC_LT) && (unit.getInternal(UnitUtils.LOC_LEFT_ARM) > 0))) {
                return MESSAGES.getString(
                      "getSalvageMessage.MayNotSalvageUntilAdjacentArmInternalStructureIsFullyRepaired",
                      unit.getShortNameRaw(),
                      unit.getLocationName(location));
            }

            if ((location == UnitUtils.LOC_CENTER_TORSO)
                      && (unit.getInternal(UnitUtils.LOC_LEFT_ARM) > 0)
                      && (unit.getInternal(UnitUtils.LOC_RIGHT_ARM) > 0)) {
                return MESSAGES.getString(
                      "getSalvageMessage.MayNotSalvageUntilAdjacentTorsoInternalStructureIsFullyRepaired",
                      unit.getShortNameRaw(),
                      unit.getLocationName(location));
            }

            if (UnitUtils.hasUndamagedCriticalSlots(unit, location)
                      || UnitUtils.hasCriticalSlotsUnderRepair(unit, location)) {
                return MESSAGES.getString(
                      "getSalvageMessage.MayNotSalvageInternalStructureUntilPartsRemoved",
                      unit.getShortNameRaw(),
                      unit.getLocationName(location));
            }
        }

        if (location >= UnitUtils.LOC_CENTER_TORSOR) {
            location -= 7;
        }

        if (armor) {

            int armorLeft = 0;
            boolean rear = (slot == UnitUtils.LOC_REAR_ARMOR);
            if (slot < UnitUtils.LOC_INTERNAL_ARMOR) {
                armorLeft = unit.getArmor(location, rear);

                if (armorLeft == 0) {
                    if (rear) {
                        salvageMessage =
                              MESSAGES.getString("getSalvageMessage.AllExternalArmorAlreadyRemovedRear",
                                    unit.getLocationAbbr(location));
                    } else {
                        salvageMessage = MESSAGES.getString("getSalvageMessage.AllExternalArmorAlreadyRemoved",
                              unit.getLocationAbbr(location));

                    }
                }
            } else {
                armorLeft = unit.getInternal(location);

                if (armorLeft == 0) {
                    salvageMessage = MESSAGES.getString("getSalvageMessage.AllInternalStructureAlreadyRemoved",
                          unit.getLocationAbbr(location));

                }

            }

        } else {// crits
            CriticalSlot criticalSlot = unit.getCritical(location, slot);

            if (criticalSlot == null) {
                salvageMessage = MESSAGES.getString("getSalvageMessage.NoCriticalSlotInThatLocationThatCanBeSalvaged");
            } else {
                if (UnitUtils.isNonRepairableCrit(unit, criticalSlot) || criticalSlot.isDamaged()) {
                    salvageMessage = MESSAGES.getString("getSalvageMessage.CriticalSlotCanNotBeSalvaged");
                }
            }
        }

        return salvageMessage;
    }

    /**
     * Checks whether a Mek/Tank location still has any critical slot that is neither damaged nor considered
     * "non-repairable" (see {@link #isNonRepairableCrit}) — i.e. equipment that would still need to be removed
     * before internal structure there could be salvaged.
     *
     * @param unit     the unit to check
     * @param location the location to check
     *
     * @return {@code true} if any such slot exists
     */
    public static boolean hasUndamagedCriticalSlots(Entity unit, int location) {
        if ((unit instanceof Mek) || (unit instanceof Tank)) {
            for (int y = 0; y < unit.getNumberOfCriticalSlots(location); y++) {
                CriticalSlot criticalSlot = unit.getCritical(location, y);
                if ((criticalSlot != null) &&
                          !criticalSlot.isDamaged() &&
                          !UnitUtils.isNonRepairableCrit(unit, criticalSlot)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether any critical slot in a Mek/Tank location currently has the "under repair" flag set.
     *
     * @param unit     the unit to check
     * @param location the location to check
     *
     * @return {@code true} if any slot in the location is mid-repair
     */
    public static boolean hasCriticalSlotsUnderRepair(Entity unit, int location) {
        if ((unit instanceof Mek) || (unit instanceof Tank)) {
            for (int y = 0; y < unit.getNumberOfCriticalSlots(location); y++) {
                CriticalSlot criticalSlot = unit.getCritical(location, y);
                if ((criticalSlot != null) && criticalSlot.isRepairing()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks equipment critical slots for special armor/structure/misc types that are represented as critical
     * slots but conceptually aren't "repaired" the normal way — e.g. Ferro-Fibrous/Reactive/Reflective/Stealth
     * armor slots, Endo Steel/Composite/Reinforced internal structure slots, TSM, and (for Clan units) CASE.
     * These are identified heuristically by checking whether the mounted equipment's description text contains
     * the type's display name. Non-equipment critical slots (systems) always return {@code false}. Any exception
     * while inspecting the mount (e.g. an unexpected {@code null}) is logged and treated as "repairable"
     * ({@code false}).
     * <p>
     * Note: the checks for {@code T_STRUCTURE_REINFORCED} appear twice in a row (lines checking the same
     * constant), which looks like a copy-paste duplicate rather than an intentional second check.
     *
     * @param unit         the unit that owns the slot (currently unused by the logic, but part of the signature)
     * @param criticalSlot the critical slot to check
     *
     * @return {@code true} if this slot represents a special armor/structure/misc type that doesn't need normal
     *       repair tracking
     */
    public static boolean isNonRepairableCrit(Entity unit, CriticalSlot criticalSlot) {
        // only equipment slots should be checked
        if (criticalSlot.getType() != CriticalSlot.TYPE_EQUIPMENT) {
            return false;
        }

        try {
            Mounted<?> mounted = criticalSlot.getMount();

            if (mounted.getDesc().contains(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS))) {
                return true;
            }

            if (mounted.getDesc().contains(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO))) {
                return true;
            }

            if (mounted.getDesc().contains(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HARDENED))) {
                return true;
            }

            if (mounted.getDesc().contains(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_FERRO))) {
                return true;
            }

            if (mounted.getDesc().contains(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_FERRO))) {
                return true;
            }

            if (mounted.getDesc().contains(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PATCHWORK))) {
                return true;
            }

            if (mounted.getDesc().contains(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE))) {
                return true;
            }

            if (mounted.getDesc().contains(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE))) {
                return true;
            }

            if (mounted.getDesc().contains(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH))) {
                return true;
            }

            if (mounted.getDesc().contains(EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_ENDO_STEEL))) {
                return true;
            }

            if (mounted.getDesc().contains(EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_COMPOSITE))) {
                return true;
            }

            if (mounted.getDesc()
                      .contains(EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE))) {
                return true;
            }

            if (mounted.getDesc().contains(EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_REINFORCED))) {
                return true;
            }

            if (mounted.getDesc().contains(EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_REINFORCED))) {
                return true;
            }

            if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_TSM)) {
                return true;
            }

            if ((mounted.getType() instanceof MiscType) &&
                      mounted.getType().hasFlag(MiscType.F_CASE) &&
                      unit.isClan()) {
                return true;
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Error in UnitUtils.isNonRepairableCrit");
            return false;
        }

        return false;
    }

    /**
     * Checks whether a repair target is currently eligible to be worked on at all, independent of whether it is
     * already fully repaired. Armor is always viable to work on; internal structure/critical slots require the
     * location's internal structure to already match its original value (i.e. any deeper IS damage must be fixed
     * before crits/armor logic proceeds), and arm locations additionally require the adjacent torso's internal
     * structure to already be fully repaired. Note the method name is misspelled ("Viabile" instead of "Viable")
     * — kept as-is since renaming would change the public API.
     *
     * @param unit     the unit being inspected
     * @param location the location containing the target
     * @param slot     a critical-slot index (unused when {@code armor} is {@code true})
     * @param armor    {@code true} to check armor eligibility, {@code false} to check critical-slot eligibility
     *
     * @return {@code true} if the target can currently be repaired/salvaged
     */
    public static boolean isRepairViabile(Entity unit, int location, int slot, boolean armor) {
        if (((location == UnitUtils.LOC_RIGHT_ARM) &&
                   (unit.getInternal(UnitUtils.LOC_RT) != unit.getOInternal(UnitUtils.LOC_RT)))
                  ||
                  ((location == UnitUtils.LOC_LEFT_ARM) &&
                         (unit.getInternal(UnitUtils.LOC_LT) != unit.getOInternal(UnitUtils.LOC_LT)))) {
            return false;
        }

        if (armor) {
            return true;
        }

        return unit.getInternal(location) == unit.getOInternal(location);
    }

    /**
     * Checks whether a unit has a Targeting Computer mounted, by scanning its misc equipment for the
     * {@code F_TARGETING_COMPUTER} flag.
     * <p>
     * This logic was intentionally duplicated here from MegaMek (rather than calling MegaMek's own equivalent)
     * to work around a MegaMek bug where a damaged Targeting Computer's bonus could be doubled by re-repairing it
     * after the TC had already been (incorrectly) set to some other value.
     *
     * @param unit the unit to check
     *
     * @return {@code true} if a Targeting Computer is mounted
     */
    public static boolean hasTargetingComputer(Entity unit) {
        for (MiscMounted mounted : unit.getMisc()) {
            if ((mounted.getType() != null) && mounted.getType().hasFlag(MiscType.F_TARGETING_COMPUTER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether every ammo bin on a unit is fully stocked. An ammo bin not yet assigned to a location
     * ({@code Entity.LOC_NONE}) is treated specially: it must have exactly 1 usable shot left (representing an
     * as-yet-unallocated single reload unit) rather than a full bin's worth.
     *
     * @param unit the unit to check
     *
     * @return {@code true} if every ammo bin is full (per the rules above)
     */
    public static boolean hasAllAmmo(Entity unit) {
        for (AmmoMounted ammo : unit.getAmmo()) {
            int shots;

            if (ammo.getUsableShotsLeft() > 0) {
                shots = ammo.getOriginalShots();
            } else {
                shots = ammo.getType().getShots();
            }

            if (ammo.getLocation() == Entity.LOC_NONE && ammo.getUsableShotsLeft() != 1) {
                return false;
            } else if (ammo.getUsableShotsLeft() != shots) {
                return false;
            }
        }
        return true;
    }

    /** @return {@code true} if the unit carries no ammo-fed weapons at all (no ammo bins). */
    public static boolean isAmmoless(Entity unit) {
        return unit.getAmmo().isEmpty();
    }

    /**
     * Checks whether any ammo bin is partially depleted: has some shots remaining but fewer than a full reload
     * (or, for an unallocated bin, has been used up to 0).
     *
     * @param unit the unit to check
     *
     * @return {@code true} if any ammo bin is partially or (for unallocated bins) fully depleted
     */
    public static boolean hasLowAmmo(Entity unit) {
        for (AmmoMounted ammo : unit.getAmmo()) {
            if (ammo == null) {
                continue;
            }

            int shots = getShots(ammo);

            if (ammo.getLocation() == Entity.LOC_NONE && ammo.getUsableShotsLeft() == 0) {
                return true;
            } else if ((ammo.getUsableShotsLeft() < shots) && (ammo.getUsableShotsLeft() > 0)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the "full bin" shot count to compare an ammo mount's current shots against: if the bin still has
     * shots, its own recorded original count; if it's been fully depleted, the ammo type's default shot count
     * (since the original-shots bookkeeping may no longer be meaningful once empty).
     *
     * @param mounted the ammo mount to inspect
     *
     * @return the shot count representing a full bin of this ammo
     */
    public static int getShots(Mounted<?> mounted) {
        if (mounted.getUsableShotsLeft() > 0) {
            return mounted.getOriginalShots();
        } else {
            return ((AmmoType) mounted.getType()).getShots();
        }
    }

    /**
     * Checks whether any ammo bin on the unit is completely empty.
     *
     * @param unit the unit to check
     *
     * @return {@code true} if any ammo bin has 0 usable shots left
     */
    public static boolean hasEmptyAmmo(Entity unit) {
        for (AmmoMounted ammo : unit.getAmmo()) {
            if (ammo.getUsableShotsLeft() == 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Looks up the localized short abbreviation for the armor type at a given location (e.g. Ferro-Fibrous,
     * Stealth, Hardened), falling back to the "Standard" abbreviation for any unrecognized/standard armor type.
     *
     * @param unit     the unit to inspect
     * @param location the location whose armor type is being named
     *
     * @return the localized short armor-type name
     */
    public static String getArmorShortName(Entity unit, int location) {
        return switch (unit.getArmorType(location)) {
            case EquipmentType.T_ARMOR_FERRO_FIBROUS -> MESSAGES.getString("EquipmentType.T_ARMOR_FERRO_FIBROUS_SHORT");
            case EquipmentType.T_ARMOR_REACTIVE -> MESSAGES.getString("EquipmentType.T_ARMOR_REACTIVE_SHORT");
            case EquipmentType.T_ARMOR_REFLECTIVE -> MESSAGES.getString("EquipmentType.T_ARMOR_REFLECTIVE_SHORT");
            case EquipmentType.T_ARMOR_HARDENED -> MESSAGES.getString("EquipmentType.T_ARMOR_HARDENED_SHORT");
            case EquipmentType.T_ARMOR_LIGHT_FERRO -> MESSAGES.getString("EquipmentType.T_ARMOR_LIGHT_FERRO_SHORT");
            case EquipmentType.T_ARMOR_HEAVY_FERRO -> MESSAGES.getString("EquipmentType.T_ARMOR_HEAVY_FERRO_SHORT");
            case EquipmentType.T_ARMOR_PATCHWORK -> MESSAGES.getString("EquipmentType.T_ARMOR_PATCHWORK_SHORT");
            case EquipmentType.T_ARMOR_STEALTH -> MESSAGES.getString("EquipmentType.T_ARMOR_STEALTH_SHORT");
            case EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO ->
                  MESSAGES.getString("EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO_SHORT");
            default -> MESSAGES.getString("EquipmentType.T_ARMOR_STANDARD_SHORT");
        };
    }

    /**
     * Looks up the localized short abbreviation for the unit's internal-structure type (e.g. Endo Steel,
     * Reinforced, Composite), falling back to the "Standard" abbreviation otherwise.
     *
     * @param unit the unit to inspect
     *
     * @return the localized short internal-structure-type name
     */
    public static String getInternalShortName(Entity unit) {
        return switch (unit.getStructureType()) {
            case EquipmentType.T_STRUCTURE_ENDO_STEEL ->
                  MESSAGES.getString("EquipmentType.T_STRUCTURE_ENDO_STEEL_SHORT");
            case EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE ->
                  MESSAGES.getString("EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE_SHORT");
            case EquipmentType.T_STRUCTURE_REINFORCED ->
                  MESSAGES.getString("EquipmentType.T_STRUCTURE_REINFORCED_SHORT");
            case EquipmentType.T_STRUCTURE_COMPOSITE -> MESSAGES.getString("EquipmentType.T_STRUCTURE_COMPOSITE_SHORT");
            default -> MESSAGES.getString("EquipmentType.T_STRUCTURE_STANDARD_SHORT");
        };
    }

    /**
     * Determines whether a piece of equipment is destroyed beyond repair, versus merely damaged and repairable.
     * A slot flagged "missing" is always destroyed. Otherwise, since a single item of equipment can occupy
     * multiple critical slots (and, if split, span two locations), this counts how many of the mount's own slots
     * are damaged versus the total it occupies: if more than half are damaged, the whole item is considered
     * destroyed rather than just damaged.
     *
     * @param unit         the unit that owns the slot
     * @param criticalSlot the critical slot to check
     *
     * @return {@code true} if destroyed (unrepairable), {@code false} if merely damaged (repairable)
     *
     * @author Torren (Jason Tighe)
     */
    public static boolean isDestroyedOrDamaged(Entity unit, CriticalSlot criticalSlot) {

        if (criticalSlot.isMissing()) {
            return criticalSlot.isMissing();
        }

        Mounted<?> mount = criticalSlot.getMount();

        if (mount == null) {
            return true;
        }

        int totalCrits = 0;
        int damagedCrits = 0;

        if (mount.isSplit()) {
            int location = mount.getLocation();

            int numberOfSlots = unit.getNumberOfCriticalSlots(location);
            for (int slot = 0; slot < numberOfSlots; slot++) {
                CriticalSlot crit = unit.getCritical(location, slot);
                if ((crit != null) && crit.getMount().equals(criticalSlot.getMount())) {
                    totalCrits++;
                    if (crit.isDamaged()) {
                        damagedCrits++;
                    }
                }
            }

            location = mount.getSecondLocation();
            numberOfSlots = unit.getNumberOfCriticalSlots(location);
            for (int slot = 0; slot < numberOfSlots; slot++) {
                CriticalSlot crit = unit.getCritical(location, slot);
                if ((crit != null) && crit.getMount().equals(criticalSlot.getMount())) {
                    totalCrits++;
                    if (crit.isDamaged()) {
                        damagedCrits++;
                    }
                }
            }

            // more than 50% of the total crits are damages its toast.
            return damagedCrits > (totalCrits / 2);
        } else {
            int numberOfSlots = unit.getNumberOfCriticalSlots(mount.getLocation());
            for (int slot = 0; slot < numberOfSlots; slot++) {
                CriticalSlot crit = unit.getCritical(mount.getLocation(), slot);
                if ((crit != null) && crit.getMount().equals(criticalSlot.getMount())) {
                    totalCrits++;
                    if (crit.isDamaged()) {
                        damagedCrits++;
                    }
                }
            }

            // more then 50% of the total crits are damages its toast.
            return damagedCrits > (totalCrits / 2);
        }
    }

    /**
     * Returns how many points/slots of a repair target are currently damaged: for armor/internal-structure targets
     * this is the difference between the original and current point totals; for critical slots it's the number of
     * damaged slots backing the equipment (accounting for split mounts spanning two locations) or, for engine and
     * other system slots, delegates to the specialized engine/system counting methods.
     *
     * @param unit  the unit being inspected
     * @param slot  a critical-slot index, or {@link #LOC_INTERNAL_ARMOR} to check internal structure
     * @param loc   the location containing the target; a rear pseudo-location is normalized to front-facing armor
     * @param armor {@code true} to check armor/internal-structure point damage, {@code false} for critical slots
     *
     * @return the number of damaged points or critical slots
     */
    public static int getNumberOfDamagedCrits(Entity unit, int slot, int loc, boolean armor) {
        if (armor) {
            if (slot == UnitUtils.LOC_INTERNAL_ARMOR) {
                return unit.getOInternal(loc) - unit.getInternal(loc);
            }

            if (loc >= UnitUtils.LOC_CENTER_TORSOR) {
                return unit.getOArmor(loc - 7, true) - unit.getArmor(loc - 7, true);
            }

            return unit.getOArmor(loc) - unit.getArmor(loc);
        }

        CriticalSlot criticalSlot = unit.getCritical(loc, slot);

        if (UnitUtils.isEngineCrit(criticalSlot)) {
            return UnitUtils.getNumberOfDamagedEngineCrits(unit);
        }

        if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
            Mounted<?> mount = criticalSlot.getMount();

            int damagedCrits = 0;

            if ((mount != null) && mount.isSplit()) {
                int location = mount.getLocation();

                int numberOfSlots = unit.getNumberOfCriticalSlots(location);
                for (int pos = 0; pos < numberOfSlots; pos++) {
                    CriticalSlot crit = unit.getCritical(location, pos);
                    if ((crit != null) && crit.getMount().equals(mount)) {
                        if (crit.isDamaged()) {
                            damagedCrits++;
                        }
                    }
                }

                location = mount.getSecondLocation();
                numberOfSlots = unit.getNumberOfCriticalSlots(location);
                for (int pos = 0; pos < numberOfSlots; pos++) {
                    CriticalSlot crit = unit.getCritical(location, pos);
                    if ((crit != null) && crit.getMount().equals(mount)) {
                        if (crit.isDamaged()) {
                            damagedCrits++;
                        }
                    }
                }

            } else {
                if (mount != null) {
                    int numberOfSlots = unit.getNumberOfCriticalSlots(mount.getLocation());
                    for (int pos = 0; pos < numberOfSlots; pos++) {
                        CriticalSlot crit = unit.getCritical(mount.getLocation(), pos);
                        if ((crit != null) && crit.getMount().equals(mount)) {
                            if (crit.isDamaged()) {
                                damagedCrits++;
                            }
                        }
                    }
                }

            }
            return damagedCrits;

        }

        return UnitUtils.getNumberOfDamagedSystemCriticalSlots(unit, criticalSlot);
    }

    /**
     * Counts how many critical slots belonging to the same Mek system (gyro, or life support/sensors) are
     * currently damaged, following the same multi-location counting logic as
     * {@link #getNumberOfSystemCriticalSlots}. Actuators and other high-index systems only ever occupy 1 slot, so
     * they return 0 or 1 directly.
     *
     * @param unit         the unit being inspected
     * @param criticalSlot a representative slot for the system being checked (its index determines which)
     *
     * @return the number of damaged critical slots for this system
     */
    // Sets multiple system crits to repairing.
    // Gyro Life support and Sensors.
    public static int getNumberOfDamagedSystemCriticalSlots(Entity unit, CriticalSlot criticalSlot) {
        int count = 0;

        // actuators are always 1.
        if (criticalSlot.getIndex() > Mek.SYSTEM_GYRO) {
            if (criticalSlot.isDamaged()) {
                return 1;
            }

            return 0;
        }

        if (criticalSlot.getIndex() == Mek.SYSTEM_GYRO) {
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(Mek.LOC_CENTER_TORSO); slot++) {
                CriticalSlot crit = unit.getCritical(Mek.LOC_CENTER_TORSO, slot);

                if ((crit == null) || (crit.getType() != CriticalSlot.TYPE_SYSTEM) || !crit.isDamaged()) {
                    continue;
                }

                if (crit.getIndex() == criticalSlot.getIndex()) {
                    count++;
                }
            }
        }// if it's not a GYRO then its sensors or life support
        // as engines have already been filtered
        else {
            if (((Mek) unit).getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
                for (int location = LOC_CENTER_TORSO; location <= LOC_LT; location++) {
                    count = getCountOfDamagedCriticalSystems(unit, criticalSlot, count, location);
                }
            }// Normal cockpit in the head.
            else {
                count = getCountOfDamagedCriticalSystems(unit, criticalSlot, count, LOC_HEAD);
            }
        }

        return count;
    }

    /** Adds to {@code count} the number of damaged system critical slots in {@code location} matching {@code criticalSlot}'s index. */
    private static int getCountOfDamagedCriticalSystems(Entity unit, CriticalSlot criticalSlot, int count,
          int location) {
        for (int slot = 0; slot < unit.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot crit = unit.getCritical(location, slot);
            if ((crit == null) || (crit.getType() != CriticalSlot.TYPE_SYSTEM) || !crit.isDamaged()) {
                continue;
            }

            if (crit.getIndex() == criticalSlot.getIndex()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns a short, generic category label for a repair/salvage target rather than the specific equipment
     * name: armor/internal-structure type name (or "IS (STD)"/"Armor (STD)" for standard), "Actuator" for limb
     * actuators, "Ammo Bin" for ammo, the engine/gyro/cockpit type name for those Mek systems, or the raw system
     * name for anything else. Falls back to the mounted equipment's internal name for plain equipment slots. Use
     * {@link #getCritExternalName} instead for the equipment's actual display name.
     *
     * @param unit     the unit being inspected
     * @param slot     a critical-slot index, or one of the {@code LOC_*_ARMOR} pseudo-slot codes when {@code armor}
     *                 is {@code true}
     * @param location the location containing the target
     * @param armor    {@code true} to name armor/internal structure, {@code false} to name a critical slot
     *
     * @return a short category/type label, or an empty string if the slot is empty
     */
    public static String getCritName(Entity unit, int slot, int location, boolean armor) {

        if (armor) {
            if (slot == UnitUtils.LOC_INTERNAL_ARMOR) {
                if (EquipmentType.getArmorTypeName(unit.getStructureType()).equalsIgnoreCase("Standard")) {
                    return "IS (STD)";
                }

                return EquipmentType.getStructureTypeName(unit.getStructureType());

            } else {
                if (EquipmentType.getArmorTypeName(unit.getArmorType(location)).equalsIgnoreCase("Standard")) {
                    return "Armor (STD)";
                }
                return EquipmentType.getArmorTypeName(unit.getArmorType(location));
            }
        }
        CriticalSlot crit = unit.getCritical(location, slot);
        if (crit == null) {
            return "";
        }

        if (UnitUtils.isActuator(crit)) {
            return "Actuator";
        }

        if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
            Mounted mounted = crit.getMount();
            if (mounted.getType() instanceof AmmoType) {
                return "Ammo Bin";
            }
        }

        if ((unit instanceof Mek) && (crit.getType() == CriticalSlot.TYPE_SYSTEM)) {

            if (crit.getIndex() == Mek.SYSTEM_ENGINE) {
                return UnitUtils.ENGINE_TECH_STRING[UnitUtils.getEngineType(unit)];
            }

            if (crit.getIndex() == Mek.SYSTEM_GYRO) {
                return Mek.getGyroTypeString(unit.getGyroType());
            }

            if (crit.getIndex() == Mek.SYSTEM_COCKPIT) {
                return Mek.getCockpitTypeString(((Mek) unit).getCockpitType());
            }

            return ((Mek) unit).getSystemName(crit.getIndex());
        }// end CS type if

        return crit.getMount().getType().getInternalName();

    }

    /**
     * Infers a Mek's engine type ({@link #STANDARD_ENGINE}, Light, XL, or XXL) purely from how many engine
     * critical slots it occupies (via {@link #getNumberOfEngineCrits}) and whether the unit is Clan or Inner
     * Sphere tech, per BattleTech construction rules: IS Light=10 slots, IS XL=12, IS XXL=18; Clan XL=10, Clan
     * XXL=12 (Clan engines are more compact). Any non-Mek unit, or a Mek whose slot count doesn't match one of
     * these known patterns, is reported as {@link #STANDARD_ENGINE}.
     *
     * @param unit An @Entity unit
     *
     * @return engine type of the Mek Used for getting what engine type the entity has.
     *
     * @author Torren (Jason Tighe)
     */
    public static int getEngineType(Entity unit) {
        int engineNumber = UnitUtils.getNumberOfEngineCrits(unit);

        // only check meks everyone else gets STD engine returned
        if (unit instanceof Mek) {
            // Check to see if its a clan unit.
            if (unit.isClan()) {

                if (engineNumber == 12) {
                    return UnitUtils.CLAN_XXL_ENGINE;
                }
                if (engineNumber == 10) {
                    return UnitUtils.CLAN_XL_ENGINE;
                }
            }// end tech level if
            // Else they are IS
            else {
                if (engineNumber == 18) {
                    return UnitUtils.IS_XXL_ENGINE;
                }
                if (engineNumber == 12) {
                    return UnitUtils.IS_XL_ENGINE;
                }
                if (engineNumber == 10) {
                    return UnitUtils.IS_LIGHT_ENGINE;
                }
            }// end tech level else
        }// end istanceof if

        return UnitUtils.STANDARD_ENGINE;
    }

    /**
     * Like {@link #getCritName}, but returns the equipment's actual display name (via
     * {@code Mounted.getName()}) for equipment slots instead of a generic "Ammo Bin"/internal-name category —
     * intended for player-facing labels. Note the {@code armor} branch here reads the armor type from {@code slot}
     * rather than {@code location} (see the {@code unit.getArmorType(slot)} calls below), which differs from
     * {@link #getCritName}'s equivalent branch that uses {@code location}; whether that is intentional or a
     * parameter mix-up is unclear from context.
     *
     * @param unit     the unit being inspected
     * @param slot     a critical-slot index, or one of the {@code LOC_*_ARMOR} pseudo-slot codes when {@code armor}
     *                 is {@code true} (also used in place of {@code location} for the armor-type lookup)
     * @param location the location containing the target
     * @param armor    {@code true} to name armor/internal structure, {@code false} to name a critical slot
     *
     * @return a display name for the target, or an empty string if the slot is empty
     */
    public static String getCritExternalName(Entity unit, int slot,
          int location, boolean armor) {

        if (armor) {
            if (slot == UnitUtils.LOC_INTERNAL_ARMOR) {
                if (EquipmentType.getArmorTypeName(unit.getStructureType()).equalsIgnoreCase("Standard")) {
                    return "IS (STD)";
                }

                return EquipmentType.getStructureTypeName(unit.getStructureType());

            } else {
                if (EquipmentType.getArmorTypeName(unit.getArmorType(slot)).equalsIgnoreCase("Standard")) {
                    return "Armor (STD)";
                }
                return EquipmentType.getArmorTypeName(unit.getArmorType(slot));
            }
        }

        CriticalSlot crit = unit.getCritical(location, slot);

        if (crit == null) {
            return "";
        }

        if (UnitUtils.isActuator(crit)) {
            return "Actuator";
        }

        if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
            Mounted<?> mounted = crit.getMount();
            if (mounted.getType() instanceof AmmoType) {
                return "Ammo Bin";
            }

            return mounted.getName();
        }

        if ((unit instanceof Mek) && (crit.getType() == CriticalSlot.TYPE_SYSTEM)) {

            if (crit.getIndex() == Mek.SYSTEM_ENGINE) {
                return UnitUtils.ENGINE_TECH_STRING[UnitUtils.getEngineType(unit)];
            }

            if (crit.getIndex() == Mek.SYSTEM_GYRO) {
                return Mek.getGyroTypeString(unit.getGyroType());
            }

            if (crit.getIndex() == Mek.SYSTEM_COCKPIT) {
                return Mek.getCockpitTypeString(((Mek) unit).getCockpitType());
            }

            return ((Mek) unit).getSystemName(crit.getIndex());
        }// end CS type if

        return crit.getMount().getType().getInternalName();
    }

    /**
     * Determines whether a part's tech level is available/compatible for a house/faction's tech level, using
     * MegaMek's {@link TechConstants} tiers. A house is always compatible with a part at or below the "ALL"
     * placeholder levels, with a part identical to its own tier, or with any part below the Intro Box Set
     * baseline. Otherwise, unlike {@link #isCompatibleTech} (which checks a unit's own tech level against a
     * repairing tech's level), this checks IS and Clan tech ladders independently and correctly falls through
     * each tier to every strictly-lower tier via nested switch expressions (no fallthrough bug here, in contrast
     * to {@link #isCompatibleTech}).
     *
     * @param partTechLevel  the tech level of the part in question
     * @param houseTechLevel the tech level of the house/faction that would use the part
     *
     * @return {@code true} if the house's tech level can field/use a part of {@code partTechLevel}
     */
    public static boolean isSameTech(int partTechLevel, int houseTechLevel) {
        if ((houseTechLevel >= TechConstants.T_ALL)
                  || (partTechLevel >= TechConstants.T_ALL)
                  || (partTechLevel < TechConstants.T_INTRO_BOX_SET)
                  || (partTechLevel == houseTechLevel)) {
            return true;
        }

        return switch (houseTechLevel) {
            case TechConstants.T_INTRO_BOX_SET -> (partTechLevel == TechConstants.T_INTRO_BOX_SET);
            case TechConstants.T_IS_TW_NON_BOX -> switch (partTechLevel) {
                case TechConstants.T_INTRO_BOX_SET,
                     TechConstants.T_IS_TW_NON_BOX -> true;
                default -> false;
            };
            case TechConstants.T_IS_TW_ALL -> switch (partTechLevel) {
                case TechConstants.T_INTRO_BOX_SET,
                     TechConstants.T_IS_TW_NON_BOX,
                     TechConstants.T_IS_TW_ALL -> true;
                default -> false;
            };
            case TechConstants.T_IS_ADVANCED -> switch (partTechLevel) {
                case TechConstants.T_INTRO_BOX_SET,
                     TechConstants.T_IS_TW_NON_BOX,
                     TechConstants.T_IS_TW_ALL,
                     TechConstants.T_IS_ADVANCED -> true;
                default -> false;
            };
            case TechConstants.T_IS_EXPERIMENTAL -> switch (partTechLevel) {
                case TechConstants.T_INTRO_BOX_SET,
                     TechConstants.T_IS_TW_NON_BOX,
                     TechConstants.T_IS_TW_ALL,
                     TechConstants.T_IS_ADVANCED,
                     TechConstants.T_IS_EXPERIMENTAL -> true;
                default -> false;
            };
            case TechConstants.T_IS_UNOFFICIAL,
                 TechConstants.T_ALL_IS -> switch (partTechLevel) {
                case TechConstants.T_INTRO_BOX_SET,
                     TechConstants.T_IS_TW_NON_BOX,
                     TechConstants.T_IS_TW_ALL,
                     TechConstants.T_IS_ADVANCED,
                     TechConstants.T_IS_EXPERIMENTAL,
                     TechConstants.T_IS_UNOFFICIAL,
                     TechConstants.T_ALL_IS -> true;
                default -> false;
            };
            case TechConstants.T_CLAN_TW -> partTechLevel == TechConstants.T_CLAN_TW;
            case TechConstants.T_CLAN_ADVANCED -> switch (partTechLevel) {
                case TechConstants.T_CLAN_TW,
                     TechConstants.T_CLAN_ADVANCED -> true;
                default -> false;
            };
            case TechConstants.T_CLAN_EXPERIMENTAL -> switch (partTechLevel) {
                case TechConstants.T_CLAN_TW,
                     TechConstants.T_CLAN_ADVANCED,
                     TechConstants.T_CLAN_EXPERIMENTAL -> true;
                default -> false;
            };
            case TechConstants.T_CLAN_UNOFFICIAL,
                 TechConstants.T_ALL_CLAN -> switch (partTechLevel) {
                case TechConstants.T_CLAN_TW,
                     TechConstants.T_CLAN_ADVANCED,
                     TechConstants.T_CLAN_EXPERIMENTAL,
                     TechConstants.T_CLAN_UNOFFICIAL,
                     TechConstants.T_ALL_CLAN -> true;
                default -> false;
            };
            default -> false;
        };
    }

    /**
     * Loads a MegaMek {@link Entity} from the global {@link MekSummaryCache} by file/model/chassis name. First
     * tries an exact cache lookup by {@code fileName}, then a trimmed lookup, then falls back to a linear scan of
     * every cached unit comparing entry name, model, and chassis (case-insensitively, trimmed). If found, parses
     * the actual unit file via {@link MekFileParser}. This method never returns {@code null}: any lookup/parse
     * failure, or a unit that can't be found at all, results in a placeholder "error unit" from
     * {@link #createOMG()} instead — this is a deliberate design choice so callers don't need to null-check, at
     * the cost of silently masking missing/corrupt unit files as an obviously-fake Mek.
     *
     * @param fileName the unit's cache entry name, model, or chassis to search for
     *
     * @return the loaded {@link Entity}, or a placeholder error unit if it could not be found/loaded
     */
    public static @Nonnull Entity createEntity(String fileName) {
        Entity unitEntity = null;

        try {
            MekSummary mekSummary = MekSummaryCache.getInstance().getMek(fileName);

            if (mekSummary == null) {
                mekSummary = MekSummaryCache.getInstance().getMek(fileName.trim());

                if (mekSummary == null) {
                    MekSummary[] units = MekSummaryCache.getInstance().getAllMeks();
                    for (MekSummary unit : units) {
                        if (unit.getEntryName().equalsIgnoreCase(fileName)
                                  || unit.getModel().trim().equalsIgnoreCase(fileName.trim())
                                  || unit.getChassis().trim().equalsIgnoreCase(fileName.trim())) {
                            mekSummary = unit;
                            break;
                        }
                    }
                }
            }

            if (mekSummary != null) {
                unitEntity = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName()).getEntity();
            }
        } catch (Exception ex) {
            unitEntity = UnitUtils.createOMG();// new
        }

        if (unitEntity == null) {
            return UnitUtils.createOMG();
        }

        return unitEntity;
    }

    /**
     * Builds a placeholder "error unit": a lightweight 25-ton standard biped Mek named "Error"/"OMG-UR-FD" with 1
     * point of armor everywhere, used as a fallback whenever a real unit file cannot be located or parsed (see
     * {@link #createEntity}). Its fluff history text explicitly tells the player this is an error and to report
     * it, so it should be immediately recognizable as bogus if it ever surfaces in play.
     *
     * @return a fully-constructed but intentionally nonsensical placeholder {@link Entity}
     */
    public static Entity createOMG() {
        Mek entity = new BipedMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD);

        entity.setYear(2075);
        entity.setTechLevel(TechConstants.T_INTRO_BOX_SET);
        entity.setWeight(25);
        entity.setEngine(new Engine(325, Engine.NORMAL_ENGINE, 0));
        entity.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        entity.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);

        entity.addGyro();
        entity.addEngineCrits();
        entity.addCockpit();
        entity.addEngineSinks(entity.getEngine().integralHeatSinkCapacity(false), MiscType.F_HEAT_SINK, false);

        entity.autoSetInternal();

        for (int loc = 0; loc <= Mek.LOC_LEFT_LEG; loc++) {
            entity.initializeArmor(1, loc);
            if (entity.hasRearArmor(loc)) {
                entity.initializeRearArmor(1, loc);
            }
        }

        entity.getFluff()
              .setHistory("This is an Error Unit! If you've received this unit in error please let someone know.");
        entity.setModel("OMG-UR-FD");
        entity.setChassis("Error");
        return entity;
    }

    /**
     * Finds the unit-cache entry name (source file identifier) for a given loaded {@link Entity}, the inverse
     * lookup of {@link #createEntity}. Tries a direct cache lookup by the entity's short name first; if that
     * fails, falls back to a linear scan of every cached unit comparing model and chassis (trimmed,
     * case-insensitive).
     *
     * @param entity the entity to identify
     *
     * @return the matching cache entry name, or an empty string if no match is found
     */
    public static String getEntityFileName(Entity entity) {
        String unitFile = "";

        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(entity.getShortNameRaw());

        if (mekSummary == null) {
            MekSummary[] units = MekSummaryCache.getInstance().getAllMeks();
            for (MekSummary unit : units) {
                if (unit.getModel().trim().equalsIgnoreCase(entity.getModel().trim()) &&
                          unit.getChassis().trim().equalsIgnoreCase(entity.getChassis().trim())) {
                    return unit.getEntryName();
                }
            }
        } else {
            unitFile = UnitUtils.getMekSummaryFileName(mekSummary);
        }

        return unitFile;
    }

    /**
     * Extracts a bare file name from a {@link MekSummary}'s entry name, falling back to the underlying source
     * file's own name if the entry name is missing or the literal string {@code "null"}. Strips any directory
     * path prefix (handling both {@code /} and {@code \} separators, e.g. when the unit came from inside a zipped
     * archive).
     *
     * @param mekSummary the unit-cache summary to extract a file name from
     *
     * @return the bare unit file name
     */
    public static String getMekSummaryFileName(MekSummary mekSummary) {
        String unitFile = "";

        unitFile = mekSummary.getEntryName();

        if ((unitFile == null) || unitFile.equals("null")) {
            unitFile = mekSummary.getSourceFile().getName();
        }

        if (unitFile.contains("/")) {
            unitFile = unitFile.substring(unitFile.lastIndexOf("/") + 1);
        } else if (unitFile.contains("\\")) {
            unitFile = unitFile.substring(unitFile.lastIndexOf("\\") + 1);
        }

        return unitFile;
    }

    /**
     * Checks whether a unit is "cored" — its core structural location has been destroyed, meaning the unit is
     * effectively dead/unsalvageable as a chassis. For a Tank this means any front-hull location (front through
     * just before the turret) has negative internal structure; for a Mek this delegates to the inverse of
     * {@link #canStartUp} (center-torso/head/cockpit/engine checks). Other unit types are never considered cored.
     *
     * @param unit the unit to check
     *
     * @return {@code true} if the unit is cored
     */
    public static boolean isCored(Entity unit) {
        if (unit instanceof Tank) {
            for (int loc = Tank.LOC_FRONT; loc < Tank.LOC_TURRET; loc++) {
                if (unit.getInternal(loc) < 0) {
                    return true;
                }
            }
        } else if (unit instanceof Mek) {
            return !UnitUtils.canStartUp(unit);
        }

        return false;
    }

    /**
     * Method that determines whether a unit can successfully start its engine/reactor. Non-Mek units always have
     * working engines. Meks with fewer than 3 engine-critical slots have working fusion reactors. Also, a missing
     * cockpit or head will mean you cannot start up either.
     */
    public static boolean canStartUp(Entity unit) {
        int engineHits = 0;

        // non-Meks may always start engines
        if (!(unit instanceof Mek)) {
            return true;
        }

        // no head no startup
        if (unit.getInternal(LOC_HEAD) <= 0) {
            return false;
        }

        // no cockpit no startup
        if (unit.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, Mek.LOC_HEAD) > 0) {
            return false;
        }

        // else, check for engine-critical slots
        engineHits = UnitUtils.getNumberOfDamagedEngineCrits(unit);

        return (engineHits < 3);
    }

    /**
     * Salvages (destroys beyond repair) the cockpit critical slot of a Mek — in the head for a normal cockpit, or
     * in the center torso for a torso-mounted cockpit. No-op for non-Mek units.
     *
     * @param unit the unit whose cockpit is to be destroyed
     */
    public static void destroyCockPit(Entity unit) {
        if (!(unit instanceof Mek mek)) {
            return;
        }

        int location = Mek.LOC_HEAD;

        if (mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
            location = Mek.LOC_CENTER_TORSO;
        }

        for (int y = 0; y < unit.getNumberOfCriticalSlots(location); y++) {
            CriticalSlot criticalSlot = unit.getCritical(location, y);
            if ((criticalSlot != null) && (criticalSlot.getIndex() == Mek.SYSTEM_COCKPIT)) {
                UnitUtils.salvageCriticalSlot(criticalSlot);
            }
        }
    }

    /**
     * Builds a MegaMek {@link Crew} (single pilot) from a MekWars {@link Unit}'s pilot record, so the unit can be
     * loaded into an actual MegaMek {@link Entity} for gameplay. Copies name, gunnery/piloting skills, and hit
     * count, then translates MekWars pilot options ({@link MegaMekPilotOption}) into MegaMek's crew
     * {@code PilotOptions}: the "weapon_specialist" option carries the specialized weapon name, "edge" also pulls
     * in the four "edge_when_*" trigger flags (head hit, TAC, KO, explosion) from the pilot's Edge skill and other
     * settings, and any other option is copied through as a plain boolean value.
     *
     * @param mek the MekWars unit whose pilot should be converted; if it has no pilot assigned, an empty default
     *            {@link Crew} is returned instead
     *
     * @return a MegaMek {@link Crew} representing the unit's pilot
     */
    public static Crew createEntityPilot(Unit mek) {
        // get and set the options
        Crew pilot;

        if (mek.getPilot() == null) {
            pilot = new Crew(CrewType.SINGLE);
            return pilot;
        } else {
            pilot = new Crew(CrewType.SINGLE,
                  mek.getPilot().getName(),
                  1,
                  mek.getPilot().getGunnery(),
                  mek.getPilot().getPiloting(),
                  Gender.RANDOMIZE,
                  false,
                  null);
        }

        // Hits defaults to 0 so no reason to keep checking over and over again.
        pilot.setHits(mek.getPilot().getHits(), 0);

        for (MegaMekPilotOption megaMekPilotOption : mek.getPilot().getMegaMekOptions()) {
            if (megaMekPilotOption.getMegaMekName().equals("weapon_specialist")) {
                pilot.getOptions().getOption(megaMekPilotOption.getMegaMekName())
                      .setValue(mek.getPilot().getWeapon());
            } else if (megaMekPilotOption.getMegaMekName().equals("edge")) {
                pilot.getOptions()
                      .getOption(megaMekPilotOption.getMegaMekName())
                      .setValue(mek.getPilot().getSkills()
                                      .getPilotSkill(PilotSkill.EdgeSkillID)
                                      .getLevel());
                pilot.getOptions().getOption("edge_when_headhit")
                      .setValue(mek.getPilot().getHeadHit());
                pilot.getOptions().getOption("edge_when_tac")
                      .setValue(mek.getPilot().getTac());
                pilot.getOptions().getOption("edge_when_ko")
                      .setValue(mek.getPilot().getKO());
                pilot.getOptions().getOption("edge_when_explosion")
                      .setValue(mek.getPilot().getExplosion());
            } else {
                pilot.getOptions().getOption(megaMekPilotOption.getMegaMekName())
                      .setValue(megaMekPilotOption.isValue());
            }
        }

        return pilot;
    }

    /**
     * Checks whether a piece of equipment is Clan tech as of a given year (Clan TW, Advanced, Experimental, or
     * Unofficial tech level at that point in time).
     *
     * @param eq   the equipment type to check
     * @param year the game year to evaluate the tech level at
     *
     * @return {@code true} if the equipment is Clan tech in that year
     */
    public static boolean isClanEQ(EquipmentType eq, int year) {
        return (eq.getTechLevel(year) == TechConstants.T_CLAN_ADVANCED)
                     || (eq.getTechLevel(year) == TechConstants.T_CLAN_EXPERIMENTAL)
                     || (eq.getTechLevel(year) == TechConstants.T_CLAN_TW)
                     || (eq.getTechLevel(year) == TechConstants.T_CLAN_UNOFFICIAL);
    }

    /**
     * Serializes a unit's current battle damage (armor, internal structure, critical slots, and optionally ammo
     * usage) into the compact wire-format string used to sync damage state between server and client, by
     * delegating to the unit-type-specific handler from {@link UnitDamageHandlerFactory}.
     *
     * @param unit     the unit to serialize damage for
     * @param sendAmmo {@code true} to include ammo bin state in the report
     *
     * @return the serialized damage-report string
     */
    public static String unitBattleDamage(Entity unit, boolean sendAmmo) {
        return UnitDamageHandlerFactory.getHandler(unit).buildDamageString(
              unit, sendAmmo);
    }

    /**
     * Parses a serialized battle-damage report string (as produced by {@link #unitBattleDamage}) and applies it to
     * a unit, restoring its damage state, via the unit-type-specific handler from {@link UnitDamageHandlerFactory}.
     *
     * @param unit        the unit to apply damage to
     * @param report      the serialized damage-report string to parse
     * @param isRepairing {@code true} if this damage is being applied as part of an in-progress repair state
     *                    rather than raw combat damage
     */
    public static void applyBattleDamage(Entity unit, String report,
          boolean isRepairing) {
        UnitDamageHandlerFactory.getHandler(unit).applyDamageString(unit,
              report, isRepairing);
    }
}
