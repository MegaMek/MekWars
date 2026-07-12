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

import common.UnitFactory;
import megamek.common.TechConstants;
import mekwars.server.campaign.CampaignMain;

public class PurchaseFactoryCommand implements server.campaign.commands.Command {

    // Starting out at mod level this can be lowered as needed
    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Factory Name#Type#Weight#Planet";

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
        server.campaign.SPlanet planet;
        server.campaign.SHouse house;
        double cost = 0.0;
        double flu = 0.0;
        int type = server.campaign.SUnit.MEK;
        int weight = server.campaign.SUnit.LIGHT;
        String name = "";
        int buildType = UnitFactory.BUILDMEK;

        name = command.nextToken();
        type = Integer.parseInt(command.nextToken());
        weight = Integer.parseInt(command.nextToken());
        planet = CampaignMain.campaignMain.getPlanetFromPartialString(command.nextToken(), null);
        house = player.getMyHouse();

        if (planet == null) {
            CampaignMain.campaignMain.toUser("Unable to find planet.", Username);
            return;
        }

        if (house.isNewbieHouse()) {
            CampaignMain.campaignMain.toUser(CampaignMain.campaignMain.getConfig("NewbieHouseName") +
                                                   " cannot purchase new factories!", Username);
            return;
        }

        if (type == server.campaign.SUnit.BATTLEARMOR && house.getTechLevel() < TechConstants.T_IS_TW_ALL) {
            CampaignMain.campaignMain.toUser(
                  "Your factions tech level is not high enough to purchase Battle Armor factories",
                  Username);
            return;
        }

        if (type == server.campaign.SUnit.PROTOMEK && house.getTechLevel() < TechConstants.T_CLAN_TW) {
            CampaignMain.campaignMain.toUser(
                  "Your factions tech level is not high enough to purchase ProtoMek factories",
                  Username);
            return;
        }

        if (!planet.isOwner(house.getId())) {
            CampaignMain.campaignMain.toUser("You do not own " + planet.getName(), Username);
            return;
        }

        String buildTable = server.campaign.BuildTable.getFileName("Common",
              server.campaign.SUnit.getWeightClassDesc(weight),
              server.campaign.BuildTable.STANDARD,
              type);

        if (!new java.io.File(buildTable).exists()) {
            CampaignMain.campaignMain.toUser("Sorry but That type of factory cannot be built.", Username);
            return;
        }

        switch (type) {
            case server.campaign.SUnit.MEK:
                buildType = UnitFactory.BUILDMEK;
                break;
            case server.campaign.SUnit.INFANTRY:
                buildType = UnitFactory.BUILDINFANTRY;
                break;
            case server.campaign.SUnit.VEHICLE:
                buildType = UnitFactory.BUILDVEHICLES;
                break;
            case server.campaign.SUnit.BATTLEARMOR:
                buildType = UnitFactory.BUILDBATTLEARMOR;
                break;
            case server.campaign.SUnit.PROTOMEK:
                buildType = UnitFactory.BUILDPROTOMECHS;
                break;
        }

        cost = CampaignMain.campaignMain.getDoubleConfig("NewFactoryBaseCost");
        cost *= CampaignMain.campaignMain.getDoubleConfig("NewFactoryCostModifier" +
                                                                server.campaign.SUnit.getWeightClassDesc(weight));
        cost *= CampaignMain.campaignMain.getDoubleConfig("NewFactoryCostModifier" +
                                                                server.campaign.SUnit.getTypeClassDesc(type));

        cost = Math.round(cost);

        flu = CampaignMain.campaignMain.getDoubleConfig("NewFactoryBaseFlu");
        flu *= CampaignMain.campaignMain.getDoubleConfig("NewFactoryFluModifier" +
                                                               server.campaign.SUnit.getWeightClassDesc(weight));
        flu *= CampaignMain.campaignMain.getDoubleConfig("NewFactoryFluModifier" +
                                                               server.campaign.SUnit.getTypeClassDesc(type));

        flu = Math.round(flu);

        if (player.getMoney() < cost) {
            CampaignMain.campaignMain.toUser("AM:You need " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         true,
                                                         (int) cost) +
                                                   " to purchase a factory.", Username);
            return;
        }

        if (player.getInfluence() < flu) {
            CampaignMain.campaignMain.toUser("AM:You need " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(false,
                                                         true,
                                                         (int) flu) +
                                                   " to purchase a factory.", Username);
            return;
        }

        cost = Math.max(0, cost);
        flu = Math.max(0, flu);

        player.addMoney((int) -cost);
        player.addInfluence((int) -flu);

        server.campaign.SUnitFactory fac = new server.campaign.SUnitFactory(name,
              planet,
              server.campaign.SUnit.getWeightClassDesc(weight),
              house.getName(),
              0,
              CampaignMain.campaignMain.getIntegerConfig("BaseFactoryRefreshRate"),
              buildType,
              server.campaign.BuildTable.STANDARD,
              0);
        java.util.Vector<UnitFactory> uf = planet.getUnitFactories();
        uf.add(fac);
        fac.setPlanet(planet);
        planet.setOwner(null, house, true);

        house.updated();
        planet.updated();

        CampaignMain.campaignMain.toUser("AM:You have purchased a factory, " +
                                               name +
                                               ", on planet " +
                                               planet.getName() +
                                               " for " +
                                               CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                     true,
                                                     (int) cost) +
                                               " and " +
                                               CampaignMain.campaignMain.moneyOrFluMessage(false,
                                                     true,
                                                     (int) flu), Username, true);
        CampaignMain.campaignMain.doSendHouseMail(house,
              "NOTE",
              Username + " has purchased a factory, " + name + ", on planet " + planet.getName() + ".");
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}// end RequestSubFactionPromotionCommand class
