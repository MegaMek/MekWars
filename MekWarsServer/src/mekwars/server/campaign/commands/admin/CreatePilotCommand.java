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

package mekwars.server.campaign.commands.admin;

import server.campaign.pilot.SPilot;
import server.campaign.pilot.SPilotSkills;
import server.campaign.pilot.skills.SPilotSkill;
import server.campaign.pilot.skills.TraitSkill;

// syntanx /c createunit#filename#flavortext#gunnery#pilot#skill1,skill2,skill3
public class CreatePilotCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "target player#gunnery#pilot#weightclass#type#skill1,skill2,skill3[Random]";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }


        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
        server.campaign.SHouse h = p.getMyHouse();

        if (!h.getBooleanConfig("AllowPersonalPilotQueues")) {return;}

        server.campaign.SPlayer target;
        String gunnery;
        String piloting;
        int type;
        int weight;

        try {
            target = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());
            gunnery = command.nextToken();
            piloting = command.nextToken();
            type = server.campaign.SUnit.getTypeIDForName(command.nextToken());
            weight = server.campaign.SUnit.getWeightIDForName(command.nextToken());
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser(syntax, Username);
            return;
        }

        if (target == null) {
            server.campaign.CampaignMain.cm.toUser("Cannot find target player", Username);
        }


        if (p.getPersonalPilotQueue().getPilotQueue(type, weight).size() > 0
                  && !h.getBooleanConfig("AllowPlayerToBuyPilotsFromHouseWhenPoolIsFull")) {
            server.campaign.CampaignMain.cm.toUser("AM:" +
                                                         target.getName() +
                                                         " does not have enough room for a new pilot.", Username, true);
            return;
        }

        SPilot pilot = null;
        pilot = new SPilot(SPilot.getRandomPilotName(server.campaign.CampaignMain.cm.getR()),
              Integer.parseInt(gunnery),
              Integer.parseInt(piloting));

        pilot.setCurrentFaction("Common");
        if (command.hasMoreTokens()) {
            String skillTokens = command.nextToken();
            java.util.StringTokenizer skillList = new java.util.StringTokenizer(skillTokens, ",");

            while (skillList.hasMoreTokens()) {
                String skill = skillList.nextToken();
                SPilotSkill pSkill = null;
                if (skill.equalsIgnoreCase("random")) {pSkill = SPilotSkills.getRandomSkill(pilot, type);} else {
                    pSkill = SPilotSkills.getPilotSkill(skill);
                }

                if (pSkill != null) {
                    if (pSkill instanceof TraitSkill) {
                        ((TraitSkill) pSkill).assignTrait(pilot);
                    }
                    pSkill.addToPilot(pilot);
                    pSkill.modifyPilot(pilot);
                }
            }
        }

        target.getPersonalPilotQueue().addPilot(pilot, type, weight);
        server.campaign.CampaignMain.cm.toUser("PL|AP2PPQ|" + type + "|" + weight + "|" + pilot.toFileFormat("#", true),
              target.getName(),
              false);

        server.campaign.CampaignMain.cm.toUser("AM:" +
                                                     server.campaign.SUnit.getWeightClassDesc(weight) +
                                                     " " +
                                                     server.campaign.SUnit.getTypeClassDesc(type) +
                                                     " Pilot created: " +
                                                     pilot.getName() +
                                                     " for " +
                                                     target.getName() +
                                                     " (" +
                                                     gunnery +
                                                     "/" +
                                                     piloting +
                                                     ") [" +
                                                     pilot.getSkillString(true) +
                                                     "].", Username);
        server.campaign.CampaignMain.cm.toUser("AM:" +
                                                     Username +
                                                     " has created a " +
                                                     server.campaign.SUnit.getWeightClassDesc(weight) +
                                                     " " +
                                                     server.campaign.SUnit.getTypeClassDesc(type) +
                                                     " pilot for you.  " +
                                                     pilot.getName() +
                                                     " (" +
                                                     gunnery +
                                                     "/" +
                                                     piloting +
                                                     ") [" +
                                                     pilot.getSkillString(true) +
                                                     "]", target.getName());
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username +
                    " created a " +
                    server.campaign.SUnit.getWeightClassDesc(weight) +
                    " " +
                    server.campaign.SUnit.getTypeClassDesc(type) +
                    " pilot for " +
                    target.getName() +
                    ".  " +
                    pilot.getName() +
                    " (" +
                    gunnery +
                    "/" +
                    piloting +
                    ") [" +
                    pilot.getSkillString(true) +
                    "]");
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
