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
 * Stores all params and info relevant to this particular
 * Operation (launch params). SmallOperations hold single-game
 * info. LongOperations hold status for multigame affairs.
 * 
 * There's actually very little interaction between an Operation
 * and ongoing games/resolutions. Operation should be looked at
 * as a template or guidebook. When fed to a resolver in conjuntion
 * with a functional operation (long or short), actual results are
 * generated.
 * 
 * Generally speaking, there is only one instance of each type of
 * operation on a server.
 * 
 * RELATIONSHIPS OF NOTE:
 * - ShortOperation.java
 * - LongOperation.java
 * - ModifiyingOperations.java [userland: Special Ops]
 */
package common.campaign.operations;

//IMPORTS
import java.util.Properties;
import java.util.TreeMap;

import common.MWXMLWriter;
import common.MWXmlSerializable;
import common.util.MMNetXStream;
import common.util.MWLogger;

public class Operation implements MWXmlSerializable {
	
	//IVARS
	
	/*
	 * Static ints, used as quick indicators. In particualar:
	 * - indicate that op is pure short (no long portion)
	 * - indicate that modifiers can be used with an op
	 * [Expect more over time ???]
	 */
	public static int TYPE_SHORTONLY = 0;//default
	public static int TYPE_SHORTANDLONG = 1;
	
	public static int MODS_NOTACCEPTED = 0;//default
	public static int MODS_ACCEPTED = 1;
	
	//private ints which hold current state
	private int type_indicator;
	private int mods_indicator;
	
	//TreeMap of modifiers. As modifiers are loaded, those
	//targetting an operation are added to this map.
	TreeMap<String,ModifyingOperation> modifyingOperations;
	
	//Operation properties (hashtable of configured params)
	Properties opValues;
	
	//other loads ...
	DefaultOperation opsDefaults;
	String opName;//Name of this op. EG - "Assault"
	
	/**
	 * Operation CONSTRUCTOR. Takes a name (used to assemble
	 * filenames for param loading) and a set of default vals.
	 * 
	 * Operations are constructed in OperationLoader.java
	 */
	public Operation(String opName, DefaultOperation defaults, Properties params) {
		
		//save name
		this.opName = opName;
		
		//save the default paramaters
		opsDefaults = defaults;
		
		//set the default indicators
		type_indicator = Operation.TYPE_SHORTONLY;
		mods_indicator = Operation.MODS_NOTACCEPTED;
		
		//create mod map
		modifyingOperations = new TreeMap<String, ModifyingOperation>();
		
		//set the value tables
		opValues = params;
        
	}
	
	public String getValue(String valToGet) {
		return getValue(valToGet, true);
	}
	
	/**
	 * Method which attempts to look up the value of a given Paramater
	 * in an Operation's local Tree. If the value is unavailable, for
	 * any reason (typo, intentionally unset), a default value is checked
	 * and returned.
	 */
	public String getValue(String valToGet, boolean log) {
		
		//look in the short list every time
		String toReturn = (String)opValues.get(valToGet);
		
		//if not present, load a default
		if (toReturn == null)
			toReturn = opsDefaults.getDefault(valToGet);
		
		//catastrophic failue. sysexit.
		if (toReturn == null && log) {
			MWLogger.errLog("Failed getting value \"" + valToGet + "\" from " + this.getName() + " and DefaultOp. Returning null.");
			try{
				throw new Exception();
			}catch(Exception ex){
				MWLogger.errLog(ex);
			}
		}
			
		return toReturn;
	}
	
	public boolean getBooleanValue(String valToGet) {
		try {
			return Boolean.parseBoolean(getValue(valToGet));
		}catch (Exception ex) {
			return false;
		}
	}
	
	public int getIntValue(String valToGet) {
		try {
			return Integer.parseInt(getValue(valToGet));
		}catch (Exception ex) {
			return -1;
		}
	}
	
	public double getDoubleValue(String valToGet) {
		try {
			return Double.parseDouble(getValue(valToGet));
		}catch (Exception ex) {
			return -1;
		}
	}
	
	public float getFloatValue(String valToGet) {
		try {
			return Float.parseFloat(getValue(valToGet));
		}catch (Exception ex) {
			return -1;
		}
	}
	
	/**
	 * Method which adds a mod op to this operation's
	 * tree of valid mods. Set from OperationManager @
	 * load time, drawn from modops' target params.
	 * 
	 * Toggle mods indicator to show that this op does
	 * have potential mods to check for @ startup and
	 * during resolution.
	 */
	public void addModifyingOperation(ModifyingOperation m) {
		modifyingOperations.put(m.getName(), m);
		mods_indicator = Operation.MODS_ACCEPTED;
	}
	
	
	/**
	 * Methods which return and set type info via a
	 * boolean (short only, long+short, etc.)
	 */
	public int getTypeIndicator() {
		return type_indicator;
	}
	
	public void setTypeIndicator(int i) {
		type_indicator = i;
	}
	
	/**
	 * Methods which set and returns modifier status
	 * (accepts or no-mods, etc)
	 */
	public int getModsIndicator() {
		return mods_indicator;
	}
	
	public void setModsIndicator(int i) {
		mods_indicator = i;
	}
	
	/**
	 * Method which returns name of an operation,
	 * as drawn from filename.
	 */
	public String getName() {
		return this.opName;
	}

	@Override
	public void writeToXmlFile(String folderName, String fileName) {
		MWXMLWriter writer = new MWXMLWriter(folderName, fileName, opValues);
		writer.writeToFile();
	}

	@Override
	public String getXmlString() {
		MMNetXStream xml = new MMNetXStream();
		return xml.toXML(opValues);
	}
			
}//end OperationsManager class