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
package mekwars.common.I18N;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import megamek.logging.MMLogger;

/**
 * Base class for handling I18N Messages in a way that allows each class to have their own resource file as well as a
 * parent resource that has shared messages used in multiple classes as well as a shared group of messages for the
 * package the class is a part of.
 */
public class I18NMessages {
    private static final MMLogger LOGGER = MMLogger.create(I18NMessages.class);
    private final Map<String, String> messages = new HashMap<>();
    private final MessageFormat messageFormat;

    public I18NMessages(Class<?> clazz) {
        Locale locale = Locale.getDefault();
        this.messageFormat = new MessageFormat("", locale);

        loadProperties("global", locale);

        String packageName = clazz.getPackage().getName().replace(".", "/");
        loadProperties(packageName + "/package", locale);

        String className = clazz.getName().replace(".", "/");
        loadProperties(className, locale);
    }

    private void loadProperties(String baseName, Locale locale) {
        String localizedPath = "/" + baseName + "_" + locale.getLanguage() + ".properties";

        if (loadFromPath(localizedPath)) {
            return;
        }

        String basePath = "/" + baseName + ".properties";
        loadFromPath(basePath);
    }

    private boolean loadFromPath(String path) {
        try (InputStream input = I18NMessages.class.getResourceAsStream(path)) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);

                for (String key : props.stringPropertyNames()) {
                    messages.put(key, props.getProperty(key));
                }

                return true;
            }
        } catch (IOException ex) {
            LOGGER.error(ex, "Unable to load messages from path: {}", path);
        }

        return false;
    }

    public String getString(String key) {
        return messages.getOrDefault(key, "!!" + key + "!!");
    }

    public String getString(String key, Object... args) {
        String pattern = messages.get(key);

        if (pattern == null) {
            return "!!" + key + "!!";
        }

        synchronized (messageFormat) {
            messageFormat.applyPattern(pattern);
            return messageFormat.format(args);
        }
    }
}
