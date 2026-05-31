/*
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
package mekwars.common.util.unitdamage;

import java.util.Iterator;
import java.util.StringTokenizer;

import megamek.codeUtilities.MathUtility;
import megamek.common.CriticalSlot;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekwars.common.util.UnitUtils;

public class VehicleDamageHandler extends AbstractUnitDamageHandler {
    private static final MMLogger LOGGER = MMLogger.create(VehicleDamageHandler.class);

    @Override
    public String buildDamageString(Entity unit, boolean sendAmmo) {
        StringBuilder result = new StringBuilder();
        String delimiter = "-";
        String delimiter2 = "%";
        boolean hasData = false;

        try {
            // External armor
            for (int loc = Tank.LOC_BODY; loc <= Tank.LOC_TURRET; loc++) {

                if (unit.getArmor(loc) == unit.getOArmor(loc)) {
                    continue;
                }

                hasData = true;
                result.append(loc);
                result.append(delimiter2);
                result.append(Math.max(unit.getArmor(loc), 0));
                result.append(delimiter2);

            }

            if (!hasData) {
                result.append(delimiter2);
                result.append(delimiter2);
            }

            result.append(delimiter);
            hasData = false;
            // Internal Armor
            for (int loc = Tank.LOC_BODY; loc <= Tank.LOC_TURRET; loc++) {

                if (unit.getInternal(loc) == unit.getOInternal(loc)) {
                    continue;
                }

                result.append(loc);
                result.append(delimiter2);
                result.append(Math.max(unit.getInternal(loc), 0));
                result.append(delimiter2);
                hasData = true;

            }

            if (!hasData) {
                result.append(delimiter2);
                result.append(delimiter2);
            }

            result.append(delimiter);
            hasData = false;
            // Crits report both Systems and Equipment
            // * for Damaged X for Breached
            // location#Crit Number#Damage

            for (int x = 0; x < unit.locations(); x++) {
                for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
                    CriticalSlot criticalSlot = unit.getCritical(x, y);

                    if (criticalSlot == null) {
                        continue;
                    }

                    if (UnitUtils.isNonRepairableCrit(unit, criticalSlot)) {
                        continue;
                    }

                    if (criticalSlot.isRepairing()) {
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("!");
                        result.append(delimiter2);
                        hasData = true;
                    }

                    // the criticalSlot is damaged lets see if the whole Mounted has taken enough
                    // damage to it to be labeled destroyed
                    if (criticalSlot.isDamaged() && UnitUtils.isDestroyedOrDamaged(unit, criticalSlot)) {
                        criticalSlot.setMissing(true);
                    }

                    // Missing items do not need to worry about damage or breach.
                    if (criticalSlot.isMissing()) {
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("@");
                        result.append(delimiter2);
                        hasData = true;
                    } else {
                        if (criticalSlot.isDamaged()) {
                            result.append(x);
                            result.append(delimiter2);
                            result.append(y);
                            result.append(delimiter2);
                            result.append("^");
                            result.append(delimiter2);
                            hasData = true;
                        }
                        if (criticalSlot.isBreached()) {
                            result.append(x);
                            result.append(delimiter2);
                            result.append(y);
                            result.append(delimiter2);
                            result.append("X");
                            result.append(delimiter2);
                            hasData = true;
                        }
                    }
                }
            }

            if (!hasData) {
                result.append(delimiter2);
                result.append(delimiter2);
            }

            result.append(delimiter);
            hasData = false;

            if (sendAmmo) {
                int location = 0;
                for (AmmoMounted ammoMounted : unit.getAmmo()) {
                    if (ammoMounted.isDestroyed()) {
                        hasData = true;
                        result.append(location);
                        result.append(delimiter2);
                        result.append(0);
                        result.append(delimiter2);
                    } else if (ammoMounted.getUsableShotsLeft() != UnitUtils.getShots(ammoMounted)) {
                        hasData = true;
                        result.append(location);
                        result.append(delimiter2);
                        result.append(Math.max(0, ammoMounted.getUsableShotsLeft()));
                        result.append(delimiter2);
                    }

                    location++;
                }
            }

            if (!hasData) {
                result.append(delimiter2);
                result.append(delimiter2);
            }
            result.append(delimiter);

        } catch (Exception ex) {
            LOGGER.error(ex, "Entity: {}", unit.getShortNameRaw());
            return "%%-%%-%%";
        }

        return result.toString();
    }

    @Override
    public void applyDamageString(Entity unit, String report, boolean isRepairing) {
        StringTokenizer entry = new StringTokenizer(report, "-");
        StringTokenizer externalArmor = new StringTokenizer(entry.nextToken(), "%");
        StringTokenizer internalArmor = new StringTokenizer(entry.nextToken(), "%");
        StringTokenizer crits = new StringTokenizer(entry.nextToken(), "%");
        StringTokenizer ammo = null;

        if (entry.hasMoreTokens()) {
            ammo = new StringTokenizer(entry.nextToken(), "%");
        }

        while (externalArmor.hasMoreTokens()) {
            int location = MathUtility.parseInt(externalArmor.nextToken(), 0);
            int armor = MathUtility.parseInt(externalArmor.nextToken(), 0);
            unit.setArmor(armor, location);

            if (!isRepairing) {
                UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, location);
            }
        }

        while (internalArmor.hasMoreTokens()) {
            int location = MathUtility.parseInt(internalArmor.nextToken(), 0);
            int armor = MathUtility.parseInt(internalArmor.nextToken(), 0);

            if (armor <= 0) {
                armor = IArmorState.ARMOR_DESTROYED;
            }

            unit.setInternal(armor, location);

            if (!isRepairing) {
                UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_INTERNAL_ARMOR, location);
            }
        }

        while (crits.hasMoreTokens()) {
            int location = MathUtility.parseInt(crits.nextToken(), 0);
            int slot = MathUtility.parseInt(crits.nextToken(), 0);
            String damageType = crits.nextToken();

            CriticalSlot critSlot = unit.getCritical(location, slot);

            if (damageType.equals("@")) {
                critSlot.setMissing(true);
                if (critSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted<?> mounted = critSlot.getMount();
                    // check to see if it has ammo. If so set it 0 as the
                    // ammo bin has gone bye.
                    if (mounted.getUsableShotsLeft() > 0) {
                        mounted.setShotsLeft(0);
                    }
                }
            }

            if (damageType.equals("^")) {
                critSlot.setDestroyed(true);
                if (critSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted<?> mounted = critSlot.getMount();
                    // check to see if it has ammo. If so set it 0 as the
                    // ammo bin has gone bye.
                    if (mounted.getUsableShotsLeft() > 0) {
                        mounted.setShotsLeft(0);
                    }
                }
            }

            if (damageType.equals("!")) {
                if (isRepairing) {
                    critSlot.setRepairing(true);
                    if (!critSlot.isBreached() && !critSlot.isDamaged()) {
                        critSlot.setDestroyed(true);
                        if (critSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                            Mounted<?> mounted = critSlot.getMount();
                            // check to see if it has ammo. If so set it 0 as
                            // the ammo bin has gone bye.
                            if (mounted.getUsableShotsLeft() > 0) {
                                mounted.setShotsLeft(0);
                            }
                        }
                    }
                }
            }

            if (damageType.equals("X")) {
                critSlot.setBreached(true);
            }

            unit.setCritical(location, slot, critSlot);
        }

        if ((ammo != null) && ammo.hasMoreTokens()) {
            int locationCount = 0;
            Iterator<AmmoMounted> munitions = unit.getAmmo().iterator();

            // make sure the unit actually has ammo.
            if (munitions.hasNext()) {
                Mounted<AmmoType> weapon = munitions.next();

                try {
                    while (ammo.hasMoreTokens()) {
                        int location = MathUtility.parseInt(ammo.nextToken(), 0);
                        int ammoLeft = MathUtility.parseInt(ammo.nextToken(), 0);

                        while (location != locationCount) {
                            weapon = munitions.next();
                            locationCount++;
                        }

                        weapon.setShotsLeft(ammoLeft);
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex, "Error while parsing ammo Moving along");
                }
            }
        }
    }

}
