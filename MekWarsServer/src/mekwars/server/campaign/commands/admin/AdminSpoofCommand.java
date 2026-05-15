/*
 * MekWars - Copyright (C) 2004
 *
 * Original author - nmorris (urgru@users.sourceforge.net)
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
 * AdminSpoof allows an admin to issue ANY command on a player's behalf. Unlike other commands, this cannot be delegated
 * to lower userlevels. Instead, Spoof is locked IAuthenticator.ADMIN.
 * <p>
 * Format: adminspoof#targetname#command#[command options]
 */
public class AdminSpoofCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Target Name#command#[command options]";

    public String getSyntax() {return syntax;}

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}//cannot be changed

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        if (CampaignMain.campaignMain.getServer().getUserLevel(Username) <
                  server.MWChatServer.auth.IAuthenticator.ADMIN) {
            CampaignMain.campaignMain.toUser("Only admins may use the spoof command.", Username, true);
            return;
        }

        String targetPlayerName;
        String targetCommandName;
        try {
            targetPlayerName = command.nextToken();
            targetCommandName = command.nextToken();
        } catch (java.util.NoSuchElementException e) {
            CampaignMain.campaignMain.toUser(
                  "Improper format. Try: /c adminspoof#targetname#commandname#[command inputs]",
                  Username,
                  true);
            return;
        }

        //ensure the player exixts
        if (CampaignMain.campaignMain.getPlayer(targetPlayerName) == null) {
            CampaignMain.campaignMain.toUser("Spoof failed. Could not find player: " + targetPlayerName,
                  Username,
                  true);
            return;
        }

        //uppercase the command, and make sure it exists in CampaignMain tree.
        if (CampaignMain.campaignMain.getServerCommands().get(targetCommandName.toUpperCase()) == null) {
            CampaignMain.campaignMain.toUser("Spoof failed. Could not find command: " + targetCommandName,
                  Username,
                  true);
            return;
        }

        if (CampaignMain.campaignMain.getServerCommands()
                  .get(targetCommandName.toUpperCase())
                  .getExecutionLevel() > CampaignMain.campaignMain.getServer().getUserLevel(targetPlayerName)) {
            CampaignMain.campaignMain.toUser(targetPlayerName +
                                                   "'s access level is too low to use command " +
                                                   targetCommandName, Username);
            return;
        }

        //build a new string tokenizer to pass to the command, and a commandstring to show in logs, etc.
        StringBuilder issuedCommand = new StringBuilder();
        while (command.hasMoreTokens()) {issuedCommand.append(command.nextToken() + "#");}

        java.util.StringTokenizer newCommand = new java.util.StringTokenizer(issuedCommand.toString(),
              "#");//rebuild a tokenizer to pass

        //checks passed. we have a valid player and command name. tell everyone about the spoof ...
        //MWLogger.modLog(Username + " used spoof to send a command as if he were " + targetPlayerName + ": /c " + targetCommandName + "#" + issuedCommand);
        CampaignMain.campaignMain.doSendModMail("WARNING",
              Username +
                    " used spoof to send a command as if he were " +
                    targetPlayerName +
                    ": /c " +
                    targetCommandName +
                    "#" +
                    issuedCommand);
        CampaignMain.campaignMain.toUser(Username +
                                               " issued a command on your behalf: /c " +
                                               targetCommandName +
                                               "#" +
                                               issuedCommand, targetPlayerName, true);
        CampaignMain.campaignMain.toUser("You issued a command as if you were " +
                                               targetPlayerName +
                                               ": /c " +
                                               targetCommandName +
                                               "#" +
                                               issuedCommand, Username, true);

        // ... then actually do it.
        CampaignMain.campaignMain.getServerCommands()
              .get(targetCommandName.toUpperCase())
              .process(newCommand, targetPlayerName);

    }//end process()
}//end AdminTerminateAllCommand
