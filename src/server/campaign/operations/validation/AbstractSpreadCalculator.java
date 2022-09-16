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

import common.Unit;

/**
 * @author 	Spork
 * @version	1.0
 * @since	2013-06-19
 * <p>
 * Calculate the spread.  Intended to allow for multiple spread
 * types (such as BV and tonnage).  
 * <p>
 * Subclasses should implement 2 static methods:
 * <p>
 * <code>public static int calcMax(SArmy a, boolean countSupport, boolean countInfantry, boolean countVehicles, boolean countAero, boolean countProtos, boolean ignorePilot)</code>
 * <p>
 * and
 * <p>
 * <code>public static int calcMax(SArmy a, boolean countSupport, boolean countInfantry, boolean countVehicles, boolean countAero, boolean countProtos, boolean ignorePilot)</code>
 * <p>
 * However, because of the static keyword in those, I cannot define them in either
 * the abstract class or the interface
 */
public abstract class AbstractSpreadCalculator implements I_SpreadCalculator {
	
	public static int SPREADTYPE_BV = 0;
	public static int SPREADTYPE_TONNAGE = 1;
	
/**
 * @author Spork
 * @param u The unit in question
 * @param countSupport Are support units counted?  Pulled from Operation configs
 * @param countInfantry Are infantry and Battlearmor counted?  Pulled from Operation configs
 * @param countVehicles Are vehicles counted?  Pulled from Operation configs
 * @param countAero Are aero units counted?  Pulled from Operation configs
 * @param countProtos Are protos counted?  Pulled from Operation configs
 * @return boolean true if unit is counted, false if not
 */
	protected static boolean countUnit(Unit u, boolean countSupport, boolean countInfantry, boolean countVehicles, boolean countAero, boolean countProtos) {
		int type = u.getType();
		if (u.isSupportUnit() && !countSupport) {
			return false;
		} 
		if (type == Unit.AERO && !countAero){
			return false;
		}
		if (type == Unit.VEHICLE && !countVehicles) {
			return false;
		}
		if (type == Unit.PROTOMEK && !countProtos) {
			return false;
		}
		if ((type == Unit.INFANTRY || type == Unit.BATTLEARMOR) && !countInfantry) {
			return false;
		}
		return true;
	}
	
}
