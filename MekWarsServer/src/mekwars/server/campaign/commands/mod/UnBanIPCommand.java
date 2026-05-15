/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - Jason Tighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package mekwars.server.campaign.commands.mod;


import mekwars.server.campaign.CampaignMain;

/**
 * Moving the UnBanIP command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c UnBanIP#Number
 */
public class UnBanIPCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Number";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        try {
            //make sure that the an acceptably formatted ip was sent and checks if it is on the list and if yes then delete
            java.net.InetAddress ip = null;
            String toUnBan = command.nextToken().trim();//ip to unban
            ip = java.net.InetAddress.getByName(toUnBan);
            if (!CampaignMain.campaignMain.getServer().getBanIps().containsKey(ip)) {
                CampaignMain.campaignMain.toUser("AM:Value (" + ip + ") not found in banlist.", Username);
                return;
            }
            CampaignMain.campaignMain.getServer().getBanIps().remove(ip);
            CampaignMain.campaignMain.getServer().bansUpdate();
            CampaignMain.campaignMain.toUser("AM:You unbanned: " + ip.toString(), Username);
            CampaignMain.campaignMain.doSendModMail("NOTE", Username + " unbanned " + ip.toString());
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser(
                  "AM:Syntax: unbanip (IPAddress)<br>Where IPAddress corresponds to the IPAddress in the ipban list like 12.12.12.12.",
                  Username);
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
