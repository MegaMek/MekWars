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
        if (server.campaign.CampaignMain.cm.getServer().getUserLevel(Username) <
                  server.MWChatServer.auth.IAuthenticator.ADMIN) {
            server.campaign.CampaignMain.cm.toUser("Only admins may use the spoof command.", Username, true);
            return;
        }

        String targetPlayerName;
        String targetCommandName;
        try {
            targetPlayerName = command.nextToken();
            targetCommandName = command.nextToken();
        } catch (java.util.NoSuchElementException e) {
            server.campaign.CampaignMain.cm.toUser(
                  "Improper format. Try: /c adminspoof#targetname#commandname#[command inputs]",
                  Username,
                  true);
            return;
        }

        //ensure the player exixts
        if (server.campaign.CampaignMain.cm.getPlayer(targetPlayerName) == null) {
            server.campaign.CampaignMain.cm.toUser("Spoof failed. Could not find player: " + targetPlayerName,
                  Username,
                  true);
            return;
        }

        //uppercase the command, and make sure it exists in CampaignMain tree.
        if (server.campaign.CampaignMain.cm.getServerCommands().get(targetCommandName.toUpperCase()) == null) {
            server.campaign.CampaignMain.cm.toUser("Spoof failed. Could not find command: " + targetCommandName,
                  Username,
                  true);
            return;
        }

        if (server.campaign.CampaignMain.cm.getServerCommands()
                  .get(targetCommandName.toUpperCase())
                  .getExecutionLevel() > server.campaign.CampaignMain.cm.getServer().getUserLevel(targetPlayerName)) {
            server.campaign.CampaignMain.cm.toUser(targetPlayerName +
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
        server.campaign.CampaignMain.cm.doSendModMail("WARNING",
              Username +
                    " used spoof to send a command as if he were " +
                    targetPlayerName +
                    ": /c " +
                    targetCommandName +
                    "#" +
                    issuedCommand);
        server.campaign.CampaignMain.cm.toUser(Username +
                                                     " issued a command on your behalf: /c " +
                                                     targetCommandName +
                                                     "#" +
                                                     issuedCommand, targetPlayerName, true);
        server.campaign.CampaignMain.cm.toUser("You issued a command as if you were " +
                                                     targetPlayerName +
                                                     ": /c " +
                                                     targetCommandName +
                                                     "#" +
                                                     issuedCommand, Username, true);

        // ... then actually do it.
        server.campaign.CampaignMain.cm.getServerCommands()
              .get(targetCommandName.toUpperCase())
              .process(newCommand, targetPlayerName);

    }//end process()
}//end AdminTerminateAllCommand
