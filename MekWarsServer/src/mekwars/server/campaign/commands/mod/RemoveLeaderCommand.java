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
 * Remove a part from a player.
 */
public class RemoveLeaderCommand implements server.campaign.commands.Command {

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
        server.campaign.SPlayer player;

        try {
            String target = command.nextToken();
            player = CampaignMain.campaignMain.getPlayer(target);
            player.getMyHouse().removeLeader(player.getName());
            int level = CampaignMain.campaignMain.getIntegerConfig("factionLeaderLevel");
            //if they where just a normal leader then send them back to level 2
            //if they where giving higher then normal access because of mods or admin
            //status or even a admin lacky then let them keep their level.
            if (player.getPassword().getAccess() <= level) {
                level = 2;
                CampaignMain.campaignMain.updatePlayersAccessLevel(target, level);
            }
            CampaignMain.campaignMain.toUser("AM:You have been demoted as a faction leader by " + Username + ".",
                  target);
            CampaignMain.campaignMain.doSendHouseMail(player.getMyHouse(),
                  "Note",
                  player.getName() + " has been demoted from the faction leadership.");
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  Username + " has removed " + player.getName() + " as a faction leader.");
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("Invalid syntax: /addleader UserName", Username);
        }

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
