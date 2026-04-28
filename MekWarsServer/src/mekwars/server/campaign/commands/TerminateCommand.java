/*
 * MekWars - Copyright (C) 2005
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

/**
 * Terminate command is analagous to the old cancel command used for Tasks. Should be mirrored in the CampaignMain
 * command tree.
 */
public class TerminateCommand implements Command {

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

        //get the Op ID and the Army ID
        int opID = -1;
        server.campaign.operations.ShortOperation so = null;

        //get the player
        server.campaign.SPlayer tp = server.campaign.CampaignMain.cm.getPlayer(Username);
        if (tp == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Null player. Report this immediately!", Username, true);
            return;
        }

        if (command.hasMoreTokens()) {
            try {
                opID = Integer.parseInt(command.nextToken());
                so = server.campaign.CampaignMain.cm.getOpsManager().getRunningOps().get(opID);
            } catch (Exception e) {
                server.campaign.CampaignMain.cm.toUser("AM:Improper format. Try: /c terminate#attack number",
                      Username,
                      true);
                return;
            }
        } else {
            so = server.campaign.CampaignMain.cm.getOpsManager().getShortOpForPlayer(tp);
        }

        //check the attack

        if (so == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Terminate failed. Attack #" + opID + " does not exist.",
                  Username,
                  true);
            return;
        }

        //if the player isnt in the game, reject
        if (!so.getAllPlayerNames().contains(tp.getName().toLowerCase())) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Terminate failed. You must be a participant in order to terminate an Attack.",
                  Username,
                  true);
            return;
        }

        //don't cancel finished or reporting games
        if (so.getStatus() == server.campaign.operations.ShortOperation.STATUS_FINISHED ||
                  so.getStatus() == server.campaign.operations.ShortOperation.STATUS_REPORTING) {
            server.campaign.CampaignMain.cm.toUser("AM:Terminate failed. You may not terminate a completed game.",
                  Username,
                  true);
            return;
        }

        if (so.getStatus() == server.campaign.operations.ShortOperation.STATUS_WAITING) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Terminate failed. You may not terminate a game that has yet to start!",
                  Username);
            return;
        }

        so.getCancelledPlayers().add(Username.toLowerCase());

        // Check if opponents are offline.  Otherwise, this game cannot be cancelled by a user who has been disconnected on
        for (String currPlayerName : so.getAllPlayerNames()) {
            if (server.campaign.CampaignMain.cm.getPlayer(currPlayerName).getDutyStatus() ==
                      server.campaign.SPlayer.STATUS_LOGGEDOUT) {
                if (!so.getCancelledPlayers().contains(currPlayerName.toLowerCase())) {
                    so.getCancelledPlayers().add(currPlayerName.toLowerCase());
                }
            }
        }

        if (so.getCancelledPlayers().size() >= so.getAllPlayerNames().size()) {

            String msg = "Cancelling Operation " + so.getName();
            for (String currPlayerName : so.getAllPlayerNames()) {
                server.campaign.CampaignMain.cm.toUser(msg, currPlayerName);
            }
            //terminate
            server.campaign.CampaignMain.cm.getOpsManager()
                  .terminateOperation(so, server.campaign.operations.OperationManager.TERM_TERMCOMMAND, tp);
        } else {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Informing all other participants of your wish to cancel the operation.",
                  Username);

            String msg = "AM:" +
                               Username +
                               " wishes to cancel Operation #" +
                               so.getShortID() +
                               " " +
                               so.getName() +
                               " <a href=\"MEKWARS/c terminate#" +
                               so.getShortID() +
                               "\">click here to confirm</a>";
            for (String currPlayerName : so.getAllPlayerNames()) {

                if (!so.getCancelledPlayers().contains(currPlayerName.toLowerCase()) &&
                          !Username.equalsIgnoreCase(currPlayerName)) {
                    server.campaign.CampaignMain.cm.toUser(msg, currPlayerName);
                }
            }
        }
    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

}//end TerminateCommand
