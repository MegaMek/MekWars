package common.util.unitdamage;

import megamek.common.Entity;

public abstract class AbstractUnitDamageHandler {
	
	public abstract String buildDamageString(Entity unit, boolean sendAmmo);
	
	public abstract void applyDamageString(Entity unit, String report, boolean isRepairing);
	
	public AbstractUnitDamageHandler () {
	}
}
