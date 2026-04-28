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

import server.util.MWPasswd;

/**
 * Add a Leader to a faction.
 */
public class AddLeaderCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
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
            player = server.campaign.CampaignMain.cm.getPlayer(target);
            player.getMyHouse().addLeader(player.getName());
            int level = server.campaign.CampaignMain.cm.getIntegerConfig("factionLeaderLevel");
            if (player.getPassword().getAccess() < level) {
                //CampaignMain.cm.updatePlayersAccessLevel(target,level);
                MWPasswd.getRecord(target).setAccess(level);
                server.campaign.CampaignMain.cm.getServer().getClient(target).setAccessLevel(level);
                server.campaign.CampaignMain.cm.getServer().getUser(target).setLevel(level);
                server.campaign.CampaignMain.cm.getServer().sendRemoveUserToAll(target, false);
                server.campaign.CampaignMain.cm.getServer().sendNewUserToAll(target, false);
                MWPasswd.writeRecord(player.getPassword(), target);
                if (player != null) {
                    server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers("PI|DA|" +
                                                                                   server.campaign.CampaignMain.cm.getPlayerUpdateString(
                                                                                         player), false);
                }
            }
            server.campaign.CampaignMain.cm.toUser("AM:You have been promoted to the faction leadership by " +
                                                         Username +
                                                         ".", target);
            server.campaign.CampaignMain.cm.doSendHouseMail(player.getMyHouse(),
                  "NOTE",
                  player.getName() + " has been promoted to the faction leadership.");
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username + " has added promoted " + target + " to faction leader.");

        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("AM:Invalid syntax: /addleader UserName", Username);
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
