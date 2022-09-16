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
import common.campaign.pilot.skills.PilotSkill;
import megamek.common.Entity;
import megamek.common.Mech;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.pilot.SPilot;

/**
 * NOTE: This is a unofficial rule. A pilot with this skill receives only 1 pilot hit from ammunition explosions.
 * @@author Torren (Jason Tighe)
 */
public class IronManSkill extends SPilotSkill {

    public IronManSkill(int id) {
        super(id, "Iron Man", "IM");
        setDescription("NOTE: This is a unofficial rule. A pilot with this skill receives only 1 pilot hit from ammunition explosions.");
    }

    public IronManSkill() {
    	//TODO: replace with ReflectionProvider
    }

    @Override
	public int getChance(int unitType, Pilot pilot) {
    	if (pilot.getSkills().has(this)) {
            return 0;
        }

       	if ( unitType != Unit.MEK) {
            return 0;
        }

       	String chance = "chancefor"+getAbbreviation()+"for"+Unit.getTypeClassDesc(unitType);

		SHouse house = CampaignMain.cm.getHouseFromPartialString(pilot.getCurrentFaction());

		if ( house == null ) {
            return CampaignMain.cm.getIntegerConfig(chance);
        }

		return house.getIntegerConfig(chance);
    }

    @Override
	public void modifyPilot(Pilot pilot) {
        pilot.addMegamekOption(new MegaMekPilotOption("iron_man",true));
       // pilot.setBvMod(pilot.getBVMod() +  0.02);
    }

    @Override
	public int getBVMod(Entity unit){
        return 0;
    }

    @Override
	public int getBVMod(Entity unit, SPilot pilot){
        int IronManBVBaseMod = CampaignMain.cm.getIntegerConfig("IronManBaseBVMod");

        if ( pilot.getSkills().has(PilotSkill.PainResistanceSkillID) ) {
            return 0;
        }
    	//BK - changing PR costs to % of bv for CASE/CASEII units, 0 for other units
    	//this is because in Megamek, IM reduces pilot hits from 2 to 1 for any ammo explosion
    	//The previous IM costs were 30 or 40 per ammo bin which is silly if you have no 
    	//CASE (mek gets gutted anyways). PainResistance and IM do not stack so this costs 0 if the pilot also has PR
        boolean b = false;
        if (unit instanceof Mech) { //BK - this section of code is tested! ty STK9A
        	Mech m = (Mech)unit; 
        	b = m.hasCASEIIAnywhere(); 
        }
        if(unit.hasCase() || b) {
        	return (int) (unit.calculateBattleValue(false, true) *  IronManBVBaseMod/100);
      	} else {
        	return 0;
    	}

    }

}

