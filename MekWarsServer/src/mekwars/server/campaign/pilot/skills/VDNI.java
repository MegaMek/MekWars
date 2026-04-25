/*
 * MekWars - Copyright (C) 2008
 *
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

package mekwars.server.campaign.pilot.skills;

import common.MegaMekPilotOption;
import common.Unit;
import common.campaign.pilot.Pilot;
import megamek.common.Entity;

/**
 * VDNI
 *
 * @author Jason Tighe
 */
public class VDNI extends SPilotSkill {

    public VDNI(int id) {
        super(id, "VDNI", "VDNI");
        setDescription("VDNI MD Skill");
    }

    @Override
    public void modifyPilot(Pilot p) {
        // super.addToPilot(p);
        p.addMegamekOption(new MegaMekPilotOption("vdni", true));
        p.setBvMod(p.getBVMod() + 0.01);
    }

    @Override
    public int getBVMod(Entity unit) {
        return server.campaign.CampaignMain.cm.getIntegerConfig("VDNIBaseBVMod");
    }

    @Override
    public int getBVMod(Entity unit, server.campaign.pilot.SPilot p) {
        server.campaign.SHouse house = server.campaign.CampaignMain.cm.getHouseFromPartialString(p.getCurrentFaction());
        return house.getIntegerConfig("VDNIBaseBVMod");
    }

    @Override
    public int getChance(int unitType, Pilot p) {
        if (p.getSkills().has(this)) {
            return 0;
        }

        if ((unitType != Unit.MEK) && (unitType != Unit.VEHICLE)) {
            return 0;
        }

        String chance = "chancefor" + getAbbreviation() + "for" + Unit.getTypeClassDesc(unitType);

        server.campaign.SHouse house = server.campaign.CampaignMain.cm.getHouseFromPartialString(p.getCurrentFaction());

        if (house == null) {
            return server.campaign.CampaignMain.cm.getIntegerConfig(chance);
        }

        return house.getIntegerConfig(chance);
    }

}
