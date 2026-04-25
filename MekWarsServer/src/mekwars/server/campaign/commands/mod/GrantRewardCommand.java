/*
 * MekWars - Copyright (C) 2004
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

package mekwars.server.campaign.commands.mod;

public class GrantRewardCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name#Amount";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());
        int amount = Integer.parseInt(command.nextToken());
        if (p != null) {
            p.setReward(p.getReward() + amount);

            String toRecipient = "AM:" +
                                       Username +
                                       " granted you " +
                                       amount +
                                       " " +
                                       server.campaign.CampaignMain.cm.getConfig("RPLongName");
            if (amount > 0) {
                toRecipient += " [<a href=\"MWUSERP\">Use " +
                                     server.campaign.CampaignMain.cm.getConfig("RPShortName") +
                                     "</a>]";
            }
            toRecipient += ".";
            server.campaign.CampaignMain.cm.toUser(toRecipient, p.getName(), true);

            server.campaign.CampaignMain.cm.toUser("AM:You granted " +
                                                         amount +
                                                         " " +
                                                         server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                         " to " +
                                                         p.getName(), Username, true);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username +
                        " granted " +
                        amount +
                        " " +
                        server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                        " to " +
                        p.getName());
        }

    }//end process()

}
