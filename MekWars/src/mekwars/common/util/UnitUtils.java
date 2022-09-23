/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - Torren (torren@users.sourceforge.net)
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

package common.util;

import java.util.Iterator;

import common.CampaignData;
import common.MegaMekPilotOption;
import common.Unit;
import common.campaign.pilot.skills.PilotSkill;
import common.util.unitdamage.UnitDamageHandlerFactory;
import megamek.common.AmmoType;
import megamek.common.BipedMech;
import megamek.common.Crew;
import megamek.common.CrewType;
import megamek.common.CriticalSlot;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.TechConstants;

public class UnitUtils {

    // Engines
    public static final int STANDARD_ENGINE = 0;
    public static final int IS_LIGHT_ENGINE = 1;
    public static final int IS_XL_ENGINE = 2;
    public static final int IS_XXL_ENGINE = 3;
    public static final int CLAN_XL_ENGINE = 4;
    public static final int CLAN_XXL_ENGINE = 5;

    public static final String[] ENGINE_SHORT_STRING = { "Standard Engine",
            "Light Engine", "XL Engine", "XXL Engine", "XL Engine",
            "XXL Engine" };

    public static final String[] ENGINE_TECH_STRING = { "Standard Engine",
            "IS Light Engine", "IS XL Engine", "IS XXL Engine",
            "Clan XL Engine", "Clan XXL Engine" };

    // Locations for Advanced Repair.
    public static final int LOC_HEAD = 0;
    public static final int LOC_CT = 1;
    public static final int LOC_RT = 2;
    public static final int LOC_LT = 3;
    public static final int LOC_RARM = 4;
    public static final int LOC_LARM = 5;
    public static final int LOC_RLEG = 6;
    public static final int LOC_LLEG = 7;
    public static final int LOC_CTR = 8;
    public static final int LOC_RTR = 9;
    public static final int LOC_LTR = 10;
    public static final int LOC_FRONT_ARMOR = 13;
    public static final int LOC_REAR_ARMOR = 14;
    public static final int LOC_INTERNAL_ARMOR = 15;

    // Tech levels
    public static final int TECH_GREEN = 0;
    public static final int TECH_REG = 1;
    public static final int TECH_VET = 2;
    public static final int TECH_ELITE = 3;
    public static final int TECH_PILOT = 4;
    public static final int TECH_REWARD_POINTS = 5;

    // Used for simple repairs
    public static final int ARMOR = 1;
    public static final int INTERNAL = 2;
    public static final int WEAPONS = 3;
    public static final int EQUIPMENT = 4;
    public static final int SYSTEMS = 5;
    public static final int ENGINES = 6;

    /**
     * Method which determines whether or not a unit can succesfully start its
     * engine/reactor. Non-mech units always have working engines. Mechs with
     * fewer than 3 engine criticals have working fusion reactors. Also a
     * missing cockpit or head will mean you cannot startup either.
     */
    public static boolean canStartUp(Entity unit) {
        int engineHits = 0;

        // non-mechs may always start engines
        if (!(unit instanceof Mech)) {
            return true;
        }

        // no head no startup
        if (unit.getInternal(LOC_HEAD) <= 0) {
            return false;
        }

        // no cockpit no startup
        if (unit.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT,
                Mech.LOC_HEAD) > 0) {
            return false;
        }

        // else, check for engine criticals
        engineHits = UnitUtils.getNumberOfDamagedEngineCrits(unit);

