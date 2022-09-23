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
import common.util.MWLogger;
import server.campaign.SArmy;

/**
 * @author 	Spork
 * @version	1.0
 * @since	2013-06-19
 */
public class PercentBVSpreadValidator extends BVSpreadValidator implements
		I_SpreadValidator {

	protected double percent;
	protected int base;
	
	@Override
	public boolean validate(SArmy a, Operation o) {
		int maximum = base;
		int spreadPercent = (int) (a.getBV() * percent);
		maximum += spreadPercent;
		
		if(getDebug()) {
			MWLogger.debugLog("Base = " + base + ", spreadPercent = " + spreadPercent + ", maximum = " + maximum);
			MWLogger.debugLog("Army BV = " + a.getBV() + ", percent = " + percent);
		}

		
		setMaxAllowed(maximum);
		setMinActual(a, o);
		setMaxActual(a, o);
		
		calcError();
		
		if(getDebug()) {
			logDebugInfo(a, o);
		}
		
		if(getError() == I_SpreadValidator.ERROR_NONE) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Validates BV spread based on percent of army total BV
	 * 
	 * @param base		BV added to the percent spread
	 * @param percent	Percent of army BV added to the base
	 */
	public PercentBVSpreadValidator(int base, double percent) {
		setValidatorClass(I_SpreadValidator.VALIDATOR_CLASS_BV_PERCENT);
		setSpreadType(I_SpreadValidator.SPREADTYPE_BV);
		setMinAllowed(0);
		this.base = base;
		this.percent = percent;
	}
}
