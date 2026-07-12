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

/**
 * {@link AbstractUnitDamageHandler} strategy for combat vehicles ({@link Tank} and its
 * subtypes), covering the {@link Tank#LOC_BODY} through {@link Tank#LOC_TURRET} location range.
 * <p>
 * Structurally this mirrors {@link MekDamageHandler}'s four-section, {@code "-"}-delimited
 * damage string format (external armor, internal structure, critical slots, optional ammo -
 * see {@link MekDamageHandler} for the general format description), but is simpler in several
 * ways that reflect real differences between vehicles and Meks:
 * <ul>
 * <li><b>No front/rear armor split.</b> Unlike Meks (whose torso locations have independent
 * front and rear armor facings requiring separate encoding), vehicle armor here is read/written
 * with a single {@code getArmor(loc)}/{@code setArmor(armor, location)} call per location, with
 * no rear-facing special case and no location-code ambiguity bug analogous to the one noted in
 * {@link MekDamageHandler#applyDamageString}.</li>
 * <li><b>No shield-equipment capacity accounting.</b> Vehicles don't mount the multi-hit
 * "shield" equipment Meks can, so the critical-slot loop has no equivalent of
 * {@link MekDamageHandler}'s shield-capacity-to-marker conversion, and no {@code hasISLeft}
 * suppression of the missing marker.</li>
 * <li><b>Ammo shot totals</b> are looked up via {@link UnitUtils#getShots}, a shared helper,
 * rather than the inline original/shots fallback logic duplicated in
 * {@link MekDamageHandler}.</li>
 * </ul>
 *
 * @see UnitDamageHandlerFactory#getHandler(Entity)
 * @see MekDamageHandler
 */
public class VehicleDamageHandler extends AbstractUnitDamageHandler {
    /** Logger used to record unexpected failures while building/applying a damage string. */
    private static final MMLogger LOGGER = MMLogger.create(VehicleDamageHandler.class);

