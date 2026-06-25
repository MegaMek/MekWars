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

package mekwars.common.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * HTML Helper Class for creation of various elements in a uniform manner handling classes, styles, content, open and
 * close tags, etc.
 */
public class HTMLHelper {

    /**
     * Overrode method to only accept the relative path for the image.
     *
     * @param relativePath Relative path for the file in relation to the Root of thr program path.
     *
     * @return A string of the above items added to an image tag.
     */
    @Nonnull
    public static String imageTag(@Nonnull String relativePath) {
        return imageTag(relativePath, null, null, null, null);
    }

    /**
     * Creates an image tag with the specified parameters. Relative is the first parameter here as it is the only
     * required parameter.
     *
     * @param relativePath The path to the image relative to the root of the program.
     * @param parentPath   An absolute path to the parent folder of the image.
     * @param altText      The alt text for the image.
     * @param classes      Any classes that should be added
     * @param styles       Any custom styles for this image.
     *
     * @return A string of the above items added to an image tag.
     */
    @Nonnull
    public static String imageTag(String relativePath, @Nullable String parentPath, @Nullable String altText,
          @Nullable String classes, @Nullable String styles) {
        return String.format("<img src='%s%s' alt='%s' class='%s' style='%s'>", parentPath, relativePath, altText,
              classes, styles);
    }

    /**
     * Creates an image tag with the specified parameters. Note that the Parent Path is FIRST on this method instead of
     * second on the main method. This is intentional as this is how most people think about it.
     *
     * @param parentPath   An absolute path to the parent folder of the image.
     * @param relativePath The path to the image relative to the root of the program.
     *
     * @return A string of the above items added to an image tag.
     */
    @Nonnull
    public static String imageTag(@Nonnull String parentPath, @Nonnull String relativePath) {
        return imageTag(relativePath, parentPath, null, null, null);
    }
}
