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

package mekwars.server.campaign.commands.mod;

import common.Unit;
import mekwars.server.campaign.CampaignMain;

//Syntax sethousebasepilotskills house#pilotType#Gunnery#Piloting
public class SetHouseBasePilotSkillsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Faction Name#Pilot Type#Gunnery#Piloting";

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
        int gunnery;
        int piloting;

        try {
            house = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), Username);
            pilotType = Integer.parseInt(command.nextToken());
            gunnery = Integer.parseInt(command.nextToken());
            piloting = Integer.parseInt(command.nextToken());
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser(
                  "Invalid Syntax: sethousebasepilotskills house#pilotType#Gunnery#Piloting",
                  Username);
            return;
        }

        if (house == null) {return;}

        if (pilotType >= Unit.MAXBUILD || pilotType < 0) {
            CampaignMain.campaignMain.toUser("Invalid unit type:<br>Mek " +
                                                   Unit.MEK +
                                                   "<br>Vehicle " +
                                                   Unit.VEHICLE +
                                                   "<br>Infantry " +
                                                   Unit.INFANTRY +
                                                   "<br>Battle Armor " +
                                                   Unit.BATTLEARMOR +
                                                   "<br>ProtoMek " +
                                                   Unit.PROTOMEK +
                                                   "<br>Aero " +
                                                   Unit.AERO, Username);
            return;
        }

        house.getPilotQueues().setBaseGunnery(gunnery, pilotType);
        house.getPilotQueues().setBasePiloting(piloting, pilotType);

        //log, and inform mods.
        CampaignMain.campaignMain.toUser("You set have set the gunnery and piloting for unit " +
                                               Unit.getTypeClassDesc(pilotType) +
                                               " for house " +
                                               house.getName() +
                                               " to " +
                                               gunnery +
                                               "/" +
                                               piloting, Username);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username +
                    " has set the gunnery and piloting for unit " +
                    Unit.getTypeClassDesc(pilotType) +
                    " for house " +
                    house.getName() +
                    " to " +
                    gunnery +
                    "/" +
                    piloting +
                    ".");

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
