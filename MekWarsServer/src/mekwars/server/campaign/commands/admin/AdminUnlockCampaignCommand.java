/*
 * MekWars - Copyright (C) 2004
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

public class AdminUnlockCampaignCommand implements server.campaign.commands.Command {

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

        if (Boolean.parseBoolean(server.campaign.CampaignMain.cm.getConfig("CampaignLock")) != true) {
            server.campaign.CampaignMain.cm.toUser("AM:Campaign is already unlocked.", Username, true);
            return;
        }

        //reset the lock property so players can activate
        server.campaign.CampaignMain.cm.getConfig().setProperty("CampaignLock", "false");

        //tell the admin he has unlocked the campaign
        server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers("AM:" + Username + " unlocked the campaign!", true);
        server.campaign.CampaignMain.cm.toUser("AM:You unlocked the campaign. Players may now activate.",
              Username,
              true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " unlocked the campaign");

    }//end Process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}
