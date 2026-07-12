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

import java.util.StringTokenizer;

import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SHouse;
import mekwars.server.campaign.SPlayer;
import mekwars.server.campaign.commands.Command;

public class ViewFactionPartsCacheCommand implements Command {
    int accessLevel = CampaignMain.campaignMain.getIntegerConfig("factionLeaderLevel");
    String syntax = "";

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser(String.format("AM:Insufficient access level for command. Level: %s. Required: %s.", userLevel, accessLevel),
                      Username,
                      true);
                return;
            }
        }

        int year = CampaignMain.campaignMain.getIntegerConfig("CampaignYear");

        SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
        SHouse house = player.getMyHouse();

        if (command.hasMoreElements() && CampaignMain.campaignMain.getServer().isModerator(Username)) {
            house = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), Username);
        }

        if (house == null) {
            return;
        }

        String results = String.format("SM|%s", house.getUnitParts().tableComponents(year));

        CampaignMain.campaignMain.toUser(results, Username, false);
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int executionLevel) {
        accessLevel = executionLevel;
    }

    public String getSyntax() {
        return syntax;
    }
}// end RequestSubFactionPromotionCommand class
