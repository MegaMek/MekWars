/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - Jason Tighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package mekwars.server.campaign.commands.admin;

import common.util.MWLogger;
import server.campaign.util.scheduler.MWScheduler;
import server.util.MWPasswd;


/**
 * Moving the Shutdown command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c Shutdown
 */
public class ShutdownCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }
        MWScheduler.getInstance().shutdown();

        server.campaign.CampaignMain.cm.getMarket().removeAllListings();
        server.campaign.CampaignMain.cm.toFile();
        server.campaign.CampaignMain.cm.forceSavePlayers(Username);
        server.campaign.CampaignMain.cm.saveBannedAmmo();
        server.campaign.CampaignMain.cm.getDefaultPlayerFlags().save();
        server.campaign.CampaignMain.cm.toUser("AM:You halted the server. Have a nice day.", Username, true);
        MWLogger.infoLog(Username + " halted the server. Have a nice day!");
        server.campaign.CampaignMain.cm.addToNewsFeed("Server halted!", "Server News", "");
        server.campaign.CampaignMain.cm.postToDiscord("Server halted!");
        try {
            MWPasswd.save();
        } catch (Exception ex) {
            MWLogger.errLog("Unable to save passwords before shutdown!");
            MWLogger.errLog(ex);
        }

        System.exit(0);
    }
}
