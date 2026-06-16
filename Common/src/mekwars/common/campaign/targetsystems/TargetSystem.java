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

public class TargetSystem {
    public final static int TS_TYPE_STANDARD = 0;
    public final static int TS_TYPE_ANTIAIR = 1;
    public final static int TS_TYPE_SHORT = 2;
    public final static int TS_TYPE_MEDIUM = 3;
    public final static int TS_TYPE_LONG = 4;
    public final static int TS_TYPE_MAX = 4;
    private final static MMLogger LOGGER = MMLogger.create(TargetSystem.class);
    private Entity entity;
    private int currentType = TS_TYPE_STANDARD;

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

    private void setTargetSystem(String type, boolean on) {
        if (entity != null) {entity.getQuirks().getOption(type).setValue(on);}
    }

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

    public void setEntity(Entity e) {
        entity = e;
    }

    public int getCurrentType() {
        return currentType;
    }

    public String getCurrentTypeName() {
        String name = "";
        try {
            name = getTypeName(currentType);
        } catch (TargetTypeOutOfBoundsException | TargetTypeNotImplementedException e) {
            LOGGER.error(e, "Error getting target system name");
        }
        return name;
    }

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
