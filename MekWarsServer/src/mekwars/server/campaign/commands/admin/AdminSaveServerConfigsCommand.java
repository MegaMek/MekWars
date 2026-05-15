/*
 * MekWars - Copyright (C) 2004
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
 * @author jtighe
 *       <p>
 *       Command Saves the server config to its defined file
 */
package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;

public class AdminSaveServerConfigsCommand implements server.campaign.commands.Command {

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

        server.campaign.DefaultServerOptions dso = new server.campaign.DefaultServerOptions();
        dso.createConfig();
        CampaignMain.campaignMain.toUser("AM:Status saved!", Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " has saved the server configs");

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
