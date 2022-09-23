/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)
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
 * ModifyingOperaions are much more closely related to Operations
 * proper than the functional ops (Long and Small). Modifiers are
 * like the basic Op in several ways:
 * 
 *  - generally only a single instance created per server
 *  - store campaign related entry requirements (XP, etc)
 *  - read configs from disk instead of another class
 * 
 * What exactly is a modifying operation? In practice, it should
 * have more restrictive entry requirements than an operation, and
 * modify an Operation's variables.
 * 
 * Example: There is a "Planetary Assault" operation which is generally
 * available. The ModifyingOperation "Fast Mover" has Planetary Assault
 * set as a legal target. A player who meets the Fast Mover requirements
 * can choose to use is modifiers.
 * 
 * Modifiers should be used to:
 * 1) create high risk games. require pay-ins or disincentives (salvage
 *    reductions, CBill costs) for higher payouts, or
 * 2) Encourage diversity of play. Use modifier requirements and advantages
 *    to get players to use unusual or otherwise undesirable forces, offer
 *    incentives for games w/ no assault units, etc.
 * 
 * ModOp params SUPERCEDE those set in an Operation. Some things are 0-checked;
 * however, many paramaters will accept potentially damaging negative settings or
 * params which strongly conflict with the underlying Operation. This is an
 * Operator request (maximum flexibility), but will require strenuous testing
 * of ModOp settings. 
 */

package common.campaign.operations;

import java.util.Properties;

//IMPORTS

public class ModifyingOperation {
	
	//IVARS
	private String opName;
	private Properties modValues;
	
	/**
	 * ModifyingOperation CONSTRUCTOR. Takes a name (same as used to
	 * assemble filenames for param loading) and a set of param values.
	 * 
	 * ModifyingOperations are constructed in OperationLoader.java
	 */
	public ModifyingOperation(String opName, Properties modValues) {
		this.opName = opName;
		this.modValues = modValues;
	}

	//METHODS
	/**
	 * Method which attempts to look up the value of a given Paramater
	 * in an ModOperation's local Tree. If the value is unavailable, a
	 * null is returned.
	 */
	public Object getModValue(String valToGet) {
		Object toReturn = modValues.get(valToGet);
		return toReturn;
	}
	
	/**
	 * Method which returns values, pre-cast to string.
	 */
	public String getValueAsString(String valToGet) {
		return (String)getModValue(valToGet);
	}
	
	/**
	 * Method which returns name of ModOp, as
	 * derived from filename @ loadtime.
	 */
	public String getName() {
		return this.opName;
	}
	
}//end OperationsManager class