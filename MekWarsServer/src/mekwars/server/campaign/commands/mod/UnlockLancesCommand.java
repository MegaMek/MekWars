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

public class UnlockLancesCommand implements server.campaign.commands.Command {

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

        //get the player
        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(command.nextToken());

        p.lockArmy(-1);

        CampaignMain.campaignMain.toUser("You unlocked " + p.getName() + "'s armies.", Username, true);
        //server.MWLogger.modLog(Username + " unlocked " + p.getName() + "'s armies.");
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " unlocked " + p.getName() + "'s armies.");
        //server.MWLogger.modLog(Username + " unlocked " + p.getName() + "'s armies.");

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
