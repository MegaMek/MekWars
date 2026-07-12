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

import java.io.File;

/**
 * A {@link FlagSet} for per-player feature toggles/preferences, always persisted to the same fixed file
 * ({@code ./data/pFlags.dat}) rather than a caller-supplied {@link File}.
 */
public class PlayerFlags extends FlagSet {

    /** Creates an empty player flag set. */
    public PlayerFlags() {
        super();
        flagType = FLAG_TYPE_PLAYER;
    }

    /** Saves this flag set to the fixed {@code ./data/pFlags.dat} location. */
    public void save() {
        File file = new File("./data/pFlags.dat");
        super.save(file);
    }

    /** Loads this flag set from the fixed {@code ./data/pFlags.dat} location. */
    public void loadFromDisk() {
        File file = new File("./data/pFlags.dat");
        super.loadFromDisk(file);
    }

    /** @return {@code true} if no flags have been set. */
    public boolean isEmpty() {
        return (flags.isEmpty());
    }

}
