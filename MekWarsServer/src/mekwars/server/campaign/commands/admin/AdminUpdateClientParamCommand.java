/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - jtighe (torren@sourceforge.net)
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

import mekwars.server.campaign.CampaignMain;

/**
 * AdminUpdateClientParam allows an admin to set a client param for a single player or all players online. Unlike other
 * commands, this cannot be delegated to lower userlevels. Instead, it is locked IAuthenticator.ADMIN.
 * <p>
 * Format: adminupdateclientparam#targetname[ALL]#param#param value
 */
public class AdminUpdateClientParamCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Player Name[ALL]#param#param value";

    public String getSyntax() {return syntax;}

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}//cannot be changed

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check. hard check'ed to admin.
        if (CampaignMain.campaignMain.getServer().getUserLevel(Username) <
                  server.MWChatServer.auth.IAuthenticator.ADMIN) {
            CampaignMain.campaignMain.toUser("Only admins may use the update client param command.",
                  Username,
                  true);
            return;
        }

        String playerName;
        String param;
        String paramValue;
        try {
            playerName = command.nextToken();
            param = command.nextToken();
            paramValue = command.nextToken();
        } catch (java.util.NoSuchElementException e) {
            CampaignMain.campaignMain.toUser(
                  "Improper format. Try: /adminupdateclientparam player Name[ALL]#param#param value",
                  Username,
                  true);
            return;
        }

        //ensure the player exits
        if (!playerName.equalsIgnoreCase("all") && CampaignMain.campaignMain.getPlayer(playerName) == null) {
            CampaignMain.campaignMain.toUser("update client param failed. Could not find player: " + playerName,
                  Username,
                  true);
            return;
        }

        if (!playerName.equalsIgnoreCase("all")) {
            CampaignMain.campaignMain.toUser("PL|UCP|" + param + "|" + paramValue, playerName, false);
        } else {
            CampaignMain.campaignMain.doSendToAllOnlinePlayers("PL|UCP|" + param + "|" + paramValue, false);
        }

        //checks passed. we have a valid player and command name. tell everyone about the spoof ...
        //MWLogger.modLog(Username + " used update client param to update " + playerName + "'s " + param + " to " + paramValue);
        CampaignMain.campaignMain.doSendModMail("WARNING",
              Username + " used update client param to update " + playerName + "'s " + param + " to " + paramValue);
        CampaignMain.campaignMain.toUser("you updated " + playerName + "'s " + param + " to " + paramValue,
              Username,
              true);

    }//end process()
}//end AdminTerminateAllCommand
