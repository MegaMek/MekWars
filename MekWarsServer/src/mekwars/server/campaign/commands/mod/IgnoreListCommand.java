/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - Jason Tighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package mekwars.server.campaign.commands.mod;


import mekwars.server.campaign.CampaignMain;

/**
 * Moving the IgnoreList command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c IgnoreList
 */
public class IgnoreListCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "";

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
        CampaignMain.campaignMain.toUser("AM:Curently ignored people: " +
                                               CampaignMain.campaignMain.getServer()
                                                     .getIgnoreList()
                                                     .toString(), Username);
        CampaignMain.campaignMain.toUser("AM:Curently faction ignored people: " +
                                               CampaignMain.campaignMain.getServer()
                                                     .getFactionLeaderIgnoreList()
                                                     .toString(), Username);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
