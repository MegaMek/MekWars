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

public class AdminSaveFactionConfigsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
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
            server.campaign.CampaignMain.cm.toUser("Invalid syntax. Try: AdminSaveFactionConfigs#faction",
                  Username,
                  true);
            return;
        }

        server.campaign.SHouse h = server.campaign.CampaignMain.cm.getHouseFromPartialString(faction, Username);

        if (h == null) {return;}

        // Need to repopulate this in case they've changed.
        h.populateUnitLimits();
        h.populateBMLimits();

        h.saveConfigFile();
        h.setUsedMekBayMultiplier(h.getFloatConfig("UsedPurchaseCostMulti"));
        server.campaign.CampaignMain.cm.toUser("AM:Status saved!", Username, true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " has saved " + faction + "'s configs");

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
