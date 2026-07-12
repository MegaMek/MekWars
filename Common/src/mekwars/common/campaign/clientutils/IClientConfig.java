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

/**
 * Client-side configuration store: a simple key/value settings file (plus a handful of image-loading helpers used by
 * the GUI). Backed on disk by {@link #CONFIG_FILE}, with {@link #CONFIG_BACKUP_FILE} used as a backup copy.
 * Implemented by {@code mekwars.common.gui.GUIClientConfig} (the full-GUI client) and
 * {@code mekwars.dedicatedhost.DedConfig} (the headless dedicated host). Accessed by {@link
 * mekwars.common.campaign.clientutils.protocol.IClient#getConfig()}.
 */
public interface IClientConfig {

    /** Path to the on-disk config file that stores all client settings as key/value pairs. */
    String CONFIG_FILE = "./data/mwconfig.txt";
    /** Path to the backup copy of {@link #CONFIG_FILE}, used to recover from a corrupted/missing config. */
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
     *
     * @param param the config key to look up
     * @return the stored value, or an implementation-defined default/empty value if the key is unset
     */
    String getParam(String param);

    /**
     * Set a config value.
     *
     * @param param the config key to set
     * @param value the value to store (in memory; call {@link #saveConfig()} to persist it)
     */
    void setParam(String param, String value);

    /**
     * See if a parameter is enabled (YES, TRUE or ON).
     *
     * @param param the config key to check
     * @return true if the stored value case-insensitively matches one of the "enabled" tokens
     */
    boolean isParam(String param);

    /**
     * Return the int value of a given config property. Return a 0 if the property is a non-number. Used mostly by the
     * misc. mail tab checks.
     *
     * @param param the config key to look up
     * @return the parsed integer value, or 0 if the stored value is missing or not a valid number
     */
    int getIntParam(String param);

    /**
     * Write the config file out to ./data/mwconfig.txt.
     */
    void saveConfig();

    /**
     * Load and return a cached image (e.g. a repair/status icon) by logical name.
     *
     * @param repair the logical image name/key to resolve
     * @return the loaded icon, or null/placeholder if it could not be found (implementation-dependent)
     */
    ImageIcon getImage(String repair);

    /**
     * @return true if the client is configured to show small status icons (e.g. next to unit/user entries) rather
     *         than plain text.
     */
    boolean isUsingStatusIcons();

    /**
     * Load and cache an image, optionally applying a camo pattern, for later retrieval via {@link #getImage(String)}.
     *
     * @param s     the base image name/path to load
     * @param camo  the camo pattern identifier to apply, or null/empty for none
     * @param i     implementation-specific sizing/index parameter (e.g. target width)
     * @param i1    implementation-specific sizing/index parameter (e.g. target height)
     */
    void loadImage(String s, String camo, int i, int i1);
}
