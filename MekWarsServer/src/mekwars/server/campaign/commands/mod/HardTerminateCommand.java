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

public class HardTerminateCommand implements server.campaign.commands.Command {

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
            CampaignMain.campaignMain.toUser("Improper format. Try: /c hardterminate#game number",
                  Username,
                  true);
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
            CampaignMain.campaignMain.toUser("Terminate failed. Attack #" + opID + " does not exist.",
                  Username,
                  true);
            return;
        }

        //terminate
        CampaignMain.campaignMain.getOpsManager()
              .terminateOperation(so, server.campaign.operations.OperationManager.TERM_TERMCOMMAND, tp, true);

        CampaignMain.campaignMain.toUser("AM:You hard-terminated Attack #" + opID + ".", Username, true);
        //server.MWLogger.modLog(Username + " hard-terminated Attack #" + opID + ".");
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " hard-terminated Attack #" + opID + ".");

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}//end ModCancelCommand
