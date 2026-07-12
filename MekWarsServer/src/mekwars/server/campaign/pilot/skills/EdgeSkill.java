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

import common.MegaMekPilotOption;
import common.Unit;
import common.campaign.pilot.Pilot;
import common.campaign.pilot.skills.PilotSkill;
import megamek.common.Entity;
import mekwars.server.campaign.CampaignMain;

//import common.Unit;

/**
 * Reduces the bay-consume of the unit.
 *
 * @author Helge Richter
 */
public class EdgeSkill extends SPilotSkill {

    boolean edge_when_tac = true;
    boolean edge_when_ko = true;
    boolean edge_when_headhit = true;
    boolean edge_when_explosion = true;

    public EdgeSkill(int id) {
        super(id, "Edge", "ED");
        setDescription("Allows Pilot to reroll 1 roll(per level) per game.");
    }

    public EdgeSkill() {
        // TODO: Remove when no longer necessary
    }

    @Override
    public void modifyPilot(Pilot p) {
        p.addMegamekOption(new MegaMekPilotOption("edge", true));
    }

    @Override
    public void addToPilot(Pilot pilot) {
        // this.setLevel(1);
        pilot.getSkills().add(this);
    }

    @Override
    public int getChance(int unitType, Pilot p) {

        if (unitType != Unit.MEK) {
            return 0;
        }

        if (p.getSkills().has(PilotSkill.EdgeSkillID) &&
                  (p.getSkills().getPilotSkill(PilotSkill.EdgeSkillID).getLevel() >
                         CampaignMain.campaignMain.getIntegerConfig("MaxEdgeChanges"))) {
            return 0;
        }

        String chance = "chancefor" + getAbbreviation() + "for" + Unit.getTypeClassDesc(unitType);

        server.campaign.SHouse house = CampaignMain.campaignMain.getHouseFromPartialString(p.getCurrentFaction());

        if (house == null) {
            return CampaignMain.campaignMain.getIntegerConfig(chance);
        }

        return house.getIntegerConfig(chance);
    }

    @Override
    public int getBVMod(Entity unit) {
        return CampaignMain.campaignMain.getIntegerConfig("EdgeBaseBVMod");
    }

    @Override
    public int getBVMod(Entity unit, server.campaign.pilot.SPilot p) {
        server.campaign.SHouse house = CampaignMain.campaignMain.getHouseFromPartialString(p.getCurrentFaction());

        if (house != null) {
            return house.getIntegerConfig("EdgeBaseBVMod");
        }
        return CampaignMain.campaignMain.getIntegerConfig("EdgeBaseBVMod");
    }

    /**
     * @param level The level to set.
     */
    @Override
    public void setLevel(int level) {

        if (level < 1) {
            super.setLevel(1);
        } else {
            super.setLevel(level);
        }
    }

    public boolean getTac() {
        return edge_when_tac;
    }

    public void setTac(boolean value) {
        edge_when_tac = value;
    }

    public boolean getKO() {
        return edge_when_ko;
    }

    public void setKO(boolean value) {
        edge_when_ko = value;
    }

    public boolean getHeadHit() {
        return edge_when_headhit;
    }

    public void setHeadHit(boolean value) {
        edge_when_headhit = value;
    }

    public boolean getExplosion() {
        return edge_when_explosion;
    }

    public void setExplosion(boolean value) {
        edge_when_explosion = value;
    }
}
