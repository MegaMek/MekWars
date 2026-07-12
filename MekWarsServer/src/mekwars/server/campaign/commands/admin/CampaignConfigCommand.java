/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

package mekwars.server.campaign.commands.admin;
import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;

public class CampaignConfigCommand implements server.campaign.commands.Command {
    private static final MMLogger LOGGER = MMLogger.create(CampaignConfigCommand.class);

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

        try {//Try to read the config file
            CampaignMain.campaignMain.getConfig().load(new java.io.FileInputStream(
                  CampaignMain.campaignMain.getServer().getConfigParam("CAMPAIGNCONFIG")));
        } catch (Exception ex) {
            LOGGER.error(ex, "");
            CampaignMain.campaignMain.toUser("Failed to read campaign config.", Username, true);
        }
        CampaignMain.campaignMain.toUser("Campaign config reread!", Username, true);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
