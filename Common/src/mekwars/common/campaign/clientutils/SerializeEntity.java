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

package mekwars.common.campaign.clientutils;

import java.util.Enumeration;

import megamek.common.CriticalSlot;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.Aero;
import megamek.common.units.BipedMek;
import megamek.common.units.EjectedCrew;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.MekWarrior;
import megamek.common.units.ProtoMek;
import megamek.common.units.QuadMek;
import megamek.common.units.Tank;
import mekwars.common.Unit;
import mekwars.common.util.UnitUtils;

public class SerializeEntity {
    public static String serializeEntity(Entity entity, boolean fullStatus, boolean forceDevastate,
          boolean useRepairs) {

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
            externalID = entity.getExternalId();
        } catch (NumberFormatException ex) {
            externalID = -1;
        }

        if (fullStatus) {
            if (!(entity instanceof EjectedCrew)) {
                result.append(externalID).append("*");
                result.append(entity.getOwner().getName().trim()).append("*");
                result.append(entity.getCrew().getHits()).append("*");

                if (forceDevastate) {result.append(IEntityRemovalConditions.REMOVE_DEVASTATED + "*");} else {
                    result.append(entity.getRemovalCondition()).append("*");
                }

                switch (entity) {
                    case BipedMek ignored -> result.append(Unit.MEK + "*");
                    case QuadMek ignored -> result.append(Unit.QUAD + "*");
                    case Tank ignored -> result.append(Unit.VEHICLE + "*");
                    case ProtoMek ignored -> result.append(Unit.PROTOMEK + "*");
                    case BattleArmor ignored -> result.append(Unit.BATTLEARMOR + "*");
                    case Aero ignored -> result.append(Unit.AERO + "*");
                    default -> result.append(Unit.INFANTRY + "*");
                }

                //Collect kills
                Enumeration<Entity> en = entity.getKills();
                //No kills? Add an empty space
                if (!en.hasMoreElements()) {result.append(" *");}
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
                    if (en.hasMoreElements()) {result.append("~");} else {result.append("*");}
                }
            }

            switch (entity) {
                case Mek ignored -> {
                    result.append(entity.getCrew().isUnconscious()).append("*");
                    result.append(entity.getInternal(Mek.LOC_CENTER_TORSO)).append("*");
                    result.append(entity.getInternal(Mek.LOC_HEAD)).append("*");
                    result.append(entity.getInternal(Mek.LOC_LEFT_LEG)).append("*");
                    result.append(entity.getInternal(Mek.LOC_RIGHT_LEG)).append("*");
                    result.append(entity.getInternal(Mek.LOC_LEFT_ARM)).append("*");
                    result.append(entity.getInternal(Mek.LOC_RIGHT_ARM)).append("*");
                    result.append(entity.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                Mek.SYSTEM_GYRO,
                                Mek.LOC_CENTER_TORSO))
                          .append("*");
                    result.append(((Mek) entity).getCockpitType()).append("*");
                    if (useRepairs) {
                        result.append(UnitUtils.unitBattleDamage(entity, true)).append("*");
                    }
                    result.append(UnitUtils.getEntityFileName(entity));
                }
                case Tank ignored -> {
                    result.append(entity.isRepairable()).append("*");
                    result.append(entity.isImmobile()).append("*");
                    result.append(entity.getCrew().isDead()).append("*");
                    if (useRepairs) {
                        result.append(UnitUtils.unitBattleDamage(entity, true)).append("*");
                    }
                    result.append(UnitUtils.getEntityFileName(entity));
                }
                case Aero ignored -> {
                    result.append(entity.isRepairable()).append("*");
                    result.append(entity.isImmobile()).append("*");
                    result.append(entity.getCrew().isDead()).append("*");
                    result.append(UnitUtils.getEntityFileName(entity));
                }
                case MekWarriorWarrior mw -> {
                    result.append("MW*");
                    result.append(mw.getOriginalRideExternalId()).append("*");
                    result.append(mw.getPickedUpByExternalId()).append("*");
                    result.append(mw.isDestroyed()).append("*");
                }
                default -> {
                }
            }

            if (entity.isOffBoard()) {
                result.append("*").append(entity.getOffBoardDistance());
            }
        }

        /*
         * FullStatus is used when auto reporting. This status, which
         * sends less information, is used for InProgressUpdates.
         */
        else {
            //if the entity is a mechwarrior, send an IPU command
            //(InProgressUpdate) to the server.
            if (entity instanceof MekWarrior mw) {
                result.append("MW*").append(mw.getOriginalRideExternalId()).append("*");
                result.append(mw.getPickedUpByExternalId()).append("*");
                result.append(mw.isDestroyed()).append("*");
            } else {
                result.append(entity.getOwner().getName()).append("*");
                result.append(externalID).append("*");

                if (forceDevastate) {result.append(IEntityRemovalConditions.REMOVE_DEVASTATED + "*");} else {
                    result.append(entity.getRemovalCondition()).append("*");
                }

                if (entity instanceof Mek) {
                    result.append(entity.getInternal(Mek.LOC_CENTER_TORSO)).append("*");
                    result.append(entity.getInternal(Mek.LOC_HEAD)).append("*");
                } else {
                    result.append("1*");
                    result.append("1*");
                }
                result.append(entity.isRepairable()).append("*");
            }
        } //end else(un-full status)

        return result.toString();
    }
}
