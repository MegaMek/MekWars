/*
 * Copyright (C) 2007 - Torren (torren@users.sourceforge.net)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
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
         * MM was changed to assign a UUID to the externalID field of an entity when it was created in MM. This was
         * causing issues with Mekwars, because MW uses integers to keep track of Unit ID's internally. This block of
         * code would attempt to call Entity.getExternalId and would fail because Integer.parse() won't parse a UUID
         * stored as a string correctly. The fix is to catch the exception, assign a sane default and then let MW
         * go on its way.
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

                if (forceDevastate) {result.append(STR."\{IEntityRemovalConditions.REMOVE_DEVASTATED}*");} else {
                    result.append(entity.getRemovalCondition()).append("*");
                }

                switch (entity) {
                    case BipedMek ignored -> result.append(STR."\{Unit.MEK}*");
                    case QuadMek ignored -> result.append(STR."\{Unit.QUAD}*");
                    case Tank ignored -> result.append(STR."\{Unit.VEHICLE}*");
                    case ProtoMek ignored -> result.append(STR."\{Unit.PROTOMEK}*");
                    case BattleArmor ignored -> result.append(STR."\{Unit.BATTLEARMOR}*");
                    case Aero ignored -> result.append(STR."\{Unit.AERO}*");
                    default -> result.append(STR."\{Unit.INFANTRY}*");
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
                    result.append(entity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM,
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
                case MekWarrior mw -> {
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
         * FullStatus is used when auto reporting. This status, which sends less information, is used for
         * InProgressUpdates.
         */
        else {
            // if the entity is a mechwarrior, send an IPU command (InProgressUpdate) to the server.
            if (entity instanceof MekWarrior mw) {
                result.append("MW*").append(mw.getOriginalRideExternalId()).append("*");
                result.append(mw.getPickedUpByExternalId()).append("*");
                result.append(mw.isDestroyed()).append("*");
            } else {
                result.append(entity.getOwner().getName()).append("*");
                result.append(externalID).append("*");

                if (forceDevastate) {result.append(STR."\{IEntityRemovalConditions.REMOVE_DEVASTATED}*");} else {
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
        } //end else(unfull status)

        return result.toString();
    }
}
