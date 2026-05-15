/*
 * MekWars - Copyright (C) 2010
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


import mekwars.server.campaign.CampaignMain;

public class AdminSetServerTargetBanCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Banned TargetSystem String - list of integers separated by #";

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

        CampaignMain.campaignMain.getData().getBannedTargetingSystems().clear();
        java.util.Vector<Integer> bans = new java.util.Vector<Integer>(1, 1);
        while (command.hasMoreTokens()) {
            bans.add(Integer.parseInt(command.nextToken()));
        }
        CampaignMain.campaignMain.getData().setBannedTargetingSystems(bans);

        // Send updates to everyone
        StringBuilder sb = new StringBuilder();
        sb.append("SBT|");
        for (int ban : bans) {
            sb.append(ban);
            sb.append("|");
        }
        CampaignMain.campaignMain.saveBannedTargetSystems();
        CampaignMain.campaignMain.doSendToAllOnlinePlayers(sb.toString(), false);
        CampaignMain.campaignMain.toUser("AM: Server Target Bans set", Username, true);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
