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
 * Moving the unban command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c unban#name
 */
public class UnBanCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

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
            String account = command.nextToken().toLowerCase();
            if (server.campaign.CampaignMain.cm.getServer().getBanAccounts().get(account) != null) {
                server.campaign.CampaignMain.cm.getServer().getBanAccounts().remove(account);
                server.campaign.CampaignMain.cm.getServer().bansUpdate();
                server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " unbanned " + account);
                //MWLogger.modLog(Username + " unbanned " + account);

                server.campaign.CampaignMain.cm.toUser("AM:You unbanned " + account, Username);
                server.campaign.CampaignMain.cm.toUser(
                      "AM:Don't forget to unban any assotiated IP's as well with the unbanip command",
                      Username);
            } else {
                server.campaign.CampaignMain.cm.toUser("AM:Unban failed for " + account, Username);
                server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                      Username + " tried to uban " + account + ", but failed.");
                //MWLogger.modLog(Username + " tried to uban " + account + ", but failed.");
            }
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Syntax: unban (Username)<br>Don't forget to unban any assotiated IP's as well with the unbanip command",
                  Username);
        }
    }
}
