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

package mekwars.server.campaign.commands;

import mekwars.server.campaign.CampaignMain;

/**
 * @author Salient for miniCampaigns, will report to user status of their campaign
 */
public class ReportStatusMC implements Command {

    int accessLevel = 1;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {
        //access level checks
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);

        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        if (!Boolean.parseBoolean(CampaignMain.campaignMain.getConfig("Enable_MiniCampaign"))) {
            CampaignMain.campaignMain.toUser("AM:This command is disabled on this server.", Username, true);
            return;
        }

        p.reportStatusMC();
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
