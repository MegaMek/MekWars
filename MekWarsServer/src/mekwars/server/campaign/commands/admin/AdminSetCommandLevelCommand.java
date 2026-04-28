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


public class AdminSetCommandLevelCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Command#Level";

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


        java.util.Hashtable<String, server.campaign.commands.Command> commandTable = server.campaign.CampaignMain.cm.getServerCommands();

        String commandName = command.nextToken().toUpperCase();
        int commandLevel = Integer.parseInt(command.nextToken());

        if (commandTable.containsKey(commandName)) {
            commandTable.get(commandName).setExecutionLevel(commandLevel);
        } else {
            server.campaign.CampaignMain.cm.toUser("Command " + commandName + " not found!", Username, true);
            return;
        }

        server.campaign.CampaignMain.cm.toUser("Command level changed on " +
                                                     commandName.toLowerCase() +
                                                     " to " +
                                                     commandLevel, Username, true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has changed the command level for " + commandName.toLowerCase() + " to " + commandLevel);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
