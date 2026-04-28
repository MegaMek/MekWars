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
 * Remove a part from a player.
 */
public class RemovePartsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name#Part Name#Amount";

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
            server.campaign.CampaignMain.cm.toUser("AM:Parts repair not used on this server!", Username);
            return;
        }


        server.campaign.SPlayer p;
        String part;
        int amount;
        try {
            p = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());
            part = command.nextToken();
            amount = Integer.parseInt(command.nextToken());
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("AM:Syntax: RemoveParts#Name#PartName#Amount", Username);
            return;
        }

        p.updatePartsCache(part, -amount);

        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has removed " + amount + " " + part + " from " + p.getName() + ".");
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
