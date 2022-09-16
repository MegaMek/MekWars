/*
 * MekWars - Copyright (C) 2013 
 * 
 * Original author - Spork (billypinhead@users.sourceforge.net)
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
package server.campaign.operations.validation;

import common.campaign.operations.Operation;
import server.campaign.SArmy;
/**
 * @author 	Spork
 * @version	1.0
 * @since	2013-06-19
 */
public interface I_SpreadValidator {
	public static int SPREADTYPE_BV = 0;
	public static int SPREADTYPE_TONS = 1;
	
	public static int ERROR_UNKNOWN = -1;
	public static int ERROR_NONE = 0;
	public static int ERROR_SPREAD_TOO_LARGE = 1;
	public static int ERROR_SPREAD_TOO_SMALL = 2;
	
	public static int VALIDATOR_CLASS_BV_STANDARD = 0;
	public static int VALIDATOR_CLASS_BV_PERCENT = 1;
	
	/**
	 * Validates an army against an Operation
	 * 
	 * @param a	The army being validated
	 * @param o The Operation being validated against
	 * 
	 * @return	True if the army passes Spread validation, false if not
	 */
	public boolean validate(SArmy a, Operation o);
	
	/**
	 * Set debug logging on/off
	 * @param debug
	 */
	public void setDebug(boolean debug);

	/**
	 * See if debug logging is on
	 * @return True if debug is set, else false
	 */
	public boolean getDebug();
	
	/**
	 * Returns the actual spread between highest and lowest units
	 * @return	the spread
	 */
	public int getSpread();
	
	/**
	 * Calculates the maximum BV of any unit in the army
	 * 
	 * @param a The army being validated
	 * @param o The operation being validated against
	 */
	public void setMaxActual(SArmy a, Operation o);
	
	/**
	 * Calculates the minimum BV of any unit in the army
	 * 
	 * @param a The army being validated
	 * @param o The operation being validated against
	 */
	public void setMinActual(SArmy a, Operation o);
	
	/**
	 * Sets the maximum spread allowed for the Operation
	 * @param max
	 */
	public void setMaxAllowed(int max);
	
	/**
	 * Sets the minimum spread allowed for the Operation
	 * @param min
	 */
	public void setMinAllowed(int min);
	
	/**
	 * Returns the maximum spread allowed by the Operation
	 * @return the maximum
	 */
	public int getMaxAllowed();
	
	/**
	 * Returns the minimum spread allowed by the Operation
	 * @return the minimum
	 */
	public int getMinAllowed();
	
	/**
	 * Returns the minimum BV in the army. Meaningless until setMinActual is called
	 * @return minimum BV
	 */
	public int getMinActual();
	
	/**
	 * Returns the maximum BV in the army. Meaningless until setMaxActual is called
	 * @return maximum BV
	 */
	public int getMaxActual();
	
	/**
	 * Returns the type of Validator (BV, Tonnage, etc)
	 * @return Type (I_SpreadValidator.SPREADTYPE_*)
	 */
	public int getSpreadType();
	
	/**
	 * Sets the type of Validator (BV, Tonnage, etc)
	 * @param type I_SpreadValidator.SPREADTYPE_*  
	 */
	public void setSpreadType(int type);
	
	/**
	 * Returns the error level generated by Validate()
	 * @return	I_SpreadValidator.ERROR_*  
	 */
	public int getError();
}
