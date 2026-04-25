package mekwars.common.util.unitdamage;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import mekwars.common.util.MWLogger;

public final class UnitDamageHandlerFactory {
    public static AbstractUnitDamageHandler getHandler(Entity e) {
        if (e instanceof Mek) {
            return new MekDamageHandler();
        }

        if (e instanceof BattleArmor) {
            return new BattleArmorDamageHandler();
        }

        if (e instanceof Aero) {
            return new AeroDamageHandler();
        }

        if (e instanceof ProtoMek) {
            return new ProtoDamageHandler();
        }

        if (e instanceof Tank) {
            return new VehicleDamageHandler();
        }

        if (e instanceof Infantry) {
            return new InfantryDamageHandler();
        }

        MWLogger.errLog("Unknown Unit Type in UnitDamageHandlerFactory.getHandler(): " + e.getModel());

        return new GenericDamageHandler();
    }
}
