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

package server.campaign.pilot.skills;

import common.MegaMekPilotOption;
import common.Unit;
import common.campaign.pilot.Pilot;
import megamek.common.Entity;
import megamek.common.Mech;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.pilot.SPilot;
/**
 * @author Helge Richter
 */
public class PainResistanceSkill extends SPilotSkill {

    public PainResistanceSkill() {
        // TODO: replace with ReflectionProvider
    }

    public PainResistanceSkill(int id) {
        super(id, "Pain Resistance", "PR");
        setDescription("When making consciousness rolls, 1 is added to all rolls. Also, damage received from ammo explosions is reduced to 1. Note: This ability is only used for BattleMechs.");
    }

    @Override
    public void modifyPilot(Pilot p) {
        // super.addToPilot(p);
        p.addMegamekOption(new MegaMekPilotOption("pain_resistance", true));
        p.setBvMod(p.getBVMod() + 0.01);
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

    @Override
    public int getBVMod(Entity unit) {
    	//BK - changing PR costs to % of bv, doubled for CASE/CASEII units
    	//this is because in Megamek, PR gives +1 on any KO roll (hidden modifier) and reduces pilot hits from 2 to 1 for any ammo explosion
    	//The previous PR costs were 30 or 40 per ammo bin which is silly if you have no 
    	//CASE (mek gets gutted anyways) and silly if you have nothing to explode (free +1 on KO rolls)
        int PainResistanceBVBaseMod = CampaignMain.cm.getIntegerConfig("PainResistanceBaseBVMod");
        boolean b = false;
        if (unit instanceof Mech) { //BK - this section of code is tested! ty STK9A
        	Mech m = (Mech)unit; 
        	b = m.hasCASEIIAnywhere(); 
        }
        if(unit.hasCase() || b) {
        	return (int) (2 * unit.calculateBattleValue(false, true) *  PainResistanceBVBaseMod/100);
      	} else {
        	return (int) (unit.calculateBattleValue(false, true) *  PainResistanceBVBaseMod/100);
    	}
    }

    @Override
    public int getBVMod(Entity unit, SPilot p) {
    	//BK repeat of comments in getBVMod(Entity unit)
        SHouse house = CampaignMain.cm.getHouseFromPartialString(p.getCurrentFaction());
        int PainResistanceBVBaseMod = house.getIntegerConfig("PainResistanceBaseBVMod");
        boolean b = false;
        if (unit instanceof Mech) { 
        	Mech m = (Mech)unit; 
        	b = m.hasCASEIIAnywhere(); 
        }
        if(unit.hasCase() || b) {
        	return (int) (2 * unit.calculateBattleValue(false, true) *  PainResistanceBVBaseMod/100);
      	} else {
        	return (int) (unit.calculateBattleValue(false, true) *  PainResistanceBVBaseMod/100);
    	}
    }
}