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
 * @author jtighe
 *       <p>
 *       Command Saves the black market sales and Production data.
 */
package mekwars.server.campaign.commands.admin;

import common.Equipment;
import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;

public class AdminSaveBlackMarketConfigsCommand implements server.campaign.commands.Command {
    private static final MMLogger LOGGER = MMLogger.create(AdminSaveBlackMarketConfigsCommand.class);

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

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


        try {
            java.io.PrintStream ps = new java.io.PrintStream(new java.io.FileOutputStream(
                  "./data/blackmarketsettings.dat"));
            ps.println("#Timestamp=" + System.currentTimeMillis());

            for (String key : CampaignMain.campaignMain.getBlackMarketEquipmentTable().keySet()) {
                Equipment bme = CampaignMain.campaignMain.getBlackMarketEquipmentTable().get(key);
                if (bme.getMaxProduction() <= 0) {continue;}
                ps.print(bme.getEquipmentInternalName());
                ps.print("#");//if the maxCost is less then mincost set min cost to the same as max
                ps.print(Math.min(bme.getMaxCost(), bme.getMinCost()));
                ps.print("#");
                ps.print(bme.getMaxCost());
                ps.print("#");//if the maxProduction is less then minProduction set minProduction to the same as max.
                ps.print(Math.min(bme.getMaxProduction(), bme.getMinProduction()));
                ps.print("#");
                ps.print(bme.getMaxProduction());
                ps.println("#");
            }
            ps.close();
        } catch (java.io.FileNotFoundException fe) {
            LOGGER.error("blackmarketsettings.dat not found");
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }

        CampaignMain.campaignMain.toUser("AM:Black Market Settings saved!", Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " has saved the Black Market Settings");

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
