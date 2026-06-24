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
package mekwars.common.campaign.clientutils;

import javax.swing.ImageIcon;

public interface IClientConfig {

    String CONFIG_FILE = "./data/mwconfig.txt";
    String CONFIG_BACKUP_FILE = "./data/mwconfig.txt.bak";

    // Creates a new config file
    /*
     * All this does ATM is created an empty mwconfig.txt. Lines commented out
     * are old MMNET options that the client code supports, but which are not
     * presented to the user in the MekWars client GUI. The vast majority are
     * totally unused because the players don't know about them. Over time, the
     * options will be made public or removed.
     */
    void createConfig();

    /**
     * Get a config value.
     */
    String getParam(String param);

    /**
     * Set a config value.
     */
    void setParam(String param, String value);

    /**
     * See if a parameter is enabled (YES, TRUE or ON).
     */
    boolean isParam(String param);

    /**
     * Return the int value of a given config property. Return a 0 if the property is a non-number. Used mostly by the
     * misc. mail tab checks.
     */
    int getIntParam(String param);

    /**
     * Write the config file out to ./data/mwconfig.txt.
     */
    void saveConfig();

    ImageIcon getImage(String repair);

    boolean isUsingStatusIcons();

    void loadImage(String s, String camo, int i, int i1);
}
