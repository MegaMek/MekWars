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

import common.util.StringUtils;
import megamek.common.Entity;
import mekwars.server.campaign.CampaignMain;

public class ResearchUnitCommand implements server.campaign.commands.Command {

    // Starting out at mod level this can be lowered as needed
    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "UnitFileName";

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
        String unitFileName = command.nextToken();
        Entity ent = server.campaign.SUnit.loadMech(unitFileName);
        String buildTableFile;

        if (house.isNewbieHouse()) {
            CampaignMain.campaignMain.toUser("AM:" +
                                                   CampaignMain.campaignMain.getConfig("NewbieHouseName") +
                                                   " cannot research technology!", Username);
            return;
        }

        if (ent.getModel().equals("OMG-UR-FD")) {
            CampaignMain.campaignMain.toUser("AM:Unknown Unit " + unitFileName, Username);
            return;
        }

        int unitTechLevel = house.getTechResearchLevel(ent.getTechLevel());

        if (unitTechLevel > house.getTechResearchLevel()) {
            CampaignMain.campaignMain.toUser("AM:Your faction is unable to research " +
                                                   StringUtils.aOrAn(ent.getShortNameRaw(), true, true) +
                                                   " at this time, as your factions technology level is to low!",
                  Username);
            return;
        }

        buildTableFile = server.campaign.BuildTable.getFileName(house.getName(),
              server.campaign.SUnit.getWeightClassDesc(
                    server.campaign.SUnit.getEntityWeight(ent)),
              server.campaign.BuildTable.STANDARD,
              server.campaign.SUnit.getEntityType(ent));

        java.io.File unitsFile = new java.io.File(buildTableFile);
        java.util.concurrent.ConcurrentHashMap<String, Integer> unitList = server.campaign.BuildTable.loadBuildTable(
              unitsFile);

        if (unitList.containsKey(unitFileName) &&
                  unitList.get(unitFileName) >=
                        CampaignMain.campaignMain.getIntegerConfig("MaxUnitResearchPoints")) {
            CampaignMain.campaignMain.toUser("AM:Sorry you've researched this unit as much as possible.",
                  Username);
            return;
        }

        cost = CampaignMain.campaignMain.getDoubleConfig("BaseResearchCost");
        if (unitTechLevel > 1) {
            cost *= CampaignMain.campaignMain.getDoubleConfig("ResearchTechLevelCostModifer") * unitTechLevel;
        }
        cost *= CampaignMain.campaignMain.getDoubleConfig("ResearchCostModifier" +
                                                                server.campaign.SUnit.getTypeClassDesc(
                                                                      server.campaign.SUnit.getEntityType(ent)));
        cost *= CampaignMain.campaignMain.getDoubleConfig("ResearchCostModifier" +
                                                                server.campaign.SUnit.getWeightClassDesc(
                                                                      server.campaign.SUnit.getEntityWeight(ent)));

        cost = Math.round(cost);

        flu = CampaignMain.campaignMain.getDoubleConfig("BaseResearchFlu");
        if (unitTechLevel > 1) {
            flu *= CampaignMain.campaignMain.getDoubleConfig("ResearchTechLevelFluModifer") * unitTechLevel;
        }
        flu *= CampaignMain.campaignMain.getDoubleConfig("ResearchFluModifier" +
                                                               server.campaign.SUnit.getTypeClassDesc(
                                                                     server.campaign.SUnit.getEntityType(ent)));
        flu *= CampaignMain.campaignMain.getDoubleConfig("ResearchFluModifier" +
                                                               server.campaign.SUnit.getWeightClassDesc(
                                                                     server.campaign.SUnit.getEntityWeight(ent)));

        flu = Math.round(flu);

        if (player.getMoney() < cost) {
            CampaignMain.campaignMain.toUser("AM:You need " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         true,
                                                         (int) cost) +
                                                   " to research " +
                                                   StringUtils.aOrAn(ent.getShortNameRaw(), true, true) +
                                                   ".", Username);
            return;
        }

        if (player.getInfluence() < flu) {
            CampaignMain.campaignMain.toUser("AM:You need " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(false,
                                                         true,
                                                         (int) flu) +
                                                   " to research " +
                                                   StringUtils.aOrAn(ent.getShortNameRaw(), true, true) +
                                                   ".", Username);
            return;
        }

        cost = Math.max(0, cost);
        flu = Math.max(0, flu);

        player.addMoney((int) -cost);
        player.addInfluence((int) -flu);

        if (unitList.containsKey(unitFileName)) {
            unitList.put(unitFileName, unitList.get(unitFileName) + 1);
        } else {unitList.put(unitFileName, 1);}

        server.campaign.BuildTable.saveBuildTableFile(new java.io.File(buildTableFile), unitList);
        buildTableFile = buildTableFile.replaceAll(server.campaign.BuildTable.STANDARD,
              server.campaign.BuildTable.REWARD);
        server.campaign.BuildTable.saveBuildTableFile(new java.io.File(buildTableFile), unitList);

        CampaignMain.campaignMain.toUser("AM:You research " +
                                               StringUtils.aOrAn(ent.getShortNameRaw(), true, true) +
                                               " for " +
                                               CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                     true,
                                                     (int) cost) +
                                               " and " +
                                               CampaignMain.campaignMain.moneyOrFluMessage(false,
                                                     true,
                                                     (int) flu), Username);
        CampaignMain.campaignMain.doSendHouseMail(house,
              "NOTE",
              Username + " has researched " + StringUtils.aOrAn(ent.getShortNameRaw(), true, true) + ".");
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}// end RequestSubFactionPromotionCommand class
