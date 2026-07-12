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

public class SetComponentConversionCommand implements server.campaign.commands.Command {

    int accessLevel = CampaignMain.campaignMain.getIntegerConfig("factionLeaderLevel");
    String syntax = "Crit Name#Weight#Type#Max Production#House[Optional Staff Only]";

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
        String crit = command.nextToken();
        int weight = Integer.parseInt(command.nextToken());
        int type = Integer.parseInt(command.nextToken());
        int maxProduction = Integer.parseInt(command.nextToken());

        if (CampaignMain.campaignMain.getServer().isModerator(Username) && command.hasMoreElements()) {
            house = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), Username);
        }

        if (crit.equalsIgnoreCase("all")) {
            house.getComponentConverter().clear();
            ComponentToCritsConverter converter = new ComponentToCritsConverter();
            converter.setComponentUsedType(type);
            converter.setComponentUsedWeight(weight);
            converter.setMinCritLevel(maxProduction);
            house.getComponentConverter().put(converter.getCritName(), converter);
        } else {
            house.getComponentConverter().remove("All");
            ComponentToCritsConverter converter = new ComponentToCritsConverter();
            converter.setCritName(crit);
            converter.setComponentUsedType(type);
            converter.setComponentUsedWeight(weight);
            converter.setMinCritLevel(maxProduction);
            house.getComponentConverter().put(converter.getCritName(), converter);
        }

        CampaignMain.campaignMain.doSendHouseMail(house,
              "NOTE",
              player.getName() +
                    " has set components to crit conversion for " +
                    crit +
                    " for " +
                    server.campaign.SUnit.getWeightClassDesc(weight) +
                    "/" +
                    server.campaign.SUnit.getTypeClassDesc(type) +
                    " to a max of " +
                    maxProduction +
                    "  crits.");
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}
