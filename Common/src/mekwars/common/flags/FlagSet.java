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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

/**
 * Base class for a named set of boolean "flags" (feature toggles / house rules) backed by a {@link BitSet}, with
 * support for exporting to and re-importing from a compact delimited string so flag sets can be persisted to disk
 * or sent over the network.
 * <p>
 * Each flag has both a stable integer key (its position in the {@link BitSet}) and a display name; the two are
 * kept in sync via {@link #flagNames}. Subclasses ({@link PlayerFlags}, {@link ResultsFlags}) specialize this for
 * particular flag categories (see {@link #FLAG_TYPE_PLAYER}, {@link #FLAG_TYPE_RESULTS}) and typically add their
 * own default file location for {@link #save(File)}/{@link #loadFromDisk(File)}.
 */
public class FlagSet {
    // Flag Types - since they load differently and all
    /** Identifies a {@link PlayerFlags} instance. */
    public static final int FLAG_TYPE_PLAYER = 0;
    /** Identifies a {@link ResultsFlags} instance. */
    public static final int FLAG_TYPE_RESULTS = 1;
    private static final MMLogger LOGGER = MMLogger.create(FlagSet.class);

    /** The actual true/false state of each flag, indexed by the integer key from {@link #flagNames}. */
    protected BitSet flags = new BitSet();

    /** Maps each flag's integer key to its display name. */
    protected Map<Integer, String> flagNames;

    /** Which {@code FLAG_TYPE_*} this instance represents; set by subclass constructors. */
    protected int flagType;

    /** Creates an empty flag set with no flags defined yet. */
    public FlagSet() {
        flagNames = new TreeMap<>();
    }

    /**
     * Returns a Vector<String> of all flag names.  Used to create menus and such with the names
     *
     * @return Vector<String>
     */
    public Vector<String> getFlagNames() {
        Vector<String> vector = new Vector<>();

        for (int i : flagNames.keySet()) {
            vector.add(flagNames.get(i));
        }

        return vector;
    }

    /**
     * Gets the boolean status of a named flag
     *
     * @param name the flag's display name (case-insensitive)
     *
     * @return the flag's current value, or {@code false} (logged as an error) if no such flag exists
     */
    public boolean getFlagStatus(String name) {
        int flag = getFlagKey(name);
        if (flag != -1) {
            return flags.get(flag);
        } else {
            LOGGER.error("Unknown Flag Status: {}", name);
            return false;
        }
    }

