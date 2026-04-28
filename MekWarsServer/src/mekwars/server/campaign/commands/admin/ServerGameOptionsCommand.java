/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - nmorris (urgru@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

/**
 * @author Torren (Jason Tighe) 08/18/2005
 *       <p>
 *       Receives the MegaMek games options from the client. parses it and saves it.
 */

package mekwars.server.campaign.commands.admin;


public class ServerGameOptionsCommand implements server.campaign.commands.Command {

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

        if (!command.hasMoreElements()) {
            server.campaign.CampaignMain.cm.toUser(
                  "No file data found. This may be due to the fact that the command line was used intead of the GUI.",
                  Username,
                  true);
            return;
        }

        server.campaign.CampaignMain.cm.saveMegaMekGameOptions(command);

        server.campaign.CampaignMain.cm.getMegaMekClient().getGame().getOptions().loadOptions();

        server.campaign.CampaignMain.cm.toUser("You have set the MegaMek Game Options", Username, true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has set the MegaMek game options for the server.");
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
