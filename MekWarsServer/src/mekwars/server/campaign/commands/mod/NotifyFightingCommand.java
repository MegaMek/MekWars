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

package mekwars.server.campaign.commands.mod;

import common.House;

public class NotifyFightingCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Message";

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

        //load the message
        String Message = (String) command.nextElement();

        //send to all fighters from all houses
        for (House h : server.campaign.CampaignMain.cm.getData().getAllHouses()) {
            server.campaign.SHouse currH = (server.campaign.SHouse) h;
            for (String currName : currH.getFightingPlayers().keySet()) {
                server.campaign.CampaignMain.cm.toUser("PM|SERVER|" + Message, currName, false);
            }
        }

        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " sent a message to all fighting players: " + Message);
        server.campaign.CampaignMain.cm.toUser("Message sent to all fighting players: " + Message, Username, true);

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}//end notifyfightingcommand.java
