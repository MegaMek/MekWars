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
import server.campaign.SArmy;       //Baruk Khazad! 20150929
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.pilot.SPilot;
import server.campaign.pilot.SPilotSkills;
import server.campaign.pilot.skills.AstechSkill;
import server.campaign.pilot.skills.EdgeSkill;
import server.campaign.pilot.skills.SPilotSkill;
import server.campaign.pilot.skills.TraitSkill;
import server.campaign.pilot.skills.WeaponSpecialistSkill;

public class PromotePilotCommand implements Command {

    int accessLevel = 0;
    String syntax = "promotepilot#unitid#skill";

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
        int cost = 0;
        SPilot pilot;
        SPilotSkill ps = null;

        if (!player.getMyHouse().getBooleanConfig("PlayersCanBuyPilotUpgrades")) {
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

        if (player.getMyHouse().getIntegerConfig("MaxPilotUpgrades") >= 0 && pilot.getSkills().size() >= player.getMyHouse().getIntegerConfig("MaxPilotUpgrades")) {
            CampaignMain.cm.toUser("AM:" + pilot.getName() + " already has the maximum allowed skills", Username, true);
            return;
        }

        if (skill.trim().length() < 1) {
            CampaignMain.cm.toUser("AM:A skill needs to be provided", Username, true);
            return;
        }
        
        //start code section 1 of 2 - Baruk Khazad! 20150929
        //disallow Promotion if unit is in army and player is not in reserve
        //this code belongs in both PromotePilotCommand.java and DemotePilotCommand.java
        boolean isInArmy = false;
        for (SArmy currA : player.getArmies()) {
             if (currA.isUnitInArmy(unit)) {
                   isInArmy = true;
                   break;
             }
        }
        if (isInArmy && player.getDutyStatus()!= SPlayer.STATUS_RESERVE) {
             CampaignMain.cm.toUser("AM:Your pilot is on patrol or fighting and needs to return to base for this training.", Username, true);
             return;
        }
        //end code section 1 of 2 - Baruk Khazad! 20150929

        if (skill.equalsIgnoreCase("gunnery")) {
            int gun = pilot.getGunnery();
            int piloting = pilot.getPiloting();

            if (pilot.getSkills().has(PilotSkill.NaturalAptitudeGunnerySkillID)) {
                gun++;
            }

            if (pilot.getSkills().has(PilotSkill.NaturalAptitudePilotingSkillID)) {
                piloting++;
            }

            if (piloting - (gun - 1) > 1 && player.getMyHouse().getBooleanConfig("PilotsMustLevelEvenly")) {
                CampaignMain.cm.toUser("AM:You must evenly level your pilots skills. Try leveling piloting first.", Username);
                return;
            }

            // Since the max base skill is 10 no reason to make your total skill 10 or higher so you always have
            // to pay the min exp.
            int totalSkill = Math.min(9, gun + piloting);

            cost = player.getMyHouse().getIntegerConfig("BaseRollToLevel");
            cost *= player.getMyHouse().getIntegerConfig("MultiplierPerPreviousLevel");
            cost *= 10 - totalSkill;

        } else if (skill.equalsIgnoreCase("piloting")) {
            int gun = pilot.getGunnery();
            int piloting = pilot.getPiloting();

            if (pilot.getSkills().has(PilotSkill.NaturalAptitudeGunnerySkillID)) {
                gun++;
            }

            if (pilot.getSkills().has(PilotSkill.NaturalAptitudePilotingSkillID)) {
                piloting++;
            }

            if (gun - (piloting - 1) > 1 && player.getMyHouse().getBooleanConfig("PilotsMustLevelEvenly")) {
                CampaignMain.cm.toUser("AM:You must evenly level your pilots skills. Try leveling gunnery first.", Username);
                return;
            }

            int totalSkill = Math.min(9, gun + piloting);

            cost = player.getMyHouse().getIntegerConfig("BaseRollToLevel");
            cost *= player.getMyHouse().getIntegerConfig("MultiplierPerPreviousLevel");
            cost *= 10 - totalSkill;

        } else {
            ps = SPilotSkills.getPilotSkill(skill);

            skill = ps.getName();
            if (pilot.getSkills().has(ps.getId()) && pilot.getSkills().getPilotSkill(ps.getId()).getLevel() >= 0) {
                if (ps.getId() == PilotSkill.AstechSkillID) {

                    ps = (SPilotSkill) pilot.getSkills().getPilotSkill(PilotSkill.AstechSkillID);
                    if (ps.getLevel() >= 2) {
                        CampaignMain.cm.toUser("AM:You cannot raise your pilots AstechSkill any higher!", Username);
                        return;
                    }

                    cost = player.getMyHouse().getIntegerConfig("chancefor" + ps.getAbbreviation() + "for" + Unit.getTypeClassDesc(unit.getType()));
                    cost *= ps.getLevel() + 2;
                } else if (ps.getId() == PilotSkill.EdgeSkillID) {
                    ps = (SPilotSkill) pilot.getSkills().getPilotSkill(PilotSkill.EdgeSkillID);
                    if (ps.getLevel() >= player.getMyHouse().getIntegerConfig("MaxEdgeChanges")) {
                        CampaignMain.cm.toUser("AM:You cannot raise your pilots Edge any higher!", Username);
                        return;
                    }
                    cost = player.getMyHouse().getIntegerConfig("chancefor" + ps.getAbbreviation() + "for" + Unit.getTypeClassDesc(unit.getType()));
                    cost *= ps.getLevel() + 1;
                } else {
                    CampaignMain.cm.toUser("AM:Your pilot already has that skill!", Username);
                    return;
                }
            } else {
                cost = player.getMyHouse().getIntegerConfig("chancefor" + ps.getAbbreviation() + "for" + Unit.getTypeClassDesc(unit.getType()));
            }

        }

        if (pilot.getSkills().has(PilotSkill.GiftedID)) {
            cost *= (1 - player.getMyHouse().getDoubleConfig("GiftedPercent"));
        }

        if (cost <= 0) {
            CampaignMain.cm.toUser("AM:" + pilot.getName() + " can not purchase that skill", Username);
            return;
        }

        if (pilot.getExperience() < cost) {
            CampaignMain.cm.toUser("AM:" + pilot.getName() + " does not have enough experience to purchase that skill", Username);
            return;
        }

        if (skill.equalsIgnoreCase("gunnery")) {
            pilot.setGunnery(pilot.getGunnery() - 1);
        } else if (skill.equalsIgnoreCase("piloting")) {
            pilot.setPiloting(pilot.getPiloting() - 1);
        } else {
            // special accomidation for WS and Trait
            if (ps instanceof WeaponSpecialistSkill) {
                ((WeaponSpecialistSkill) ps).assignWeapon(unit.getEntity(), pilot);
            } else if (ps instanceof TraitSkill) {
                ((TraitSkill) ps).assignTrait(pilot);
            } else if (ps instanceof EdgeSkill) {
                if (pilot.getSkills().has(ps)) {
                    ((EdgeSkill) ps).setLevel(ps.getLevel() + 1);
                }
            } else if (ps instanceof AstechSkill) {
                if (pilot.getSkills().has(ps)) {
                    ((AstechSkill) ps).setLevel(ps.getLevel() + 1);
                }
            }

            (ps).addToPilot(pilot);
            (ps).modifyPilot(pilot);
        }

        pilot.setExperience(pilot.getExperience() - cost);

        unit.setPilot(pilot);

        CampaignMain.cm.toUser("AM:Skill " + skill + " purchased for pilot " + pilot.getName() + " for " + cost + " exp.", Username);
        CampaignMain.cm.toUser("PL|UU|" + unit.getId() + "|" + unit.toString(true), Username, false);
        //start code section 2 of 2 - Baruk Khazad! 20150929
        // correct the BV of any army which contains the unit
        for (SArmy currA : player.getArmies()) {
             if (currA.isUnitInArmy(unit)) {
                  currA.setBV(0);
                  CampaignMain.cm.toUser("PL|SAD|" + currA.toString(true, "%"), player.getName(), false);
                  CampaignMain.cm.getOpsManager().checkOperations(currA, true);
             }
        }
        //end code section 2 of 2 - Baruk Khazad! 20150929

    }// end process()

}