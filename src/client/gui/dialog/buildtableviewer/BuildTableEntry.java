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
package client.gui.dialog.buildtableviewer;

/**
 * An object representing one line of a build table, either a unit or a link to another table
 * @author Spork
 *
 */
public class BuildTableEntry implements Comparable<BuildTableEntry> {

	public final static int ENTRYTYPE_TABLE = 0;
	public final static int ENTRYTYPE_UNIT = 1;
	
	private int chance = 0;
	private String entry = "";
	private int type = -1;
	
	/**
	 * Constructor
	 */
	public BuildTableEntry() {
		
	}
	
	/**
	 * Constructor
	 * @param type the type of entry (table, unit)
	 * @param chance the frequency of the entry
	 * @param entry the String describing the entry
	 */
	public BuildTableEntry(int type, int chance, String entry) {
		this.type = type;
		this.chance = chance;
		this.entry = entry;
	}
	
	/**
	 * Set the type of entry (table, unit)
	 * @param type the type to set
	 */
	public void setType(int type) {
		this.type = type;
	}
	
	/**
	 * Set the frequency of the entry
	 * @param chance the chance to set
	 */
	public void setChance(int chance) {
		this.chance = chance;
	}
	
	/**
	 * Set the text of the entry
	 * @param entry the text to set
	 */
	public void setEntry(String entry) {
		this.entry = entry;
	}
	
	/**
	 * Get the type of entry
	 * @return the type
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Get the frequency of the entry
	 * @return the frequency
	 */
	public int getChance() {
		return chance;
	}
	
	/**
	 * Get the entry text
	 * @return the entry text
	 */
	public String getEntry() {
		return entry;
	}

	/**
	 * Compare to another BuildTableEntry.  Sorts by type first, then by frequency
	 */
	@Override
	public int compareTo(BuildTableEntry o) {
		int result;
		
		result = Integer.valueOf(getType()).compareTo(Integer.valueOf(o.getType()));
		if (result != 0) {
			return result;
		}
		
		return (Integer.valueOf(getChance()).compareTo(Integer.valueOf(o.getChance())))*-1;
	}
}
