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

/**
 * Utility for flattening a MegaMek {@link Entity} into a compact, delimited string suitable for transmission over
 * the MekWars client/server protocol (e.g. inside "IPU" in-progress-update commands and end-of-game reports). The
 * format is a "*"-delimited (and, for kill lists, "~"-delimited) positional encoding whose exact fields depend on
 * the entity's type and on whether a "full status" or abbreviated report is requested — there is no shared schema
 * object, so the server-side parser must decode these fields in the same order they are appended here.
 */
public class SerializeEntity {

    /**
     * Serializes an {@link Entity}'s status into the MekWars wire format described in the class Javadoc.
     * <p>
     * When {@code fullStatus} is true, the output includes (for non-{@link EjectedCrew} entities) the external ID,
     * owner name, crew hit count, removal condition, a MekWars unit-type code, and a "~"-delimited list of external
     * IDs of entities this one destroyed (kills) — followed by type-specific fields (internal structure per
     * location, cockpit type, battle-damage cost if {@code useRepairs}, and the unit's file name for
     * {@link Mek}/{@link Tank}/{@link Aero}; ride/pickup/destroyed info for a {@link MekWarrior} — i.e. an ejected
     * pilot); and, if the entity is off-board, its off-board distance.
     * <p>
     * When {@code fullStatus} is false, a shorter "in-progress update" form is produced instead: for a
     * {@link MekWarrior} just the ride/pickup/destroyed fields, otherwise the owner name, external ID, removal
     * condition, center-torso/head internal structure (or literal {@code "1*1*"} placeholders for non-{@link Mek}
     * entities), and whether the unit is repairable.
     * <p>
     * Note: if {@link Entity#getExternalId()} cannot be parsed as an integer (MegaMek may assign a UUID string
     * instead), the external ID is silently replaced with {@code -1} rather than propagating the parse failure —
     * this applies both to the entity itself and to each entity in its kill list.
     *
     * @param entity         the entity whose status should be serialized
     * @param fullStatus     true to produce the verbose end-of-game/auto-save report; false to produce the shorter
     *                       in-progress-update form
     * @param forceDevastate true to force the reported removal condition to
     *                       {@link IEntityRemovalConditions#REMOVE_DEVASTATED} regardless of the entity's actual
     *                       removal condition (used when the caller already knows the unit was devastated)
     * @param useRepairs     true to include computed battle-damage/repair-cost information (only consulted for
     *                       {@link Mek} and {@link Tank} entities in the full-status branch)
     * @return the serialized, "*"-delimited status string
     */
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

                if (forceDevastate) {result.append(String.format("%s*", IEntityRemovalConditions.REMOVE_DEVASTATED));} else {
                    result.append(entity.getRemovalCondition()).append("*");
                }

                switch (entity) {
                    case BipedMek ignored -> result.append(String.format("%s*", Unit.MEK));
                    case QuadMek ignored -> result.append(String.format("%s*", Unit.QUAD));
                    case Tank ignored -> result.append(String.format("%s*", Unit.VEHICLE));
                    case ProtoMek ignored -> result.append(String.format("%s*", Unit.PROTOMEK));
                    case BattleArmor ignored -> result.append(String.format("%s*", Unit.BATTLEARMOR));
                    case Aero ignored -> result.append(String.format("%s*", Unit.AERO));
                    default -> result.append(String.format("%s*", Unit.INFANTRY));
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
                case MekWarrior mekWarrior -> {
                    result.append("MW*");
                    result.append(mekWarrior.getOriginalRideExternalId()).append("*");
                    result.append(mekWarrior.getPickedUpByExternalId()).append("*");
                    result.append(mekWarrior.isDestroyed()).append("*");
                }
                default -> {
                }
            }

            if (entity.isOffBoard()) {
                result.append("*").append(entity.getOffBoardDistance());
            }
        }

        /*
         * FullStatus is used when auto-reporting. This status, which sends less information, is used for
         * InProgressUpdates.
         */
        else {
            // if the entity is a MekWarrior, send an IPU command (InProgressUpdate) to the server.
            if (entity instanceof MekWarrior mw) {
                result.append("MW*").append(mw.getOriginalRideExternalId()).append("*");
                result.append(mw.getPickedUpByExternalId()).append("*");
                result.append(mw.isDestroyed()).append("*");
            } else {
                result.append(entity.getOwner().getName()).append("*");
                result.append(externalID).append("*");

                if (forceDevastate) {result.append(String.format("%s*", IEntityRemovalConditions.REMOVE_DEVASTATED));} else {
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
