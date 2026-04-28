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

package mekwars.server.campaign.commands.admin;


public class InvisCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

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

        server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);
        if (player == null) {return;}

        if (player.getDutyStatus() != server.campaign.SPlayer.STATUS_RESERVE) {
            server.campaign.CampaignMain.cm.toUser("AM:You must be in reserve to change visibility.", Username, true);
            return;
        }

        player.setInvisible(!player.isInvisible());

        server.campaign.CampaignMain.cm.getServer().sendRemoveUserToAll(Username, false);

        server.campaign.CampaignMain.cm.getServer().getUser(Username).setInvis(player.isInvisible());
        server.campaign.CampaignMain.cm.getServer().sendNewUserToAll(Username, false);

        //Fix for BUG 1491951: post-invisibility status
        server.campaign.CampaignMain.cm.sendPlayerStatusUpdate(player, true);

        server.campaign.CampaignMain.cm.toUser("AM:You have become " + (player.isInvisible() ? "invisible" : "visible"),
              Username,
              true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has become " + (player.isInvisible() ? "invisible" : "visible"));
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
