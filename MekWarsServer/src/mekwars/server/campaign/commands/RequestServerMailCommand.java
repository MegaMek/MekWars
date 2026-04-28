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

// refreshfactory#planet#factory
public class RequestServerMailCommand implements Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.GUEST;
    String syntax = "";

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

        if (server.campaign.CampaignMain.cm.getServer().getServerMail().get(Username.toLowerCase()) != null) {
            server.campaign.CampaignMain.cm.toUser("PM|SERVER|" +
                                                         (server.campaign.CampaignMain.cm.getServer()
                                                                .getServerMail()
                                                                .get(Username.toLowerCase())), Username, false);
            server.campaign.CampaignMain.cm.getServer().getServerMail().remove(Username.toLowerCase());
            server.campaign.CampaignMain.cm.getServer().doWriteMailFile();
        } else {
            server.campaign.CampaignMain.cm.toUser("AM:Sorry but you do not have any mail waiting for you.",
                  Username,
                  true);
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
