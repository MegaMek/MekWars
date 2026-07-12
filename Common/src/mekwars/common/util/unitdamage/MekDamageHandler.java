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
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;
import mekwars.common.util.UnitUtils;

/**
 * {@link AbstractUnitDamageHandler} strategy for full-size BattleMeks ({@link Mek} and its
 * subtypes), the most fully-featured and complex handler in this package since Meks have the
 * richest damage model MekWars tracks: per-location front/rear armor, per-location internal
 * structure, per-critical-slot equipment/system status (including special handling for
 * multi-point "shield" equipment), and ammo bin quantities.
 * <p>
 * The damage string produced by {@link #buildDamageString} and consumed by
 * {@link #applyDamageString} consists of four sections separated by {@code "-"}, in this
 * fixed order:
 * <ol>
 * <li><b>External (front/rear) armor</b> - repeated {@code location%points%} pairs for every
 * location whose current armor differs from its original ("O") armor. Front armor for every
 * Mek location ({@link Mek#LOC_HEAD} through {@link Mek#LOC_LEFT_LEG}) is emitted using the raw
 * Mek location index; rear torso armor is emitted separately using the
 * {@link UnitUtils#LOC_CENTER_TORSO}/{@link UnitUtils#LOC_LTR}/{@link UnitUtils#LOC_RTR} pseudo
 * location codes intended to distinguish it from front armor on decode (see the implementation
 * note on {@link #applyDamageString} - the center-torso rear case appears to reuse the same
 * numeric code as center-torso front armor, which looks like a location-encoding bug).</li>
 * <li><b>Internal structure</b> - repeated {@code location%points%} pairs for every location
 * whose current internal structure differs from its original value.</li>
 * <li><b>Critical slots</b> - repeated {@code location%slot%marker%} triples, where marker is
 * one of {@code !} (flagged for repair), {@code ^} (damaged/destroyed), {@code @} (missing,
 * e.g. blown off), or {@code X} (breached).</li>
 * <li><b>Ammo</b> (only present when {@code sendAmmo} is {@code true}) - repeated
 * {@code binIndex%shotsRemaining%} pairs, in ammo-bin iteration order, for every ammo bin whose
 * remaining shots differ from a full load (or that has been destroyed, recorded as 0 shots).</li>
 * </ol>
 * Any section with no changes to report is encoded as an empty {@code %%} placeholder so the
 * fixed section count/order is preserved for {@link #applyDamageString} to parse positionally.
 *
 * @see UnitDamageHandlerFactory#getHandler(Entity)
 * @see VehicleDamageHandler
 */
public class MekDamageHandler extends AbstractUnitDamageHandler {
    /** Logger used to record unexpected failures while building/applying a damage string. */
    private final static MMLogger LOGGER = MMLogger.create(MekDamageHandler.class);

