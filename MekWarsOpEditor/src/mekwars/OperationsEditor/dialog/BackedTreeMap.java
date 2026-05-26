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
package mekwars.OperationsEditor.dialog;

import java.io.Serial;
import java.util.TreeMap;

import mekwars.common.campaign.operations.DefaultOperation;

/*
 * Inner class which backs a treemap with
 * a set of default ops values.
 */
public class BackedTreeMap extends TreeMap<String, String> {

    @Serial
    private static final long serialVersionUID = 1L;
    DefaultOperation defaults;

    public BackedTreeMap(DefaultOperation dop) {
        defaults = dop;
    }

    public String getV(String key) {
        String toReturn = super.get(key);
        if (toReturn == null) {
            toReturn = defaults.getDefault(key);
        }
        return toReturn;
    }
}// end BackedTreeMap
