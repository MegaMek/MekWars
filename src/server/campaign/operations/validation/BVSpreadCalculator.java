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
import server.campaign.SArmy;
import server.campaign.SUnit;

/**
 * @author 	Spork
 * @version	1.0
 * @since	2013-06-19
 */
public class BVSpreadCalculator extends AbstractSpreadCalculator implements I_SpreadCalculator {
	
	/**
	 * Calculates the maximum BV of any unit in the army
	 * 
	 * @param a					The army
	 * @param countSupport		Do support units count?
	 * @param countInfantry		Do infantry and BA count?
	 * @param countVehicles		Do vehicles count?
	 * @param countAero			Do aero units count?
	 * @param countProtos		Do protomeks count?
	 * @param ignorePilot		Use BaseBV or BV modified by pilot skill?
	 * @return					Maximum BV
	 */
	public static int calcMax(SArmy a, boolean countSupport, boolean countInfantry, boolean countVehicles, boolean countAero, boolean countProtos, boolean ignorePilot) {
		int max = 0;
		
		for(Unit u : a.getUnits()) {
			if (countUnit(u, countSupport, countInfantry, countVehicles, countAero, countProtos)) {
				if (ignorePilot) {
					max = Math.max(max, ((SUnit) u).getBaseBV());
				} else {
					max = Math.max(max,  ((SUnit) u).getBVForMatch());
				}
			}
		}
		return max;
	}

	/**
	 * Calculates the minimum BV of any unit in the army
	 * 
	 * @param a					The army
	 * @param countSupport		Do support units count?
	 * @param countInfantry		Do infantry and BA count?
	 * @param countVehicles		Do vehicles count?
	 * @param countAero			Do aero units count?
	 * @param countProtos		Do protomeks count?
	 * @param ignorePilot		Use BaseBV or BV modified by pilot skill?
	 * @return					Minimum BV
	 */
	public static int calcMin(SArmy a, boolean countSupport, boolean countInfantry, boolean countVehicles, boolean countAero, boolean countProtos, boolean ignorePilot ) {
		int min = 99999;
			
		for(Unit u : a.getUnits()) {
			if (countUnit(u, countSupport, countInfantry, countVehicles, countAero, countProtos)) {
				if (ignorePilot) {
					min = Math.min(min, ((SUnit) u).getBaseBV());
				} else {
					min = Math.min(min,  ((SUnit) u).getBVForMatch());
				}
			}
		}
		return min;
	}
}
