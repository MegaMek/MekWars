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

public class SetComponentConversionCommand implements server.campaign.commands.Command {

    int accessLevel = server.campaign.CampaignMain.cm.getIntegerConfig("factionLeaderLevel");

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    String syntax = "Crit Name#Weight#Type#Max Production#House[Optional Staff Only]";

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
        String crit = command.nextToken();
        int weight = Integer.parseInt(command.nextToken());
        int type = Integer.parseInt(command.nextToken());
        int maxProduction = Integer.parseInt(command.nextToken());

        if (server.campaign.CampaignMain.cm.getServer().isModerator(Username) && command.hasMoreElements()) {
            house = server.campaign.CampaignMain.cm.getHouseFromPartialString(command.nextToken(), Username);
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

        server.campaign.CampaignMain.cm.doSendHouseMail(house,
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
}
