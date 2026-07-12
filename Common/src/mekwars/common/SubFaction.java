/*
 * Copyright (C) 2007 jtighe (torren@users.sourceforge.net)
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

package mekwars.common;

import java.util.Properties;
import java.util.StringTokenizer;

import megamek.logging.MMLogger;

/**
 * Represents a sub-division ("sub-faction") of a {@link House}. Where a House is a major faction (e.g. a
 * Great House or Clan) with galaxy-wide settings, a SubFaction models a smaller unit within it (a regiment,
 * militia, mercenary command, etc.) that can have its own access level and its own restrictions on what unit
 * types/weights it may purchase new or used, plus its own minimum ELO/experience requirements.
 * <p>
 * A {@link House} keeps a name-keyed collection of its SubFactions (see {@code House#getSubFactionList()}).
 * All per-key settings are stored as free-form string properties (see {@link #factionSettings}) rather than
 * typed fields, and are serialized to/from a single delimited string via {@link #toString()} and
 * {@link #fromString(String)} for persistence.
 *
 * @author jtighe (torren)
 */
public class SubFaction {
    private static final MMLogger LOGGER = MMLogger.create(SubFaction.class);

    /**
     * Shared table of default settings (name, access level, per unit-type/weight purchase permissions,
     * minimum ELO/experience) used as the fallback {@link Properties} defaults for every SubFaction
     * instance, and rebuilt/returned by {@link #getDefault()}.
     */
    private static final Properties defaultSettings = new Properties();

    /**
     * This sub-faction's own settings, keyed by setting name (e.g. "Name", "AccessLevel",
     * "CanBuyNew&lt;weight&gt;&lt;type&gt;"). Falls back to {@link #defaultSettings} for any key not
     * explicitly overridden here (see the {@code new Properties(defaults)} constructor idiom).
     */
    private final Properties factionSettings;

    /** Database row id for this sub-faction; not currently read/written elsewhere in this class. */
    private int DBId = 0;

    /**
     * Creates a SubFaction with all settings falling back to {@link #getDefault()}.
     */
    public SubFaction() {
        factionSettings = new Properties(SubFaction.getDefault());
    }

    /**
     * (Re)builds and returns the shared default settings table: empty name, access level 0, every
     * unit type/weight combination purchasable both new and used, and minimum ELO/experience of 0.
     * <p>
     * Note: this mutates and returns the single static {@link #defaultSettings} instance on every call
     * (it is not a fresh copy), so all SubFactions share the same default-settings object.
     *
     * @return the shared default settings table.
     */
    public static Properties getDefault() {
        defaultSettings.setProperty("Name", "");
        defaultSettings.setProperty("AccessLevel", "0");

        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight <= Unit.ASSAULT; weight++) {
                String setting = String.format("CanBuyNew%s", buildUnitWeightAndTypeString(weight, type));
                defaultSettings.setProperty(setting, "true");
                setting = String.format("CanBuyUsed%s", buildUnitWeightAndTypeString(weight, type));
                defaultSettings.setProperty(setting, "true");
            }
        }

        defaultSettings.setProperty("MinELO", "0");
        defaultSettings.setProperty("MinExp", "0");

        return defaultSettings;
    }

    /**
     * Builds the setting-name suffix used for per unit-type/weight purchase flags, combining the weight
     * class description (e.g. "Light", "Assault") and the unit type description (e.g. "Mek", "Vehicle").
     *
     * @param weight the unit weight class (see {@link Unit} weight constants).
     * @param type   the unit build type (see {@link Unit} type constants).
     * @return the concatenated weight+type description string used as a settings-key suffix.
     */
    public static String buildUnitWeightAndTypeString(int weight, int type) {
        return String.format("%s%s", Unit.getWeightClassDesc(weight), Unit.getTypeClassDesc(type));
    }

    /**
     * Creates a named SubFaction with all other settings falling back to {@link #getDefault()}.
     *
     * @param name the sub-faction's display name.
     */
    public SubFaction(String name) {
        factionSettings = new Properties(SubFaction.getDefault());
        factionSettings.setProperty("Name", name);
    }

    /**
     * Creates a named SubFaction with an explicit access level; all other settings fall back to
     * {@link #getDefault()}.
     *
     * @param name        the sub-faction's display name.
     * @param accessLevel the access level, as a string (numeric permission tier).
     */
    public SubFaction(String name, String accessLevel) {
        factionSettings = new Properties(SubFaction.getDefault());
        factionSettings.setProperty("Name", name);
        factionSettings.setProperty("AccessLevel", accessLevel);
    }

    /**
     * Looks up a setting by key, falling back first to this instance's {@link Properties} defaults chain,
     * then to the static {@link #getDefault()} table, and finally logging an error and returning
     * {@code "-1"} if the key is unknown anywhere.
     *
     * @param key the setting name to look up.
     * @return the setting's string value, or {@code "-1"} if the key does not exist at all.
     */
    public String getConfig(String key) {

        if (!factionSettings.containsKey(key)) {

            if (SubFaction.getDefault().containsKey(key)) {
                return SubFaction.getDefault().getProperty(key);
            }

            LOGGER.error("Unable to find subfaction config: {}", key);
            return "-1";
        }

        return factionSettings.getProperty(key);
    }

    /**
     * Serializes all of this sub-faction's own settings (not including inherited defaults) into a single
     * {@code "#"}-delimited "key#value#key#value#..." string, suitable for storage and later
     * reconstruction via {@link #fromString(String)}.
     *
     * @return the encoded settings string, or {@code "# #"} if there are no settings.
     */
    public String toString() {
        StringBuilder result = new StringBuilder();

        if (factionSettings.isEmpty()) {
            return "# #";
        }

        for (Object key : factionSettings.keySet()) {
            result.append(key.toString());
            result.append("#");
            result.append(factionSettings.getProperty(key.toString()));
            result.append("#");
        }

        return result.toString();
    }

    /**
     * Parses a {@code "#"}-delimited "key#value#key#value#..." string (as produced by {@link #toString()})
     * and applies each key/value pair via {@link #setConfig(String, String)}.
     * <p>
     * If the string has a trailing key with no matching value, that dangling key is silently ignored and
     * parsing stops (the method returns early rather than throwing).
     *
     * @param settings the encoded settings string to parse.
     */
    public void fromString(String settings) {
        StringTokenizer propertyList = new StringTokenizer(settings, "#");

        while (propertyList.hasMoreElements()) {

            String key = propertyList.nextToken();

            if (!propertyList.hasMoreElements()) {
                return;
            }

            String value = propertyList.nextToken();
            setConfig(key, value);
        }
    }

    /**
     * Sets (or overrides) a single named setting for this sub-faction.
     *
     * @param key   the setting name.
     * @param value the setting value.
     */
    public void setConfig(String key, String value) {
        factionSettings.setProperty(key, value);
    }

    /**
     * @return this sub-faction's display name (the "Name" setting).
     */
    public String getName() {
        return factionSettings.getProperty("Name");
    }
}
