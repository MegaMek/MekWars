/*
 * MekWars - Copyright (C) 2007 
 * 
 * Original author - Torren (torren@users.sourceforge.net)
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

package common.campaign.clientutils;

import java.util.Enumeration;

import common.Unit;
import common.util.UnitUtils;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.BipedMech;
import megamek.common.CriticalSlot;
import megamek.common.EjectedCrew;
import megamek.common.Entity;
import megamek.common.IEntityRemovalConditions;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.Protomech;
import megamek.common.QuadMech;
import megamek.common.Tank;

public class SerializeEntity{
	public static String serializeEntity (Entity e, boolean fullStatus, boolean forceDevastate, boolean useRepairs) {
		
		StringBuilder result = new StringBuilder();

		int externalID;

		 /*
		  * James Allred (wildj79@gmail.com) 2016-08-09
		  *
		  * MM was changed to assign a UUID to the externalID field of
		  * an entity when it was created in MM. This was causing issues
		  * with Mekwars, because MW uses integers to keep track of Unit ID's
		  * internally. This block of code would attempt to call Entity.getExternalId
		  * and would fail because Integer.parse() won't parse a UUID that is stored as
		  * a string correctly.  The fix is to catch the exception, assign a sane default
		  * and then let MW go on it's way.
		  */
		try {
			externalID = e.getExternalId();
		} catch (NumberFormatException ex) {
			externalID = -1;
		}

		if (fullStatus) {
			if ( !(e instanceof MechWarrior) && !(e instanceof EjectedCrew))
			{
				result.append(externalID + "*");
				result.append(e.getOwner().getName().trim() + "*");
				result.append(e.getCrew().getHits() + "*");
				
				if (forceDevastate)
					result.append(IEntityRemovalConditions.REMOVE_DEVASTATED + "*");
				else
					result.append(e.getRemovalCondition() + "*");
				
				if ( e instanceof BipedMech )
					result.append(Unit.MEK +"*");
				else if ( e instanceof QuadMech )
					result.append(Unit.QUAD + "*");
				else if ( e instanceof Tank)
					result.append(Unit.VEHICLE +"*");
				else if ( e instanceof Protomech)
					result.append(Unit.PROTOMEK +"*");
				else if ( e instanceof BattleArmor )
					result.append(Unit.BATTLEARMOR+"*");
                else if ( e instanceof Aero )
                    result.append(Unit.AERO+"*");
				else
					result.append(Unit.INFANTRY +"*");
				//result.append(e.getMovementType() + "*"); bad code
				//Collect kills
				Enumeration<Entity> en = e.getKills();
				//No kills? Add an empty space
				if (!en.hasMoreElements())
					result.append(" *");
				while (en.hasMoreElements()) {
					Entity kill = en.nextElement();

					// James Allred (wildj79@gmail.com) 2016-08-09
					// Same issue as above. UUID's and int's don't mix.
					try {
						externalID = kill.getExternalId();
					} catch (NumberFormatException ex) {
						externalID = -1;
					}
					result.append(externalID);
					if (en.hasMoreElements())
						result.append("~");
					else
						result.append("*");
				}
			}
			
			if (e instanceof Mech ) {
				result.append(e.getCrew().isUnconscious() + "*");
				result.append(e.getInternal(Mech.LOC_CT) + "*");
				result.append(e.getInternal(Mech.LOC_HEAD) + "*");
				result.append(e.getInternal(Mech.LOC_LLEG) + "*");
				result.append(e.getInternal(Mech.LOC_RLEG) + "*");
				result.append(e.getInternal(Mech.LOC_LARM) + "*");
				result.append(e.getInternal(Mech.LOC_RARM) + "*");
				result.append(e.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) + "*");
				result.append(((Mech)e).getCockpitType()+"*");
				if ( useRepairs ){
					result.append(UnitUtils.unitBattleDamage(e, true)+"*");
				}
	            result.append(UnitUtils.getEntityFileName(e));
			} else if (e instanceof Tank ) {
				result.append(e.isRepairable() + "*");
				result.append(e.isImmobile() + "*");
				result.append(e.getCrew().isDead() + "*");
				if ( useRepairs ){
					result.append(UnitUtils.unitBattleDamage(e, true)+"*");
				}
	            result.append(UnitUtils.getEntityFileName(e));
			}else if (e instanceof Aero ) {
                result.append(e.isRepairable() + "*");
                result.append(e.isImmobile() + "*");
                result.append(e.getCrew().isDead() + "*");
                result.append(UnitUtils.getEntityFileName(e));
            }
			else if (e instanceof MechWarrior) {
				MechWarrior mw = (MechWarrior)e;
				result.append("MW*");
				result.append(mw.getOriginalRideExternalId() + "*");
				result.append(mw.getPickedUpByExternalId() + "*");
				result.append(mw.isDestroyed()+"*");
			}
			
			if (  e.isOffBoard() ){
				result.append("*" + e.getOffBoardDistance());
			}
		}
		
		/*
		 * FullStatus is used when autoreporting. This status, which 
		 * sends less information, is used for InProgressUpdates.
		 */
		else {
			//if the entity is a mechwarrior, send an IPU command
			//(InProgressUpdate) to the server.
			if (e instanceof MechWarrior) {
				MechWarrior mw = (MechWarrior)e;
				result.append("MW*" + mw.getOriginalRideExternalId() + "*");
				result.append(mw.getPickedUpByExternalId() + "*");
				result.append(mw.isDestroyed()+"*");
			} 
			
			//else (the entity is a real unit)
			else {
				result.append(e.getOwner().getName() + "*");
				result.append(externalID + "*");
				
				if (forceDevastate)
					result.append(IEntityRemovalConditions.REMOVE_DEVASTATED + "*");
				else
					result.append(e.getRemovalCondition() + "*");
				
				if (e instanceof Mech ) {
					result.append(e.getInternal(Mech.LOC_CT) + "*");
					result.append(e.getInternal(Mech.LOC_HEAD) + "*");
				} else {
					result.append("1*");
					result.append("1*");
				}
				result.append(e.isRepairable() + "*");
			}
		} //end else(un-full status)
		
		return result.toString();
	}
}