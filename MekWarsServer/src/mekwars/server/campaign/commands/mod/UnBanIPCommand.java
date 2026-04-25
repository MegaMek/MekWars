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


/**
 * Moving the UnBanIP command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c UnBanIP#Number
 */
public class UnBanIPCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Number";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
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
            if (!server.campaign.CampaignMain.cm.getServer().getBanIps().containsKey(ip)) {
                server.campaign.CampaignMain.cm.toUser("AM:Value (" + ip + ") not found in banlist.", Username);
                return;
            }
            server.campaign.CampaignMain.cm.getServer().getBanIps().remove(ip);
            server.campaign.CampaignMain.cm.getServer().bansUpdate();
            server.campaign.CampaignMain.cm.toUser("AM:You unbanned: " + ip.toString(), Username);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " unbanned " + ip.toString());
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Syntax: unbanip (IPAddress)<br>Where IPAddress corresponds to the IPAddress in the ipban list like 12.12.12.12.",
                  Username);
        }
    }
}
