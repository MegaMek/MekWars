package common.util.unitdamage;

import megamek.common.Entity;

public class GenericDamageHandler extends AbstractUnitDamageHandler {

	@Override
	public String buildDamageString(Entity unit, boolean sendAmmo) {
		// Nothing really to do here.  We've not implemented whatever
		// unit type this is attached to
		return "%%-%%-%%";
	}

	@Override
	public void applyDamageString(Entity unit, String report, boolean isRepairing) {
		// Nothing to do here - this type doesn't take damage
		return;
	}

}
