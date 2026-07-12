/*
 * MekWars - Copyright (C) 2004
 *
 * Original Author: jtighe
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

import mekwars.server.campaign.CampaignMain;

public class AdminSetHousePlayerColorCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#htmlhexcolor";

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

        String HouseName = "";
        server.campaign.SHouse faction = null;
        String houseColor;
        server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);

        try {
            HouseName = command.nextToken();
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser(
                  "Improper command. Try: /c adminsethouseplayercolormod#faction#htmlhexcolor",
                  Username,
                  true);
            return;
        }

        faction = CampaignMain.campaignMain.getHouseFromPartialString(HouseName);
        if (faction == null) {
            CampaignMain.campaignMain.toUser("Couldn't find a faction with that name.", Username, true);
            return;
        }

        if (userLevel < server.MWChatServer.auth.IAuthenticator.MODERATOR) {faction = player.getMyHouse();}

        try {
            houseColor = command.nextToken();
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser(
                  "Improper command. Try: /c adminsethouseplayercolormod#faction#htmlhexcolor",
                  Username,
                  true);
            return;
        }

        faction.setHousePlayerColors(houseColor);
        faction.updated();

        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " changed the faction playerlist color for " + HouseName);
        //server.MWLogger.modLog(Username + " changed the faction playerlist color for " + HouseName);

        CampaignMain.campaignMain.toUser(HouseName + " color changed.", Username, true);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
