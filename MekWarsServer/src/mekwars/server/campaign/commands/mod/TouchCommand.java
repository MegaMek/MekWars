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

package mekwars.server.campaign.commands.mod;


import mekwars.server.campaign.CampaignMain;

public class TouchCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

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

        String player = command.nextToken();
        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(player);
        if (p.getDutyStatus() != server.campaign.SPlayer.STATUS_LOGGEDOUT) {
            CampaignMain.campaignMain.toUser(p.getName() + " is already on-line and doesn't need a pfile update.",
                  Username);
            return;
        }

        p.setLastOnline(System.currentTimeMillis());
        p.setSave();

        CampaignMain.campaignMain.toUser("AM:You touched " + p.getName() + ".", Username, true);
        //server.MWLogger.modLog(Username + " touched " + p.getName() + ".");
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " touched " + p.getName() + ".");

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
