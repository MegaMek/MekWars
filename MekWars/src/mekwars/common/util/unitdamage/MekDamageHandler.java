package common.util.unitdamage;

import java.util.Iterator;
import java.util.StringTokenizer;

import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.IArmorState;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;


public class MekDamageHandler extends AbstractUnitDamageHandler {

	@Override
	public String buildDamageString(Entity unit, boolean sendAmmo) {
		StringBuilder result = new StringBuilder();
        String delimiter = "-";
        String delimiter2 = "%";
        boolean hasData = false;

        try {
            // External armor
            for (int loc = Mech.LOC_HEAD; loc <= Mech.LOC_LLEG; loc++) {

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
            if (unit.getArmor(Mech.LOC_CT, true) != unit.getOArmor(Mech.LOC_CT, true)) {
                result.append(UnitUtils.LOC_CTR);
                result.append(delimiter2);
                if (unit.getArmor(Mech.LOC_CT, true) < 0) {
                    result.append(0);
                } else {
                    result.append(unit.getArmor(Mech.LOC_CT, true));
                }
                result.append(delimiter2);
                hasData = true;
            }
            if (unit.getArmor(Mech.LOC_LT, true) != unit.getOArmor(Mech.LOC_LT, true)) {
                result.append(UnitUtils.LOC_LTR);
                result.append(delimiter2);
                if (unit.getArmor(Mech.LOC_LT, true) < 0) {
                    result.append(0);
                } else {
                    result.append(unit.getArmor(Mech.LOC_LT, true));
                }
                result.append(delimiter2);
                hasData = true;
            }
            if (unit.getArmor(Mech.LOC_RT, true) != unit.getOArmor(Mech.LOC_RT, true)) {
                result.append(UnitUtils.LOC_RTR);
                result.append(delimiter2);
                if (unit.getArmor(Mech.LOC_RT, true) < 0) {
                    result.append(0);
                } else {
                    result.append(unit.getArmor(Mech.LOC_RT, true));
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
            // Internal Armor
            for (int loc = Mech.LOC_HEAD; loc <= Mech.LOC_LLEG; loc++) {

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
                int shieldHitsLeft = -1;
                // Need to get IS amount - critical slots marked as missing because
                // of a blown-off arm or what have you should not be marked missing for
                // MW purposes.  If it's missing, and there is IS left, it should
                // be unmarked instead.
                boolean hasISLeft = (unit.getInternal(x)>0);
                
                for (int y = 0; y < unit.getNumberOfCriticals(x); y++) {
                    CriticalSlot cs = unit.getCritical(x, y);

                    
                    if (cs == null) {
                        continue;
                    }

                    if (UnitUtils.isNonRepairableCrit(unit, cs)) {
                        continue;
                    }

                    Mounted m = cs.getMount();
                    if ((m != null) && (m.getType() instanceof MiscType) && ((MiscType) m.getType()).isShield() && (m.getBaseDamageCapacity() != m.getCurrentDamageCapacity(unit, x)) && (shieldHitsLeft == -1) && ((x == Mech.LOC_LARM) || (x == Mech.LOC_RARM))) {
                        float shieldcrits = Math.max(1, UnitUtils.getNumberOfCrits(unit, cs));
                        float basePoints = m.getBaseDamageCapacity();
                        float currentPoints = m.getCurrentDamageCapacity(unit, x);
                        float tempHits = 0;

                        tempHits = shieldcrits / basePoints;
                        tempHits *= currentPoints;

                        tempHits = Math.abs(tempHits - shieldcrits);

                        shieldHitsLeft = Math.max(1, Math.round(tempHits));
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

                    // Crit can only be missing or damaged or Breached.
                    if (cs.isDamaged()) { // Moving this to the head of the line - isMissing seems to be overriding damaged.
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("^");
                        result.append(delimiter2);
                        hasData = true;
                    } else if (cs.isMissing() && !hasISLeft) {  // Experimental addition of hasISLeft - if testing doesn't work, remove it
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("@");
                        result.append(delimiter2);
                        hasData = true;
                    } else if (cs.isBreached()) {
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("X");
                        result.append(delimiter2);
                        hasData = true;
                    } else if ((m != null) && (m.getType() instanceof MiscType) && ((MiscType) m.getType()).isShield() && ((x == Mech.LOC_LARM) || (x == Mech.LOC_RARM)) && (shieldHitsLeft > 0)) {
                        result.append(x);
                        result.append(delimiter2);
                        result.append(y);
                        result.append(delimiter2);
                        result.append("^");
                        result.append(delimiter2);
                        hasData = true;
                        shieldHitsLeft--;
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
                    int shots = 0;
                    if (weap.byShot()) {
                    	shots = weap.getOriginalShots();
                    } else {
                    	shots = ((AmmoType) weap.getType()).getShots();
                    }
                    if (weap.isDestroyed()) {
                        hasData = true;
                        result.append(location);
                        result.append(delimiter2);
                        result.append(0);
                        result.append(delimiter2);
                    } else if (weap.getUsableShotsLeft() != shots) {
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
            if (location >= UnitUtils.LOC_CTR) {
                unit.setArmor(armor, location - 7, true);
                if (!isRepairing) {
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_REAR_ARMOR, location - 7);
                }
            } else {
                unit.setArmor(armor, location);
                if (!isRepairing) {
                    UnitUtils.removeArmorRepair(unit, UnitUtils.LOC_FRONT_ARMOR, location);
                }
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
            try {
                int location = Integer.parseInt(crits.nextToken());
                int slot = Integer.parseInt(crits.nextToken());
                String damageType = crits.nextToken();

                CriticalSlot critSlot = unit.getCritical(location, slot);

                if (damageType.equals("@")) {
                    critSlot.setMissing(true);
                    if (critSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted mounted = critSlot.getMount();
                        mounted.setMissing(true);
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
                        mounted.setDestroyed(true);
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
                                mounted.setDestroyed(true);
                                // check to see if it has ammo. If so set it 0
                                // as the ammobin has gone bye bye.
                                if (mounted.getUsableShotsLeft() > 0) {
                                    mounted.setShotsLeft(0);
                                }
                            }
                        }
                    }
                }
                if (damageType.equals("X")) {
                    critSlot.setBreached(true);
                    if (critSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted mounted = critSlot.getMount();
                        mounted.setBreached(true);
                    }
                }
                unit.setCritical(location, slot, critSlot);
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
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
