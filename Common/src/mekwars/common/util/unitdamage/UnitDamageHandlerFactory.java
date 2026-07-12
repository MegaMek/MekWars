/*
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
package mekwars.common.util.unitdamage;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;

/**
 * Factory that selects the correct {@link AbstractUnitDamageHandler} strategy implementation
 * for a given {@link Entity} based on its runtime type.
 * <p>
 * This is the single entry point callers should use to obtain a damage handler; callers should
 * not instantiate handler subclasses directly. The type checks are ordered so that more
 * specific/derived MegaMek entity types are checked before broader ones (e.g. {@link ProtoMek}
 * before {@link Tank}), since some MegaMek unit classes participate in overlapping inheritance
 * hierarchies.
 *
 * @see AbstractUnitDamageHandler
 */
public final class UnitDamageHandlerFactory {
    private static final MMLogger LOGGER = MMLogger.create(UnitDamageHandlerFactory.class);

    /**
     * Returns a new damage handler appropriate for the runtime type of {@code entity}.
     * <p>
     * Note: only {@link Mek} and {@link Tank} entities are backed by a fully implemented
     * handler ({@link MekDamageHandler} and {@link VehicleDamageHandler} respectively); the
     * {@link Aero}, {@link BattleArmor}, {@link ProtoMek}, and {@link Infantry} handlers are
     * currently stubs that report/apply no damage (see their class Javadoc). If {@code entity}
     * does not match any known type, this logs an error and falls back to
     * {@link GenericDamageHandler}, which is also a no-op.
     *
     * @param entity the entity to inspect; its concrete type determines which handler is
     *               returned
     * @return a new {@link AbstractUnitDamageHandler} instance matching {@code entity}'s type,
     *         or a {@link GenericDamageHandler} if the type is unrecognized
     */
    public static AbstractUnitDamageHandler getHandler(Entity entity) {
        if (entity instanceof Mek) {
            return new MekDamageHandler();
        }

        if (entity instanceof BattleArmor) {
            return new BattleArmorDamageHandler();
        }

        if (entity instanceof Aero) {
            return new AeroDamageHandler();
        }

        if (entity instanceof ProtoMek) {
            return new ProtoDamageHandler();
        }

        if (entity instanceof Tank) {
            return new VehicleDamageHandler();
        }

        if (entity instanceof Infantry) {
            return new InfantryDamageHandler();
        }

        LOGGER.error("Unknown Unit Type in UnitDamageHandlerFactory.getHandler(): {}", entity.getModel());

        return new GenericDamageHandler();
    }
}
