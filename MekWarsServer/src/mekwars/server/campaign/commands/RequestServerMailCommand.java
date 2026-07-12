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
 * @author Torren (Jason Tighe) sends server stored mail to the user!
 *
 */
package mekwars.server.campaign.commands;

import mekwars.server.campaign.CampaignMain;

// refreshfactory#planet#factory
public class RequestServerMailCommand implements Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.GUEST;
    String syntax = "";

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

        if (CampaignMain.campaignMain.getServer().getServerMail().get(Username.toLowerCase()) != null) {
            CampaignMain.campaignMain.toUser("PM|SERVER|" +
                                                   (CampaignMain.campaignMain.getServer()
                                                          .getServerMail()
                                                          .get(Username.toLowerCase())), Username, false);
            CampaignMain.campaignMain.getServer().getServerMail().remove(Username.toLowerCase());
            CampaignMain.campaignMain.getServer().doWriteMailFile();
        } else {
            CampaignMain.campaignMain.toUser("AM:Sorry but you do not have any mail waiting for you.",
                  Username,
                  true);
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
