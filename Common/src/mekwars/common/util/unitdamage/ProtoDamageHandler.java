package common.util.unitdamage;

import megamek.common.Entity;

public class ProtoDamageHandler extends AbstractUnitDamageHandler {

	@Override
	public String buildDamageString(Entity unit, boolean sendAmmo) {
		// Damage to this unit type not yet implemented
		return "%%-%%-%%";
	}

	@Override
	public void applyDamageString(Entity unit, String report, boolean isRepairing) {
		// Damage to this unit type not yet implemented
		return;
	}
}
