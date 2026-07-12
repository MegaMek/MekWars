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

    /** All resolved key/value pairs for the current locale, merged from global, package, then class resources. */
    private final Map<String, String> messages = new HashMap<>();

    /** Reused formatter for the parameterized {@link #getString(String, Object...)} overload. */
    private final MessageFormat messageFormat;

    /**
     * Builds the message set for {@code clazz} by loading, in order, the shared "global" resource bundle, the
     * bundle shared by every class in {@code clazz}'s package (named {@code package.properties}), and finally the
     * bundle specific to {@code clazz} itself (named after its fully-qualified class name). Later loads take
     * precedence over earlier ones for keys that appear in more than one file, so a class-specific translation
     * always wins over a package- or global-level default.
     * <p>
     * All three lookups first try a locale-suffixed file (e.g. {@code _fr.properties}) for {@link Locale#getDefault()}
     * and fall back to the unsuffixed file if that is missing.
     *
     * @param clazz the class whose package and name determine which resource bundles are loaded
     */
    public I18NMessages(Class<?> clazz) {
        Locale locale = Locale.getDefault();
        this.messageFormat = new MessageFormat("", locale);

        loadProperties("global", locale);

        String packageName = clazz.getPackage().getName().replace(".", "/");
        loadProperties(packageName + "/package", locale);

        String className = clazz.getName().replace(".", "/");
        loadProperties(className, locale);
    }

    /**
     * Attempts to load {@code baseName} as a locale-specific properties resource first (e.g.
     * {@code baseName_en.properties}), falling back to the locale-neutral {@code baseName.properties} if no
     * localized version is found. Missing resources are not an error — a class may simply have no bundle of its
     * own and rely entirely on the global/package bundles.
     */
    private void loadProperties(String baseName, Locale locale) {
        String localizedPath = "/" + baseName + "_" + locale.getLanguage() + ".properties";

        if (loadFromPath(localizedPath)) {
            return;
        }

        String basePath = "/" + baseName + ".properties";
        loadFromPath(basePath);
    }

    /**
     * Loads a single properties resource from the classpath at {@code path}, merging its entries into
     * {@link #messages}.
     *
     * @return {@code true} if the resource was found and loaded, {@code false} if it does not exist on the
     *         classpath (not itself treated as an error) or could not be read (logged and treated as absent).
     */
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

    /**
     * Looks up a plain, non-parameterized message.
     *
     * @param key the resource bundle key to look up
     *
     * @return the localized message, or {@code "!!key!!"} if no bundle defined this key — making missing
     *         translations obvious in the UI rather than silently showing nothing.
     */
    public String getString(String key) {
        return messages.getOrDefault(key, "!!" + key + "!!");
    }

    /**
     * Looks up a message and substitutes {@code args} into it using {@link MessageFormat} placeholder syntax
     * (e.g. {@code "Welcome, {0}!"}).
     *
     * @param key  the resource bundle key to look up
     * @param args positional arguments to substitute into the message pattern
     *
     * @return the formatted, localized message, or {@code "!!key!!"} if no bundle defined this key
     */
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
