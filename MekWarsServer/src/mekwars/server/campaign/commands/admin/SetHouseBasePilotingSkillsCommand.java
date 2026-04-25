/*
 * MekWars - Copyright (C) 2006
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

import common.Unit;

//Syntax sethousebasepilotingskills house#pilotType#Skill$Skill
public class SetHouseBasePilotingSkillsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#[Mek,Vehicle,Infantry,Proto,BattleArmor,Aero]#PilotingSkill$PilotingSkill";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

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

        server.campaign.SHouse house;
        int pilotType;
        String skills = "";

        try {
            house = server.campaign.CampaignMain.cm.getHouseFromPartialString(command.nextToken(), Username);
            pilotType = Unit.getTypeIDForName(command.nextToken());
            skills = command.nextToken() + "$";
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser(syntax, Username);
            return;
        }

        if (house == null) {return;}

        if (pilotType >= Unit.MAXBUILD || pilotType < 0) {
            server.campaign.CampaignMain.cm.toUser(syntax, Username);
            return;
        }

        house.getPilotQueues().setBasePilotSkill(skills, pilotType);

        house.updated();
        //log, and inform mods.
        server.campaign.CampaignMain.cm.toUser("You added a piloting skill for unit " +
                                                     Unit.getTypeClassDesc(pilotType) +
                                                     " for house " +
                                                     house.getName() +
                                                     " to " +
                                                     skills, Username);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username +
                    " has added a piloting skill for unit " +
                    Unit.getTypeClassDesc(pilotType) +
                    " for house " +
                    house.getName() +
                    " to " +
                    skills +
                    ".");

    }//end process
}
