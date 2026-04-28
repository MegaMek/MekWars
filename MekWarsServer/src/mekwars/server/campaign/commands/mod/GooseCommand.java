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


public class GooseCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

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

        try {
            String player = command.nextToken();
            server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(player);

            if (p == null) {
                server.campaign.CampaignMain.cm.toUser("AM:Sorry you cannot find " + player + " to goose!", Username);
                return;
            }

            if (p.getName().equalsIgnoreCase("torren") ||
                      p.getName().equalsIgnoreCase("spork") ||
                      userLevel < server.campaign.CampaignMain.cm.getServer().getUserLevel(p.getName())) {
                server.campaign.CampaignMain.cm.toUser(p.getName() +
                                                             " grabs your hand and breaks it just before your able to goose 'em!",
                      Username);
                server.campaign.CampaignMain.cm.toUser(Username + " tried to goose you but you deftly avoided it!",
                      p.getName());
                server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                      Username + " tried to goose " + p.getName() + " and nearly lost their hand for it.");
                return;
            }


            server.campaign.CampaignMain.cm.toUser("AM:You goose " + p.getName() + ".", Username, true);
            server.campaign.CampaignMain.cm.toUser("AM:" + Username + " goosed you!", p.getName());
            server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " goosed " + p.getName() + ".");
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("AM:You really need to specify whom you would like to goose",
                  Username);
            return;
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
