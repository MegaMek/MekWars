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

public class SubFaction {
    private static final MMLogger LOGGER = MMLogger.create(SubFaction.class);
    private static final Properties defaultSettings = new Properties();
    private final Properties factionSettings;
    public int DBId = 0;

    public SubFaction() {
        factionSettings = new Properties(SubFaction.getDefault());
    }

    public SubFaction(String name) {
        factionSettings = new Properties(SubFaction.getDefault());
        factionSettings.setProperty("Name", name);
    }

    public SubFaction(String name, String accessLevel) {
        factionSettings = new Properties(SubFaction.getDefault());
        factionSettings.setProperty("Name", name);
        factionSettings.setProperty("AccessLevel", accessLevel);
    }

    public static Properties getDefault() {
        defaultSettings.setProperty("Name", "");
        defaultSettings.setProperty("AccessLevel", "0");

        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight <= Unit.ASSAULT; weight++) {
                String setting = STR."CanBuyNew\{Unit.getWeightClassDesc(weight)}\{Unit.getTypeClassDesc(type)}";
                defaultSettings.setProperty(setting, "true");
                setting = STR."CanBuyUsed\{Unit.getWeightClassDesc(weight)}\{Unit.getTypeClassDesc(type)}";
                defaultSettings.setProperty(setting, "true");
            }
        }

        defaultSettings.setProperty("MinELO", "0");
        defaultSettings.setProperty("MinExp", "0");

        return defaultSettings;
    }

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

    public void setConfig(String key, String value) {
        factionSettings.setProperty(key, value);
    }

    public String getName() {
        return factionSettings.getProperty("Name");
    }
}
