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

public class UnlockLancesCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

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

        //get the player
        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());

        p.lockArmy(-1);

        server.campaign.CampaignMain.cm.toUser("You unlocked " + p.getName() + "'s armies.", Username, true);
        //server.MWLogger.modLog(Username + " unlocked " + p.getName() + "'s armies.");
        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " unlocked " + p.getName() + "'s armies.");
        //server.MWLogger.modLog(Username + " unlocked " + p.getName() + "'s armies.");

    }
}
