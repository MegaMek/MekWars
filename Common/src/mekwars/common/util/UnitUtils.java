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

public class UnitUtils {
    // Engines
    public static final int STANDARD_ENGINE = 0;
    public static final int IS_LIGHT_ENGINE = 1;
    public static final int IS_XL_ENGINE = 2;
    public static final int IS_XXL_ENGINE = 3;
    public static final int CLAN_XL_ENGINE = 4;
    public static final int CLAN_XXL_ENGINE = 5;
    public static final String[] ENGINE_SHORT_STRING = { "Standard Engine", "Light Engine", "XL Engine", "XXL Engine",
                                                         "XL Engine", "XXL Engine" };
    public static final String[] ENGINE_TECH_STRING = { "Standard Engine", "IS Light Engine", "IS XL Engine",
                                                        "IS XXL Engine", "Clan XL Engine", "Clan XXL Engine" };
    // Locations for Advanced Repair.
    public static final int LOC_HEAD = 0;
    public static final int LOC_CENTER_TORSO = 1;
    public static final int LOC_RT = 2;
    public static final int LOC_LT = 3;
    public static final int LOC_RIGHT_ARM = 4;
    public static final int LOC_LEFT_ARM = 5;
    public static final int LOC_RIGHT_LEG = 6;
    public static final int LOC_LEFT_LEG = 7;
    public static final int LOC_CENTER_TORSOR = 8;
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
    private final static I18NMessages MESSAGES = new I18NMessages(UnitUtils.class);
    private static final MMLogger LOGGER = MMLogger.create(UnitUtils.class);

    public static boolean hasArmorDamage(Entity unit) {
        if (unit instanceof Infantry) {
            return false;
        }

        return (unit.getTotalArmor() != unit.getTotalOArmor()) || (unit.getTotalInternal() != unit.getTotalOInternal());
    }

    public static boolean hasISDamage(Entity unit) {
        if (unit instanceof Infantry) {
            return false;
        }

        return unit.getTotalInternal() != unit.getTotalOInternal();
    }

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
     * This destroys all the engine crits in the unit this means a botched salvage job or cored unit
     *
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

    public static boolean isEngineCrit(CriticalSlot criticalSlot) {
        return (criticalSlot != null) &&
                     (criticalSlot.getType() == CriticalSlot.TYPE_SYSTEM) &&
                     (criticalSlot.getIndex() == Mek.SYSTEM_ENGINE);
    }

    /**
     * Salvage the crit and its mount set them all to destroyed.
     *
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
     * Some EQ can take up multiple slots this will track them down and repair them.
     *
     */
    public static void repairEquipment(Mounted<?> equipment, Entity unit, int location) {
        if (equipment.isSplit()) {
            UnitUtils.repairSplitEquipment(equipment, unit);
            return;
        }

        setUnitCriticalSlots(equipment, unit, location);
    }

    /**
     * Repairs weapons that are split between locations Used for Meks Only.
     *
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
     * Salvage the crits.
     *
     */
    public static void salvageEquipment(Mounted<?> equipment, Entity unit, int location) {
        if (equipment.isSplit()) {
            UnitUtils.salvageSplitEquipment(equipment, unit);
            return;
        }

        setSalvageCriticalSlots(equipment, unit, location);
    }

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
     * Repairs all the engines in a unit.
     *
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
     * Fix a crit based on if it's damaged or breached. Breached flags are fixed first.
     *
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
     * Set all engine crits to repairing!
     *
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

    public static int getTechRoll(Entity unit, int location, int slot, int techType, boolean armor, int techLevel) {
        return UnitUtils.getTechRoll(unit, location, slot, techType, armor, techLevel, false);
    }

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

    public static int techBaseRoll(int techType) {
        int roll = 9;

        if (techType != TECH_GREEN) {
            roll = 8 - techType;
        }

        return roll;
    }

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
     * Repairs all the engines in a unit.
     *
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
    public static void removeRepairEquipment(Mounted<?> equipment, Entity unit, int location) {

        if (equipment.isSplit()) {
            UnitUtils.removeRepairSplitEquipment(equipment, unit);
            return;
        }

        removeRepairingCriticalSlotEquipment(equipment, unit, location);
    }

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
     * Repairs weapons that are split between locations Used for Meks Only.
     *
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

    /*
     * This method checks equipment slots for critical skits that shouldn't really
     * need repairing. i.e., endo armor slots. Non-equipment slots are
     * automatically returned as false.
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

    /*
     * Had to lift this from MM. Was a bug you could set your level 3 targeting
     * system to anything if your TC was damaged then repair your TC and get
     * double the bonus.
     */
    public static boolean hasTargetingComputer(Entity unit) {
        for (MiscMounted mounted : unit.getMisc()) {
            if ((mounted.getType() != null) && mounted.getType().hasFlag(MiscType.F_TARGETING_COMPUTER)) {
                return true;
            }
        }
        return false;
    }

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

    public static boolean isAmmoless(Entity unit) {
        return unit.getAmmo().isEmpty();
    }

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

    public static int getShots(Mounted<?> mounted) {
        if (mounted.getUsableShotsLeft() > 0) {
            return mounted.getOriginalShots();
        } else {
            return ((AmmoType) mounted.getType()).getShots();
        }
    }

    public static boolean hasEmptyAmmo(Entity unit) {
        for (AmmoMounted ammo : unit.getAmmo()) {
            if (ammo.getUsableShotsLeft() == 0) {
                return true;
            }
        }

        return false;
    }

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
     * @return <code>true</code> if destroyed <code>false</code> if damaged
     *
     * @author Torren (Jason Tighe) This wacky method is designed to find out if a CS is destoyred and needs to be
     *       replaced or damagaed and can be repaired. this is determined if more then 50% of the CS's in a Mount are
     *       damaged/missing/destroyed
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
     * Tries to set UnitEntity from the global MekFileName
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

    public static boolean isClanEQ(EquipmentType eq, int year) {
        return (eq.getTechLevel(year) == TechConstants.T_CLAN_ADVANCED)
                     || (eq.getTechLevel(year) == TechConstants.T_CLAN_EXPERIMENTAL)
                     || (eq.getTechLevel(year) == TechConstants.T_CLAN_TW)
                     || (eq.getTechLevel(year) == TechConstants.T_CLAN_UNOFFICIAL);
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
