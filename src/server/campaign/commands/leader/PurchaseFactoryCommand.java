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
import java.util.Vector;

import common.UnitFactory;
import megamek.common.TechConstants;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.BuildTable;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.SUnitFactory;
import server.campaign.commands.Command;

public class PurchaseFactoryCommand implements Command {

    // Starting out at mod level this can be lowered as needed
    int accessLevel = IAuthenticator.MODERATOR;

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    String syntax = "Factory Name#Type#Weight#Planet";

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
        SPlanet planet;
        SHouse house;
        double cost = 0.0;
        double flu = 0.0;
        int type = SUnit.MEK;
        int weight = SUnit.LIGHT;
        String name = "";
        int buildType = UnitFactory.BUILDMEK;

        name = command.nextToken();
        type = Integer.parseInt(command.nextToken());
        weight = Integer.parseInt(command.nextToken());
        planet = CampaignMain.cm.getPlanetFromPartialString(command.nextToken(), null);
        house = player.getMyHouse();

        if (planet == null) {
            CampaignMain.cm.toUser("Unable to find planet.", Username);
            return;
        }

        if (house.isNewbieHouse()) {
            CampaignMain.cm.toUser(CampaignMain.cm.getConfig("NewbieHouseName") + " cannot purchase new factories!", Username);
            return;
        }

        if ( type == SUnit.BATTLEARMOR && house.getTechLevel() < TechConstants.T_IS_TW_ALL ) {
            CampaignMain.cm.toUser("Your factions tech level is not high enough to purchase Battle Armor factories", Username);
            return;
        }
        
        if ( type == SUnit.PROTOMEK && house.getTechLevel() < TechConstants.T_CLAN_TW) {
            CampaignMain.cm.toUser("Your factions tech level is not high enough to purchase ProtoMek factories", Username);
            return;
        }
        
        if (!planet.isOwner(house.getId())) {
            CampaignMain.cm.toUser("You do not own " + planet.getName(), Username);
            return;
        }
        
        String buildTable = BuildTable.getFileName("Common", SUnit.getWeightClassDesc(weight), BuildTable.STANDARD, type);
        
        if ( !new File(buildTable).exists() ) {
            CampaignMain.cm.toUser("Sorry but That type of factory cannot be built.", Username);
            return;
        }
        
        switch (type) {
        case SUnit.MEK:
            buildType = UnitFactory.BUILDMEK;
            break;
        case SUnit.INFANTRY:
            buildType = UnitFactory.BUILDINFANTRY;
            break;
        case SUnit.VEHICLE:
            buildType = UnitFactory.BUILDVEHICLES;
            break;
        case SUnit.BATTLEARMOR:
            buildType = UnitFactory.BUILDBATTLEARMOR;
            break;
        case SUnit.PROTOMEK:
            buildType = UnitFactory.BUILDPROTOMECHS;
            break;
        }

        cost = CampaignMain.cm.getDoubleConfig("NewFactoryBaseCost");
        cost *= CampaignMain.cm.getDoubleConfig("NewFactoryCostModifier" + SUnit.getWeightClassDesc(weight));
        cost *= CampaignMain.cm.getDoubleConfig("NewFactoryCostModifier" + SUnit.getTypeClassDesc(type));

        cost = Math.round(cost);

        flu = CampaignMain.cm.getDoubleConfig("NewFactoryBaseFlu");
        flu *= CampaignMain.cm.getDoubleConfig("NewFactoryFluModifier" + SUnit.getWeightClassDesc(weight));
        flu *= CampaignMain.cm.getDoubleConfig("NewFactoryFluModifier" + SUnit.getTypeClassDesc(type));

        flu = Math.round(flu);

        if (player.getMoney() < cost) {
            CampaignMain.cm.toUser("AM:You need " + CampaignMain.cm.moneyOrFluMessage(true, true, (int) cost) + " to purchase a factory.", Username);
            return;
        }

        if (player.getInfluence() < flu) {
            CampaignMain.cm.toUser("AM:You need " + CampaignMain.cm.moneyOrFluMessage(false, true, (int) flu) + " to purchase a factory.", Username);
            return;
        }

        cost = Math.max(0, cost);
        flu = Math.max(0, flu);

        player.addMoney((int)-cost);
        player.addInfluence((int)-flu);
        
        SUnitFactory fac = new SUnitFactory(name, planet, SUnit.getWeightClassDesc(weight), house.getName(), 0, CampaignMain.cm.getIntegerConfig("BaseFactoryRefreshRate"), buildType, BuildTable.STANDARD, 0);
        Vector<UnitFactory> uf = planet.getUnitFactories();
        uf.add(fac);
        fac.setPlanet(planet);
        planet.setOwner(null,house, true);
        
        house.updated();
        planet.updated();
        
        CampaignMain.cm.toUser("AM:You have purchased a factory, "+name+", on planet "+planet.getName()+" for "+CampaignMain.cm.moneyOrFluMessage(true, true, (int) cost) + " and "+CampaignMain.cm.moneyOrFluMessage(false, true, (int) flu), Username, true);
        CampaignMain.cm.doSendHouseMail(house,"NOTE", Username+" has purchased a factory, "+name+", on planet "+planet.getName()+".");
    }
}// end RequestSubFactionPromotionCommand class
