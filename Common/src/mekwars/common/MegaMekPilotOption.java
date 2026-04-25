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

/*
 * Created on 18.04.2004
 *
 */
package common;

/**
 * This can be used as a container for LVL3 Megamek Pilot options in MMNet
 * @author Helge Richter
 *
 */
public class MegaMekPilotOption {
	private String mmname;
	private boolean value;
	
	public MegaMekPilotOption() {
		
	}
	
	public MegaMekPilotOption(String name, boolean value) {
		mmname = name;
		this.value = value;
	}
	/**
	 * @return Returns the mmname.
	 */
	public String getMmname() {
		return mmname;
	}
	/**
	 * @return Returns the value.
	 */
	public boolean isValue() {
		return value;
	}
}