    /**
     * Builds the four-section, {@code "-"}-delimited damage string described in the class
     * Javadoc for {@code unit}: changed external armor, changed internal structure, critical
     * slot status (including special multi-hit accounting for shield equipment), and
     * optionally ammo bin counts.
     * <p>
     * Any exception raised while walking the unit's locations/criticals/ammo is caught, logged,
     * and converted into the sentinel empty-sections string {@code "%%-%%-%%"} so a single
     * malformed unit cannot fail a batch report.
     *
     * @param unit     the Mek whose current damage state should be captured
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
            // External armor: emit location%points% for every location (front armor, all
            // locations including torso front) whose current armor point total differs from
            // the unit's original ("O") armor allocation. Location codes here are the raw Mek
            // location indices (LOC_HEAD=0 .. LOC_LEFT_LEG=7).
            for (int loc = Mek.LOC_HEAD; loc <= Mek.LOC_LEFT_LEG; loc++) {

                if (unit.getArmor(loc) == unit.getOArmor(loc)) {
                    continue;
                }

                hasData = true;
                result.append(loc);
                result.append(delimiter2);
                result.append(Math.max(unit.getArmor(loc), 0));
                result.append(delimiter2);
            }

            // Rear torso armor is reported separately from the front-armor loop above (Mek's
            // getArmor(loc)/getOArmor(loc) without the rear flag only covers front-facing
            // armor). NOTE: UnitUtils.LOC_CENTER_TORSO (1) is the same numeric value as
            // Mek.LOC_CENTER_TORSO, i.e. the code used to tag CT *front* armor a few lines
            // above - this looks like it should instead be UnitUtils.LOC_CENTER_TORSOR (8),
            // mirroring how LOC_RTR/LOC_LTR are front-index + 7. As written, CT rear-armor
            // entries are indistinguishable from CT front-armor entries once encoded, and (see
            // applyDamageString) the decode-side threshold check derives from this same
            // constant, so this apparent bug affects both directions consistently.
            if (unit.getArmor(Mek.LOC_CENTER_TORSO, true) != unit.getOArmor(Mek.LOC_CENTER_TORSO, true)) {
                result.append(UnitUtils.LOC_CENTER_TORSO);
                result.append(delimiter2);
                result.append(Math.max(unit.getArmor(Mek.LOC_CENTER_TORSO, true), 0));
                result.append(delimiter2);
                hasData = true;
            }

            if (unit.getArmor(Mek.LOC_LEFT_TORSO, true) != unit.getOArmor(Mek.LOC_LEFT_TORSO, true)) {
                result.append(UnitUtils.LOC_LTR);
                result.append(delimiter2);
                result.append(Math.max(unit.getArmor(Mek.LOC_LEFT_TORSO, true), 0));
                result.append(delimiter2);
                hasData = true;
            }

            if (unit.getArmor(Mek.LOC_RIGHT_TORSO, true) != unit.getOArmor(Mek.LOC_RIGHT_TORSO, true)) {
                result.append(UnitUtils.LOC_RTR);
                result.append(delimiter2);
                result.append(Math.max(unit.getArmor(Mek.LOC_RIGHT_TORSO, true), 0));
                result.append(delimiter2);
                hasData = true;
            }

            if (!hasData) {
                // No armor changes at all: emit an empty "%%" placeholder so this section is
                // still present and applyDamageString's positional StringTokenizer parsing
                // stays in sync with the other three sections.
                result.append(delimiter2);
                result.append(delimiter2);
            }

            result.append(delimiter);
            hasData = false;
            // Internal Armor (i.e. internal structure): emit location%points% for every
            // location whose current internal structure differs from its original value.
            // Internal structure has no front/rear facing, so unlike external armor above this
            // is a single straightforward per-location loop with no rear special-casing.
            for (int loc = Mek.LOC_HEAD; loc <= Mek.LOC_LEFT_LEG; loc++) {
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
            // (in practice the markers actually emitted below are "!" repairing, "^" damaged,
            // "@" missing, and "X" breached - see the per-marker comments further down)
            // Walk every location and every critical slot in that location, looking for
            // slots that are flagged for repair, damaged, missing, or breached.
            for (int x = 0; x < unit.locations(); x++) {
                // For shield equipment (see below), tracks how many of the shield's crit
                // slots still need a "^" (damaged) marker emitted for this location; -1 means
                // "not yet computed for this location's shield, if any".
                int shieldHitsLeft = -1;
                // Need to get IS amount - critical slots marked as missing because
                // of a blown-off arm or what have you should not be marked missing for
                // MW purposes.  If it's missing, and there is IS left, it should
                // be unmarked instead.
                // hasISLeft: true when this location still has internal structure remaining.
                // Used below to suppress the "@" (missing) marker for a crit slot that
                // MegaMek reports as missing but whose location hasn't actually been severed
                // (its internal structure is still intact) - MekWars only wants "@" to mean
                // "gone" in a repair-actionable sense.
                boolean hasISLeft = (unit.getInternal(x) > 0);

                for (int y = 0; y < unit.getNumberOfCriticalSlots(x); y++) {
                    CriticalSlot criticalSlot = unit.getCritical(x, y);

                    if (criticalSlot == null) {
                        continue;
                    }

                    if (UnitUtils.isNonRepairableCrit(unit, criticalSlot)) {
                        // Skip crit slots that represent non-repairable structural/armor tech
                        // (e.g. Endo-Steel, Ferro-Fibrous, TSM) rather than actual equipment.
                        continue;
                    }

                    // Shield equipment (arms only) absorbs damage into a multi-point "capacity"
                    // pool instead of the usual single damaged/destroyed crit flag, so its
                    // remaining capacity has to be converted into an equivalent number of
                    // "damaged" crit-slot markers for the damage string. This block computes
                    // that count once per shield (guarded by shieldHitsLeft == -1) by scaling
                    // the shield's total critical slot count by the fraction of damage
                    // capacity already lost.
                    Mounted<?> mounted = criticalSlot.getMount();
                    if ((mounted instanceof MiscMounted miscMounted) &&
                              (mounted.getType() instanceof MiscType miscType) &&
                              miscType.isShield() &&
                              (miscType.getBaseDamageCapacity() != miscMounted.getCurrentDamageCapacity(unit, x)) &&
                              (shieldHitsLeft == -1) &&
                              ((x == Mek.LOC_LEFT_ARM) || (x == Mek.LOC_RIGHT_ARM))) {
                        float shieldCrits = Math.max(1, UnitUtils.getNumberOfCrits(unit, criticalSlot));
                        float basePoints = miscType.getBaseDamageCapacity();
                        float currentPoints = miscMounted.getCurrentDamageCapacity(unit, x);
                        float tempHits;

                        tempHits = shieldCrits / basePoints;
                        tempHits *= currentPoints;

                        tempHits = Math.abs(tempHits - shieldCrits);

                        shieldHitsLeft = Math.max(1, Math.round(tempHits));
                    }

                    if (criticalSlot.isRepairing()) {
                        // "!" marker: this slot is currently queued/flagged for repair. Note
                        // this does not "continue"/exit early - a slot can also independently
                        // be reported as damaged/missing/breached below, meaning more than one
                        // marker triple can be emitted for the same (x, y) location/slot pair.
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("!");
                        result.append(delimiter2);
                        hasData = true;
                    }

                    // the criticalSlot is damaged lets see if the whole Mounted has taken enough damage to it to be
                    // labeled destroyed
                    if (criticalSlot.isDamaged() && UnitUtils.isDestroyedOrDamaged(unit, criticalSlot)) {
                        criticalSlot.setMissing(true);
                    }

                    // Crit can only be missing or damaged or Breached.
                    // "^" marker: damaged/destroyed slot. Checked first because, per the
                    // comment below, MegaMek's isMissing() can otherwise take priority over a
                    // slot that should really be reported as damaged.
                    if (criticalSlot.isDamaged()) { // Moving this to the head of the line - isMissing seems to be overriding damaged.
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("^");
                        result.append(delimiter2);
                        hasData = true;
                    } else if (criticalSlot.isMissing() && !hasISLeft) {  // Experimental addition of hasISLeft - if
                        // testing doesn't work, remove it
                        // "@" marker: slot/equipment is missing (e.g. limb blown off), and
                        // only reported as such when the location has no internal structure
                        // left (see hasISLeft above).
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("@");
                        result.append(delimiter2);
                        hasData = true;
                    } else if (criticalSlot.isBreached()) {
                        // "X" marker: slot is breached (e.g. hostile environment breach),
                        // distinct from ordinary combat damage.
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("X");
                        result.append(delimiter2);
                        hasData = true;
                    } else if ((mounted != null) &&
                                     (mounted.getType() instanceof MiscType) &&
                                     ((MiscType) mounted.getType()).isShield() &&
                                     ((x == Mek.LOC_LEFT_ARM) || (x == Mek.LOC_RIGHT_ARM)) &&
                                     (shieldHitsLeft > 0)) {
                        // Emit a synthetic "^" (damaged) marker for one of the shield's crit
                        // slots to represent accumulated capacity damage computed above, since
                        // shields don't set isDamaged() the normal way. One marker is emitted
                        // per remaining shieldHitsLeft, decrementing as we go so each crit slot
                        // of the shield gets at most one synthetic marker.
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("^");
                        result.append(delimiter2);
                        hasData = true;
                        shieldHitsLeft--;
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
                // "location" here is not a Mek body location - it's a sequential index over
                // this unit's ammo bins in iteration order (see unit.getAmmo()), used purely
                // to correlate bins between buildDamageString and applyDamageString.
                int location = 0;
                for (AmmoMounted ammoMounted : unit.getAmmo()) {
                    int shots;

                    if (ammoMounted.getUsableShotsLeft() > 0) {
                        shots = ammoMounted.getOriginalShots();
                    } else {
                        shots = ammoMounted.getType().getShots();
                    }

                    if (ammoMounted.isDestroyed()) {
                        hasData = true;
                        result.append(location);
                        result.append(delimiter2);
                        result.append(0);
                        result.append(delimiter2);
                    } else if (ammoMounted.getUsableShotsLeft() != shots) {
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
     * <b>Implementation note / possible bug:</b> the external-armor section decides whether an
     * entry is rear-torso armor via {@code location >= UnitUtils.LOC_CENTER_TORSO}. Since
     * {@code UnitUtils.LOC_CENTER_TORSO == Mek.LOC_CENTER_TORSO == 1}, this threshold is true
     * for every location code except {@code Mek.LOC_HEAD} (0) - i.e. it also catches CT/RT/LT
     * front-torso and arm/leg front-armor entries (codes 1-7), routing them into the
     * {@code location - 7} rear-armor branch instead of the front-armor branch, which yields
     * negative or otherwise incorrect location indices for everything except head armor. A
     * threshold of {@code UnitUtils.LOC_CENTER_TORSOR} (8) - the unused constant that mirrors
     * how {@code LOC_RTR}/{@code LOC_LTR} are defined as front-index + 7 - looks like what was
     * intended. This is documented as observed behavior only; it has not been changed here.
     * <p>
     * Critical slot markers are applied as: {@code "@"} sets the slot (and its mounted
     * equipment, if any) missing, and zeroes any remaining ammo shots on that equipment;
     * {@code "^"} sets the slot/equipment destroyed, likewise zeroing ammo; {@code "!"}, only
     * when {@code isRepairing} is {@code true}, flags the slot as under repair, and - if it
     * isn't already breached or damaged - also marks it destroyed (representing "sent to the
     * repair bay") while zeroing ammo; {@code "X"} marks the slot/equipment breached. Any
     * exception while decoding an individual crit entry is logged and swallowed so the loop
     * continues with the remaining entries. Ammo bin entries are matched to the unit's current
     * ammo iteration order positionally (advancing the iterator until the bin index lines up),
     * so this assumes the unit's ammo bin order/count hasn't changed since the string was built.
     *
     * @param unit        the Mek to mutate in place
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

        while (externalArmor.hasMoreTokens()) {
            int location = MathUtility.parseInt(externalArmor.nextToken(), 0);
            int armor = MathUtility.parseInt(externalArmor.nextToken(), 0);

            // See the "possible bug" note in this method's Javadoc: this threshold check
            // catches far more than just rear-torso entries.
            if (location >= UnitUtils.LOC_CENTER_TORSO) {
                unit.setArmor(armor, location - 7, true);

                if (!isRepairing) {
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_REAR_ARMOR, location - 7);
                }
            } else {
                unit.setArmor(armor, location);

                if (!isRepairing) {
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, location);
                }
            }
        }

        // Internal structure has no front/rear distinction, so it is applied directly by
        // location with no branching. A parsed value of 0 or less is normalized to the
        // IArmorState.ARMOR_DESTROYED sentinel rather than left as a raw non-positive number.
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
        // buildDamageString's critical-slot loop.
        while (crits.hasMoreTokens()) {
            try {
                int location = MathUtility.parseInt(crits.nextToken(), 0);
                int slot = MathUtility.parseInt(crits.nextToken(), 0);
                String damageType = crits.nextToken();

                CriticalSlot critSlot = unit.getCritical(location, slot);

                if (damageType.equals("@")) {
                    critSlot.setMissing(true);
                    if (critSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted<?> mounted = critSlot.getMount();
                        mounted.setMissing(true);
                        // check to see if it has ammo. If so set it 0 as the
                        // ammo bin has gone bye bye.
                        if (mounted.getUsableShotsLeft() > 0) {
                            mounted.setShotsLeft(0);
                        }
                    }
                }
                if (damageType.equals("^")) {
                    critSlot.setDestroyed(true);
                    if (critSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted<?> mounted = critSlot.getMount();
                        mounted.setDestroyed(true);
                        // check to see if it has ammo. If so set it 0 as the
                        // ammobin has gone bye.
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
                                mounted.setDestroyed(true);
                                // check to see if it has ammo. If so set it 0
                                // as the ammo bin has gone bye.
                                if (mounted.getUsableShotsLeft() > 0) {
                                    mounted.setShotsLeft(0);
                                }
                            }
                        }
                    }
                }
                if (damageType.equals("X")) {
                    critSlot.setBreached(true);
                    if (critSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted<?> mounted = critSlot.getMount();
                        mounted.setBreached(true);
                    }
                }
                unit.setCritical(location, slot, critSlot);
            } catch (Exception ex) {
                LOGGER.error(ex, "ApplyDamageString error: {}", ex.getLocalizedMessage());
            }
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
