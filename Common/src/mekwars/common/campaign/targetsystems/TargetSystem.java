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
package mekwars.common.campaign.targetsystems;

import java.util.Vector;

import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Represents a targeting computer's current mode on a MegaMek {@link Entity} (Standard, Anti-Air, or one of the
 * Improved/Poor Targeting range bands), and applies that mode to the entity by toggling the corresponding MegaMek
 * quirk options (e.g. {@code "anti_air"}, {@code "imp_target_short"}).
 * <p>
 * Each mode maps to a fixed combination of quirks turned on or off; see {@link #setTargetSystem(int)} for exactly
 * which quirks each {@code TS_TYPE_*} enables.
 */
public class TargetSystem {
    /** No special targeting bonuses/penalties. */
    public final static int TS_TYPE_STANDARD = 0;
    /** Optimized against airborne targets, at the cost of ground-target accuracy. */
    public final static int TS_TYPE_ANTIAIR = 1;
    /** Improved accuracy at short range, reduced accuracy at medium/long range. */
    public final static int TS_TYPE_SHORT = 2;
    /** Improved accuracy at medium range, reduced accuracy at short/long range. */
    public final static int TS_TYPE_MEDIUM = 3;
    /** Improved accuracy at long range, reduced accuracy at short/medium range. */
    public final static int TS_TYPE_LONG = 4;
    /** Highest valid {@code TS_TYPE_*} value; used for bounds checks and iteration. */
    public final static int TS_TYPE_MAX = 4;
    private final static MMLogger LOGGER = MMLogger.create(TargetSystem.class);

    /** The unit this targeting system is attached to; quirks are applied here. May be {@code null}. */
    private Entity entity;
    private int currentType = TS_TYPE_STANDARD;

    /**
     * Switches this targeting system to {@code type}, toggling the underlying MegaMek quirk options on
     * {@link #entity} to match. If {@link #entity} is {@code null}, the type is still recorded but no quirks are
     * changed (they'll take effect once {@link #setEntity(Entity)} is called and this method is invoked again).
     *
     * @param type one of the {@code TS_TYPE_*} constants
     *
     * @throws TargetTypeOutOfBoundsException if {@code type} is negative or greater than {@link #TS_TYPE_MAX}
     */
    public void setTargetSystem(int type) throws TargetTypeOutOfBoundsException {
        if (type < 0 || type > TS_TYPE_MAX) {
            throw new TargetTypeOutOfBoundsException(type);
        }

        if (type == TS_TYPE_ANTIAIR) {
            setTargetSystem("anti_air", true);
            setTargetSystem("imp_target_short", false);
            setTargetSystem("poor_target_short", true);
            setTargetSystem("imp_target_med", false);
            setTargetSystem("poor_target_med", false);
            setTargetSystem("imp_target_long", false);
            setTargetSystem("poor_target_long", false);
            currentType = TS_TYPE_ANTIAIR;
        } else if (type == TS_TYPE_STANDARD) {
            setTargetSystem("anti_air", false);
            setTargetSystem("imp_target_short", false);
            setTargetSystem("poor_target_short", false);
            setTargetSystem("imp_target_med", false);
            setTargetSystem("poor_target_med", false);
            setTargetSystem("imp_target_long", false);
            setTargetSystem("poor_target_long", false);
            currentType = TS_TYPE_STANDARD;
        } else if (type == TS_TYPE_SHORT) {
            setTargetSystem("anti_air", false);
            setTargetSystem("imp_target_short", true);
            setTargetSystem("poor_target_short", false);
            setTargetSystem("imp_target_med", false);
            setTargetSystem("poor_target_med", true);
            setTargetSystem("imp_target_long", false);
            setTargetSystem("poor_target_long", true);
            currentType = TS_TYPE_SHORT;
        } else if (type == TS_TYPE_MEDIUM) {
            setTargetSystem("anti_air", false);
            setTargetSystem("imp_target_short", false);
            setTargetSystem("poor_target_short", true);
            setTargetSystem("imp_target_med", true);
            setTargetSystem("poor_target_med", false);
            setTargetSystem("imp_target_long", false);
            setTargetSystem("poor_target_long", true);
            currentType = TS_TYPE_MEDIUM;
        } else {
            setTargetSystem("anti_air", false);
            setTargetSystem("imp_target_short", false);
            setTargetSystem("poor_target_short", true);
            setTargetSystem("imp_target_med", false);
            setTargetSystem("poor_target_med", true);
            setTargetSystem("imp_target_long", true);
            setTargetSystem("poor_target_long", false);
            currentType = TS_TYPE_LONG;
        }
    }

    /** Sets a single named quirk option on {@link #entity} to {@code on}/{@code off}; a no-op if entity is unset. */
    private void setTargetSystem(String type, boolean on) {
        if (entity != null) {entity.getQuirks().getOption(type).setValue(on);}
    }

    /**
     * @param name a display name as returned by {@link #getTypeName(int)} (e.g. {@code "Anti-Air"})
     *
     * @return the matching {@code TS_TYPE_*} constant, or {@link #TS_TYPE_STANDARD} if {@code name} is not
     *         recognized
     */
    public int getTypeByName(String name) {
        if (name.equalsIgnoreCase("anti-air")) {
            return TS_TYPE_ANTIAIR;
        } else if (name.equalsIgnoreCase("Standard")) {
            return TS_TYPE_STANDARD;
        } else if (name.equalsIgnoreCase("Short-Range")) {
            return TS_TYPE_SHORT;
        } else if (name.equalsIgnoreCase("Medium-Range")) {
            return TS_TYPE_MEDIUM;
        } else if (name.equalsIgnoreCase("Long-Range")) {
            return TS_TYPE_LONG;
        } else {
            return 0;
        }
    }

    /** @param e the unit this targeting system's mode should be applied to; does not itself re-apply quirks. */
    public void setEntity(Entity e) {
        entity = e;
    }

    /** @return the currently active {@code TS_TYPE_*} constant. */
    public int getCurrentType() {
        return currentType;
    }

    /** @return the display name of the currently active type, or {@code ""} if it could not be resolved. */
    public String getCurrentTypeName() {
        String name = "";
        try {
            name = getTypeName(currentType);
        } catch (TargetTypeOutOfBoundsException | TargetTypeNotImplementedException e) {
            LOGGER.error(e, "Error getting target system name");
        }
        return name;
    }

    /**
     * @param type one of the {@code TS_TYPE_*} constants
     *
     * @return the human-readable display name for {@code type}
     *
     * @throws TargetTypeOutOfBoundsException     if {@code type} is negative or greater than {@link #TS_TYPE_MAX}
     * @throws TargetTypeNotImplementedException  declared for forward-compatibility; not currently thrown by this
     *                                             implementation
     */
    public String getTypeName(int type) throws TargetTypeOutOfBoundsException, TargetTypeNotImplementedException {
        if (type < 0 || type > TS_TYPE_MAX) {
            throw new TargetTypeOutOfBoundsException(type);
        } else if (type == TS_TYPE_STANDARD) {
            return "Standard";
        } else if (type == TS_TYPE_ANTIAIR) {
            return "Anti-Air";
        } else if (type == TS_TYPE_SHORT) {
            return "Short-Range";
        } else if (type == TS_TYPE_MEDIUM) {
            return "Medium-Range";
        } else {
            return "Long-Range";
        }
    }

    /** @return the display names of every {@code TS_TYPE_*}, in ascending type order; suitable for a combo box. */
    public String[] getNameArray() {
        Vector<String> names = new Vector<>(1, 1);

        for (int i = TS_TYPE_STANDARD; i <= TS_TYPE_MAX; i++) {
            try {
                names.add(getTypeName(i));
            } catch (TargetTypeOutOfBoundsException | TargetTypeNotImplementedException e) {
                LOGGER.error(e, "Error getting name array");
            }
        }
        String[] toReturn = new String[names.size()];
        names.toArray(toReturn);
        return toReturn;
    }

    /**
     * Like {@link #getNameArray()}, but excludes any types whose {@code TS_TYPE_*} constant appears in
     * {@code bans}. Used to populate a selection UI when certain targeting systems are disallowed (e.g. by house
     * rules).
     *
     * @param bans the {@code TS_TYPE_*} constants to exclude
     *
     * @return the display names of every non-banned {@code TS_TYPE_*}, in ascending type order
     */
    public String[] getNonBannedNameArray(Vector<Integer> bans) {
        Vector<String> names = new Vector<>(1, 1);
        for (int i = TS_TYPE_STANDARD; i <= TS_TYPE_MAX; i++) {
            try {
                if (!bans.contains(i)) {
                    names.add(getTypeName(i));
                }
            } catch (TargetTypeOutOfBoundsException | TargetTypeNotImplementedException e) {
                LOGGER.error(e, "Error getting non banned name array.");
            }
        }
        String[] toReturn = new String[names.size()];
        names.toArray(toReturn);
        return toReturn;
    }
}
