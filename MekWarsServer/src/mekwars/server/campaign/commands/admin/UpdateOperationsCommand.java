/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - jtighe (torren@users.sourceforge.net)
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

//Syntax updateoperations
public class UpdateOperationsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

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

        if (!server.campaign.CampaignMain.cm.getBooleanConfig("CampaignLock")) {
            server.campaign.CampaignMain.cm.toUser("The campaign must be locked before you can update the operations.",
                  Username,
                  true);
            return;
        }

        server.campaign.CampaignMain.cm.getOpsManager().loadOperations();

        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " ops manager updated.");

        server.campaign.CampaignMain.cm.updateAllOnlinePlayerArmies();
        server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers("PL|UDAO|1", false);
    }
}//end RetrieveOperation
