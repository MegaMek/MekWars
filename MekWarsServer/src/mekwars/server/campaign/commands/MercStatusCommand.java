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

package mekwars.server.campaign.commands;


import mekwars.server.campaign.CampaignMain;

public class MercStatusCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

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

        String targetPlayer = "";
        try {
            targetPlayer = command.nextToken();
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper format. Try: /c mercstatus#name", Username, true);
            return;
        }

        server.campaign.SPlayer merc = CampaignMain.campaignMain.getPlayer(targetPlayer);
        if (merc == null) {
            CampaignMain.campaignMain.toUser("AM:No player named " + targetPlayer + "!", Username, true);
        } else if ((merc.getMyHouse()).isMercHouse()) {
            String s = merc.getReadableMercStatus();
            CampaignMain.campaignMain.toUser(s, Username, true);
        } else {CampaignMain.campaignMain.toUser("AM:Target player is not a mercenary", Username, true);}

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
