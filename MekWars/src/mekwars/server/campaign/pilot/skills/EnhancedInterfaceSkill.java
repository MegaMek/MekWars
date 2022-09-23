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
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.pilot.SPilot;

/**
 * @author Helge Richter
 */
public class EnhancedInterfaceSkill extends SPilotSkill {

    public EnhancedInterfaceSkill() {
        // TODO: replace with ReflectionProvider
    }

    public EnhancedInterfaceSkill(int id) {
        super(id, "Enhanced Interface", "EI");
        setDescription("Neural interface to the clan enhanced imaging system -1 To PSR +2 when targeting with TC instead of +3 Can Target without TC at +6 Reduces all forest and Smoke mods to 1 Pilot receives 1 point of damage every time Units IS is hit, If you fail a roll of 7+ BA's recieve 1 extra point of damage every time they are hit.");
    }

    @Override
    public void modifyPilot(Pilot p) {
        // super.addToPilot(p);
        p.addMegamekOption(new MegaMekPilotOption("ei_implant", true));
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

        int EnhancedInterfaceBVBaseMod = CampaignMain.cm.getIntegerConfig("EnhancedInterfaceBaseBVMod");

        return EnhancedInterfaceBVBaseMod;

    }

    @Override
    public int getBVMod(Entity unit, SPilot p) {
        SHouse house = CampaignMain.cm.getHouseFromPartialString(p.getCurrentFaction());

        if (house != null) {
            return house.getIntegerConfig("EnhancedInterfaceBaseBVMod");
        }
        return CampaignMain.cm.getIntegerConfig("EnhancedInterfaceBaseBVMod");
    }

}