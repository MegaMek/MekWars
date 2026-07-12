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

import mekwars.server.campaign.CampaignMain;

public class ResearchTechLevelCommand implements server.campaign.commands.Command {

    // Starting out at mod level this can be lowered as needed
    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "";

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
        double cost = 0.0;
        double flu = 0.0;

        if (house.isNewbieHouse()) {
            CampaignMain.campaignMain.toUser("AM:" +
                                                   CampaignMain.campaignMain.getConfig("NewbieHouseName") +
                                                   " cannot research technology!", Username);
            return;
        }

        int currentTech = house.getTechResearchLevel();

        if (currentTech >= 6) {
            CampaignMain.campaignMain.toUser("AM:You Faction has researched all known technology!", Username);
            return;
        }


        cost = CampaignMain.campaignMain.getDoubleConfig("TechPointCost");
        if (currentTech > 1) {
            cost *= CampaignMain.campaignMain.getDoubleConfig("TechLevelTechPointCostModifier") *
                          (currentTech - 1);
        }

        cost = Math.round(cost);

        flu = CampaignMain.campaignMain.getDoubleConfig("TechPointFlu");
        if (currentTech > 1) {
            flu *= CampaignMain.campaignMain.getDoubleConfig("TechLevelTechPointFluModifier") * (currentTech - 1);
        }

        flu = Math.round(flu);

        if (player.getMoney() < cost) {
            CampaignMain.campaignMain.toUser("AM:You need " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         true,
                                                         (int) cost) +
                                                   " to research technology.", Username);
            return;
        }

        if (player.getInfluence() < flu) {
            CampaignMain.campaignMain.toUser("AM:You need " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(false,
                                                         true,
                                                         (int) flu) +
                                                   " to research technology.", Username);
            return;
        }

        cost = Math.max(0, cost);
        flu = Math.max(0, flu);

        player.addMoney((int) -cost);
        player.addInfluence((int) -flu);

        house.addTechResearchPoint(1);

        if (house.getTechResearchPoints() >=
                  CampaignMain.campaignMain.getIntegerConfig("TechPointsNeedToLevel")) {
            house.updateHouseTechLevel();
            CampaignMain.campaignMain.doSendHouseMail(house,
                  "NOTE",
                  Username + " has increased your factions Tech Level!");
        } else {
            CampaignMain.campaignMain.doSendHouseMail(house,
                  "NOTE",
                  Username + " has taken your faction another step closer to the next technology level!");
        }
        house.updated();
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}// end RequestSubFactionPromotionCommand class
