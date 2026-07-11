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
import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;
import server.campaign.util.scheduler.MWScheduler;
import server.util.MWPasswd;


/**
 * Moving the Shutdown command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c Shutdown
 */
public class ShutdownCommand implements server.campaign.commands.Command {
    private static final MMLogger LOGGER = MMLogger.create(ShutdownCommand.class);

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

    public String getSyntax() {return syntax;}

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
        MWScheduler.getInstance().shutdown();

        CampaignMain.campaignMain.getMarket().removeAllListings();
        CampaignMain.campaignMain.toFile();
        CampaignMain.campaignMain.forceSavePlayers(Username);
        CampaignMain.campaignMain.saveBannedAmmo();
        CampaignMain.campaignMain.getDefaultPlayerFlags().save();
        CampaignMain.campaignMain.toUser("AM:You halted the server. Have a nice day.", Username, true);
        LOGGER.info(Username + " halted the server. Have a nice day!");
        CampaignMain.campaignMain.addToNewsFeed("Server halted!", "Server News", "");
        CampaignMain.campaignMain.postToDiscord("Server halted!");
        try {
            MWPasswd.save();
        } catch (Exception ex) {
            LOGGER.error("Unable to save passwords before shutdown!");
            LOGGER.error(ex, "");
        }

        System.exit(0);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
