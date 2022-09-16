/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package server.campaign;


public class NonConqHouse extends SHouse {
	
	/**
     * 
     */
    private static final long serialVersionUID = 7378173181135107451L;

    public NonConqHouse (int id, String name, String HouseColor, int BaseGunner, int BasePilot, String abbreviation) {
		super(id,name,HouseColor,BaseGunner,BasePilot,abbreviation);
		setConquerable(false);
	}
	
	public NonConqHouse(int id) {
        super(id);
	}
    
    /**
     * Used for serialization
     */
    public NonConqHouse() {
    	super();
    }

    @Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("[C]");
		result.append(super.toString());
		return result.toString();
	}
}
	