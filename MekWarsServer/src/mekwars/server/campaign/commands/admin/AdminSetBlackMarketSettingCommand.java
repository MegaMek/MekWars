/*
 * MekWars - Copyright (C) 2007
 *
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

/**
 * @author jtighe This command is used to set Black Market Settings for max/min cost and production.
 *
 */
package mekwars.server.campaign.commands.admin;

import common.Equipment;
import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;

public class AdminSetBlackMarketSettingCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Item Name#Min Cost#Max Cost#Min Production#Max Production";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        //get config var and new setting
        String key = "";
        String minCost = "";
        String maxCost = "";
        String minProduction = "";
        String maxProduction = "";


        try {
            key = command.nextToken();
            minCost = command.nextToken();
            maxCost = command.nextToken();
            minProduction = command.nextToken();
            maxProduction = command.nextToken();

            Equipment bme = CampaignMain.campaignMain.getBlackMarketEquipmentTable().get(key);

            if (bme == null) {
                bme = new Equipment();
                bme.setEquipmentInternalName(key);
            }

            bme.setMinCost(Double.parseDouble(minCost));
            bme.setMaxCost(Double.parseDouble(maxCost));
            bme.setMinProduction(Integer.parseInt(minProduction));
            bme.setMaxProduction(Integer.parseInt(maxProduction));

            CampaignMain.campaignMain.getBlackMarketEquipmentTable().put(key, bme);

        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

        //NOTE:
        //NO MODMAIL for setting changes. Server Config GUI would spam too much.

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
