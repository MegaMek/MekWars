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
package mekwars.common.flags;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import megamek.logging.MMLogger;

/**
 * A {@link FlagSet} for battle-results-related flags (e.g. house rules that apply when scoring a fight), where
 * each flag additionally records whether it applies to the attacker, the defender, or both.
 */
public class ResultsFlags extends PlayerFlags {
    /** Bit added to a flag's "applies to" value in {@link #flagsApplyTo} when it affects the attacking side. */
    public static final int APPLIES_TO_ATTACKER = 1;
    /** Bit added to a flag's "applies to" value in {@link #flagsApplyTo} when it affects the defending side. */
    public static final int APPLIES_TO_DEFENDER = 2;
    private static final MMLogger LOGGER = MMLogger.create(ResultsFlags.class);

    /** For each flag's integer key, which side(s) it applies to: sum of {@link #APPLIES_TO_ATTACKER}/{@code _DEFENDER}. */
    private final Map<Integer, Integer> flagsApplyTo;

    /** Creates an empty results flag set. */
    public ResultsFlags() {
        super();
        flagsApplyTo = new TreeMap<>();
        flagType = FLAG_TYPE_RESULTS;
    }

    /**
     * Adds a flag to the list, additionally recording which side(s) of a battle it applies to.
     *
     * @param name              the flag's display name
     * @param id                the flag's integer key
     * @param value             the flag's initial value
     * @param appliesToAttacker whether this flag affects the attacking side
     * @param appliesToDefender whether this flag affects the defending side
     */
    public void addFlag(String name, int id, boolean value, boolean appliesToAttacker, boolean appliesToDefender) {
        setFlagName(id, name);
        setFlag(name, value);
        int appliesTo = 0;

        if (appliesToAttacker) {
            appliesTo += ResultsFlags.APPLIES_TO_ATTACKER;
        }

        if (appliesToDefender) {
            appliesTo += ResultsFlags.APPLIES_TO_DEFENDER;
        }

        flagsApplyTo.put(id, appliesTo);
        LOGGER.debug("Setting flag {}(id: {}) to value {}", name, id, value);
    }

    /**
     * @param name the flag's display name
     *
     * @return {@code true} if the {@link #APPLIES_TO_DEFENDER} bit is set for this flag, {@code false} if the flag
     *         doesn't exist or doesn't apply to the defender
     */
    public boolean flagAppliesToDefender(String name) {
        int id = getFlagKey(name);

        if (id == -1) {
            // invalid name
            return false;
        }

        int appliesTo = flagsApplyTo.get(id);

        return appliesTo > 1;
    }

    /**
     * @param name the flag's display name
     *
     * @return {@code true} if the {@link #APPLIES_TO_ATTACKER} bit is set for this flag, {@code false} if the flag
     *         doesn't exist or doesn't apply to the attacker
     */
    public boolean flagAppliesToAttacker(String name) {
        int id = getFlagKey(name);

        if (id == -1) {
            // invalid name
            return false;
        }

        int appliesTo = flagsApplyTo.get(id);

        return (appliesTo % 2) == 1;
    }

    /**
     * Loads personally set flags from a string.  This should only be called after defaults are set, as any flags that
     * are listed in this string that do not already exist due to defaults will be ignored.  This way, old flags that
     * may have been deleted by the admins will not continue to hang around but will be pruned every time a player
     * loads.
     *
     */
    public void loadPersonal(String data) {
        if (data.equalsIgnoreCase(" ")) {
            return;
        }

        System.out.println(data);
        StringTokenizer stringTokenizer = new StringTokenizer(data, "$");
        while (stringTokenizer.hasMoreTokens()) {
            String element = stringTokenizer.nextToken();
            StringTokenizer elementToken = new StringTokenizer(element, "#");
            String name = elementToken.nextToken();
            elementToken.nextToken();  // This isn't needed but is included in the export (flag id).  Ignore it.
            boolean value = Boolean.parseBoolean(elementToken.nextToken());

            if (getFlagKey(name) >= 0) {
                setFlag(name, value);
            }

            if (elementToken.hasMoreTokens()) {
                // Using the newer ResultsFlags, rather than the older PlayerFlags
                elementToken.nextToken();
            }

        }
    }

    /**
     * Sets a named flag to true or false
     *
     */
    public void setFlag(String name, boolean value) {
        int flag = getFlagKey(name);

        if (flag != -1) {
            flags.set(flag, value);
        } else {
            LOGGER.error("Unknown Flag checked: {}", name);
        }
    }

    /**
     * Clears a single flag, removing it from the names and flags
     *
     */
    public void clearFlag(String name) {
        int id = getFlagKey(name);

        if (id == -1) {
            // invalid name
            return;
        }

        flagNames.remove(id);
        flags.clear(id);
        flagsApplyTo.remove(id);
    }

    /**
     * Builds the string that is imported by load (String data) above Used server-side only, as I envision it, so I
     * might move this method to SPlayer
     *
     * @return String flag settings - name, ID, and value
     */
    public String export() {
        StringBuilder toReturn = new StringBuilder();

        if (flagNames.isEmpty()) {
            return "";
        }

        toReturn.append(this.flagType).append("$");

        for (int key : flagNames.keySet()) {
            String name = flagNames.get(key);
            String isTrue = Boolean.toString(flags.get(key));
            String appliesTo = Integer.toString(flagsApplyTo.get(key));
            toReturn.append(name)
                  .append("#")
                  .append(key)
                  .append("#")
                  .append(isTrue)
                  .append("#")
                  .append(appliesTo)
                  .append("$");
        }

        return toReturn.toString();
    }

}
