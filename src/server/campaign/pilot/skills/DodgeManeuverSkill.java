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
import megamek.common.Entity;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.pilot.SPilot;

/**
 * @author Helge Richter
 * 
 */
public class DodgeManeuverSkill extends SPilotSkill {

    public DodgeManeuverSkill(int id) {
        super(id, "Dodge Maneuver", "DM");
        setDescription("Enables the unit to make a dodge maneuver instead of a physical attack. This maneuver adds +2 to the BTH to physical attacks against the unit.");
    }

    public DodgeManeuverSkill() {
        // TODO: replace with ReflectionProvider
    }

    @Override
    public void modifyPilot(Pilot p) {
        p.addMegamekOption(new MegaMekPilotOption("dodge_maneuver", true));
        // p.setBvMod(p.getBVMod() + 0.01);
    }

    @Override
    public int getBVMod(Entity unit) {
        return CampaignMain.cm.getIntegerConfig("DodgeManeuverBaseBVMod");
    }

    @Override
    public int getBVMod(Entity unit, SPilot p) {
        SHouse house = CampaignMain.cm.getHouseFromPartialString(p.getCurrentFaction());

        if (house != null) {
            return house.getIntegerConfig("DodgeManeuverBaseBVMod");
        }
        return CampaignMain.cm.getIntegerConfig("DodgeManeuverBaseBVMod");
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
