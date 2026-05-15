/*
 * MekWars - Copyright (C) 2008
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

package mekwars.server.campaign.commands.mod;

import mekwars.server.campaign.CampaignMain;

public class GrantTechPointsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Faction Name#Amount";

    public String getSyntax() {
        return syntax;
    }

    public void process(java.util.StringTokenizer command, String Username) {

        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        server.campaign.SHouse faction = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(),
              Username);
        int amount = Integer.parseInt(command.nextToken());
        if (faction != null) {
            faction.addTechResearchPoint(amount);

            String toRecipient = "AM:" + Username + " granted you " + amount + " Tech Research Points";

            CampaignMain.campaignMain.doSendHouseMail(faction, "NOTE", toRecipient);
            if (faction.getTechResearchPoints() >=
                      CampaignMain.campaignMain.getIntegerConfig("TechPointsNeedToLevel")) {
                faction.updateHouseTechLevel();
                CampaignMain.campaignMain.doSendHouseMail(faction,
                      "NOTE",
                      Username + " has increased your factions Tech Level!");
            }
            CampaignMain.campaignMain.toUser("AM:You granted " +
                                                   amount +
                                                   " Tech Research Points to " +
                                                   faction.getName(), Username, true);
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  Username + " granted " + amount + " Tech Research Points to " + faction.getName());
        }
    }// end process()

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

}