    /**
     * Builds the four-section, {@code "-"}-delimited damage string for {@code unit}: changed
     * external armor, changed internal structure, critical slot status, and optionally ammo
     * bin counts. See the class Javadoc and {@link MekDamageHandler} for the general format;
     * unlike Meks, vehicle armor has no front/rear split so each location is encoded once.
     * <p>
     * Any exception raised while walking the unit's locations/criticals/ammo is caught, logged,
     * and converted into the sentinel empty-sections string {@code "%%-%%-%%"} so a single
     * malformed unit cannot fail a batch report.
     *
     * @param unit     the vehicle whose current damage state should be captured
     * @param sendAmmo whether a fourth, ammo-bin section should be appended
     * @return the encoded damage string, or {@code "%%-%%-%%"} if an error occurred while
     *         building it
     */
    @Override
    public String buildDamageString(Entity unit, boolean sendAmmo) {
        StringBuilder result = new StringBuilder();
        String delimiter = "-";
        String delimiter2 = "%";
        boolean hasData = false;

        try {
            // External armor: emit location%points% for every location (LOC_BODY..LOC_TURRET)
            // whose current armor differs from the vehicle's original ("O") armor allocation.
            // Vehicles have no separate front/rear armor facing to account for here, unlike
            // Mek torso locations - a single getArmor(loc) call covers the whole location.
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
                // No armor changes: emit an empty "%%" placeholder so this section is still
                // present and applyDamageString's positional parsing stays in sync.
                result.append(delimiter2);
                result.append(delimiter2);
            }

            result.append(delimiter);
            hasData = false;
            // Internal Armor (internal structure): emit location%points% for every location
            // whose current internal structure differs from its original value.
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
            // (markers actually emitted below: "!" repairing, "@" missing, "^" damaged,
            // "X" breached)

            // Walk every location and every critical slot in that location, looking for
            // slots that are flagged for repair, missing, damaged, or breached. Unlike
            // MekDamageHandler, there is no shield-equipment capacity accounting here since
            // vehicles do not mount that equipment type.
            for (int x = 0; x < unit.locations(); x++) {
                for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
                    CriticalSlot criticalSlot = unit.getCritical(x, y);

                    if (criticalSlot == null) {
                        continue;
                    }

                    if (UnitUtils.isNonRepairableCrit(unit, criticalSlot)) {
                        // Skip crit slots that represent non-repairable structural/armor tech
                        // rather than actual equipment.
                        continue;
                    }

                    if (criticalSlot.isRepairing()) {
                        // "!" marker: slot is currently queued/flagged for repair. This does
                        // not prevent the missing/damaged/breached checks below from also
                        // firing for the same slot, so more than one marker triple can be
                        // emitted for the same (x, y) pair.
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
                    // "@" marker: slot/equipment missing - checked first and, unlike
                    // MekDamageHandler, unconditionally (no internal-structure-remaining
                    // suppression) since a missing crit and a damaged/breached crit are
                    // treated as mutually exclusive states for vehicles.
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
                            // "^" marker: damaged/destroyed slot.
                            result.append(x);
                            result.append(delimiter2);
                            result.append(y);
                            result.append(delimiter2);
                            result.append("^");
                            result.append(delimiter2);
                            hasData = true;
                        }
                        if (criticalSlot.isBreached()) {
                            // "X" marker: breached slot.
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
                // "location" here is not a vehicle location - it's a sequential index over
                // this unit's ammo bins in iteration order (see unit.getAmmo()), used purely
                // to correlate bins between buildDamageString and applyDamageString.
                int location = 0;
                for (AmmoMounted ammoMounted : unit.getAmmo()) {
                    if (ammoMounted.isDestroyed()) {
                        hasData = true;
                        result.append(location);
                        result.append(delimiter2);
                        result.append(0);
                        result.append(delimiter2);
                    } else if (ammoMounted.getUsableShotsLeft() != UnitUtils.getShots(ammoMounted)) {
                        // UnitUtils.getShots() returns the bin's original shot count if usable
                        // shots remain, otherwise falls back to the ammo type's default shots -
                        // equivalent to the shots-fallback logic MekDamageHandler inlines.
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

    /**
     * Parses a four-section damage string produced by {@link #buildDamageString} (external
     * armor, internal structure, critical slots, and optional ammo) using nested
     * {@link StringTokenizer}s split first on {@code "-"} then on {@code "%"}, and replays the
     * encoded state onto {@code unit}.
     * <p>
     * Unlike {@link MekDamageHandler#applyDamageString}, external armor here is applied
     * unconditionally as front armor via a single {@code setArmor(armor, location)} call - no
     * front/rear branching or location-code arithmetic is needed since vehicle armor has no
     * rear-facing concept in this encoding.
     * <p>
     * Critical slot markers are applied as: {@code "@"} sets the slot (and its mounted
     * equipment, if any) missing, zeroing any remaining ammo shots on that equipment;
     * {@code "^"} sets the slot/equipment destroyed, likewise zeroing ammo; {@code "!"}, only
     * when {@code isRepairing} is {@code true}, flags the slot as under repair, and - if it
     * isn't already breached or damaged - also marks it destroyed (representing "sent to the
     * repair bay") while zeroing ammo; {@code "X"} marks the slot breached (equipment is not
     * separately marked breached here, unlike {@link MekDamageHandler}). Unlike
     * {@link MekDamageHandler}, exceptions while decoding an individual crit entry are not
     * caught locally - a malformed crit token will propagate out of this method. Ammo bin
     * entries are matched to the unit's current ammo iteration order positionally (advancing
     * the iterator until the bin index lines up), so this assumes the unit's ammo bin
     * order/count hasn't changed since the string was built.
     *
     * @param unit        the vehicle to mutate in place
     * @param report      the delimited damage string to parse, as produced by
     *                    {@link #buildDamageString}
     * @param isRepairing {@code true} when applying this report as a repair-bay action (affects
     *                    only the {@code "!"} / repairing marker handling above); {@code false}
     *                    when simply restoring/recording damage, in which case any outstanding
     *                    armor-repair queue entries for touched locations are also cleared via
     *                    {@link UnitUtils#removeArmorRepair}
     */
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

        // Always applied as front armor - vehicles have no rear-armor encoding in this format.
        while (externalArmor.hasMoreTokens()) {
            int location = MathUtility.parseInt(externalArmor.nextToken(), 0);
            int armor = MathUtility.parseInt(externalArmor.nextToken(), 0);
            unit.setArmor(armor, location);

            if (!isRepairing) {
                UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, location);
            }
        }

        // Internal structure is applied directly by location. A parsed value of 0 or less is
        // normalized to the IArmorState.ARMOR_DESTROYED sentinel rather than left as a raw
        // non-positive number.
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

        // Each iteration consumes one location%slot%marker% triple emitted by
        // buildDamageString's critical-slot loop. Note this loop, unlike
        // MekDamageHandler.applyDamageString's equivalent, is not wrapped in a try/catch, so a
        // malformed or out-of-range token here will throw out of applyDamageString entirely.
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
            // locationCount tracks the sequential ammo-bin index as the iterator advances, to
            // line up with the "location" (really just a bin index, not a body location)
            // encoded by buildDamageString. This only works if the unit's ammo bin order/count
            // matches what it was when the string was built, and if bin indices in the report
            // appear in non-decreasing order (the iterator only moves forward).
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
