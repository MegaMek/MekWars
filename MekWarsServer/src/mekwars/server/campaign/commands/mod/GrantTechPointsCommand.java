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

public class GrantTechPointsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Faction Name#Amount";

    public String getSyntax() {
        return syntax;
    }

    public void process(java.util.StringTokenizer command, String Username) {

        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        server.campaign.SHouse faction = server.campaign.CampaignMain.cm.getHouseFromPartialString(command.nextToken(),
              Username);
        int amount = Integer.parseInt(command.nextToken());
        if (faction != null) {
            faction.addTechResearchPoint(amount);

            String toRecipient = "AM:" + Username + " granted you " + amount + " Tech Research Points";

            server.campaign.CampaignMain.cm.doSendHouseMail(faction, "NOTE", toRecipient);
            if (faction.getTechResearchPoints() >=
                      server.campaign.CampaignMain.cm.getIntegerConfig("TechPointsNeedToLevel")) {
                faction.updateHouseTechLevel();
                server.campaign.CampaignMain.cm.doSendHouseMail(faction,
                      "NOTE",
                      Username + " has increased your factions Tech Level!");
            }
            server.campaign.CampaignMain.cm.toUser("AM:You granted " +
                                                         amount +
                                                         " Tech Research Points to " +
                                                         faction.getName(), Username, true);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
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
