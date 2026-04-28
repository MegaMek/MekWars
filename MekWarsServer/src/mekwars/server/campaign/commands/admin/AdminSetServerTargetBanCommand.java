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


public class AdminSetServerTargetBanCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Banned TargetSystem String - list of integers separated by #";

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

        server.campaign.CampaignMain.cm.getData().getBannedTargetingSystems().clear();
        java.util.Vector<Integer> bans = new java.util.Vector<Integer>(1, 1);
        while (command.hasMoreTokens()) {
            bans.add(Integer.parseInt(command.nextToken()));
        }
        server.campaign.CampaignMain.cm.getData().setBannedTargetingSystems(bans);

        // Send updates to everyone
        StringBuilder sb = new StringBuilder();
        sb.append("SBT|");
        for (int ban : bans) {
            sb.append(ban);
            sb.append("|");
        }
        server.campaign.CampaignMain.cm.saveBannedTargetSystems();
        server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers(sb.toString(), false);
        server.campaign.CampaignMain.cm.toUser("AM: Server Target Bans set", Username, true);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
