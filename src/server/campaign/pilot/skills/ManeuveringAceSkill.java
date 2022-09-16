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
 * Maneuvering like a quad.
 * 
 * @author Helge Richter and Immanuel Scholz
 */
public class ManeuveringAceSkill extends SPilotSkill {

    public ManeuveringAceSkill(int id) {
        super(id, "Maneuvering Ace", "MA");
        setDescription("Enables the unit to move laterally like a Quad. Units also receive a -1 BTH to rolls against skidding.");
    }

    public ManeuveringAceSkill() {
        // TODO: replace with ReflectionProvider
    }

    @Override
    public int getChance(int unitType, Pilot pilot) {
        if (pilot.getSkills().has(this)) {
            return 0;
        }

        String chance = "chancefor" + getAbbreviation() + "for" + Unit.getTypeClassDesc(unitType);

        SHouse house = CampaignMain.cm.getHouseFromPartialString(pilot.getCurrentFaction());

        if (house == null) {
            return CampaignMain.cm.getIntegerConfig(chance);
        }

        return house.getIntegerConfig(chance);
    }

    @Override
    public void modifyPilot(Pilot pilot) {
        pilot.addMegamekOption(new MegaMekPilotOption("maneuvering_ace", true));
        // pilot.setBvMod(pilot.getBVMod() + 0.01);
    }

    @Override
    public int getBVMod(Entity unit) {
        try {

            double topSpeed = unit.getRunMP();
            double baseBVMod = CampaignMain.cm.getDoubleConfig("ManeuveringAceBaseBVMod");
            double speedRating = CampaignMain.cm.getDoubleConfig("ManeuveringAceSpeedRating");
            double total = topSpeed / speedRating;
            total *= baseBVMod;
            return (int) total;
        } catch (Exception ex) {
            // MWLogger.errLog(ex);
            return 0;
        }
    }

    @Override
    public int getBVMod(Entity unit, SPilot p) {
        try {

            double topSpeed = unit.getRunMP();
            SHouse house = CampaignMain.cm.getHouseFromPartialString(p.getCurrentFaction());
            double baseBVMod = house.getDoubleConfig("ManeuveringAceBaseBVMod");
            double speedRating = house.getDoubleConfig("ManeuveringAceSpeedRating");
            double total = topSpeed / speedRating;
            total *= baseBVMod;
            return (int) total;
        } catch (Exception ex) {
            // MWLogger.errLog(ex);
            return 0;
        }
    }
}
