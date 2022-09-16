package common.util.unitdamage;

import java.util.Iterator;
import java.util.StringTokenizer;

import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.IArmorState;
import megamek.common.Mounted;
import megamek.common.Tank;

public class VehicleDamageHandler extends AbstractUnitDamageHandler {

	@Override
	public String buildDamageString(Entity unit, boolean sendAmmo) {
        StringBuilder result = new StringBuilder();
        String delimiter = "-";
        String delimiter2 = "%";
        boolean hasData = false;

        try {
            // External armor
            for (int loc = Tank.LOC_BODY; loc <= Tank.LOC_TURRET; loc++) {

                if (unit.getArmor(loc) == unit.getOArmor(loc)) {
                    continue;
                }
                hasData = true;
                result.append(loc);
                result.append(delimiter2);
                if (unit.getArmor(loc) < 0) {
                    result.append(0);
                } else {
                    result.append(unit.getArmor(loc));
                }
                result.append(delimiter2);

            }
            if (!hasData) {
                result.append(delimiter2);
                result.append(delimiter2);
            }
            result.append(delimiter);
            hasData = false;
            // Internal Armor
            for (int loc = Tank.LOC_BODY; loc <= Tank.LOC_TURRET; loc++) {

                if (unit.getInternal(loc) == unit.getOInternal(loc)) {
                    continue;
                }
                result.append(loc);
                result.append(delimiter2);
                if (unit.getInternal(loc) < 0) {
                    result.append(0);
                } else {
                    result.append(unit.getInternal(loc));
                }
                result.append(delimiter2);
                hasData = true;

            }
            if (!hasData) {
                result.append(delimiter2);
                result.append(delimiter2);
            }
            result.append(delimiter);
            hasData = false;
            // Crits report both Systems and Equipment
            // * for Damaged X for Breached
            // location#Crit Number#Damage
            for (int x = 0; x < unit.locations(); x++) {
                for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                    CriticalSlot cs = unit.getCritical(x, y);
                    if (cs == null) {
                        continue;
                    }

                    if (UnitUtils.isNonRepairableCrit(unit, cs)) {
                        continue;
                    }

                    if (cs.isRepairing()) {
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("!");
                        result.append(delimiter2);
                        hasData = true;
                    }

                    // the cs is damaged lets see if the whole Mounted has taken
                    // enough
                    // damage to it to be labeled destroyed
                    if (cs.isDamaged() && UnitUtils.isDestroyedOrDamaged(unit, cs)) {
                        cs.setMissing(true);
                    }

                    /*
                     * Mounted mount = cs.getMount(); //makes
                     * sure that a destroyed split weapon is marked destroyed
                     * //in all locations --Torren if ( mount != null &&
                     * mount.isSplitable() && mount.isSplit()){
                     * cs.setMissing(mount.isMissing());
                     * cs.setDestroyed(mount.isDestroyed());
                     * cs.setBreached(mount.isBreached()); }
                     */
                    // Missing items do not need to worry about damage or
                    // breach.
                    if (cs.isMissing()) {
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("@");
                        result.append(delimiter2);
                        hasData = true;
                    } else {
                        if (cs.isDamaged()) {
                            result.append(x);
                            result.append(delimiter2);
                            result.append(y);
                            result.append(delimiter2);
                            result.append("^");
                            result.append(delimiter2);
                            hasData = true;
                        }
                        if (cs.isBreached()) {
                            result.append(x);
                            result.append(delimiter2);
                            result.append(y);
                            result.append(delimiter2);
                            result.append("X");
                            result.append(delimiter2);
                            hasData = true;
                        }
                    }
                }
            }

            if (!hasData) {
                result.append(delimiter2);
                result.append(delimiter2);
            }
            result.append(delimiter);
            hasData = false;

            if (sendAmmo) {
                int location = 0;
                for (Mounted weap : unit.getAmmo()) {
                    if (weap.isDestroyed()) {
                        hasData = true;
                        result.append(location);
                        result.append(delimiter2);
                        result.append(0);
                        result.append(delimiter2);
                    } else if (weap.getUsableShotsLeft() != UnitUtils.getShots(weap)) {
                        hasData = true;
                        result.append(location);
                        result.append(delimiter2);
                        result.append(Math.max(0, weap.getUsableShotsLeft()));
                        result.append(delimiter2);
                    }

                    location++;
                }
            }

            if (!hasData) {
                result.append(delimiter2);
                result.append(delimiter2);
            }
            result.append(delimiter);

        } catch (Exception ex) {
            MWLogger.errLog("Entity: " + unit.getShortNameRaw());
            MWLogger.errLog(ex);
            return "%%-%%-%%";
        }
        return result.toString();

	}

	@Override
	public void applyDamageString(Entity unit, String report, boolean isRepairing) {
		// MWLogger.errLog(System.currentTimeMillis()+" Unit "+unit.getModel()+" applyBattleDamage: "+report);
        StringTokenizer entry = new StringTokenizer(report, "-");

        StringTokenizer externalArmor = new StringTokenizer(entry.nextToken(), "%");
        StringTokenizer internalArmor = new StringTokenizer(entry.nextToken(), "%");
        StringTokenizer crits = new StringTokenizer(entry.nextToken(), "%");
        StringTokenizer ammo = null;

        if (entry.hasMoreTokens()) {
            ammo = new StringTokenizer(entry.nextToken(), "%");
        }

        while (externalArmor.hasMoreTokens()) {
            int location = Integer.parseInt(externalArmor.nextToken());
            int armor = Integer.parseInt(externalArmor.nextToken());
            unit.setArmor(armor, location);
            if (!isRepairing) {
                UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, location);
            }
        }

        while (internalArmor.hasMoreTokens()) {
            int location = Integer.parseInt(internalArmor.nextToken());
            int armor = Integer.parseInt(internalArmor.nextToken());
            if (armor <= 0) {
                armor = IArmorState.ARMOR_DESTROYED;
            }
            unit.setInternal(armor, location);
            if (!isRepairing) {
                UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_INTERNAL_ARMOR, location);
            }
        }

        while (crits.hasMoreTokens()) {
            int location = Integer.parseInt(crits.nextToken());
            int slot = Integer.parseInt(crits.nextToken());
            String damageType = crits.nextToken();

            CriticalSlot critSlot = unit.getCritical(location, slot);

            if (damageType.equals("@")) {
                critSlot.setMissing(true);
                if (critSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted mounted = critSlot.getMount();
                    // check to see if it has ammo. If so set it 0 as the
                    // ammobin has gone bye bye.
                    if (mounted.getUsableShotsLeft() > 0) {
                        mounted.setShotsLeft(0);
                    }
                }
            }
            if (damageType.equals("^")) {
                critSlot.setDestroyed(true);
                if (critSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted mounted = critSlot.getMount();
                    // check to see if it has ammo. If so set it 0 as the
                    // ammobin has gone bye bye.
                    if (mounted.getUsableShotsLeft() > 0) {
                        mounted.setShotsLeft(0);
                    }
                }
            }
            if (damageType.equals("!")) {
                if (isRepairing) {
                    critSlot.setRepairing(true);
                    if (!critSlot.isBreached() && !critSlot.isDamaged()) {
                        critSlot.setDestroyed(true);
                        if (critSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                            Mounted mounted = critSlot.getMount();
                            // check to see if it has ammo. If so set it 0 as
                            // the ammobin has gone bye bye.
                            if (mounted.getUsableShotsLeft() > 0) {
                                mounted.setShotsLeft(0);
                            }
                        }
                    }
                }
            }
            if (damageType.equals("X")) {
                critSlot.setBreached(true);
            }
            unit.setCritical(location, slot, critSlot);
        }

        if ((ammo != null) && ammo.hasMoreTokens()) {
            int locationCount = 0;
            Iterator<Mounted> munitions = unit.getAmmo().iterator();

            // make sure the unit actually has ammo.
            if (munitions.hasNext()) {

                Mounted weapon = munitions.next();

                try {
                    while (ammo.hasMoreTokens()) {
                        int location = Integer.parseInt(ammo.nextToken());
                        int ammoLeft = Integer.parseInt(ammo.nextToken());

                        while (location != locationCount) {
                            weapon = munitions.next();
                            locationCount++;
                        }

                        weapon.setShotsLeft(ammoLeft);
                    }
                } catch (Exception ex) {
                    MWLogger.errLog("Error while parsing ammo Moving along");
                }
            }
        }
	}

}
