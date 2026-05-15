/*
 * MekWars - Copyright (C) 2004
 *
 * Original Author - Nathan Morris (urgru@users.sourceforge.net)
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

package mekwars.server.campaign.commands.mod;

import mekwars.server.campaign.CampaignMain;

public class ModDeactivateCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

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

        String targetPlayerString = (String) command.nextElement();
        server.campaign.SPlayer targetPlayer = CampaignMain.campaignMain.getPlayer(targetPlayerString);

        if (targetPlayer == null) {
            CampaignMain.campaignMain.toUser(targetPlayerString + " cannot be found.", Username, true);
            return;
        }

        if (targetPlayer.getDutyStatus() == server.campaign.SPlayer.STATUS_FIGHTING) {
            CampaignMain.campaignMain.toUser(targetPlayerString +
                                                   " is fighting. Cancel his game before deactivating.",
                  Username,
                  true);
            return;
        }

        if (targetPlayer.getDutyStatus() != server.campaign.SPlayer.STATUS_ACTIVE) {
            CampaignMain.campaignMain.toUser(
                  "Target player is logged out or is already in reserve state. Nice try though.",
                  Username,
                  true);
            return;
        }

        //the above caught all non-active players, so can continure assuming activity.
        targetPlayer.setActive(false);
        CampaignMain.campaignMain.toUser(Username + " forced you to return to reserve duty.",
              targetPlayerString,
              true);
        CampaignMain.campaignMain.toUser("You forced " + targetPlayerString + " into reserve status.",
              Username,
              true);
        //server.MWLogger.modLog(Username + " force-deactivated " + targetPlayerString + ".");
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " force-deactivated " + targetPlayerString + ".'");
        CampaignMain.campaignMain.sendPlayerStatusUpdate(targetPlayer, !Boolean.parseBoolean(
              CampaignMain.campaignMain.getConfig("HideActiveStatus")));

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}//end ModCancelCommand
