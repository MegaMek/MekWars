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
 *       Command Saves the server config to its defined file
 */
package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;

public class AdminSaveFactionConfigsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name";

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

        String faction = "";

        try {
            faction = command.nextToken();
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("Invalid syntax. Try: AdminSaveFactionConfigs#faction",
                  Username,
                  true);
            return;
        }

        server.campaign.SHouse h = CampaignMain.campaignMain.getHouseFromPartialString(faction, Username);

        if (h == null) {return;}

        // Need to repopulate this in case they've changed.
        h.populateUnitLimits();
        h.populateBMLimits();

        h.saveConfigFile();
        h.setUsedMekBayMultiplier(h.getFloatConfig("UsedPurchaseCostMulti"));
        CampaignMain.campaignMain.toUser("AM:Status saved!", Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " has saved " + faction + "'s configs");

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
