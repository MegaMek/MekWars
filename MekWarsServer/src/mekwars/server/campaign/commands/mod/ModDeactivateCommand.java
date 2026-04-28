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

public class ModDeactivateCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

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

        String targetPlayerString = (String) command.nextElement();
        server.campaign.SPlayer targetPlayer = server.campaign.CampaignMain.cm.getPlayer(targetPlayerString);

        if (targetPlayer == null) {
            server.campaign.CampaignMain.cm.toUser(targetPlayerString + " cannot be found.", Username, true);
            return;
        }

        if (targetPlayer.getDutyStatus() == server.campaign.SPlayer.STATUS_FIGHTING) {
            server.campaign.CampaignMain.cm.toUser(targetPlayerString +
                                                         " is fighting. Cancel his game before deactivating.",
                  Username,
                  true);
            return;
        }

        if (targetPlayer.getDutyStatus() != server.campaign.SPlayer.STATUS_ACTIVE) {
            server.campaign.CampaignMain.cm.toUser(
                  "Target player is logged out or is already in reserve state. Nice try though.",
                  Username,
                  true);
            return;
        }

        //the above caught all non-active players, so can continure assuming activity.
        targetPlayer.setActive(false);
        server.campaign.CampaignMain.cm.toUser(Username + " forced you to return to reserve duty.",
              targetPlayerString,
              true);
        server.campaign.CampaignMain.cm.toUser("You forced " + targetPlayerString + " into reserve status.",
              Username,
              true);
        //server.MWLogger.modLog(Username + " force-deactivated " + targetPlayerString + ".");
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " force-deactivated " + targetPlayerString + ".'");
        server.campaign.CampaignMain.cm.sendPlayerStatusUpdate(targetPlayer, !Boolean.parseBoolean(
              server.campaign.CampaignMain.cm.getConfig("HideActiveStatus")));

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}//end ModCancelCommand
