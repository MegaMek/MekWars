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
 * Add Parts to a player
 */
public class AddPartsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name#Part Name#Amount";

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

        if (!CampaignMain.campaignMain.getBooleanConfig("UsePartsRepair")) {
            CampaignMain.campaignMain.toUser("AM:Parts repair not used on this server!", Username);
            return;
        }
        server.campaign.SPlayer p;
        String part;
        int amount;
        try {
            p = CampaignMain.campaignMain.getPlayer(command.nextToken());
            part = command.nextToken();
            amount = Integer.parseInt(command.nextToken());
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("AM:Syntax: AddParts#Name#PartName#Amount", Username);
            return;
        }

        p.updatePartsCache(part, amount);

        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " has added " + amount + " " + part + " to " + p.getName() + ".");
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
