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


public class MercStatusCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

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

        String targetPlayer = "";
        try {
            targetPlayer = command.nextToken();
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("AM:Improper format. Try: /c mercstatus#name", Username, true);
            return;
        }

        server.campaign.SPlayer merc = server.campaign.CampaignMain.cm.getPlayer(targetPlayer);
        if (merc == null) {
            server.campaign.CampaignMain.cm.toUser("AM:No player named " + targetPlayer + "!", Username, true);
        } else if ((merc.getMyHouse()).isMercHouse()) {
            String s = merc.getReadableMercStatus();
            server.campaign.CampaignMain.cm.toUser(s, Username, true);
        } else {server.campaign.CampaignMain.cm.toUser("AM:Target player is not a mercenary", Username, true);}

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
