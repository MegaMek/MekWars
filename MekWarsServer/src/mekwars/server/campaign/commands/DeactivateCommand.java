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

import server.campaign.util.scheduler.MWScheduler;

/**
 * A command that takes a player out of active status.
 *
 * @version 2016.10.06
 */
public class DeactivateCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

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

        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Null player. Big Failure. Report to admin immediately.",
                  Username,
                  true);
            return;
        }

        int currentStatus = p.getDutyStatus();
        if (currentStatus == server.campaign.SPlayer.STATUS_RESERVE) {
            server.campaign.CampaignMain.cm.toUser("AM:You're already on reserve duty.", Username, true);
            return;
        }

        if (currentStatus == server.campaign.SPlayer.STATUS_FIGHTING) {
            server.campaign.CampaignMain.cm.toUser("AM:You're currently fighting! You cannot go into the reserve!",
                  Username,
                  true);
            return;
        }

        if (System.currentTimeMillis() - p.getActiveSince() <
                  Long.parseLong(server.campaign.CampaignMain.cm.getConfig("MinActiveTime")) * 1000) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You haven't even reached the front yet! (Must meet minimum activity requirement before deactivating)",
                  Username,
                  true);
            return;
        }

        // Stop the Activity Jobs
        MWScheduler.getInstance().deactivateUser(p.getName());

        p.setActive(false);
        p.leechCount = 0;
        server.campaign.CampaignMain.cm.toUser("AM:[*] You've left active duty and are now in reserve.",
              Username,
              true);

        /*
         * Note: Old code to stop offensive tasks was here. This is handled
         * elsewhere now (in SHouse.doLogout and SPlayer.setActive).
         */

        server.campaign.CampaignMain.cm.sendPlayerStatusUpdate(p,
              !Boolean.parseBoolean(server.campaign.CampaignMain.cm.getConfig("HideActiveStatus")));
    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

}//end DeactivateCommand class
