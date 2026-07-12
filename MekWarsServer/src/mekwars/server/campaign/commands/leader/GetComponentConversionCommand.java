/*
 * MekWars - Copyright (C) 2007
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

import common.util.ComponentToCritsConverter;
import mekwars.server.campaign.CampaignMain;

public class GetComponentConversionCommand implements server.campaign.commands.Command {

    int accessLevel = CampaignMain.campaignMain.getIntegerConfig("factionLeaderLevel");
    String syntax = "[house name option Staff only]";

    public String getSyntax() {
        return syntax;
    }

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

        server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
        server.campaign.SHouse house = player.getMyHouse();

        if (CampaignMain.campaignMain.getServer().isModerator(Username) && command.hasMoreElements()) {
            house = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), Username);
        }


        StringBuffer results = new StringBuffer("PL|CCC|");
        for (ComponentToCritsConverter converter : house.getComponentConverter().values()) {
            results.append(converter.toString("#"));
        }

        CampaignMain.campaignMain.toUser(results.toString(), Username, false);
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}
