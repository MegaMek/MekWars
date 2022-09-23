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
package server.campaign.pilot.skills;

import common.MegaMekPilotOption;
import common.Unit;
import common.campaign.pilot.Pilot;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Mech;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.pilot.SPilot;

/**
 * @author Helge Richter
 */
public class MeleeSpecialistSkill extends SPilotSkill {

    public MeleeSpecialistSkill(int id) {
        super(id, "Melee Specialist", "MS");
        setDescription("Enables the unit to do 1 additional point of damage with physical attacks and subtracts one from the attacker movement modifier (to a minimum of zero). Note: This ability is only used for BattleMechs.");
    }

    public MeleeSpecialistSkill() {
        // TODO: replace with ReflectionProvider
    }

    @Override
    public void modifyPilot(Pilot p) {
        p.addMegamekOption(new MegaMekPilotOption("melee_specialist", true));
        // p.setBvMod(p.getBVMod() + 0.03);
    }

    @Override
    public int getBVMod(Entity unit) {
        return 0;
    }

    @Override
    public int getBVMod(Entity unit, SPilot p) {
        double tonnage = unit.getWeight();
        double numberOfHatchets = 0;
        double hatchetMod = CampaignMain.cm.getDoubleConfig("HatchetRating");
        double baseBV = CampaignMain.cm.getDoubleConfig("MeleeSpecialistBaseBVMod");
        double speedFactor; 
        if(CampaignMain.cm.getBooleanConfig("MeleeSpecialistUseSpeedFactor")) {
        	// Adds a BV malus based on unit movement capability
        	speedFactor = Math.pow(1 + ((((double) unit.getRunMP() + (Math.round(Math.max(unit.getJumpMP(), unit.getActiveUMUCount()) / 2.0))) - 5) / 10), 1.2);
        } else {
        	speedFactor = 1.0;
        }

        SHouse house = CampaignMain.cm.getHouseFromPartialString(p.getCurrentFaction());

        if (house == null) {
            hatchetMod = CampaignMain.cm.getDoubleConfig("HatchetRating");
            baseBV = CampaignMain.cm.getDoubleConfig("MeleeSpecialistBaseBVMod");
        } else {
            hatchetMod = house.getDoubleConfig("HatchetRating");
            baseBV = house.getDoubleConfig("MeleeSpecialistBaseBVMod");
        }

        if (!(unit instanceof Mech) && !(unit instanceof BattleArmor)) {
            return 0;
        }

        numberOfHatchets = unit.getClubs().size();
        
        double total = baseBV * ((tonnage / 10) * speedFactor) + (hatchetMod * numberOfHatchets);
        return (int) total;
    }

    @Override
    public int getChance(int unitType, Pilot p) {
        if (p.getSkills().has(this)) {
            return 0;
        }

        if (unitType != Unit.MEK) {
            return 0;
        }

        String chance = "chancefor" + getAbbreviation() + "for" + Unit.getTypeClassDesc(unitType);

        SHouse house = CampaignMain.cm.getHouseFromPartialString(p.getCurrentFaction());

        if (house == null) {
            return CampaignMain.cm.getIntegerConfig(chance);
        }

        return house.getIntegerConfig(chance);
    }
}