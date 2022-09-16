/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

/*
 * Created on 24.04.2004
 */
package server.campaign.pilot.skills;

import common.Unit;
import common.campaign.pilot.Pilot;
import megamek.common.Entity;
import server.campaign.CampaignMain;
import server.campaign.SHouse;

/**
 * If a pilot has this skill they will have a +20% of returning home if ejected and left on the field.
 *
 * @author Helge Richter
 *
 */
public class SurvivalistSkill extends SPilotSkill {
    public SurvivalistSkill(int id) {
        super(id, "Survivalist", "SV");
        setDescription("If a pilot has this skill they will have a +20% of returning home if ejected andleft on the field.");
    }

    public SurvivalistSkill() {
        // TODO: replace with ReflectionProvider
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
        return 0;
    }

}