    /**
     * Returns the integer key for a given name.  Needed to map between a flag name and the actual bitset
     *
     * @return integer key ID
     */
    protected int getFlagKey(String name) {
        if (flagNames.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < flagNames.size(); i++) {
            if (flagNames.get(i).equalsIgnoreCase(name)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Loads personally set flags from a string.  This should only be called after defaults are set, as any flags that
     * are listed in this string that do not already exist due to defaults will be ignored.  This way, old flags that
     * may have been deleted by the admins will not continue to hang around but will be pruned every time a player
     * loads.
     *
     */
    public void loadPersonal(String data) {
        if (data.equalsIgnoreCase(" ")) {
            return;
        }

        StringTokenizer stringTokenizer = new StringTokenizer(data, "$");
        while (stringTokenizer.hasMoreTokens()) {
            String element = stringTokenizer.nextToken();
            StringTokenizer elementToken = new StringTokenizer(element, "#");
            String name = elementToken.nextToken();
            elementToken.nextToken();// This isn't needed but is included in the export.  Ignore it.
            boolean value = MathUtility.parseBoolean(elementToken.nextToken(), false);

            if (getFlagKey(name) >= 0) {
                setFlag(name, value);
            }
        }
    }

    /**
     * Sets a named flag to true or false
     *
     */
    public void setFlag(String name, boolean value) {
        int flag = getFlagKey(name);
        if (flag != -1) {
            flags.set(flag, value);
        } else {
            LOGGER.error("Unknown Flag checked: {}", name);
        }
    }

    /**
     * Clears a single flag, removing it from the names and flags
     *
     */
    public void clearFlag(String name) {
        int id = getFlagKey(name);

        if (id == -1) {
            // invalid name
            return;
        }

        flagNames.remove(id);
        flags.clear(id);
    }

    /**
     * Saves the flags to disk.  This should be overloaded in any class that extends the FlagSet, so a simple ".save()"
     * can be sent to the instance
     */
    public void save(File file) {
        //File file = new File("./data/pFlags.dat");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LOGGER.error(e, "Unable to create pFlags.dat");
                return;
            }
        }

        try {
            FileWriter fileWriter = new FileWriter("./data/pFlags.dat");
            BufferedWriter out = new BufferedWriter(fileWriter);
            out.write(export());
            out.close();
            fileWriter.close();
        } catch (IOException e) {
            LOGGER.error(e, "Error saving pFlags.dat");
        }

    }

    /**
     * Builds the string that is imported by load (String data) above Used server-side only, as I envision it, so I
     * might move this method to SPlayer
     *
     * @return String flag settings - name, ID, and value
     */
    public String export() {
        StringBuilder toReturn = new StringBuilder();
        if (flagNames.isEmpty()) {
            return " ";
        }

        for (int key : flagNames.keySet()) {
            String name = flagNames.get(key);
            String isTrue = Boolean.toString(flags.get(key));
            toReturn.append(name).append("#").append(key).append("#").append(isTrue).append("$");
        }

        return toReturn.toString();
    }

    /**
     * Reads a data file from disk.  This should be overloaded by any class extending FlagSet to allow for a simple
     * .loadFromDisk() to be sent.
     */
    public void loadFromDisk(File file) {
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String string;

            try {
                while ((string = bufferedReader.readLine()) != null) {
                    loadDefaults(string);
                }
            } catch (IOException e) {
                LOGGER.error(e, "Error reading pFlags.dat");
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e, "No pFlags.dat. Returning");
        }
    }

    /**
     * Loads a set of flags from a string.  This will be called only at player logon.  This should only be used to load
     * the defaults so we can make sure all the proper flags exist.  If you're loading personal flags, use
     * loadPersonal() instead
     *
     */
    public void loadDefaults(String data) {
        if (data.equalsIgnoreCase(" ")) {
            return;
        }
        // clear out the existing flags, just in case

        empty();
        StringTokenizer stringTokenizer = new StringTokenizer(data, "$");
        while (stringTokenizer.hasMoreTokens()) {
            String element = stringTokenizer.nextToken();
            StringTokenizer elementToken = new StringTokenizer(element, "#");
            String name = elementToken.nextToken();
            int id = Integer.parseInt(elementToken.nextToken());
            boolean value = Boolean.parseBoolean(elementToken.nextToken());
            addFlag(name, id, value);
        }
    }

    /**
     * Removes all player flags
     */
    public void empty() {
        flagNames.clear();
        flags.clear();
    }

    /**
     * Adds a flag to the list
     *
     */
    public void addFlag(String name, int id, boolean value) {
        setFlagName(id, name);
        setFlag(name, value);
    }

    /**
     * Adds the flag name to the map.  Used so that the SOs can use flag names that make sense to them, rather than
     * integers
     *
     */
    public void setFlagName(int key, String name) {
        flagNames.put(key, name);
    }

    /**
     * Finds an unused integer key that a new flag can be assigned to.
     * <p>
     * Note this scans the full {@code [0, size]} range and keeps the <em>last</em> gap found rather than returning
     * the first one, so the result is not necessarily the lowest available ID.
     *
     * @return an integer key not currently present in {@link #flagNames}, or {@code -1} if none was found (which
     *         should not normally happen given the range scanned)
     */
    public int getAvailableID() {
        int toReturn = -1;

        for (int i = 0; i <= flagNames.size(); i++) {
            if (!flagNames.containsKey(i)) {
                toReturn = i;
            }
        }

        return toReturn;
    }

    /** @return the set of all integer keys currently in use by this flag set. */
    public Set<Integer> getKeySet() {
        return flagNames.keySet();
    }
}
