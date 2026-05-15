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

package mekwars.server.campaign.commands.mod;

import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;


/**
 * Moving the Kick command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c Kick#Player
 */
public class KickCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

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

        String toKick = null;
        try {
            toKick = command.nextToken();
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("Improper format. Try: /c kick#NAME", Username);
            return;
        }

        if (CampaignMain.campaignMain.getServer().isAdmin(toKick) && !Username.startsWith("[Dedicated]")) {
            CampaignMain.campaignMain.toUser("AM:You may not kick an admin.", Username);
            return;
        }

        CampaignMain.campaignMain.toUser("AM:You were kicked by " + Username, toKick, true);
        CampaignMain.campaignMain.toUser("PL|GBB|Bye Bye", toKick, false);

        //Use this to kick ghost players from the clients.
        CampaignMain.campaignMain.getServer().sendRemoveUserToAll(toKick, false);
        CampaignMain.campaignMain.getServer().sendChat("AM:" + Username + " kicked " + toKick);
        MWLogger.modLog(Username + " kicked " + toKick);

		/*try {
			Thread.sleep(100);//Why do we sleep here? Anyone?
		} catch (Exception ex) {
			MWLogger.errLog(ex);
		} */

        try {

            CampaignMain.campaignMain.getOpsManager().doDisconnectCheckOnPlayer(toKick);
            CampaignMain.campaignMain.doLogoutPlayer(toKick);
            if (CampaignMain.campaignMain.getServer()
                      .getClient(server.MWChatServer.MWChatServer.clientKey(toKick)) != null) {
                CampaignMain.campaignMain.getServer().killClient(toKick, Username);
            }

        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }


    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
