/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package mekwars.common.gui.dialogs.buildtableviewer;

/**
 * An object representing one line of a build table, either a unit or a link to another table
 *
 * @author Spork
 *
 */
public class BuildTableEntry implements Comparable<BuildTableEntry> {

    /** Entry type indicating this line is a link/reference to another {@link BuildTable}, not a unit. */
    public final static int ENTRY_TYPE_TABLE = 0;
    /** Entry type indicating this line refers directly to a specific unit. */
    public final static int ENTRY_TYPE_UNIT = 1;

    /** Relative frequency/weight of this entry within its build table; higher values are more likely to be picked. */
    private int chance = 0;
    /** The entry's text: either a unit file reference or the name of a linked build table, depending on {@link #type}. */
    private String entry = "";
    /** The kind of entry this is: {@link #ENTRY_TYPE_TABLE} or {@link #ENTRY_TYPE_UNIT} (defaults to -1/unset). */
    private int type = -1;

    /**
     * Constructor
     */
    public BuildTableEntry() {

    }

    /**
     * Constructor
     *
     * @param type   the type of entry (table, unit)
     * @param chance the frequency of the entry
     * @param entry  the String describing the entry
     */
    public BuildTableEntry(int type, int chance, String entry) {
        this.type = type;
        this.chance = chance;
        this.entry = entry;
    }

    /**
     * Get the entry text
     *
     * @return the entry text
     */
    public String getEntry() {
        return entry;
    }

    /**
     * Set the text of the entry
     *
     * @param entry the text to set
     */
    public void setEntry(String entry) {
        this.entry = entry;
    }

    /**
     * Compare to another BuildTableEntry.  Sorts by type first, then by frequency
     */
    @Override
    public int compareTo(BuildTableEntry buildTableEntry) {
        int result;

        result = Integer.compare(getType(), buildTableEntry.getType());
        if (result != 0) {
            return result;
        }

        return (Integer.compare(getChance(), buildTableEntry.getChance())) * -1;
    }

    /**
     * Get the type of entry
     *
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * Set the type of entry (table, unit)
     *
     * @param type the type to set
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Get the frequency of the entry
     *
     * @return the frequency
     */
    public int getChance() {
        return chance;
    }

    /**
     * Set the frequency of the entry
     *
     * @param chance the chance to set
     */
    public void setChance(int chance) {
        this.chance = chance;
    }
}
