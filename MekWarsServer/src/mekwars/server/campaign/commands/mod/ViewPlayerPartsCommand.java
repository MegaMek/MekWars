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

import java.util.StringTokenizer;

import mekwars.server.MWChatServer.auth.AccessRole;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SPlayer;
import mekwars.server.campaign.commands.Command;

/**
 * Return a human-readable string that describes the player's Parts cache.
 */
public class ViewPlayerPartsCommand implements Command {

    AccessRole accessLevel = AccessRole.MODERATOR;
    String syntax = "Player Name";

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != AccessRole.NONE) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser(String.format("AM:Insufficient access level for command. Level: %s. Required: %s.", userLevel, accessLevel),
                      Username,
                      true);
                return;
            }
        }

        if (!CampaignMain.campaignMain.getBooleanConfig("UsePartsRepair")) {
            CampaignMain.campaignMain.toUser("Parts repair not used on this server!", Username);
            return;
        }
        //get the player you wish to use

        if (!command.hasMoreTokens()) {
            CampaignMain.campaignMain.toUser("Syntax: ViewPlayerParts#Name#", Username);
            return;
        }

        SPlayer player = CampaignMain.campaignMain.getPlayer(command.nextToken());

        String toReturn = String.format("Parts cache for %s.<br>", player.getName()) + player.getUnitParts()
                                                                                 .tableComponents(CampaignMain.campaignMain.getIntegerConfig(
                                                                                       "CampaignYear"));

        CampaignMain.campaignMain.toUser(String.format("SM|%s", toReturn), Username, false);

        CampaignMain.campaignMain.doSendModMail("NOTE",
              String.format("%s has viewed %s's parts cache.", Username, player.getName()));
    }

    public AccessRole getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(AccessRole accessRole) {
        accessLevel = accessRole;
    }

    public String getSyntax() {
        return syntax;
    }
}
