/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package server.campaign.commands;

import java.util.StringTokenizer;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Mounted;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;

public class SetUnitAmmoCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
                return;
            }
        }

        SPlayer p = CampaignMain.cm.getPlayer(Username);

        int unitid = 0;// ID# of the mech which is to set ammo change
        int weaponType = 0;// Standard weapon
        String ammoName = ""; // Standard Ammo
        int weaponLocation = 0; // starting position for weapon
        int shots = 100; // more then 0 0 = dumpped ammo
        int shotsLeft = 0;
        boolean hotloaded = false;
        SUnit unit = null;
        SHouse faction = null;
        Entity en = null;
        boolean usingCrits = CampaignMain.cm.getBooleanConfig("UsePartsRepair");

        try {
            unitid = Integer.parseInt(command.nextToken());
            weaponLocation = Integer.parseInt(command.nextToken());
            weaponType = Integer.parseInt(command.nextToken());
            ammoName = command.nextToken();
            shots = Integer.parseInt(command.nextToken());
            hotloaded = Boolean.parseBoolean(command.nextToken());
            unit = p.getUnit(unitid);
            faction = p.getMyHouse();
            en = unit.getEntity();
        } catch (NumberFormatException ex) {
            CampaignMain.cm.toUser("AM:SetUnitAmmo command failed. Check your input. It should be something like this: /c setUnitAmmo#unitid#weaponlocation#weaponType#ammoname#rounds", Username, true);
            return;
        } catch (Exception e) {
            return;
        }

        int location = 0;
        Mounted mWeapon = null;

        for (Mounted Weapon : en.getAmmo()) {
            if (location == weaponLocation) {
                mWeapon = Weapon;
                break;
            }
            location++;
        }

        if (mWeapon == null) {
            return;
        }

        AmmoType currAmmo = (AmmoType) mWeapon.getType();
        AmmoType at = unit.getEntityAmmo(weaponType, ammoName);

        if (mWeapon.isHotLoaded() != hotloaded) {
            mWeapon.setHotLoad(hotloaded);
            unit.setEntity(en);
            String result = at.getName() + "(" + en.getLocationAbbr(mWeapon.getLocation()) + ")";
            if (hotloaded) {
                result += " hot-loaded.";
            } else {
                result += " safties engaged.";
            }
            CampaignMain.cm.toUser(result, Username);
            CampaignMain.cm.toUser("PL|UU|" + unit.getId() + "|" + unit.toString(true), Username, false);
        }

        shotsLeft = mWeapon.getUsableShotsLeft();

        if (currAmmo.getInternalName().equalsIgnoreCase(at.getInternalName()) && (shots == shotsLeft)) {
            return;
        }

        if (shots == 0) {// dumping ammo

            if (usingCrits) {
                p.updatePartsCache(currAmmo.getInternalName(), mWeapon.getUsableShotsLeft());
            }

            mWeapon.changeAmmoType(at);
            mWeapon.setShotsLeft(0);

            unit.setEntity(en);

            // Don't have to set BV to 0 and recalculate in this class -
            // unit.toString(true) does it for us.
            CampaignMain.cm.toUser("PL|UU|" + unit.getId() + "|" + unit.toString(true), Username, false);
            p.checkAndUpdateArmies(unit);
            CampaignMain.cm.toUser("AM:Ammo dumped. BV Recalculated", Username, true);
            return;
        }

        String munitionType = Long.toString(at.getMunitionType());

        // dont make players confirm the command on a server which doesnt charge
        // for ammo
        double ammoCharge = CampaignMain.cm.getAmmoCost(at.getInternalName());

        if ((CampaignMain.cm.getData().getServerBannedAmmo().get(munitionType) != null) || (faction.getBannedAmmo().get(munitionType) != null) || ((ammoCharge < 0) && !usingCrits)) {
            CampaignMain.cm.toUser("AM:<font color=green>Quartermaster Command regretfully informs you that " + at.getName() + " is out of stock.</font>", Username, true);
            return;
        }

        String strConfirm = "";
        if (command.hasMoreTokens()) {
            strConfirm = command.nextToken();
        }

        if ((ammoCharge > 0) || usingCrits) {

            int refillShots = at.getShots();
            
            if (mWeapon.byShot()) {
            	refillShots = mWeapon.getOriginalShots();
            }

            if (unit.getEntity() instanceof BattleArmor) {
                refillShots = getWeaponRefillShots(unit, mWeapon);
            }

            if (!currAmmo.getInternalName().equalsIgnoreCase(at.getInternalName())) {
                shotsLeft = 0;
            }

            // nothing to refill so just return
            if (shotsLeft == refillShots) {
                return;
            }
            int fullMagazine = refillShots;
            // Single shot weapons should only cost 1 shot.
            if (mWeapon.getLocation() == Entity.LOC_NONE) {
                refillShots = 1;
            }// Partial Reloads
            else {
                refillShots -= shotsLeft;
                ammoCharge *= refillShots;
            }

            int loc = 0;
            if (mWeapon.getLocation() == Entity.LOC_NONE) {
                // oneshot weapons don't have a location of their own
                Mounted linkedBy = mWeapon.getLinkedBy();
                loc = linkedBy.getLocation();
            } else {
                loc = mWeapon.getLocation();
            }

            if (usingCrits) {
                ammoCharge = 0;
                // unload all of old ammo
                p.getUnitParts().add(currAmmo.getInternalName(), mWeapon.getUsableShotsLeft());
                int newAmmoAmount = p.getPartsAmount(at.getInternalName());

                if (p.getAutoReorder() && (newAmmoAmount < refillShots)) {
                    String newCommand = at.getInternalName() + "#" + (refillShots - newAmmoAmount);
                    CampaignMain.cm.getServerCommands().get("BUYPARTS").process(new StringTokenizer(newCommand, "#"), Username);
                    newAmmoAmount = p.getPartsAmount(at.getInternalName());
                }

                if (newAmmoAmount == 0) {
                    String result = "After unloading " + currAmmo.getDesc() + "(" + en.getLocationAbbr(loc) + ") from unit #" + unit.getId() + " " + unit.getModelName() + " your techs realize you do not have any " + at.getDesc() + " to reload with!";
                    CampaignMain.cm.toUser(result, Username);
                } else if (newAmmoAmount < fullMagazine) {
                    String result = "After unloading " + currAmmo.getDesc() + "(" + en.getLocationAbbr(loc) + ") from unit #" + unit.getId() + " " + unit.getModelName() + " your techs realize you only had " + newAmmoAmount + " rounds of " + at.getDesc() + " to reload with!";
                    CampaignMain.cm.toUser(result, Username);
                } else {
                    CampaignMain.cm.toUser("AM:Ammo set for " + unit.getModelName() + " (#" + unit.getId() + ").", Username, true);
                    newAmmoAmount = refillShots;
                }
                p.updatePartsCache(currAmmo.getInternalName(), mWeapon.getUsableShotsLeft());
                p.updatePartsCache(at.getInternalName(), -newAmmoAmount);
                mWeapon.changeAmmoType(at);
                mWeapon.setShotsLeft(newAmmoAmount);
                unit.setEntity(en);
                p.checkAndUpdateArmies(unit);
                CampaignMain.cm.toUser("PL|UU|" + unit.getId() + "|" + unit.toString(true), Username, false);

                CampaignMain.cm.toUser("AM:Ammo set for " + unit.getModelName() + " (#" + unit.getId() + ").", Username, true);
                return;
            }
            int cost = (int) Math.ceil(ammoCharge);

            // check the confirmation
            if (!strConfirm.equals("CONFIRM")) {
                String result = "AM:Quartermaster command will charge you " + CampaignMain.cm.moneyOrFluMessage(true, false, cost) + " to change the load out on #" + unit.getId() + " " + unit.getModelName() + "<br>from " + currAmmo.getDesc() + "(" + en.getLocationAbbr(loc) + " " + shotsLeft + "/" + fullMagazine + ") to " + at.getDesc() + "(" + refillShots + "/" + refillShots + ").";
                result += "<br><a href=\"MEKWARS/c setunitammo#" + unitid + "#" + weaponLocation + "#" + weaponType + "#" + ammoName + "#" + fullMagazine + "#" + hotloaded + "#CONFIRM";
                result += "\">Click here to change the ammo.</a>";
                CampaignMain.cm.toUser(result, Username, true);
                return;
            }

            if (p.getMoney() < cost) {
                CampaignMain.cm.toUser("AM:Changing ammo costs " + CampaignMain.cm.moneyOrFluMessage(true, false, cost, false) + ", but you only have " + p.getMoney() + ".", Username, true);
                return;
            }

            p.addMoney(-cost);
        }// end else(check for confirmation)

        mWeapon.changeAmmoType(at);
        unit.setEntity(en);
        p.checkAndUpdateArmies(unit);
        CampaignMain.cm.toUser("PL|UU|" + unit.getId() + "|" + unit.toString(true), Username, false);

        CampaignMain.cm.toUser("AM:Ammo set for " + unit.getModelName() + " (#" + unit.getId() + ").", Username, true);
    }// end process()

    private int getWeaponRefillShots(SUnit unit, Mounted weapon) {
        int shots = 0;

        if (weapon.getLocation() == Entity.LOC_NONE) {
            return 1;
        }

        Entity tempEnt = SUnit.loadMech(unit.getUnitFilename());

        for (Mounted ammoWeapon : tempEnt.getWeaponList()) {
            if (ammoWeapon.equals(weapon)) {
                shots = ammoWeapon.getUsableShotsLeft();
                break;
            }
        }

        return shots;
    }
}// end SetMaintainedCommand class
