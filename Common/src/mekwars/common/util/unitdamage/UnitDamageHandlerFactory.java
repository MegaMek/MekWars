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

public final class UnitDamageHandlerFactory {
    private static final MMLogger LOGGER = MMLogger.create(UnitDamageHandlerFactory.class);

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
