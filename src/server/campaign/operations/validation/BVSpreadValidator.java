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
 * 
 * Abstract class to validate unit spreads based on BV
 */
public abstract class BVSpreadValidator implements I_SpreadValidator {
	private int validatorClass;
	
	protected int maxAllowedSpread = 0;
	protected int minAllowedSpread = 99999;
	protected int maxActualBV = 0;
	protected int minActualBV = 99999;
	
	protected int validatorType;
	protected int error = I_SpreadValidator.ERROR_UNKNOWN;
	
	private boolean debug = false;
	
	public void setMaxActual(SArmy a, Operation o) {
		maxActualBV = BVSpreadCalculator.calcMax(a, o.getBooleanValue("CountSupportUnitsForSpread"), o.getBooleanValue("CountInfForSpread"), o.getBooleanValue("CountVehsForSpread"), o.getBooleanValue("CountAerosForSpread"), o.getBooleanValue("CountProtosForSpread"), o.getBooleanValue("IgnorePilotsForBVSpread"));
	}
	
	public void setMinActual(SArmy a, Operation o) {
		minActualBV = BVSpreadCalculator.calcMin(a, o.getBooleanValue("CountSupportUnitsForSpread"), o.getBooleanValue("CountInfForSpread"), o.getBooleanValue("CountVehsForSpread"), o.getBooleanValue("CountAerosForSpread"), o.getBooleanValue("CountProtosForSpread"), o.getBooleanValue("IgnorePilotsForBVSpread"));
	}
	
	public void setMaxAllowed(int max) {
		maxAllowedSpread = max;
	}

	public void setMinAllowed(int min) {
		minAllowedSpread = min;
	}
	
	public int getMaxAllowed() {
		return maxAllowedSpread;
	}
	
	public int getMinAllowed() {
		return minAllowedSpread;
	}
	
	public int getMinActual() {
		return minActualBV;
	}
	
	public int getMaxActual() {
		return maxActualBV;
	}
	
	public int getSpreadType() {
		return validatorType;
	}
	
	public void setSpreadType(int type) {
		validatorType = type;
	}
	
	/**
	 * Sets the error level for Validate's results
	 * @param e	ISpreadValidator.ERROR_*  
	 */
	protected void setError(int e) {
		error = e;
	}
	
	/**
	 * Calculates the error for Validate()'s results
	 */
	protected void calcError() {
		int spread = getMaxActual() - getMinActual();
		int spreadError;

		if(spread > getMaxAllowed()) {
			spreadError = I_SpreadValidator.ERROR_SPREAD_TOO_LARGE;
		} else if (spread < getMinAllowed()) {
			spreadError = I_SpreadValidator.ERROR_SPREAD_TOO_SMALL;
		} else if ((spread >= getMinAllowed()) && (spread <= getMaxAllowed())) {
			spreadError = I_SpreadValidator.ERROR_NONE;
		} else {
			spreadError = I_SpreadValidator.ERROR_UNKNOWN;
		}
		setError(spreadError);
	}
	
	public int getError() {
		return error;
	}
	
	public boolean validate(SArmy a, Operation o) {
		setMinActual(a, o);
		setMaxActual(a, o);
		
		calcError();
		
		if(debug) {
			logDebugInfo(a, o);
		}
		
		if(getError() == I_SpreadValidator.ERROR_NONE) {
			return true;
		} else {
			return false;
		}
	}

	public void setDebug(boolean d) {
		debug = d;
	}
	
	public int getSpread() {
		return getMaxActual() - getMinActual();
	}
	
	/**
	 * Sets the class of the Validator. This is a specific bit of info
	 * potentially unnecessary.  Probably could just be done through
	 * an instanceof call.  I'll think about this and probably will
	 * kill it going forward.
	 * 
	 * @param type  ISpreadValidator.VALIDATOR_CLASS_*  
	 */
	protected void setValidatorClass(int type) {
		validatorClass = type;
	}
	
	protected void logDebugInfo(SArmy a, Operation o) {
		StringBuilder s = new StringBuilder("BVSpreadValidator Debug :\n");
		s.append("\tPlayer: " + a.getPlayerName() + "\n");
		s.append("\tOperation: " + o.getName() + "\n");
		s.append("\tValidatorClass: " + validatorClass + "\n");
		s.append("\tArmy: " + a.getDescription(true) + "\n");
		s.append("\tMin/Max Allowed Spread: " + minAllowedSpread + "/" + maxAllowedSpread + "\n" );
		s.append("\tMin/Max Actual BV:  " + minActualBV + "/" + maxActualBV + "\n");
		s.append("\tSpread: " + getSpread() + "\n");
		s.append("\tErrorLevel Returned: " + getError());
		MWLogger.debugLog(s.toString());		
	}
	
	public boolean getDebug() {
		return debug;
	}

}
