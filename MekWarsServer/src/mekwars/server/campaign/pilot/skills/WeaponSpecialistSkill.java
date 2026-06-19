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

/*
 * Created on 18.04.2004
 *
 */
package mekwars.server.campaign.pilot.skills;

import java.util.Enumeration;
import java.util.Hashtable;

import megamek.common.battleValue.BVCalculator;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.Entity;
import mekwars.common.MegaMekPilotOption;
import mekwars.common.Unit;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SHouse;
import mekwars.server.campaign.pilot.SPilot;

/**
 * A pilot who specializes in a particular weapon receives a -2 to hit modifier on all attacks with that weapon.
 *
 * @author Torren (Jason Tighe)
 */

public class WeaponSpecialistSkill extends SPilotSkill {

    public WeaponSpecialistSkill(int id) {
        super(id, "Weapon Specialist", "WS");
        setDescription(
              "A pilot who specializes in a particular weapon receives a -2 to hit modifier on all attacks with that weapon.");
    }

    public WeaponSpecialistSkill() {
        //TODO: replace with ReflectionProvider
    }

    @Override
    public void modifyPilot(Pilot pilot) {
        pilot.addMegaMekOption(new MegaMekPilotOption("weapon_specialist", true));
        //pilot.setBvMod(pilot.getBVMod() +  0.02);
    }

    @Override
    public int getChance(int unitType, Pilot pilot) {
        if (pilot.getSkills().has(this)) {
            return 0;
        }

        String chance = STR."chancefor\{getAbbreviation()}for\{Unit.getTypeClassDesc(unitType)}";

        SHouse house = CampaignMain.campaignMain.getHouseFromPartialString(pilot.getCurrentFaction());

        if (house == null) {
            return CampaignMain.campaignMain.getIntegerConfig(chance);
        }

        return house.getIntegerConfig(chance);
    }

    @Override
    public int getBVMod(Entity unit) {
        //no weapon spec skill for non-meks
        return 0;
    }

    @Override
    public int getBVMod(Entity unit, SPilot pilot) {
        //new bv cost for GunneryX and Weapon Specialist skills,
        //also known as "if it gets a 1 better gunnery with all its weapons then it should pay for the full level of gunnery"
        //the formula applies the "PilotBVSkillMultiplier" delta to (bv% of effected weapons verse all weapons)
        //parallel code is used in GunneryLaserSkill.java, GunneryMissileSkill.java, GunneryBallisticsSkill.java, and WeaponSpecialistSkill.java
        double sumWeaponBV = 0;
        double effectedWeaponBV = 0;
        double bvSkillDelta =
              BVCalculator.bvSkillMultiplier(unit.getCrew().getGunnery() - 2, unit.getCrew().getPiloting())
                    / BVCalculator.bvSkillMultiplier(unit.getCrew().getGunnery(), unit.getCrew().getPiloting());
        for (WeaponMounted weapon : unit.getWeaponList()) {
            sumWeaponBV += weapon.getType().getBV(unit);
            if (weapon.getName().equalsIgnoreCase(pilot.getWeapon())) {
                effectedWeaponBV += weapon.getType().getBV(unit);
            }
        }
        return (int) (unit.calculateBattleValue(false, true) * (effectedWeaponBV / sumWeaponBV) * (bvSkillDelta - 1));
    }

    public void assignWeapon(Entity entity, Pilot pilot) {
        Hashtable<String, Boolean> uniqueWeapons = new Hashtable<>();
        String bannedWeapons = CampaignMain.campaignMain.getConfig("BannedWSWeapons");
        for (WeaponMounted m : entity.getWeaponList()) {
            if (bannedWeapons.contains(m.getDesc())) {
                continue;
            }
            uniqueWeapons.put(m.getName(), true);
        }

        int selectedWeapon = 0;

        if (uniqueWeapons.isEmpty()) {
            return;
        }

        if (uniqueWeapons.size() > 1) {
            selectedWeapon = CampaignMain.campaignMain.getRandomNumber(uniqueWeapons.size());
        }


        for (Enumeration<String> e = uniqueWeapons.keys(); e.hasMoreElements(); selectedWeapon--) {
            String weaponName = e.nextElement();
            if (selectedWeapon == 0) {
                pilot.setWeapon(weaponName);
                break;
            }
        }

    }
}
