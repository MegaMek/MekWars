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
 * A utility which reads operation information.
 * 
 * Has methods to read
 *  - Operations
 *  - ModifiyingOperations
 *  - SAVED LongOperations
 * 
 * NOTE: The loader/writer DO NOT handle the reading and writing
 * of all the Operation/ModifyingOperation params. Modification of
 * these files must be done by hand, or with an external utility
 */
package server.campaign.operations;

//IMPORTS
import java.io.FileInputStream;
import java.util.Properties;

import common.campaign.operations.DefaultOperation;
import common.campaign.operations.ModifyingOperation;
import common.campaign.operations.Operation;
import common.util.MWLogger;


public class OperationLoader {
	
	//IVARS
	DefaultOperation defaults;
	
	//CONSTRUCTORS
	public OperationLoader() {
		defaults = new DefaultOperation();
	}

	//METHODS
	
	/**
	 * Method which loads an operations values from server flat
	 * files. Unspecified values will revert to hardcoded defaults.
	 * 
	 * NOTE: two files are read, but only one Properties/Hashmap
	 * is populated.
	 * 
	 * Also note: # can be used w/i op files as a comment. tabs
	 *            are treated as whitespace. property names are
	 *            case sensitive.
	 * 
	 * Suggested formatting follows ...
	 * ---
	 * 
	 * #COMMENT RE: SETTING
	 * #MORE COMMENT DETAIL
	 * Param1			= value
	 * LongNameParam	= value
	 * 
	 * #COMMENTS ON NEW BLOCK
	 * Param2			= value
	 * 
	 * - See Properties javadoc for additional commenting info.
	 * 
	 * @urgru 5/30/05 
	 */
	public Operation loadOpValues(String opName) {
		
		Properties opValues = new Properties();
		
		//attempt to load shortvals
		String shortFilename = "./data/operations/short/"+ opName;
		try {
			opValues.load(new FileInputStream(shortFilename));
		} catch (Exception e) {
			MWLogger.errLog("Problems loading short op: " + opName);
			MWLogger.errLog(e);
		}
		
		//attempt to load longvals
		String longFilename = "./data/operations/long/"+ opName;
		try {
			opValues.load(new FileInputStream(longFilename));
		} catch (Exception e) {
			//exception loading long. presume that its intentionally
			//missing and this is a short-only operation
		}
		
		opName = opName.substring(0, opName.length() - 4);//remove ".txt"
		return new Operation(opName.trim(), defaults, opValues);
	}
	
	/**
	 * Method which loads modifying operations from flat files. Unspecified
	 * values revert to those of the underlying operation first, and then to
	 * values from DefaultOperation.
	 * 
	 * Unlike standard operations, which read from 2 files, modops only read
	 * from one flat file.
	 * 
	 * See this.loadOpValues() and Properties javadoc for commenting info.
	 * 
	 * NOTE: addition of modops to standard Op's mod treemaps is handled in
	 * OperationManager.java, shortly after this load method.
	 * 
	 * @urgru 6/10/05
	 */
	public ModifyingOperation loadModOpValues(String opName) {
		
		Properties modValues = new Properties();
		
		//attempt load
		String modFilename = "./data/operations/modifiers/"+ opName;
		try {
			modValues.load(new FileInputStream(modFilename));
		} catch (Exception e) {
			MWLogger.errLog("Problems loading mod op: " + opName);
			MWLogger.errLog(e);
		}
		
		opName = opName.substring(0, opName.length() - 5);//remove ".txt"
		return new ModifyingOperation(opName.trim(), modValues);
	}
	
}//end OperationsManager class