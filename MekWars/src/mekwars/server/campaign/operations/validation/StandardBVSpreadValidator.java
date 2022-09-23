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

/**
 * @author 	Spork
 * @version	1.0
 * @since	2013-06-19
 */
public class StandardBVSpreadValidator extends BVSpreadValidator implements I_SpreadValidator {
	
	/**
	 * Validates Unit BV Spread
	 * @param min Minimum spread allowed
	 * @param max Maximum spread allowed
	 */
	public StandardBVSpreadValidator(int min, int max) {
		setValidatorClass(I_SpreadValidator.VALIDATOR_CLASS_BV_STANDARD);
		setSpreadType(I_SpreadValidator.SPREADTYPE_BV);
		
		setMaxAllowed(max);
		setMinAllowed(min);
	}

}
