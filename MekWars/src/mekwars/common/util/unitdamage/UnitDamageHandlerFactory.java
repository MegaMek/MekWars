package common.util.unitdamage;

import common.util.MWLogger;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Tank;

public final class UnitDamageHandlerFactory {
	public static AbstractUnitDamageHandler getHandler(Entity e) {
		if (e instanceof Mech) {
			return new MekDamageHandler();
		}
		
		if (e instanceof BattleArmor) {
			return new BattleArmorDamageHandler();
		}
		
		if (e instanceof Aero) {
			return new AeroDamageHandler();
		}
		
		if (e instanceof Protomech) {
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
