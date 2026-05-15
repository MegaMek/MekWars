/*
 * MekWars - Copyright (C) 2007
 *
 * Original author - jtighe (torren@users.sourceforge.net)
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

import mekwars.server.campaign.CampaignMain;

/**
 * Return a human readable string that describes the player's Parts cache.
 */
public class ViewPlayerPartsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        if (!CampaignMain.campaignMain.getBooleanConfig("UsePartsRepair")) {
            CampaignMain.campaignMain.toUser("Parts repair not used on this server!", Username);
            return;
        }
        //get the player you wish to use

        if (!command.hasMoreTokens()) {
            CampaignMain.campaignMain.toUser("Syntax: ViewPlayerParts#Name#", Username);
            return;
        }
        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(command.nextToken());

        StringBuffer toReturn = new StringBuffer("Parts cache for " + p.getName() + ".<br>");

        toReturn.append(p.getUnitParts()
                              .tableizeComponents(CampaignMain.campaignMain.getIntegerConfig("CampaignYear")));

        CampaignMain.campaignMain.toUser("SM|" + toReturn.toString(), Username, false);

        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " has viewed " + p.getName() + "'s parts cache.");
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
