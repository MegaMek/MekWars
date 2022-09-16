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

package server.campaign.commands.leader;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class ResearchTechLevelCommand implements Command {

    // Starting out at mod level this can be lowered as needed
    int accessLevel = IAuthenticator.MODERATOR;

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

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
                return;
            }
        }

        SPlayer player = CampaignMain.cm.getPlayer(Username);
        SHouse house = player.getMyHouse();
        double cost = 0.0;
        double flu = 0.0;

        if (house.isNewbieHouse()) {
            CampaignMain.cm.toUser("AM:"+CampaignMain.cm.getConfig("NewbieHouseName") + " cannot research technology!", Username);
            return;
        }

        int currentTech = house.getTechResearchLevel();
        
        if ( currentTech >= 6) {
            CampaignMain.cm.toUser("AM:You Faction has researched all known technology!", Username);
            return;
        }
        
        
        cost = CampaignMain.cm.getDoubleConfig("TechPointCost");
        if ( currentTech > 1)
            cost *= CampaignMain.cm.getDoubleConfig("TechLevelTechPointCostModifier") * (currentTech-1);

        cost = Math.round(cost);

        flu = CampaignMain.cm.getDoubleConfig("TechPointFlu");
        if ( currentTech > 1)
            flu *= CampaignMain.cm.getDoubleConfig("TechLevelTechPointFluModifier") * (currentTech-1);

        flu = Math.round(flu);

        if (player.getMoney() < cost) {
            CampaignMain.cm.toUser("AM:You need " + CampaignMain.cm.moneyOrFluMessage(true, true, (int) cost) + " to research technology.", Username);
            return;
        }

        if (player.getInfluence() < flu) {
            CampaignMain.cm.toUser("AM:You need " + CampaignMain.cm.moneyOrFluMessage(false, true, (int) flu) + " to research technology.", Username);
            return;
        }

        cost = Math.max(0, cost);
        flu = Math.max(0, flu);

        player.addMoney((int)-cost);
        player.addInfluence((int)-flu);

        house.addTechResearchPoint(1);
        
        if ( house.getTechResearchPoints() >= CampaignMain.cm.getIntegerConfig("TechPointsNeedToLevel") ) {
            house.updateHouseTechLevel();
            CampaignMain.cm.doSendHouseMail(house, "NOTE", Username+" has increased your factions Tech Level!");
        }else {
            CampaignMain.cm.doSendHouseMail(house, "NOTE", Username+" has taken your faction another step closer to the next technology level!");
        }
        house.updated();
    }
}// end RequestSubFactionPromotionCommand class
