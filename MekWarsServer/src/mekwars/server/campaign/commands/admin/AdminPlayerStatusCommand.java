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

public class AdminPlayerStatusCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
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

        //variables
        server.campaign.SPlayer p = null;

        try {
            p = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("Improper command. Try: /c adminplayerstatus#name", Username, true);
            return;
        }

        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("Couldn't find a player with that name.", Username, true);
            return;
        }

        //get the status
        server.campaign.CampaignMain.cm.toUser("SM|" + p.getReadableStatus(true), Username, false);

        //server.MWLogger.modLog(Username + " examined " + p.getName() + "'s account.");
        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " examined " + p.getName() + "'s account.");

    }
}
