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
 * Created on 18.04.2004
 */
package server.campaign.pilot.skills;

import java.util.Vector;

import common.Unit;
import common.campaign.pilot.Pilot;
import megamek.common.Entity;
import server.campaign.CampaignMain;
import server.campaign.SHouse;

/**
 * Pilot traits for use with moding the gaining of other traits
 *
 * @@author Torren (Jason Tighe)
 */
public class TraitSkill extends SPilotSkill {

    public TraitSkill(int id) {
        super(id, "Trait", "TN");
        setDescription("Pilot traits for use with moding the gaining of other skills");
    }

    public TraitSkill() {
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
    public int getBVMod(Entity unit) {
        return 0;
    }

    public void assignTrait(Pilot p) {
        int size = 0;
        String Trait = "none";
        String faction = p.getCurrentFaction();

        // MWLogger.errLog("Trait Skill Faction: "+faction);
        Vector<String> traitNames = CampaignMain.cm.getFactionTraits(faction);

        size = traitNames.size();

        // MWLogger.errLog("Trait Skill size: "+size);

        if (size < 1) {
            return;
        }

        if (size == 1) {
            Trait = traitNames.elementAt(0);
        } else {
            Trait = traitNames.elementAt(CampaignMain.cm.getRandomNumber(size));
        }
        if (Trait.indexOf("*") > -1) {
            p.setTraitName(Trait.substring(0, Trait.indexOf("*")));
        } else {
            p.setTraitName(Trait);
        }
    }
}
