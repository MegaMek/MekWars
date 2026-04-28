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

public class ResearchTechLevelCommand implements server.campaign.commands.Command {

    // Starting out at mod level this can be lowered as needed
    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
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

        server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);
        server.campaign.SHouse house = player.getMyHouse();
        double cost = 0.0;
        double flu = 0.0;

        if (house.isNewbieHouse()) {
            server.campaign.CampaignMain.cm.toUser("AM:" +
                                                         server.campaign.CampaignMain.cm.getConfig("NewbieHouseName") +
                                                         " cannot research technology!", Username);
            return;
        }

        int currentTech = house.getTechResearchLevel();

        if (currentTech >= 6) {
            server.campaign.CampaignMain.cm.toUser("AM:You Faction has researched all known technology!", Username);
            return;
        }


        cost = server.campaign.CampaignMain.cm.getDoubleConfig("TechPointCost");
        if (currentTech > 1) {
            cost *= server.campaign.CampaignMain.cm.getDoubleConfig("TechLevelTechPointCostModifier") *
                          (currentTech - 1);
        }

        cost = Math.round(cost);

        flu = server.campaign.CampaignMain.cm.getDoubleConfig("TechPointFlu");
        if (currentTech > 1) {
            flu *= server.campaign.CampaignMain.cm.getDoubleConfig("TechLevelTechPointFluModifier") * (currentTech - 1);
        }

        flu = Math.round(flu);

        if (player.getMoney() < cost) {
            server.campaign.CampaignMain.cm.toUser("AM:You need " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               true,
                                                               (int) cost) +
                                                         " to research technology.", Username);
            return;
        }

        if (player.getInfluence() < flu) {
            server.campaign.CampaignMain.cm.toUser("AM:You need " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(false,
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
                  server.campaign.CampaignMain.cm.getIntegerConfig("TechPointsNeedToLevel")) {
            house.updateHouseTechLevel();
            server.campaign.CampaignMain.cm.doSendHouseMail(house,
                  "NOTE",
                  Username + " has increased your factions Tech Level!");
        } else {
            server.campaign.CampaignMain.cm.doSendHouseMail(house,
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
