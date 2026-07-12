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

public class ModTerminateCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Game Number";

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

        //get the Op ID and the Army ID
        int opID = -1;

        try {
            opID = Integer.parseInt(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("Improper format. Try: /c modterminate#game number", Username, true);
            return;
        }

        //get the player
        server.campaign.SPlayer tp = CampaignMain.campaignMain.getPlayer(Username);
        if (tp == null) {
            CampaignMain.campaignMain.toUser("Null player. Report this immediately!", Username, true);
            return;
        }

        //check the attack
        server.campaign.operations.ShortOperation so = CampaignMain.campaignMain.getOpsManager()
                                                             .getRunningOps()
                                                             .get(opID);
        if (so == null) {
            CampaignMain.campaignMain.toUser("AM:Terminate failed. Attack #" + opID + " does not exist.",
                  Username,
                  true);
            return;
        }

        //don't cancel finished or reporting games
        if (so.getStatus() == server.campaign.operations.ShortOperation.STATUS_FINISHED) {
            CampaignMain.campaignMain.toUser("AM:Terminate failed. You may not terminate a completed game.",
                  Username,
                  true);
            return;
        }

        //terminate
        CampaignMain.campaignMain.getOpsManager()
              .terminateOperation(so, server.campaign.operations.OperationManager.TERM_TERMCOMMAND, tp);

        //Make a string which holds involved players names. Use
        //an incrementing ocunter to make sure that formatting is
        //correct for a list of any given size.
        String players = "Players were ";
        int playerCounter = 1;
        int totalPlayers = so.getAllPlayerNames().size();
        for (String currPlayerName : so.getAllPlayerNames()) {
            playerCounter++;
            players += currPlayerName;
            if (playerCounter == totalPlayers) {players += " and ";} else if (playerCounter > totalPlayers) {
                players += ".";
            } else {players += ", ";}
        }

        CampaignMain.campaignMain.toUser("AM:You terminated Attack #" + opID + ". " + players, Username, true);
        //server.MWLogger.modLog(Username + " terminated Attack #" + opID + ". " + players);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " terminated Attack #" + opID + ". " + players);

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}//end ModCancelCommand
