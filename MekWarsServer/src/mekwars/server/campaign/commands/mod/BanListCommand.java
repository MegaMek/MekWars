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
 * Moving the BanList command from MWServ into the normal command structure.
 * <p>
 * Syntax /c BanList
 */
public class BanListCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser(
                      "AM:Insufficient access level for command. Level: "
                            + userLevel + ". Required: " + accessLevel
                            + ".", Username, true);
                return;
            }
        }

        String result = "Banned accounts:<br>";
        int i = 1;

        java.util.concurrent.ConcurrentHashMap<String, String> banHash = new java.util.concurrent.ConcurrentHashMap<String, String>(
              CampaignMain.campaignMain.getServer().getBanAccounts());
        for (String banName : banHash.keySet()) {
            String banTime = banHash.get(banName);

            // Banned Accounts Username Check
            Long until = Long.valueOf(banTime);

            // If they are no longer banned remove them from the list and don't
            // display them
            if (until.longValue() < System.currentTimeMillis()
                      || until.longValue() == 0) {
                CampaignMain.campaignMain.getServer().getBanAccounts().remove(banName);
                CampaignMain.campaignMain.getServer().bansUpdate();
                continue;
            }

            Long l = Long.valueOf(banTime);
            result += Integer.toString(i++);
            result += ") ";
            result += banName;
            result += " [unban at ";
            result += new java.util.Date(l).toString();
            result += "]<br>";
        }

        i = 1;
        result += "Banned IPs:<br>";
        java.util.concurrent.ConcurrentHashMap<java.net.InetAddress, Long> banIpHash = new java.util.concurrent.ConcurrentHashMap<java.net.InetAddress, Long>(
              CampaignMain.campaignMain.getServer().getBanIps());

        for (java.net.InetAddress currAddress : banIpHash.keySet()) {
            Long until = CampaignMain.campaignMain.getServer().getBanIps().get(currAddress);

            // If they are no longer banned remove them from the list and don't
            // display them
            if (until.longValue() < System.currentTimeMillis()
                      || until.longValue() == 0) {
                CampaignMain.campaignMain.getServer().getBanIps().remove(currAddress);
                CampaignMain.campaignMain.getServer().bansUpdate();
                continue;
            }

            result += i++ + ") " + currAddress.toString() + " [unban at "
                            + new java.util.Date(until.longValue()).toString() + "]<br>";
        }

        CampaignMain.campaignMain.toUser(result, Username);
        CampaignMain.campaignMain.doSendModMail("NOTE", Username
                                                              + " checked the ban list.");
        // MWLogger.modLog(Username + " checked the ban list.");
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}
