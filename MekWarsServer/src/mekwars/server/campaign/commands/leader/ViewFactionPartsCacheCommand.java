/*
 * MekWars - Copyright (C) 2008
 *
 * Original author - jtighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package mekwars.server.campaign.commands.leader;

public class ViewFactionPartsCacheCommand implements server.campaign.commands.Command {

    int accessLevel = server.campaign.CampaignMain.cm.getIntegerConfig("factionLeaderLevel");

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    String syntax = "";

    public String getSyntax() {
        return syntax;
    }

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

        int year = server.campaign.CampaignMain.cm.getIntegerConfig("CampaignYear");

        server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);
        server.campaign.SHouse house = player.getMyHouse();

        if (command.hasMoreElements() && server.campaign.CampaignMain.cm.getServer().isModerator(Username)) {
            house = server.campaign.CampaignMain.cm.getHouseFromPartialString(command.nextToken(), Username);
        }

        if (house == null) {return;}

        String results = "SM|" + house.getUnitParts().tableizeComponents(year);

        server.campaign.CampaignMain.cm.toUser(results, Username, false);
    }
}// end RequestSubFactionPromotionCommand class
