/*
 * MekWars - Copyright (C) 2007
 *
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

/**
 * @author jtighe This Command is used bye server admins to change config items on the fly while the server is
 *       still running.
 *
 */
package mekwars.server.campaign.commands.admin;

public class AdminChangeFactionConfigCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "house#config#arg";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

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

        //get config var and new setting
        String house = command.nextToken();
        String config = command.nextToken();
        String arg = command.nextToken();

        server.campaign.SHouse h = server.campaign.CampaignMain.cm.getHouseFromPartialString(house);
        //make setting change
        h.getConfig().setProperty(config, arg);

        //NOTE:
        //NO MODMAIL for setting changes. Server Config GUI would spam too much.

    }//end process
}
