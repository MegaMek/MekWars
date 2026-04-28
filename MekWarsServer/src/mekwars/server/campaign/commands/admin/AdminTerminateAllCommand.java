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

package mekwars.server.campaign.commands.admin;

public class AdminTerminateAllCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

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

        //get the player
        server.campaign.SPlayer tp = server.campaign.CampaignMain.cm.getPlayer(Username);
        if (tp == null) {
            server.campaign.CampaignMain.cm.toUser("Null player. Report this immediately!", Username, true);
            return;
        }

        //determine which should cancel
        java.util.ArrayList<server.campaign.operations.ShortOperation> opsToCancel = new java.util.ArrayList<server.campaign.operations.ShortOperation>();
        for (server.campaign.operations.ShortOperation currO : server.campaign.CampaignMain.cm.getOpsManager()
                                                                     .getRunningOps()
                                                                     .values()) {
            if (currO.getStatus() != server.campaign.operations.ShortOperation.STATUS_REPORTING &&
                      currO.getStatus() != server.campaign.operations.ShortOperation.STATUS_FINISHED) {
                opsToCancel.add(currO);
            }
        }

        //do the cancelling
        for (server.campaign.operations.ShortOperation currO : opsToCancel) {
            server.campaign.CampaignMain.cm.getOpsManager()
                  .terminateOperation(currO, server.campaign.operations.OperationManager.TERM_TERMCOMMAND, tp);
        }

        //MWLogger.modLog(Username + " terminated all unfinished games.");
        server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers(Username + " terminated all unfinished games.", true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " terminated all unfinished games.");

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}//end AdminTerminateAllCommand
