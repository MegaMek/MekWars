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

package server.campaign.commands;

import java.util.StringTokenizer;

import common.Unit;
import common.campaign.pilot.skills.PilotSkill;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.pilot.SPilot;
import server.campaign.pilot.SPilotSkills;
import server.campaign.pilot.skills.AstechSkill;
import server.campaign.pilot.skills.EdgeSkill;
import server.campaign.pilot.skills.SPilotSkill;

public class DemotePilotCommand implements Command {

    int accessLevel = 0;
    String syntax = "demotepilot#unitid#skill";

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
                return;
            }
        }

        SUnit unit;
        SPlayer player = CampaignMain.cm.getPlayer(Username);
        String skill;
        double cost = 0;
        SPilot pilot;
        SPilotSkill ps = null;

        if (!player.getMyHouse().getBooleanConfig("PlayersCanBuyPilotUpgrades")) {
            return;
        }

        if (!player.getMyHouse().getBooleanConfig("PlayersCanSellPilotUpgrades")) {
            return;
        }

        try {
            unit = player.getUnit(Integer.parseInt(command.nextToken()));
            skill = command.nextToken();
        } catch (Exception ex) {
            CampaignMain.cm.toUser("AM:Improper format. Try: /c " + syntax, Username);
            return;
        }

        if (unit == null) {
            CampaignMain.cm.toUser("AM:Cannot find that unit!", Username);
            return;
        }

        if (unit.hasVacantPilot()) {
            CampaignMain.cm.toUser("AM:Unit " + unit.getModelName() + " has no pilot to promote!", Username);
            return;
        }

        pilot = (SPilot) unit.getPilot();

        if (skill.trim().length() < 1) {
            CampaignMain.cm.toUser("AM:A skill needs to be provided", Username, true);
            return;
        }

        ps = SPilotSkills.getPilotSkill(skill);

        if (!pilot.getSkills().has(ps)) {
            CampaignMain.cm.toUser("AM:" + pilot.getName() + " does not have " + ps.getName() + ".", Username, true);
            return;
        }

        skill = ps.getName();
        if (pilot.getSkills().has(ps.getId()) && pilot.getSkills().getPilotSkill(ps.getId()).getLevel() >= 0) {
            if (ps.getId() == PilotSkill.AstechSkillID) {

                ps = (SPilotSkill) pilot.getSkills().getPilotSkill(PilotSkill.AstechSkillID);
                cost = player.getMyHouse().getIntegerConfig("chancefor" + ps.getAbbreviation() + "for" + Unit.getTypeClassDesc(unit.getType()));
                cost *= ps.getLevel() + 1;
            } else if (ps.getId() == PilotSkill.EdgeSkillID) {
                ps = (SPilotSkill) pilot.getSkills().getPilotSkill(PilotSkill.EdgeSkillID);
                cost = player.getMyHouse().getIntegerConfig("chancefor" + ps.getAbbreviation() + "for" + Unit.getTypeClassDesc(unit.getType()));
                cost *= ps.getLevel();
            } else {
                CampaignMain.cm.toUser("AM:Your pilot already has that skill!", Username);
                return;
            }
        } else {
            cost = player.getMyHouse().getIntegerConfig("chancefor" + ps.getAbbreviation() + "for" + Unit.getTypeClassDesc(unit.getType()));
        }

        cost *= player.getMyHouse().getDoubleConfig("PilotUpgradeSellBackPercent");

        if (ps instanceof EdgeSkill) {
            if (((EdgeSkill) ps).getLevel() > 1) {
                ((EdgeSkill) ps).setLevel(ps.getLevel() - 1);
            } else {
                (ps).removeFromPilot(pilot);
            }
        } else if (ps instanceof AstechSkill) {
            if (((AstechSkill) ps).getLevel() > 1) {
                ((AstechSkill) ps).setLevel(ps.getLevel() - 1);
            } else {
                (ps).removeFromPilot(pilot);
            }
        } else {
            (ps).removeFromPilot(pilot);
        }

        (ps).modifyPilot(pilot);

        pilot.setExperience(pilot.getExperience() + (int) cost);

        unit.setPilot(pilot);

        CampaignMain.cm.toUser("AM:Skill " + skill + " removed from pilot " + pilot.getName() + " for " + (int) cost + " exp.", Username);
        CampaignMain.cm.toUser("PL|UU|" + unit.getId() + "|" + unit.toString(true), Username, false);

    }// end process()

}