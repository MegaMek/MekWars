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

public class GrantInfluenceCommand implements server.campaign.commands.Command {

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
            p.addInfluence(amount);
            server.campaign.CampaignMain.cm.toUser("AM:You've been granted " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(false,
                                                               true,
                                                               amount,
                                                               true) +
                                                         " from " +
                                                         Username, p.getName(), true);
            server.campaign.CampaignMain.cm.toUser("AM:You granted " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(false,
                                                               true,
                                                               amount,
                                                               true) +
                                                         " to " +
                                                         p.getName(), Username, true);
            //server.MWLogger.modLog(Username + " granted " +CampaignMain.cm.moneyOrFluMessage(false,true,amount,true)+" to " + p.getName());
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username +
                        " granted " +
                        server.campaign.CampaignMain.cm.moneyOrFluMessage(false, true, amount, true) +
                        " to " +
                        p.getName());
        }
    }
}
