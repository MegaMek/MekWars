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


import mekwars.server.campaign.CampaignMain;

public class ServerGameOptionsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

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

        if (!command.hasMoreElements()) {
            CampaignMain.campaignMain.toUser(
                  "No file data found. This may be due to the fact that the command line was used intead of the GUI.",
                  Username,
                  true);
            return;
        }

        CampaignMain.campaignMain.saveMegaMekGameOptions(command);

        CampaignMain.campaignMain.getMegaMekClient().getGame().getOptions().loadOptions();

        CampaignMain.campaignMain.toUser("You have set the MegaMek Game Options", Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " has set the MegaMek game options for the server.");
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
