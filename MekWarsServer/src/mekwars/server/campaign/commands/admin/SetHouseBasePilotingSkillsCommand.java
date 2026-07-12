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
import mekwars.server.campaign.CampaignMain;

//Syntax sethousebasepilotingskills house#pilotType#Skill$Skill
public class SetHouseBasePilotingSkillsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#[Mek,Vehicle,Infantry,Proto,BattleArmor,Aero]#PilotingSkill$PilotingSkill";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
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
            house = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), Username);
            pilotType = Unit.getTypeIDForName(command.nextToken());
            skills = command.nextToken() + "$";
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser(syntax, Username);
            return;
        }

        if (house == null) {return;}

        if (pilotType >= Unit.MAXBUILD || pilotType < 0) {
            CampaignMain.campaignMain.toUser(syntax, Username);
            return;
        }

        house.getPilotQueues().setBasePilotSkill(skills, pilotType);

        house.updated();
        //log, and inform mods.
        CampaignMain.campaignMain.toUser("You added a piloting skill for unit " +
                                               Unit.getTypeClassDesc(pilotType) +
                                               " for house " +
                                               house.getName() +
                                               " to " +
                                               skills, Username);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username +
                    " has added a piloting skill for unit " +
                    Unit.getTypeClassDesc(pilotType) +
                    " for house " +
                    house.getName() +
                    " to " +
                    skills +
                    ".");

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
