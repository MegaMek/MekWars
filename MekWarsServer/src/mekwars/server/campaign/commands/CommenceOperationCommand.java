/*
 * MekWars - Copyright (C) 2006
 *
 * Created by Jason Tighe(Torren)
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

import common.campaign.operations.Operation;
import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;

/**
 * This command is used for multi player opertionas. the attacker will send this command to start an operation
 */
public class CommenceOperationCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

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

        //get the Op ID and the Army ID
        int opID = -1;
        String confirm;
        try {
            opID = Integer.parseInt(command.nextToken());
            confirm = command.nextToken();
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper format. Try: /c commenceOperation#Op number#CONFIRM",
                  Username,
                  true);
            return;
        }

        //get the player
        server.campaign.SPlayer ap = CampaignMain.campaignMain.getPlayer(Username);
        if (ap == null) {
            CampaignMain.campaignMain.toUser("AM:Null player. Report this immediately!", Username, true);
            return;
        }

        if (!confirm.equals("CONFIRM")) {
            CampaignMain.campaignMain.toUser("AM:This command must be confirmed!", Username, true);
            return;
        }

        //check the attack
        server.campaign.operations.ShortOperation so = CampaignMain.campaignMain.getOpsManager()
                                                             .getRunningOps()
                                                             .get(opID);
        if (so == null) {
            CampaignMain.campaignMain.toUser("AM:Operation #" + opID + " does not exist.", Username, true);
            return;
        }

        //check FFA
        Operation o = CampaignMain.campaignMain.getOpsManager().getOperation(so.getName());
        if (!o.getBooleanValue("FreeForAllOperation")) {
            CampaignMain.campaignMain.toUser("AM:This command can only be used on Free For All Operations!",
                  Username);
            return;
        }

        if (!so.getAttackers().containsKey(ap.getName().toLowerCase())) {
            CampaignMain.campaignMain.toUser("AM:Only the attacker may commence this operation! ",
                  Username,
                  true);
            return;
        }

        //make sure the operation is accepting defenders
        if (so.getStatus() != server.campaign.operations.ShortOperation.STATUS_WAITING) {

            if (so.getStatus() == server.campaign.operations.ShortOperation.STATUS_FINISHED) {
                CampaignMain.campaignMain.toUser("AM:Operation #" + opID + " is finished.", Username, true);
                return;
            }

            //else
            CampaignMain.campaignMain.toUser("AM:Operation #" + opID + " has already commenced.", Username, true);
            return;
        }

        int minPlayers = 3;
        try {
            minPlayers = o.getIntValue("MinNumberOfPlayers");
        } catch (Exception ex) {}

        if ((so.getDefenders().size() + so.getAttackers().size()) < minPlayers) {
            CampaignMain.campaignMain.toUser("AM:Operation #" +
                                                   opID +
                                                   " does not have enough players to commence!<br>At least " +
                                                   minPlayers +
                                                   " players are needed.", Username, true);
            return;
        }

        so.changeStatus(server.campaign.operations.ShortOperation.STATUS_INPROGRESS);

        //tell the defender that he has succesfully joined the attack.
        MWLogger.gameLog("Operation Commenced: " + so.getShortID() + "/" + ap.getName());
        CampaignMain.campaignMain.toUser("AM:Operation Commenced!", Username, true);

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

}//end CommenceOperationCommand
