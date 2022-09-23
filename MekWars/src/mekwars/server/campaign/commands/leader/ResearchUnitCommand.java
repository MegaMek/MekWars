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

import java.io.File;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import common.util.StringUtils;
import megamek.common.Entity;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.BuildTable;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.commands.Command;

public class ResearchUnitCommand implements Command {

    // Starting out at mod level this can be lowered as needed
    int accessLevel = IAuthenticator.MODERATOR;

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    String syntax = "UnitFileName";

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
        String unitFileName = command.nextToken();
        Entity ent = SUnit.loadMech(unitFileName);
        String buildTableFile;

        if (house.isNewbieHouse()) {
            CampaignMain.cm.toUser("AM:" + CampaignMain.cm.getConfig("NewbieHouseName") + " cannot research technology!", Username);
            return;
        }

        if (ent.getModel().equals("OMG-UR-FD")) {
            CampaignMain.cm.toUser("AM:Unknown Unit " + unitFileName, Username);
            return;
        }

        int unitTechLevel = house.getTechResearchLevel(ent.getTechLevel());

        if ( unitTechLevel > house.getTechResearchLevel() ) {
            CampaignMain.cm.toUser("AM:Your faction is unable to research "+StringUtils.aOrAn(ent.getShortNameRaw(), true, true)+" at this time, as your factions technology level is to low!", Username);
            return;
        }
        
        buildTableFile = BuildTable.getFileName(house.getName(), SUnit.getWeightClassDesc(SUnit.getEntityWeight(ent)), BuildTable.STANDARD, SUnit.getEntityType(ent));

        File unitsFile = new File(buildTableFile);
        ConcurrentHashMap<String, Integer> unitList = BuildTable.loadBuildTable(unitsFile);

        if (unitList.containsKey(unitFileName) && unitList.get(unitFileName) >= CampaignMain.cm.getIntegerConfig("MaxUnitResearchPoints")) {
            CampaignMain.cm.toUser("AM:Sorry you've researched this unit as much as possible.", Username);
            return;
        }

        cost = CampaignMain.cm.getDoubleConfig("BaseResearchCost");
        if ( unitTechLevel > 1)
            cost *= CampaignMain.cm.getDoubleConfig("ResearchTechLevelCostModifer") * unitTechLevel;
        cost *= CampaignMain.cm.getDoubleConfig("ResearchCostModifier" + SUnit.getTypeClassDesc(SUnit.getEntityType(ent)));
        cost *= CampaignMain.cm.getDoubleConfig("ResearchCostModifier" + SUnit.getWeightClassDesc(SUnit.getEntityWeight(ent)));

        cost = Math.round(cost);

        flu = CampaignMain.cm.getDoubleConfig("BaseResearchFlu");
        if ( unitTechLevel > 1)
            flu *= CampaignMain.cm.getDoubleConfig("ResearchTechLevelFluModifer") * unitTechLevel;
        flu *= CampaignMain.cm.getDoubleConfig("ResearchFluModifier" + SUnit.getTypeClassDesc(SUnit.getEntityType(ent)));
        flu *= CampaignMain.cm.getDoubleConfig("ResearchFluModifier" + SUnit.getWeightClassDesc(SUnit.getEntityWeight(ent)));

        flu = Math.round(flu);

        if (player.getMoney() < cost) {
            CampaignMain.cm.toUser("AM:You need " + CampaignMain.cm.moneyOrFluMessage(true, true, (int) cost) + " to research " + StringUtils.aOrAn(ent.getShortNameRaw(), true, true) + ".", Username);
            return;
        }

        if (player.getInfluence() < flu) {
            CampaignMain.cm.toUser("AM:You need " + CampaignMain.cm.moneyOrFluMessage(false, true, (int) flu) + " to research " + StringUtils.aOrAn(ent.getShortNameRaw(), true, true) + ".", Username);
            return;
        }

        cost = Math.max(0, cost);
        flu = Math.max(0, flu);
        
        player.addMoney((int) -cost);
        player.addInfluence((int) -flu);

        if (unitList.containsKey(unitFileName)) {
            unitList.put(unitFileName, unitList.get(unitFileName) + 1);
        } else
            unitList.put(unitFileName, 1);

        BuildTable.saveBuildTableFile(new File(buildTableFile), unitList);
        buildTableFile = buildTableFile.replaceAll(BuildTable.STANDARD, BuildTable.REWARD);
        BuildTable.saveBuildTableFile(new File(buildTableFile), unitList);

        CampaignMain.cm.toUser("AM:You research "+StringUtils.aOrAn(ent.getShortNameRaw(), true, true)+" for "+CampaignMain.cm.moneyOrFluMessage(true, true, (int)cost)+ " and "+CampaignMain.cm.moneyOrFluMessage(false, true, (int)flu), Username);
        CampaignMain.cm.doSendHouseMail(house, "NOTE", Username + " has researched "+StringUtils.aOrAn(ent.getShortNameRaw(), true, true)+".");
    }
}// end RequestSubFactionPromotionCommand class
