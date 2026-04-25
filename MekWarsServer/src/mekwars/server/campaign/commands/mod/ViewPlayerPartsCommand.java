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

/**
 * Return a human readable string that describes the player's Parts cache.
 */
public class ViewPlayerPartsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

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

        if (!server.campaign.CampaignMain.cm.getBooleanConfig("UsePartsRepair")) {
            server.campaign.CampaignMain.cm.toUser("Parts repair not used on this server!", Username);
            return;
        }
        //get the player you wish to use

        if (!command.hasMoreTokens()) {
            server.campaign.CampaignMain.cm.toUser("Syntax: ViewPlayerParts#Name#", Username);
            return;
        }
        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());

        StringBuffer toReturn = new StringBuffer("Parts cache for " + p.getName() + ".<br>");

        toReturn.append(p.getUnitParts()
                              .tableizeComponents(server.campaign.CampaignMain.cm.getIntegerConfig("CampaignYear")));

        server.campaign.CampaignMain.cm.toUser("SM|" + toReturn.toString(), Username, false);

        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has viewed " + p.getName() + "'s parts cache.");
    }
}