        return (engineHits < 3);
    }

    public static boolean hasArmorDamage(Entity unit) {

        if (unit instanceof Infantry) {
            return false;
        }

        if ((unit.getTotalArmor() != unit.getTotalOArmor())
                || (unit.getTotalInternal() != unit.getTotalOInternal())) {
            return true;
        }
        return false;
    }

    public static boolean hasISDamage(Entity unit) {

        if (unit instanceof Infantry) {
            return false;
        }

        if (unit.getTotalInternal() != unit.getTotalOInternal()) {
            return true;
        }
        return false;
    }

    public static boolean hasCriticalDamage(Entity unit) {

        if ((unit instanceof Mech) || (unit instanceof Tank)) {
            for (int x = 0; x < unit.locations(); x++) {
                for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                    CriticalSlot cs = unit.getCritical(x, y);
                    if ((cs != null) && (cs.isDamaged() || cs.isBreached())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasUndamagedCriticals(Entity unit, int location) {

        if ((unit instanceof Mech) || (unit instanceof Tank)) {
            for (int y = 0; y < unit.getNumberOfCriticals(location); y++) {
                CriticalSlot cs = unit.getCritical(location, y);
                if ((cs != null) && !cs.isDamaged()
                        && !UnitUtils.isNonRepairableCrit(unit, cs)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasCriticalsUnderRepair(Entity unit, int location) {

        if ((unit instanceof Mech) || (unit instanceof Tank)) {
            for (int y = 0; y < unit.getNumberOfCriticals(location); y++) {
                CriticalSlot cs = unit.getCritical(location, y);
                if ((cs != null) && cs.isRepairing()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isRepairing(Entity unit) {

        if ((unit instanceof Mech) || (unit instanceof Tank)) {
            for (int x = 0; x < unit.locations(); x++) {

                // check for armor repairs first then move to crits.
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

                for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                    CriticalSlot cs = unit.getCritical(x, y);
                    if ((cs != null) && cs.isRepairing()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @author Torren (Jason Tighe)
     * @param Entity
     *            unit
     * @return the number of engine crits an Entity has.
     */
    public static int getNumberOfEngineCrits(Entity unit) {
        int engines = 0;

        if (unit instanceof Mech) {
            // no reason to check for engines anywhere othen then the torso
            for (int x = LOC_CT; x <= LOC_LT; x++) {
                for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                    CriticalSlot cs = unit.getCritical(x, y);
                    if (UnitUtils.isEngineCrit(cs)) {
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
     * This destroys all the engine crits in the unit this means a botched
     * salvage job or cored unit
     *
     * @param unit
     * @return
     */
    public static void destroyAllEngineCrits(Entity unit) {

        if (unit instanceof Mech) {
            // no reason to check for engines anywhere othen then the torso
            for (int x = LOC_CT; x <= LOC_LT; x++) {
                for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                    CriticalSlot cs = unit.getCritical(x, y);
                    if (UnitUtils.isEngineCrit(cs)) {
                        UnitUtils.salvageCriticalSlot(cs, unit);
                    }
                }
            }
        }
    }

    /**
     * @author Torren (Jason Tighe)
     * @param Entity
     *            unit
     * @return engine type of the Mek Used for getting what engine type the
     *         entity has.
     */
    public static int getEngineType(Entity unit) {
        int engineNumber = UnitUtils.getNumberOfEngineCrits(unit);

        // only check mechs everyone else gets STD engine returned
        if (unit instanceof Mech) {
            // Check to see if its a clan unit.
            if (unit.isClan()) {

                if (engineNumber == 12) {
                    return UnitUtils.CLAN_XXL_ENGINE;
                }
                if (engineNumber == 10) {
                    return UnitUtils.CLAN_XL_ENGINE;
                }
            }// end techlevel if
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
            }// end techlevel else
        }// end istanceof if

        return UnitUtils.STANDARD_ENGINE;
    }

    public static int getNumberOfDamagedEngineCrits(Entity unit) {
        int engineHits = 0;
        // no reason to check for engines anywhere othen then the torso
        for (int x = LOC_CT; x <= LOC_LT; x++) {
            for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                CriticalSlot cs = unit.getCritical(x, y);
                if (UnitUtils.isEngineCrit(cs)
                        && (cs.isBreached() || cs.isDamaged())) {
                    engineHits++;
                }
            }
        }
        return engineHits;
    }

    /**
     * Some EQ can take up multiple slots this will track them down and repair
     * them.
     *
     * @param eq
     * @param unit
     * @param location
     */
    public static void repairEquipment(Mounted eq, Entity unit, int location) {

        if (eq.isSplit()) {
            UnitUtils.repairSplitEquipment(eq, unit);
            return;
        }

        for (int slot = 0; slot < unit.getNumberOfCriticals(location); slot++) {
            CriticalSlot crit = unit.getCritical(location, slot);
            if (crit == null) {
                continue;
            }
            if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                Mounted mounted = crit.getMount();

                if (eq.equals(mounted)) {
                    UnitUtils.fixCriticalSlot(crit, unit, crit.isBreached());
                    unit.setCritical(location, slot, crit);
                }
            }// end getType() if
        }// end for
    }

    /**
     * Salvage the crit and its mount set them all to destroyed.
     *
     * @param cs
     * @param unit
     */
    public static void salvageCriticalSlot(CriticalSlot cs, Entity unit) {
        if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
            Mounted mounted = cs.getMount();
            mounted.setDestroyed(true);
            mounted.setMissing(true);
            mounted.setHit(true);
            mounted.setBreached(false);
        }

        cs.setDestroyed(true);
        cs.setHit(true);
        cs.setMissing(true);
        cs.setRepairing(false);
        cs.setBreached(false);
    }

    /**
     * Salvage the crits.
     *
     * @param eq
     * @param unit
     * @param location
     */
    public static void salvageEquipment(Mounted eq, Entity unit, int location) {

        if (eq.isSplit()) {
            UnitUtils.salvageSplitEquipment(eq, unit);
            return;
        }

        for (int slot = 0; slot < unit.getNumberOfCriticals(location); slot++) {
            CriticalSlot crit = unit.getCritical(location, slot);
            if (crit == null) {
                continue;
            }
            if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                Mounted mounted = crit.getMount();

                if (eq.equals(mounted)) {
                    UnitUtils.salvageCriticalSlot(crit, unit);
                    unit.setCritical(location, slot, crit);
                }
            }// end getType() if
        }// end for
    }

    public static void salvageSplitEquipment(Mounted eq, Entity unit) {

        // Only mechs should have split weapons crits.
        if (!(unit instanceof Mech)) {
            return;
        }

        // can only split weapons in toros and arms.
        for (int x = LOC_CT; x <= LOC_LARM; x++) {

            for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                CriticalSlot cs = unit.getCritical(x, y);
                if (cs == null) {
                    continue;
                }

                if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted mounted = cs.getMount();

                    if (eq.equals(mounted)) {
                        UnitUtils.salvageCriticalSlot(cs, unit);
                        unit.setCritical(x, y, cs);
                    }
                }// end getType() if

            }
        }
    }

    public static void salvageSystemCrit(int location, CriticalSlot cs,
            Entity unit) {

        if ((cs.getIndex() >= Mech.SYSTEM_LIFE_SUPPORT)
                && (cs.getIndex() <= Mech.SYSTEM_GYRO)) {
            for (int slot = 0; slot < unit.getNumberOfCriticals(location); slot++) {
                CriticalSlot crit = unit.getCritical(location, slot);

                if (crit == null) {
                    continue;
                }

                if (crit.getIndex() != cs.getIndex()) {
                    continue;
                }

                UnitUtils.salvageCriticalSlot(crit, unit);
            }
        } else {
            UnitUtils.salvageCriticalSlot(cs, unit);
        }
    }

    /**
     * Repairs all of the engines in a unit.
     *
     * @param unit
     */
    public static void repairDamagedEngine(Entity unit) {

        for (int x = 0; x < unit.locations(); x++) {
            for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                CriticalSlot cs = unit.getCritical(x, y);
                if (!UnitUtils.isEngineCrit(cs)) {
                    continue;
                }
                UnitUtils.fixCriticalSlot(cs, unit, cs.isBreached());
                unit.setCritical(x, y, cs);
            }
        }
    }

    /**
     * Repairs weapons that are split between locations Used for mechs Only.
     *
     * @param unit
     */
    public static void repairSplitEquipment(Mounted eq, Entity unit) {

        // Only mechs should have split weapons crits.
        if (!(unit instanceof Mech)) {
            return;
        }

        // can only split weapons in toros and arms.
        for (int x = LOC_CT; x <= LOC_LARM; x++) {

            for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                CriticalSlot cs = unit.getCritical(x, y);
                if (cs == null) {
                    continue;
                }

                if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted mounted = cs.getMount();

                    if (eq.equals(mounted)) {
                        UnitUtils.fixCriticalSlot(cs, unit, cs.isBreached());
                        unit.setCritical(x, y, cs);
                    }
                }// end getType() if

            }
        }
    }

    public static void repairSystemCrit(int location, CriticalSlot cs,
            Entity unit) {

        if ((cs.getIndex() >= Mech.SYSTEM_LIFE_SUPPORT)
                && (cs.getIndex() <= Mech.SYSTEM_GYRO)) {
            for (int slot = 0; slot < unit.getNumberOfCriticals(location); slot++) {
                CriticalSlot crit = unit.getCritical(location, slot);

                if (crit == null) {
                    continue;
                }

                if (crit.getIndex() != cs.getIndex()) {
                    continue;
                }

                UnitUtils.fixCriticalSlot(crit, unit, crit.isBreached());
            }
        } else {
            UnitUtils.fixCriticalSlot(cs, unit, cs.isBreached());
        }
    }

    /**
     * Fix a crit based on if its damaged or breached. Breached flags are fixed
     * first.
     *
     * @param cs
     * @param unit
     * @param breach
     */
    public static void fixCriticalSlot(CriticalSlot cs, Entity unit,
            boolean breach) {
        if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
            Mounted mounted = cs.getMount();
            if (breach) {
                mounted.setBreached(false);
            } else {
                mounted.setDestroyed(false);
                mounted.setMissing(false);
                mounted.setHit(false);
            }
        }

        if (breach) {
            cs.setBreached(false);
            cs.setRepairing(false);
        } else {
            cs.setDestroyed(false);
            cs.setHit(false);
            cs.setRepairing(false);
            cs.setMissing(false);
        }
    }

    public static boolean isEngineCrit(CriticalSlot cs) {
        if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM)
                && (cs.getIndex() == Mech.SYSTEM_ENGINE)) {
            return true;
        }
        return false;
    }

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

        CriticalSlot cs = unit.getCritical(location, slot);

        return UnitUtils.getNumberOfCrits(unit, cs);
    }

    public static int getNumberOfCrits(Entity unit, CriticalSlot cs) {

        if (cs == null) {
            return 0;
        }
        int numberOfCrits = 1;
        // Engine return all engine crits
        if (UnitUtils.isEngineCrit(cs)) {
            numberOfCrits = UnitUtils.getNumberOfEngineCrits(unit);
        }

        // equipment ruturn all mounted crits;
        else if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
            Mounted mounted = cs.getMount();
            numberOfCrits = mounted.getType().getCriticals(unit);
        } else {
            numberOfCrits = UnitUtils.getNumberOfSystemCriticals(unit, cs);
        }

        // always return at least 1 crit.
        return Math.max(1, numberOfCrits);
    }

    // Sets multiple system crits to repairing.
    // Gyro Life support and Sensors.
    public static void setRepairingSystems(Entity unit, CriticalSlot cs) {
        if (cs.getIndex() == Mech.SYSTEM_GYRO) {
            for (int slot = 0; slot < unit.getNumberOfCriticals(Mech.LOC_CT); slot++) {
                CriticalSlot crit = unit.getCritical(Mech.LOC_CT, slot);
                if ((crit == null)
                        || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }

                if (crit.getIndex() == cs.getIndex()) {
                    crit.setRepairing(true);
                }
            }
        }// if its not a GYRO then its sensors or life support
         // as engines have already been filtered
        else {
            if (((Mech) unit).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
                for (int location = LOC_CT; location <= LOC_LT; location++) {
                    for (int slot = 0; slot < unit
                            .getNumberOfCriticals(location); slot++) {
                        CriticalSlot crit = unit.getCritical(location, slot);
                        if ((crit == null)
                                || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                            continue;
                        }

                        if (crit.getIndex() == cs.getIndex()) {
                            crit.setRepairing(true);
                        }
                    }
                }
            }// Normal cockpit in the head.
            else {
                for (int slot = 0; slot < unit.getNumberOfCriticals(LOC_HEAD); slot++) {
                    CriticalSlot crit = unit.getCritical(LOC_HEAD, slot);
                    if ((crit == null)
                            || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                        continue;
                    }

                    if (crit.getIndex() == cs.getIndex()) {
                        crit.setRepairing(true);
                    }
                }
            }
        }
    }

    // Sets multiple system crits to repairing.
    // Gyro Life support and Sensors.
    public static int getNumberOfSystemCriticals(Entity unit, CriticalSlot cs) {
        int count = 0;

        // actuators are always 1.
        if (cs.getIndex() > Mech.SYSTEM_GYRO) {
            return 1;
        }

        if (cs.getIndex() == Mech.SYSTEM_GYRO) {
            for (int slot = 0; slot < unit.getNumberOfCriticals(Mech.LOC_CT); slot++) {
                CriticalSlot crit = unit.getCritical(Mech.LOC_CT, slot);
                if ((crit == null)
                        || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }

                if (crit.getIndex() == cs.getIndex()) {
                    count++;
                }
            }
        }// if its not a GYRO then its sensors or life support
         // as engines have already been filtered
        else {
            if (((Mech) unit).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
                for (int location = LOC_CT; location <= LOC_LT; location++) {
                    for (int slot = 0; slot < unit
                            .getNumberOfCriticals(location); slot++) {
                        CriticalSlot crit = unit.getCritical(location, slot);
                        if ((crit == null)
                                || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                            continue;
                        }

                        if (crit.getIndex() == cs.getIndex()) {
                            count++;
                        }
                    }
                }
            }// Normal cockpit in the head.
            else {
                for (int slot = 0; slot < unit.getNumberOfCriticals(LOC_HEAD); slot++) {
                    CriticalSlot crit = unit.getCritical(LOC_HEAD, slot);
                    if ((crit == null)
                            || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                        continue;
                    }

                    if (crit.getIndex() == cs.getIndex()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // Sets multiple system crits to repairing.
    // Gyro Life support and Sensors.
    public static int getNumberOfDamagedSystemCriticals(Entity unit,
            CriticalSlot cs) {
        int count = 0;

        // actuators are always 1.
        if (cs.getIndex() > Mech.SYSTEM_GYRO) {

            if (cs.isDamaged()) {
                return 1;
            }

            return 0;
        }

        if (cs.getIndex() == Mech.SYSTEM_GYRO) {
            for (int slot = 0; slot < unit.getNumberOfCriticals(Mech.LOC_CT); slot++) {
                CriticalSlot crit = unit.getCritical(Mech.LOC_CT, slot);
                if ((crit == null)
                        || (crit.getType() != CriticalSlot.TYPE_SYSTEM)
                        || !crit.isDamaged()) {
                    continue;
                }

                if (crit.getIndex() == cs.getIndex()) {
                    count++;
                }
            }
        }// if its not a GYRO then its sensors or life support
         // as engines have already been filtered
        else {
            if (((Mech) unit).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
                for (int location = LOC_CT; location <= LOC_LT; location++) {
                    for (int slot = 0; slot < unit
                            .getNumberOfCriticals(location); slot++) {
                        CriticalSlot crit = unit.getCritical(location, slot);
                        if ((crit == null)
                                || (crit.getType() != CriticalSlot.TYPE_SYSTEM)
                                || !crit.isDamaged()) {
                            continue;
                        }

                        if (crit.getIndex() == cs.getIndex()) {
                            count++;
                        }
                    }
                }
            }// Normal cockpit in the head.
            else {
                for (int slot = 0; slot < unit.getNumberOfCriticals(LOC_HEAD); slot++) {
                    CriticalSlot crit = unit.getCritical(LOC_HEAD, slot);
                    if ((crit == null)
                            || (crit.getType() != CriticalSlot.TYPE_SYSTEM)
                            || !crit.isDamaged()) {
                        continue;
                    }

                    if (crit.getIndex() == cs.getIndex()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static void setRepairing(Entity unit, CriticalSlot cs) {

        if (UnitUtils.isEngineCrit(cs)) {
            UnitUtils.setRepairingEngines(unit);
        } else if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
            if (cs.getIndex() <= Mech.SYSTEM_GYRO) {
                UnitUtils.setRepairingSystems(unit, cs);
            } else {
                cs.setRepairing(true);
            }
        } else {

            Mounted eq = cs.getMount();
            int location = eq.getLocation();

            if (eq.isSplit()) {
                UnitUtils.setRepairingSplit(eq, unit);
                return;
            }

            for (int slot = 0; slot < unit.getNumberOfCriticals(location); slot++) {
                CriticalSlot crit = unit.getCritical(location, slot);
                if (crit == null) {
                    continue;
                }

                if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted mounted = crit.getMount();

                    if (eq.equals(mounted)) {
                        crit.setRepairing(true);
                    }
                }// end getType() if
            }// end for
        }
    }

    public static void setRepairingSplit(Mounted eq, Entity unit) {

        // Only mechs should have split weapons crits.
        if (!(unit instanceof Mech)) {
            return;
        }

        // can only split weapons in toros and arms.
        for (int x = LOC_CT; x <= LOC_LARM; x++) {

            for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                CriticalSlot cs = unit.getCritical(x, y);
                if (cs == null) {
                    continue;
                }

                if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted mounted = cs.getMount();

                    if (eq.equals(mounted)) {
                        cs.setRepairing(true);
                    }
                }// end getType() if

            }
        }
    }

    /*
     * This method checks equipment slots for critcals that shouldn't really
     * need repairing. i.e. endo armor slots. Non equipment slots are
     * automatcially returned as false.
     */
    public static boolean isNonRepairableCrit(Entity unit, CriticalSlot cs) {

        // only equipment slots should be checked
        if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
            return false;
        }

        try {
            Mounted mounted = cs.getMount();

            if (mounted
                    .getDesc()
                    .indexOf(
                            EquipmentType
                                    .getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS)) != -1) {
                return true;
            }

            if (mounted
                    .getDesc()
                    .indexOf(
                            EquipmentType
                                    .getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO)) != -1) {
                return true;
            }

            if (mounted.getDesc().indexOf(
                    EquipmentType
                            .getArmorTypeName(EquipmentType.T_ARMOR_HARDENED)) != -1) {
                return true;
            }

            if (mounted
                    .getDesc()
                    .indexOf(
                            EquipmentType
                                    .getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_FERRO)) != -1) {
                return true;
            }

            if (mounted
                    .getDesc()
                    .indexOf(
                            EquipmentType
                                    .getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_FERRO)) != -1) {
                return true;
            }

            if (mounted.getDesc().indexOf(
                    EquipmentType
                            .getArmorTypeName(EquipmentType.T_ARMOR_PATCHWORK)) != -1) {
                return true;
            }

            if (mounted.getDesc().indexOf(
                    EquipmentType
                            .getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE)) != -1) {
                return true;
            }

            if (mounted
                    .getDesc()
                    .indexOf(
                            EquipmentType
                                    .getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE)) != -1) {
                return true;
            }

            if (mounted.getDesc().indexOf(
                    EquipmentType
                            .getArmorTypeName(EquipmentType.T_ARMOR_STEALTH)) != -1) {
                return true;
            }

            if (mounted
                    .getDesc()
                    .indexOf(
                            EquipmentType
                                    .getStructureTypeName(EquipmentType.T_STRUCTURE_ENDO_STEEL)) != -1) {
                return true;
            }

            if (mounted
                    .getDesc()
                    .indexOf(
                            EquipmentType
                                    .getStructureTypeName(EquipmentType.T_STRUCTURE_COMPOSITE)) != -1) {
                return true;
            }

            if (mounted
                    .getDesc()
                    .indexOf(
                            EquipmentType
                                    .getStructureTypeName(EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE)) != -1) {
                return true;
            }

            if (mounted
                    .getDesc()
                    .indexOf(
                            EquipmentType
                                    .getStructureTypeName(EquipmentType.T_STRUCTURE_REINFORCED)) != -1) {
                return true;
            }

            if (mounted
                    .getDesc()
                    .indexOf(
                            EquipmentType
                                    .getStructureTypeName(EquipmentType.T_STRUCTURE_REINFORCED)) != -1) {
                return true;
            }

            if ((mounted.getType() instanceof MiscType)
                    && mounted.getType().hasFlag(MiscType.F_TSM)) {
                return true;
            }

            if ((mounted.getType() instanceof MiscType)
                    && mounted.getType().hasFlag(MiscType.F_CASE)
                    && unit.isClan()) {
                return true;
            }

        } catch (Exception ex) {
            MWLogger.errLog("Error in UnitUtils.isNonRepairableCrit");
            MWLogger.errLog(ex);
            return false;
        }
        return false;
    }

    /**
     * Set all engine crits to repairing!
     *
     * @param unit
     */
    public static void setRepairingEngines(Entity unit) {

        for (int x = 0; x < unit.locations(); x++) {
            for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                CriticalSlot cs = unit.getCritical(x, y);
                if (!UnitUtils.isEngineCrit(cs)) {
                    continue;
                }
                cs.setRepairing(true);
                unit.setCritical(x, y, cs);
            }
        }
    }

    public static String techDescription(int tech) {
        String result = "";

        switch (tech) {
            case TECH_GREEN:
                result = "Green";
                break;
            case TECH_REG:
                result = "Reg";
                break;
            case TECH_VET:
                result = "Vet";
                break;
            case TECH_ELITE:
                result = "Elite";
                break;
            case TECH_PILOT:
                result = "Pilot";
                break;
            case TECH_REWARD_POINTS:
                result = "Reward Points";
                break;
        }
        return result;
    }

    public static int techType(String tech) {
        int techType = TECH_GREEN;

        if (tech.equalsIgnoreCase("regular") || tech.equalsIgnoreCase("reg")) {
            techType = TECH_REG;
        } else if (tech.equalsIgnoreCase("vet")
                || tech.equalsIgnoreCase("Veteran")) {
            techType = TECH_VET;
        } else if (tech.equalsIgnoreCase("Elite")) {
            techType = TECH_ELITE;
        } else if (tech.equalsIgnoreCase("Pilot")) {
            techType = TECH_PILOT;
        } else if (tech.equalsIgnoreCase("Reward Points")) {
            techType = TECH_REWARD_POINTS;
        }

        return techType;
    }

    public static int techBaseRoll(int techType) {
        int roll = 9;

        if (techType == TECH_GREEN) {
            roll = 9;
        } else {
            roll = 8 - techType;
        }
        return roll;
    }

    public static int getTechRoll(Entity unit, int location, int slot,
            int techType, boolean armor, int techLevel) {
        return UnitUtils.getTechRoll(unit, location, slot, techType, armor,
                techLevel, false);
    }

    public static int getTechRoll(Entity unit, int location, int slot,
            int techType, boolean armor, int techLevel, boolean salvage) {
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
                    UnitUtils.removeArmorRepair(unit, LOC_INTERNAL_ARMOR,
                            location);
                    armorToRepair = unit.getOInternal(location)
                            - unit.getInternal(location);
                    UnitUtils
                            .setArmorRepair(unit, LOC_INTERNAL_ARMOR, location);
                } else {
                    armorToRepair = unit.getOInternal(location)
                            - unit.getInternal(location);
                }

                // has to replace the whole location.
                if (unit.getInternal(location) <= 0) {
                    if ((location == Mech.LOC_LARM)
                            || (location == Mech.LOC_RARM)
                            || (location == Mech.LOC_RLEG)
                            || (location == Mech.LOC_LLEG)) {
                        roll += 2;
                    } else if (location == Mech.LOC_HEAD) {
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
            CriticalSlot cs = unit.getCritical(location, slot);
            // MWLogger.errLog("Location: "+location+" slot:"+slot);

            if (cs == null) {
                return roll;
            }

            // MWLogger.errLog("Crit: "+cs.getIndex()+"/"+cs.getType());
            /*
             * if ( !cs.isDamaged() && !cs.isBreached()) { return roll; }
             */

            if (cs.isBreached() && !salvage) {
                return 2;
            }

            if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                Mounted m = cs.getMount();

                if (m != null) {
                    if (!m.isDestroyed() && !m.isBreached()) {
                        return roll;
                    } else if (m.getDesc().indexOf("Heat Sink") > -1) {
                        roll--;
                    } else if (m.getDesc().indexOf("Jump Jet") > -1) {
                        roll++;
                    }
                } else {
                    if (!cs.isMissing()) {
                        int crits = UnitUtils.getNumberOfCrits(unit, cs);

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
                if (unit.isOmni()
                        && UnitUtils.isCompatibleTech(unit, techLevel)) {
                    roll -= 4;
                }
            }// end CS type if
            else {

                // MWLogger.errLog("CS is Type System!");
                // System.err.flush();

                if (UnitUtils.isEngineCrit(cs)) {
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
                    if (cs.getIndex() == Mech.SYSTEM_SENSORS) {
                        int crits = unit.getBadCriticals(
                                CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS,
                                Mech.LOC_HEAD);
                        if ((crits >= 2) && !cs.isMissing()) {
                            roll += 4;
                        } else if (crits > 0) {
                            roll++;
                        }
                    } else if (cs.getIndex() == Mech.SYSTEM_GYRO) {
                        // MWLogger.errLog("Gyro!");
                        // System.err.flush();
                        if (cs.isMissing()) {
                            roll++;
                        } else {
                            int crits = unit.getBadCriticals(
                                    CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,
                                    Mech.LOC_CT);
                            if (crits == 0) {
                                roll++;
                            } else if (crits == 1) {
                                roll += 2;
                            } else {
                                roll += 5;
                            }
                        }
                    } else if (cs.getIndex() == Mech.SYSTEM_LIFE_SUPPORT) {
                        if (!cs.isMissing()) {
                            int crits = unit.getBadCriticals(
                                    CriticalSlot.TYPE_SYSTEM,
                                    Mech.SYSTEM_LIFE_SUPPORT, Mech.LOC_HEAD);
                            if (crits == 2) {
                                roll += 2;
                            }
                        }

                    } else if (UnitUtils.isActuator(cs)) {
                        if (cs.isMissing()) {
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

    public static boolean isActuator(CriticalSlot cs) {

        if (cs.getType() != CriticalSlot.TYPE_SYSTEM) {
            return false;
        }

        if ((cs.getIndex() == Mech.ACTUATOR_FOOT)
                || (cs.getIndex() == Mech.ACTUATOR_HAND)
                || (cs.getIndex() == Mech.ACTUATOR_HIP)
                || (cs.getIndex() == Mech.ACTUATOR_LOWER_ARM)
                || (cs.getIndex() == Mech.ACTUATOR_LOWER_LEG)
                || (cs.getIndex() == Mech.ACTUATOR_SHOULDER)
                || (cs.getIndex() == Mech.ACTUATOR_UPPER_ARM)
                || (cs.getIndex() == Mech.ACTUATOR_UPPER_LEG)) {
            return true;
        }

        return false;

    }

    /**
     * Some EQ can take up multiple slots this will track them down and repair
     * them.
     *
     * @param eq
     * @param unit
     * @param location
     */
    public static void removeRepairEquipment(Mounted eq, Entity unit,
            int location) {

        if (eq.isSplit()) {
            UnitUtils.removeRepairSplitEquipment(eq, unit);
            return;
        }

        for (int slot = 0; slot < unit.getNumberOfCriticals(location); slot++) {
            CriticalSlot crit = unit.getCritical(location, slot);
            if (crit == null) {
                continue;
            }
            if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                Mounted mounted = crit.getMount();

                if (eq.equals(mounted) && crit.isRepairing()) {
                    crit.setRepairing(false);
                    unit.setCritical(location, slot, crit);
                }
            }// end getType() if
        }// end for
    }

    public static void removeRepairing(Entity unit, CriticalSlot cs) {

        if (UnitUtils.isEngineCrit(cs)) {
            UnitUtils.removeRepairDamagedEngine(unit);
        } else if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
            if (cs.getIndex() <= Mech.SYSTEM_GYRO) {
                UnitUtils.removeRepairingSystems(unit, cs);
            } else {
                cs.setRepairing(false);
            }
        } else {

            Mounted eq = cs.getMount();
            int location = eq.getLocation();

            UnitUtils.removeRepairEquipment(eq, unit, location);
        }// end else
    }// end removeRepairing

    // Sets multiple system crits to repairing.
    // Gyro Life support and Sensors.
    public static void removeRepairingSystems(Entity unit, CriticalSlot cs) {
        if (cs.getIndex() == Mech.SYSTEM_GYRO) {
            for (int slot = 0; slot < unit.getNumberOfCriticals(Mech.LOC_CT); slot++) {
                CriticalSlot crit = unit.getCritical(Mech.LOC_CT, slot);
                if ((crit == null)
                        || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }

                if (crit.getIndex() == cs.getIndex()) {
                    crit.setRepairing(false);
                }
            }
        }// if its not a GYRO then its sensors or life support
         // as engines have already been filtered
        else {
            if (((Mech) unit).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
                for (int location = LOC_CT; location <= LOC_LT; location++) {
                    for (int slot = 0; slot < unit
                            .getNumberOfCriticals(location); slot++) {
                        CriticalSlot crit = unit.getCritical(location, slot);
                        if ((crit == null)
                                || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                            continue;
                        }

                        if (crit.getIndex() == cs.getIndex()) {
                            crit.setRepairing(false);
                        }
                    }
                }
            }// Normal cockpit in the head.
            else {
                for (int slot = 0; slot < unit.getNumberOfCriticals(LOC_HEAD); slot++) {
                    CriticalSlot crit = unit.getCritical(LOC_HEAD, slot);
                    if ((crit == null)
                            || (crit.getType() != CriticalSlot.TYPE_SYSTEM)) {
                        continue;
                    }

                    if (crit.getIndex() == cs.getIndex()) {
                        crit.setRepairing(false);
                    }
                }
            }
        }
    }

    public static void removeArmorRepair(Entity unit, int slot, int location) {

        if (slot < UnitUtils.LOC_INTERNAL_ARMOR) {
            // incase something was fubared.
            if (location >= UnitUtils.LOC_CTR) {
                location -= 7;
            }
            while (unit.getArmor(location, slot == UnitUtils.LOC_REAR_ARMOR) > unit
                    .getOArmor(location, slot == UnitUtils.LOC_REAR_ARMOR)) {
                int currArmor = unit.getArmor(location,
                        slot == UnitUtils.LOC_REAR_ARMOR);
                currArmor -= 99;
                unit.setArmor(currArmor, location,
                        slot == UnitUtils.LOC_REAR_ARMOR);
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

    public static void setArmorRepair(Entity unit, int slot, int location) {
        if (slot < LOC_INTERNAL_ARMOR) {
            if (location >= UnitUtils.LOC_CTR) {
                location -= 7;
            }

            while (unit.getArmor(location, slot == UnitUtils.LOC_REAR_ARMOR) < unit
                    .getOArmor(location, slot == UnitUtils.LOC_REAR_ARMOR)) {
                int currArmor = unit.getArmor(location,
                        slot == UnitUtils.LOC_REAR_ARMOR);
                currArmor += 99;
                unit.setArmor(currArmor, location,
                        slot == UnitUtils.LOC_REAR_ARMOR);
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
     * Repairs all of the engines in a unit.
     *
     * @param unit
     */
    public static void removeRepairDamagedEngine(Entity unit) {

        for (int x = 0; x < unit.locations(); x++) {
            for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                CriticalSlot cs = unit.getCritical(x, y);
                if (!UnitUtils.isEngineCrit(cs)) {
                    continue;
                }

                if (cs.isRepairing()) {
                    cs.setRepairing(false);
                    unit.setCritical(x, y, cs);
                }
            }
        }
    }

    /**
     * Repairs weapons that are split between locations Used for mechs Only.
     *
     * @param unit
     */
    public static void removeRepairSplitEquipment(Mounted eq, Entity unit) {

        // Only mechs should have split weapons crits.
        if (!(unit instanceof Mech)) {
            return;
        }

        // can only split weapons in toros and arms.
        for (int x = LOC_CT; x <= LOC_LARM; x++) {

            for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                CriticalSlot cs = unit.getCritical(x, y);
                if (cs == null) {
                    continue;
                }

                if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted mounted = cs.getMount();

                    if (eq.equals(mounted) && cs.isRepairing()) {
                        cs.setRepairing(false);
                        unit.setCritical(x, y, cs);
                    }
                }// end getType() if

            }
        }
    }

    public static int getPartCost(Entity unit, int location, int slot,
            boolean armor, int year) {
        double cost = 0;

        if (!(unit instanceof Mech)) {
            return 0;
        }

        Mech mek = (Mech) unit;

        if (armor) {

            // External Armor
            if (slot < LOC_INTERNAL_ARMOR) {
                double points = 16.0 * EquipmentType.getArmorPointMultiplier(
                        unit.getArmorType(slot), unit.getArmorTechLevel(slot));
                double costPerTon = EquipmentType.getArmorCost(unit
                        .getArmorType(slot));

                // just in case
                if (points == 0) {
                    points = 16;
                }

                cost = costPerTon / points;
                boolean rear = slot == LOC_REAR_ARMOR;

                cost = (mek.getOArmor(location, rear) - mek.getArmor(location,
                        rear)) * cost;
            }// IS Armor
            else {
                double structureCost = EquipmentType.getStructureCost(mek
                        .getStructureType());// IS

                /*
                 * if(mek.hasEndo() || mek.hasCompositeStructure()) {
                 * structureCost=1600; } if(mek.hasReinforcedStructure()) {
                 * structureCost=6400; }
                 */
                cost = structureCost / 8;
                cost = (mek.getOInternal(location) - mek.getInternal(location))
                        * cost;
            }
        } else {// Crit
            CriticalSlot cs = unit.getCritical(location, slot);

            if (cs == null) {
                return 0;
            }

            if (cs.isBreached()) {
                return 0;
            }

            if (!cs.isDamaged()) {
                return 0;
            }

            if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                Mounted m = cs.getMount();

                if (m.getDesc().indexOf("Heat Sink") > -1) {
                    if (m.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                        if ((m.getType().getTechLevel(year) == TechConstants.T_IS_ADVANCED)
                                || (m.getType().getTechLevel(year) == TechConstants.T_IS_EXPERIMENTAL)) {
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
                    int itemCost = (int) m.getType().getCost(mek,
                            m.isArmored(), m.getLocation());
                    cost = itemCost;
                }

            }// end CS type if
            else {
                if (UnitUtils.isEngineCrit(cs)) {
                    Engine engine = mek.getEngine();
                    // (weight*walk=rating; rating*weight*cost factor = cost of
                    // engine.
                    cost = (engine.getBaseCost() * engine.getRating() * mek
                            .getWeight()) / 75.0;
                    double totalEngineCrits = UnitUtils
                            .getNumberOfEngineCrits(unit);
                    double damagedEngineCrits = UnitUtils
                            .getNumberOfDamagedEngineCrits(unit);
                    cost = cost * (damagedEngineCrits / totalEngineCrits);

                } else {
                    if (cs.getIndex() == Mech.SYSTEM_SENSORS) {
                        cost = mek.getWeight() * 2000;// sensors
                    } else if (cs.getIndex() == Mech.SYSTEM_GYRO) {
                        if (mek.getGyroType() == Mech.GYRO_XL) {
                            cost = 750000 * (int) Math
                                    .ceil((mek.getOriginalWalkMP() * mek
                                            .getWeight()) / 100f) * 0.5;
                        } else if (mek.getGyroType() == Mech.GYRO_COMPACT) {
                            cost = 400000 * (int) Math
                                    .ceil((mek.getOriginalWalkMP() * mek
                                            .getWeight()) / 100f) * 1.5;
                        } else if (mek.getGyroType() == Mech.GYRO_HEAVY_DUTY) {
                            cost = 500000 * (int) Math
                                    .ceil((mek.getOriginalWalkMP() * mek
                                            .getWeight()) / 100f) * 2;
                        } else {
                            cost = 300000 * (int) Math
                                    .ceil((mek.getOriginalWalkMP() * mek
                                            .getWeight()) / 100f);
                        }
                    } else if (cs.getIndex() == Mech.SYSTEM_LIFE_SUPPORT) {
                        cost = 50000;// life support
                    } else if (cs.getIndex() == Mech.SYSTEM_COCKPIT) {
                        if (mek.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
                            cost = 750000;
                        } else if (mek.getCockpitType() == Mech.COCKPIT_SMALL) {
                            cost = 175000;
                        } else {
                            cost = 200000;
                        }
                    } else if (UnitUtils.isActuator(cs)) {
                        if (cs.getIndex() == Mech.ACTUATOR_HAND) {
                            cost = mek.getWeight() * 80;
                        } else if (cs.getIndex() == Mech.ACTUATOR_LOWER_ARM) {
                            cost = mek.getWeight() * 50;
                        } else if ((cs.getIndex() == Mech.ACTUATOR_UPPER_ARM)
                                || (cs.getIndex() == Mech.ACTUATOR_SHOULDER)) {
                            cost = mek.getWeight() * 100;
                        } else if (cs.getIndex() == Mech.ACTUATOR_FOOT) {
                            cost = mek.getWeight() * 120;
                        } else if (cs.getIndex() == Mech.ACTUATOR_LOWER_LEG) {
                            cost = mek.getWeight() * 80;
                        } else if ((cs.getIndex() == Mech.ACTUATOR_UPPER_LEG)
                                || (cs.getIndex() == Mech.ACTUATOR_HIP)) {
                            cost = mek.getWeight() * 150;
                        }
                    }

                }
            }
        }
        return (int) Math.ceil(cost);
    }

    public static int getTotalDamagedPartCost(Entity unit, int year) {

        double totalCost = 0;

        for (int location = 0; location < unit.locations(); location++) {
            if ((location == LOC_CT) || (location == LOC_RT)
                    || (location == LOC_LT)) {
                totalCost += UnitUtils.getPartCost(unit, location,
                        LOC_FRONT_ARMOR, true, year);
                totalCost += UnitUtils.getPartCost(unit, location,
                        LOC_REAR_ARMOR, true, year);
                totalCost += UnitUtils.getPartCost(unit, location,
                        LOC_INTERNAL_ARMOR, true, year);
            } else {
                totalCost += UnitUtils.getPartCost(unit, location,
                        LOC_FRONT_ARMOR, true, year);
                totalCost += UnitUtils.getPartCost(unit, location,
                        LOC_INTERNAL_ARMOR, true, year);
            }
            for (int slot = 0; slot < unit.getNumberOfCriticals(location); slot++) {
                totalCost += UnitUtils.getPartCost(unit, location, slot, false,
                        year);
            }
        }

        return (int) Math.ceil(totalCost);

    }

    public static String getRepairMessage(Entity unit, int location, int slot,
            boolean armor) {
        String repairMessage = "";

        if ((unit instanceof Mech) && (unit.getInternal(UnitUtils.LOC_CT) < 1)) {
            repairMessage = "This unit has been cored and cannot be repaired. Either Scrap it or try to salvage it for parts!";
            return repairMessage;
        }

        if (unit instanceof Tank) {
            // Turrets can be blown off and you can still repair the unit.
            for (int loc = Tank.LOC_FRONT; loc < Tank.LOC_TURRET; loc++) {
                if (unit.getInternal(loc) < 1) {
                    repairMessage = "This unit has been cored and cannot be repaired. Either Scrap it or try to salvage it for parts!";
                    return repairMessage;
                }
            }
        }

        if (((location == UnitUtils.LOC_RARM) && (unit
                .getInternal(UnitUtils.LOC_RT) != unit
                .getOInternal(UnitUtils.LOC_RT)))
                || ((location == UnitUtils.LOC_LARM) && (unit
                        .getInternal(UnitUtils.LOC_LT) != unit
                        .getOInternal(UnitUtils.LOC_LT)))) {
            repairMessage = ("You may not repair your "
                    + unit.getShortNameRaw() + "'s "
                    + unit.getLocationName(location) + " until the adjacent torso's internal structure is fully repaired.");
            return repairMessage;
        }

        if (location >= UnitUtils.LOC_CTR) {
            location -= 7;
        }

        if (armor) {

            int armorRepaired = 0;
            boolean rear = (slot == UnitUtils.LOC_REAR_ARMOR);
            if (slot < UnitUtils.LOC_INTERNAL_ARMOR) {
                armorRepaired = unit.getOArmor(location, rear)
                        - unit.getArmor(location, rear);

                if (armorRepaired == 0) {
                    if (rear) {
                        repairMessage = ("All external armor("
                                + unit.getLocationAbbr(location) + "r) has already been repaired.");
                    } else {
                        repairMessage = ("All external armor("
                                + unit.getLocationAbbr(location) + ") has already been repaired.");
                    }
                }
            } else {
                armorRepaired = unit.getOInternal(location)
                        - unit.getInternal(location);

                if (armorRepaired == 0) {
                    repairMessage = ("All internal structure("
                            + unit.getLocationAbbr(location) + ") has already been repaired.");
                }

            }

        } else {// crits
            if (unit.getInternal(location) != unit.getOInternal(location)) {
                repairMessage = ("You may not make any repairs to the until the internal structure("
                        + unit.getLocationAbbr(location) + ") is fully repaired!");
            }

            CriticalSlot cs = unit.getCritical(location, slot);

            if (cs == null) {
                repairMessage = ("There is no critical in that location please select another critical slot to repair!");
            }

            Mounted mount = null;

            if (!UnitUtils.isActuator(cs)) {
                mount = cs.getMount();
            }

            if (mount != null) {
                if (!mount.isDestroyed() && !mount.isBreached()
                        && !mount.isMissing() && !cs.isDamaged()
                        && !cs.isBreached()) {
                    repairMessage = ("That critical is not damaged!?!?");
                }
            } else if (!cs.isDamaged() && !cs.isBreached()) {
                repairMessage = ("That critical is not damaged?!?!");
            }

        }

        return repairMessage;
    }

    public static String getSalvageMessage(Entity unit, int location, int slot,
            boolean armor) {
        String salvageMessage = "";

        if ((armor && (slot == UnitUtils.LOC_INTERNAL_ARMOR))) {
            if (((location == UnitUtils.LOC_RT) && (unit
                    .getInternal(Mech.LOC_RARM) > 0))
                    || ((location == UnitUtils.LOC_LT) && (unit
                            .getInternal(UnitUtils.LOC_LARM) > 0))) {
                salvageMessage = ("You may not salvage your "
                        + unit.getShortNameRaw() + "'s "
                        + unit.getLocationName(location) + " until the adjacent arm's internal structure is fully removed.");
                return salvageMessage;
            }

            if ((location == UnitUtils.LOC_CT)
                    && (unit.getInternal(UnitUtils.LOC_LARM) > 0)
                    && (unit.getInternal(UnitUtils.LOC_RARM) > 0)) {
                salvageMessage = ("You may not salvage your "
                        + unit.getShortNameRaw() + "'s "
                        + unit.getLocationName(location) + " until the adjacent toro's internal structure is fully removed.");
                return salvageMessage;
            }

            if (UnitUtils.hasUndamagedCriticals(unit, location)
                    || UnitUtils.hasCriticalsUnderRepair(unit, location)) {
                salvageMessage = ("You may not salvage your "
                        + unit.getShortNameRaw() + "'s "
                        + unit.getLocationName(location) + " internal structure until the parts have been fully removed.");
                return salvageMessage;
            }
        }

        if (location >= UnitUtils.LOC_CTR) {
            location -= 7;
        }

        if (armor) {

            int armorLeft = 0;
            boolean rear = (slot == UnitUtils.LOC_REAR_ARMOR);
            if (slot < UnitUtils.LOC_INTERNAL_ARMOR) {
                armorLeft = unit.getArmor(location, rear);

                if (armorLeft == 0) {
                    if (rear) {
                        salvageMessage = ("All external armor("
                                + unit.getLocationAbbr(location) + "r) has already been removed.");
                    } else {
                        salvageMessage = ("All external armor("
                                + unit.getLocationAbbr(location) + ") has already been removed.");
                    }
                }
            } else {
                armorLeft = unit.getInternal(location);

                if (armorLeft == 0) {
                    salvageMessage = ("All internal structure("
                            + unit.getLocationAbbr(location) + ") has already been removed.");
                }

            }

        } else {// crits
            CriticalSlot cs = unit.getCritical(location, slot);

            if (cs == null) {
                salvageMessage = ("There is no critical in that location please select another critical slot to salvage!");
            }

            if (UnitUtils.isNonRepairableCrit(unit, cs) || cs.isDamaged()) {
                salvageMessage = ("That critical is not salvagable!");
            }

        }

        return salvageMessage;
    }

    public static boolean checkRepairViability(Entity unit, int location,
            int slot, boolean armor) {

        if (((location == UnitUtils.LOC_RARM) && (unit
                .getInternal(UnitUtils.LOC_RT) != unit
                .getOInternal(UnitUtils.LOC_RT)))
                || ((location == UnitUtils.LOC_LARM) && (unit
                        .getInternal(UnitUtils.LOC_LT) != unit
                        .getOInternal(UnitUtils.LOC_LT)))) {
            return false;
        }

        if (armor) {
            return true;
        }

        if (unit.getInternal(location) != unit.getOInternal(location)) {
            return false;
        }

        return true;
    }

    /*
     * Had to lift this from MM. Was a bug you could set your level 3 targetting
     * system to anything if your TC was damaged then repair your TC and get
     * double the bonus.
     */
    public static boolean hasTargettingComputer(Entity unit) {
        for (Mounted m : unit.getMisc()) {
            if ((m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_TARGCOMP)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAllAmmo(Entity unit) {

        for (Mounted ammo : unit.getAmmo()) {
            int shots = 0;
            if (ammo.byShot()) {
                shots = ammo.getOriginalShots();
            } else {
                shots = ((AmmoType) ammo.getType()).getShots();
            }
            if (ammo.getLocation() == Entity.LOC_NONE) {
                if (ammo.getUsableShotsLeft() != 1) {
                    return false;
                }
            } else if (ammo.getUsableShotsLeft() != shots) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAmmoless(Entity unit) {
        return unit.getAmmo().size() == 0;
    }

    public static int getShots(Mounted m) {
        if (m.byShot()) {
            return m.getOriginalShots();
        } else {
            return ((AmmoType) m.getType()).getShots();
        }
    }

    public static boolean hasLowAmmo(Entity unit) {

        for (Mounted ammo : unit.getAmmo()) {

            if (ammo == null) {
                continue;
            }
            try {
                int shots = getShots(ammo);
                if (ammo.getLocation() == Entity.LOC_NONE) {
                    if (ammo.getUsableShotsLeft() == 0) {
                        return true;
                    }
                } else if ((ammo.getUsableShotsLeft() < shots)
                        && (ammo.getUsableShotsLeft() > 0)) {
                    return true;
                }
            } catch (Exception ex) {
                MWLogger.errLog(ex);
                continue;
            }
        }
        return false;
    }

    public static boolean hasEmptyAmmo(Entity unit) {

        for (Mounted ammo : unit.getAmmo()) {

            if (ammo.getUsableShotsLeft() == 0) {
                return true;
            }
        }
        return false;
    }

    public static String getArmorShortName(Entity unit, int location) {
        String armorName = "Standard";

        switch (unit.getArmorType(location)) {
            case EquipmentType.T_ARMOR_STANDARD:
                armorName = "Standard";
                break;
            case EquipmentType.T_ARMOR_FERRO_FIBROUS:
                armorName = "FF";
                break;
            case EquipmentType.T_ARMOR_REACTIVE:
                armorName = "Reactive";
                break;
            case EquipmentType.T_ARMOR_REFLECTIVE:
                armorName = "Reflective";
                break;
            case EquipmentType.T_ARMOR_HARDENED:
                armorName = "Hardened";
                break;
            case EquipmentType.T_ARMOR_LIGHT_FERRO:
                armorName = "LFF";
                break;
            case EquipmentType.T_ARMOR_HEAVY_FERRO:
                armorName = "HFF";
                break;
            case EquipmentType.T_ARMOR_PATCHWORK:
                armorName = "Patchwork";
                break;
            case EquipmentType.T_ARMOR_STEALTH:
                armorName = "Stealth";
                break;
            case EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO:
                armorName = "FFProto";
                break;
        }
        return armorName;
    }

    public static String getInternalShortName(Entity unit) {
        String internalName = "Standard";

        switch (unit.getStructureType()) {
            case EquipmentType.T_STRUCTURE_STANDARD:
                internalName = "Standard";
                break;
            case EquipmentType.T_STRUCTURE_ENDO_STEEL:
                internalName = "Endo";
                break;
            case EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE:
                internalName = "EndoProto";
                break;
            case EquipmentType.T_STRUCTURE_REINFORCED:
                internalName = "Reinforced";
                break;
            case EquipmentType.T_STRUCTURE_COMPOSITE:
                internalName = "Composite";
                break;
        }
        return internalName;
    }

    /**
     * @author Torren (Jason Tighe) This wacky method is designed to find out if
     *         a CS is destoyred and needs to be replaced or damagaed and can be
     *         repaired. this is determined if more then 50% of the CS's in a
     *         Mount are damaged/missing/destroyed
     * @param unit
     * @param cs
     * @return <code>true</code> if destroyed <code>false</code> if damaged
     */
    public static boolean isDestroyedOrDamaged(Entity unit, CriticalSlot cs) {

        if (cs.isMissing()) {
            return cs.isMissing();
        }

        Mounted mount = cs.getMount();

        if (mount == null) {
            return true;
        }

        int totalCrits = 0;
        int damagedCrits = 0;

        if (mount.isSplit()) {
            int location = mount.getLocation();

            int numberOfSlots = unit.getNumberOfCriticals(location);
            for (int slot = 0; slot < numberOfSlots; slot++) {
                CriticalSlot crit = unit.getCritical(location, slot);
                try {
                    if ((crit != null) && crit.getMount().equals(cs.getMount())) {
                        totalCrits++;
                        if (crit.isDamaged()) {
                            damagedCrits++;
                        }
                    }
                } catch (Exception ex) {
                }
            }

            location = mount.getSecondLocation();
            numberOfSlots = unit.getNumberOfCriticals(location);
            for (int slot = 0; slot < numberOfSlots; slot++) {
                CriticalSlot crit = unit.getCritical(location, slot);
                try {
                    if ((crit != null) && crit.getMount().equals(cs.getMount())) {
                        totalCrits++;
                        if (crit.isDamaged()) {
                            damagedCrits++;
                        }
                    }
                } catch (Exception ex) {
                }
            }

            // more then 50% of the total crits are damages its toast.
            if (damagedCrits > (totalCrits / 2)) {
                return true;
            }

        } else {
            int numberOfSlots = unit.getNumberOfCriticals(mount.getLocation());
            for (int slot = 0; slot < numberOfSlots; slot++) {
                CriticalSlot crit = unit.getCritical(mount.getLocation(), slot);
                try {
                    if ((crit != null) && crit.getMount().equals(cs.getMount())) {
                        totalCrits++;
                        if (crit.isDamaged()) {
                            damagedCrits++;
                        }
                    }
                } catch (Exception ex) {
                }
            }

            // more then 50% of the total crits are damages its toast.
            if (damagedCrits > (totalCrits / 2)) {
                return true;
            }
        }
        return false;

    }

    public static int getNumberOfDamagedCrits(Entity unit, int slot, int loc,
            boolean armor) {

        if (armor) {

            if (slot == UnitUtils.LOC_INTERNAL_ARMOR) {
                return unit.getOInternal(loc) - unit.getInternal(loc);
            }
            if (loc >= UnitUtils.LOC_CTR) {
                return unit.getOArmor(loc - 7, true)
                        - unit.getArmor(loc - 7, true);
            }
            return unit.getOArmor(loc) - unit.getArmor(loc);
        }
        CriticalSlot cs = unit.getCritical(loc, slot);

        if (UnitUtils.isEngineCrit(cs)) {
            return UnitUtils.getNumberOfDamagedEngineCrits(unit);
        }

        if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
            Mounted mount = cs.getMount();

            int damagedCrits = 0;

            if ((mount != null) && mount.isSplit()) {
                int location = mount.getLocation();

                int numberOfSlots = unit.getNumberOfCriticals(location);
                for (int pos = 0; pos < numberOfSlots; pos++) {
                    CriticalSlot crit = unit.getCritical(location, pos);
                    try {
                        if ((crit != null) && crit.getMount().equals(mount)) {
                            if (crit.isDamaged()) {
                                damagedCrits++;
                            }
                        }
                    } catch (Exception ex) {
                    }
                }

                location = mount.getSecondLocation();
                numberOfSlots = unit.getNumberOfCriticals(location);
                for (int pos = 0; pos < numberOfSlots; pos++) {
                    CriticalSlot crit = unit.getCritical(location, pos);
                    try {
                        if ((crit != null) && crit.getMount().equals(mount)) {
                            if (crit.isDamaged()) {
                                damagedCrits++;
                            }
                        }
                    } catch (Exception ex) {
                    }
                }

            } else {
                int numberOfSlots = unit.getNumberOfCriticals(mount
                        .getLocation());
                for (int pos = 0; pos < numberOfSlots; pos++) {
                    CriticalSlot crit = unit.getCritical(mount.getLocation(),
                            pos);
                    try {
                        if ((crit != null) && crit.getMount().equals(mount)) {
                            if (crit.isDamaged()) {
                                damagedCrits++;
                            }
                        }
                    } catch (Exception ex) {
                    }
                }

            }
            return damagedCrits;

        }

        return UnitUtils.getNumberOfDamagedSystemCriticals(unit, cs);
    }

    public static String getCritName(Entity unit, int slot, int location,
            boolean armor) {

        if (armor) {
            if (slot == UnitUtils.LOC_INTERNAL_ARMOR) {
                if (EquipmentType.getArmorTypeName(unit.getStructureType())
                        .equalsIgnoreCase("Standard")) {
                    return "IS (STD)";
                }

                return EquipmentType.getStructureTypeName(unit
                        .getStructureType());

            } else {
                if (EquipmentType.getArmorTypeName(unit.getArmorType(location))
                        .equalsIgnoreCase("Standard")) {
                    return "Armor (STD)";
                }
                return EquipmentType.getArmorTypeName(unit
                        .getArmorType(location));
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

        if ((unit instanceof Mech)
                && (crit.getType() == CriticalSlot.TYPE_SYSTEM)) {

            if (crit.getIndex() == Mech.SYSTEM_ENGINE) {
                return UnitUtils.ENGINE_TECH_STRING[UnitUtils
                        .getEngineType(unit)];
            }

            if (crit.getIndex() == Mech.SYSTEM_GYRO) {
                return Mech.getGyroTypeString(unit.getGyroType());
            }

            if (crit.getIndex() == Mech.SYSTEM_COCKPIT) {
                return Mech
                        .getCockpitTypeString(((Mech) unit).getCockpitType());
            }

            return ((Mech) unit).getSystemName(crit.getIndex());
        }// end CS type if

        return crit.getMount().getType().getInternalName();

    }

    public static String getCritExternalName(Entity unit, int slot,
            int location, boolean armor) {

        if (armor) {
            if (slot == UnitUtils.LOC_INTERNAL_ARMOR) {
                if (EquipmentType.getArmorTypeName(unit.getStructureType())
                        .equalsIgnoreCase("Standard")) {
                    return "IS (STD)";
                }

                return EquipmentType.getStructureTypeName(unit
                        .getStructureType());

            } else {
                if (EquipmentType.getArmorTypeName(unit.getArmorType(slot))
                        .equalsIgnoreCase("Standard")) {
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
            Mounted mounted = crit.getMount();
            if (mounted.getType() instanceof AmmoType) {
                return "Ammo Bin";
            }

            return mounted.getName();
        }

        if ((unit instanceof Mech)
                && (crit.getType() == CriticalSlot.TYPE_SYSTEM)) {

            if (crit.getIndex() == Mech.SYSTEM_ENGINE) {
                return UnitUtils.ENGINE_TECH_STRING[UnitUtils
                        .getEngineType(unit)];
            }

            if (crit.getIndex() == Mech.SYSTEM_GYRO) {
                return Mech.getGyroTypeString(unit.getGyroType());
            }

            if (crit.getIndex() == Mech.SYSTEM_COCKPIT) {
                return Mech
                        .getCockpitTypeString(((Mech) unit).getCockpitType());
            }

            return ((Mech) unit).getSystemName(crit.getIndex());
        }// end CS type if

        return crit.getMount().getType().getInternalName();

    }

    public static boolean isCompatibleTech(Entity unit, int techLevel) {
        // armor and IS are universal everything else gets a +4 to the roll if
        // the tech levels
        // are not compatible.
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
                    case TechConstants.T_INTRO_BOXSET:
                        if ((techLevel != TechConstants.T_INTRO_BOXSET)
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

    public static boolean isSameTech(int partTechLevel, int houseTechLevel) {

        if ((houseTechLevel >= TechConstants.T_ALL)
                || (partTechLevel >= TechConstants.T_ALL)
                || (partTechLevel < TechConstants.T_INTRO_BOXSET)
                || (partTechLevel == houseTechLevel)) {
            return true;
        }

        switch (houseTechLevel) {
            case TechConstants.T_INTRO_BOXSET:
                switch (partTechLevel) {
                    case TechConstants.T_INTRO_BOXSET:
                        return true;
                    default:
                        return false;
                }
            case TechConstants.T_IS_TW_NON_BOX:
                switch (partTechLevel) {
                    case TechConstants.T_INTRO_BOXSET:
                    case TechConstants.T_IS_TW_NON_BOX:
                        return true;
                    default:
                        return false;
                }
            case TechConstants.T_IS_TW_ALL:
                switch (partTechLevel) {
                    case TechConstants.T_INTRO_BOXSET:
                    case TechConstants.T_IS_TW_NON_BOX:
                    case TechConstants.T_IS_TW_ALL:
                        return true;
                    default:
                        return false;
                }
            case TechConstants.T_IS_ADVANCED:
                switch (partTechLevel) {
                    case TechConstants.T_INTRO_BOXSET:
                    case TechConstants.T_IS_TW_NON_BOX:
                    case TechConstants.T_IS_TW_ALL:
                    case TechConstants.T_IS_ADVANCED:
                        return true;
                    default:
                        return false;
                }
            case TechConstants.T_IS_EXPERIMENTAL:
                switch (partTechLevel) {
                    case TechConstants.T_INTRO_BOXSET:
                    case TechConstants.T_IS_TW_NON_BOX:
                    case TechConstants.T_IS_TW_ALL:
                    case TechConstants.T_IS_ADVANCED:
                    case TechConstants.T_IS_EXPERIMENTAL:
                        return true;
                    default:
                        return false;
                }
            case TechConstants.T_IS_UNOFFICIAL:
            case TechConstants.T_ALL_IS:
                switch (partTechLevel) {
                    case TechConstants.T_INTRO_BOXSET:
                    case TechConstants.T_IS_TW_NON_BOX:
                    case TechConstants.T_IS_TW_ALL:
                    case TechConstants.T_IS_ADVANCED:
                    case TechConstants.T_IS_EXPERIMENTAL:
                    case TechConstants.T_IS_UNOFFICIAL:
                    case TechConstants.T_ALL_IS:
                        return true;
                    default:
                        return false;
                }
            case TechConstants.T_CLAN_TW:
                switch (partTechLevel) {
                    case TechConstants.T_CLAN_TW:
                        return true;
                    default:
                        return false;
                }
            case TechConstants.T_CLAN_ADVANCED:
                switch (partTechLevel) {
                    case TechConstants.T_CLAN_TW:
                    case TechConstants.T_CLAN_ADVANCED:
                        return true;
                    default:
                        return false;
                }
            case TechConstants.T_CLAN_EXPERIMENTAL:
                switch (partTechLevel) {
                    case TechConstants.T_CLAN_TW:
                    case TechConstants.T_CLAN_ADVANCED:
                    case TechConstants.T_CLAN_EXPERIMENTAL:
                        return true;
                    default:
                        return false;
                }
            case TechConstants.T_CLAN_UNOFFICIAL:
            case TechConstants.T_ALL_CLAN:
                switch (partTechLevel) {
                    case TechConstants.T_CLAN_TW:
                    case TechConstants.T_CLAN_ADVANCED:
                    case TechConstants.T_CLAN_EXPERIMENTAL:
                    case TechConstants.T_CLAN_UNOFFICIAL:
                    case TechConstants.T_ALL_CLAN:
                        return true;
                    default:
                        return false;
                }
        }

        return false;

    }

    public static Entity createOMG() {
        Mech entity = new BipedMech(Mech.GYRO_STANDARD, Mech.COCKPIT_STANDARD);

        entity.setYear(2075);
        entity.setTechLevel(TechConstants.T_INTRO_BOXSET);
        entity.setWeight(25);
        entity.setEngine(new Engine(325, Engine.NORMAL_ENGINE, 0));
        entity.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        entity.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);

        entity.addGyro();
        entity.addEngineCrits();
        entity.addCockpit();
        entity.addEngineSinks(entity.getEngine()
                .integralHeatSinkCapacity(false), MiscType.F_HEAT_SINK, false);

        entity.autoSetInternal();
        for (int loc = 0; loc <= Mech.LOC_LLEG; loc++) {
            entity.initializeArmor(1, loc);
            if (entity.hasRearArmor(loc)) {
                entity.initializeRearArmor(1, loc);
            }
        }

        entity.getFluff()
                .setHistory(
                        "This is an Error Unit! If you've recieved this unit in error please let someone know.");
        entity.setModel("OMG-UR-FD");
        entity.setChassis("Error");
        return entity;
    }

    /**
     * Tries to set UnitEntity from the global MekFileName
     */
    public static Entity createEntity(String fileName) {
        // MMClient.mwClientLog.clientErrLog("Filename: " + getUnitFilename());
        Entity UnitEntity = null;
        try {
            MechSummary ms = MechSummaryCache.getInstance().getMech(fileName);
            if (ms == null) {
                ms = MechSummaryCache.getInstance().getMech(fileName.trim());
                if (ms == null) {
                    MechSummary[] units = MechSummaryCache.getInstance()
                            .getAllMechs();
                    // System.err.println("unit: "+getUnitFilename());
                    for (MechSummary unit : units) {
                        // System.err.println("Source file:
                        // "+unit.getSourceFile().getName());
                        // System.err.println("Model: "+unit.getModel());
                        // System.err.println("Chassis: "+unit.getChassis());
                        // System.err.flush();
                        if (unit.getEntryName().equalsIgnoreCase(fileName)
                                || unit.getModel().trim()
                                        .equalsIgnoreCase(fileName.trim())
                                || unit.getChassis().trim()
                                        .equalsIgnoreCase(fileName.trim())) {
                            ms = unit;
                            break;
                        }
                    }
                }
            }

            UnitEntity = new MechFileParser(ms.getSourceFile(),
                    ms.getEntryName()).getEntity();
        } catch (Exception exep) {
            try {
                UnitEntity = UnitUtils.createOMG();// new
            } catch (Exception exepe) {
                MWLogger.errLog("Error unit failed to load. Exiting.");
                return null;
            }
        }
        return UnitEntity;
    }

    public static String getEntityFileName(Entity en) {
        String unitFile = "";

        MechSummary ms = MechSummaryCache.getInstance().getMech(
                en.getShortNameRaw());
        if (ms == null) {
            MechSummary[] units = MechSummaryCache.getInstance().getAllMechs();
            // System.err.println("unit: "+en.getShortNameRaw());
            for (MechSummary unit : units) {
                // System.err.println("Source file:
                // "+unit.getSourceFile().getName());
                // System.err.println("Model: "+unit.getModel());
                // System.err.println("Chassis: "+unit.getChassis());
                if (unit.getModel().trim()
                        .equalsIgnoreCase(en.getModel().trim())
                        && unit.getChassis().trim()
                                .equalsIgnoreCase(en.getChassis().trim())) {
                    return unit.getEntryName();
                }
            }

        } else {
            // System.err.println("Entry: "+ms.getEntryName()+" source:
            // "+ms.getSourceFile().getName());
            unitFile = UnitUtils.getMechSummaryFileName(ms);
        }

        return unitFile;

    }

    public static String getMechSummaryFileName(MechSummary ms) {
        String unitFile = "";

        unitFile = ms.getEntryName();
        if ((unitFile == null) || unitFile.equals("null")) {
            unitFile = ms.getSourceFile().getName();
        }

        if (unitFile.indexOf("/") > -1) {
            unitFile = unitFile.substring(unitFile.lastIndexOf("/") + 1);
        } else if (unitFile.indexOf("\\") > -1) {
            unitFile = unitFile.substring(unitFile.lastIndexOf("\\") + 1);
        }

        return unitFile;
    }

    public static boolean isCored(Entity unit) {

        if (unit instanceof Tank) {

            for (int loc = Tank.LOC_FRONT; loc < Tank.LOC_TURRET; loc++) {
                if (unit.getInternal(loc) < 0) {
                    return true;
                }
            }
        } else if (unit instanceof Mech) {
            return !UnitUtils.canStartUp(unit);
        }

        return false;
    }

    public static void destroyCockPit(Entity unit) {

        if (!(unit instanceof Mech)) {
            return;
        }

        Mech mek = (Mech) unit;
        int location = Mech.LOC_HEAD;

        if (mek.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            location = Mech.LOC_CT;
        }

        for (int y = 0; y < unit.getNumberOfCriticals(location); y++) {
            CriticalSlot cs = unit.getCritical(location, y);
            if ((cs != null) && (cs.getIndex() == Mech.SYSTEM_COCKPIT)) {
                UnitUtils.salvageCriticalSlot(cs, unit);
            }
        }
    }

    public static Crew createEntityPilot(Unit mek) {
        // get and set the options
        Crew pilot = null;
        if (mek.getPilot() == null) {
			//when looking at a pilotless mek - we need a default pilot
		    pilot = new Crew(CrewType.SINGLE, "No Pilot", 1, 4, 4, 4, 5, null, null);
		    return pilot;
		} else {
            pilot = new Crew(CrewType.SINGLE, mek.getPilot().getName(), 1, mek.getPilot()
                .getGunnery(), mek.getPilot().getPiloting());
        }
        // Hits defaults to 0 so no reason to keep checking over and over again.
        pilot.setHits(mek.getPilot().getHits(), 0);

        Iterator<MegaMekPilotOption> iter = mek.getPilot().getMegamekOptions()
                .iterator();
        while (iter.hasNext()) {
            MegaMekPilotOption po = iter.next();
            if (po.getMmname().equals("weapon_specialist")) {
                pilot.getOptions().getOption(po.getMmname())
                        .setValue(mek.getPilot().getWeapon());
            } else if (po.getMmname().equals("edge")) {
                pilot.getOptions()
                        .getOption(po.getMmname())
                        .setValue(
                                mek.getPilot().getSkills()
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
                pilot.getOptions().getOption(po.getMmname())
                        .setValue(po.isValue());
            }
        }

        return pilot;
    }

    public static boolean isClanEQ(EquipmentType eq, int year) {
        if ((eq.getTechLevel(year) == TechConstants.T_CLAN_ADVANCED)
                || (eq.getTechLevel(year) == TechConstants.T_CLAN_EXPERIMENTAL)
                || (eq.getTechLevel(year) == TechConstants.T_CLAN_TW)
                || (eq.getTechLevel(year) == TechConstants.T_CLAN_UNOFFICIAL)) {
            return true;
        }
        return false;
    }

    public static String unitBattleDamage(Entity unit, boolean sendAmmo) {
        return UnitDamageHandlerFactory.getHandler(unit).buildDamageString(
                unit, sendAmmo);
    }

    public static void applyBattleDamage(Entity unit, String report,
            boolean isRepairing) {
        UnitDamageHandlerFactory.getHandler(unit).applyDamageString(unit,
                report, isRepairing);
    }
}