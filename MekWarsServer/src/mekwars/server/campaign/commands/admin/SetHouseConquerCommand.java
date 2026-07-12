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


import mekwars.server.campaign.CampaignMain;

public class SetHouseConquerCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#[true/false]";

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

        server.campaign.SHouse h = null;
        boolean conquer = true;

        try {
            h = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), Username);
            conquer = Boolean.parseBoolean(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("Improper command. Try: /c sethouseconquer#Faction#true/false",
                  Username,
                  true);
            return;
        }

        if (h == null) {
            CampaignMain.campaignMain.toUser("Couldn't find a faction with that name.", Username, true);
            return;
        }

        h.setConquerable(conquer);
        h.updated();

        CampaignMain.campaignMain.toUser("You set " + h.getName() + "'s conquer status to " + conquer,
              Username,
              true);
        //server.MWLogger.modLog(Username + " has changed the conquer status for " + h.getName()+" to "+conquer);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " has changed the conquer status for " + h.getName() + " to " + conquer);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
