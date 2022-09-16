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

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;

public class SetUnitBurstCommand implements Command {

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
        int weaponLocation = 0; // starting position for weapon
        int weaponSlot = 0;
        boolean selection = false; // burst on or off

        try {
            unitid = Integer.parseInt(command.nextToken());
        }// end try
        catch (NumberFormatException ex) {
            CampaignMain.cm.toUser("AM:SetBurstAmmo command failed. No Unit in your input. It should be something like this: /c SetunitBurst#unitid#weaponlocation#slot#true/false", Username, true);
            return;
        }// end catch

        try {
            weaponLocation = Integer.parseInt(command.nextToken());
        }// end try
        catch (Exception ex) {
            CampaignMain.cm.toUser("AM:SetBurstAmmo command failed. Weapon Location invalid. It should be something like this: /c SetunitBurst#unitid#weaponlocation#slot#true/false", Username, true);
            return;
        }// end catch

        try {
            weaponSlot = Integer.parseInt(command.nextToken());
        }// end try
        catch (Exception ex) {
            CampaignMain.cm.toUser("AM:SetBurstAmmo command failed. WeaponSlot invalid. It should be something like this: /c SetunitBurst#unitid#weaponlocation#slot#true/false", Username, true);
            return;
        }// end catch

        try {
            selection = Boolean.parseBoolean(command.nextToken());
        }// end try
        catch (Exception ex) {
            CampaignMain.cm.toUser("AM:SetBurstAmmo command failed. Boolean not found. It should be something like this: /c SetunitBurst#unitid#weaponlocation#slot#true/false", Username, true);
            return;
        }// end catch

        SUnit unit = p.getUnit(unitid);
        Entity en = unit.getEntity();
		if(en == null)
		{
            CampaignMain.cm.toUser("AM:SetBurstAmmo command failed. entity for unit is null", Username, true);
            return;
			
		}	
		
        //try {
		CriticalSlot crit = en.getCritical(weaponLocation, weaponSlot);
		Mounted mWeapon = crit.getMount();
		
		//int index = en.getCritical(weaponLocation, weaponSlot).getIndex();
//		CampaignMain.cm.toUser("AM:" + crit. + " is the index of the gun be set to rapid fire!", Username, true);
        
	//	Mounted mWeapon = en.getEquipment(index);
            //Mounted mWeapon = en.getEquipment(en.getCritical(weaponLocation, weaponSlot).getIndex());

        if (mWeapon == null) {
			CampaignMain.cm.toUser("AM:"+ weaponLocation + "," + weaponSlot + " is not a gun!", Username, true);
            return;
        }
            if (!mWeapon.getType().hasFlag(WeaponType.F_MG)) {
                CampaignMain.cm.toUser("AM:" + mWeapon.getName() + " cannot be set to rapid fire!", Username, true);
                return;
            }
            if (mWeapon.isRapidfire() == selection) {
                return;
            }
            mWeapon.setRapidfire(selection);
        //} catch (Exception ex) {
//            CampaignMain.cm.toUser("AM:SetBurstAmmo command failed. something else is wrong, you asked to set burst for" + unit.getId() + "|" + weaponLocation + "|" + weaponSlot + "|" + selection, Username, true);
        //    return;
        //}

        unit.setEntity(en);
        CampaignMain.cm.toUser("PL|UUMG|" + unit.getId() + "|" + weaponLocation + "|" + weaponSlot + "|" + selection, Username, false);

        CampaignMain.cm.toUser("AM:Rapid fire set for " + unit.getModelName() + " (#" + unit.getId() + ").", Username, true);

    }// end process()
}// end SetMaintainedCommand class
