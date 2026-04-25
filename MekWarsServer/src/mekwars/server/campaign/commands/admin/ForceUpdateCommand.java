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

package mekwars.server.campaign.commands.admin;

import common.util.MWLogger;


/**
 * Allows SO's to force clients to update without a major version change
 * <p>
 * Syntax  /c forceupdate#Key#[Player/Dedicated/All]
 * <code>Player/Dedicated/All</code> are optional and will kick those entities
 * off so that they have to update right away.
 */
public class ForceUpdateCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    String syntax = "Update Key#[Player/Dedicated/All]";

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

        String updateKey = "";
        String whoToKick = "";

        try {
            updateKey = command.nextToken();
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("You must supply a Key<br>" +
                                                         "Syntax  /c forceupdate#Key[Clear]#[Player/Dedicated/All]<br>" +
                                                         "Player/Dedicated/All are optional and will kick those entities<br>" +
                                                         "off so that they have to update right away.", Username);
            return;
        }

        if (updateKey.equalsIgnoreCase("Clear") || updateKey.equalsIgnoreCase("-1")) {updateKey = "";}

        server.campaign.CampaignMain.cm.getConfig().setProperty("ForceUpdateKey", updateKey);
        server.campaign.DefaultServerOptions dso = new server.campaign.DefaultServerOptions();
        dso.createConfig();

        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " set the Force Update Key");
        server.campaign.CampaignMain.cm.toUser("Make sure to add UPDATEKEY=" + updateKey + "<br>To the serverdata.dat",
              Username);
        if (command.hasMoreTokens()) {
            whoToKick = command.nextToken();
            server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " is kicking " + whoToKick);
            boolean players = false;
            boolean deds = false;

            if (whoToKick.equalsIgnoreCase("all")) {
                players = true;
                deds = true;
            } else if (whoToKick.toLowerCase().startsWith("player")) {players = true;} else {deds = true;}

            java.util.concurrent.ConcurrentLinkedQueue<String> users = new java.util.concurrent.ConcurrentLinkedQueue<String>(
                  server.campaign.CampaignMain.cm.getServer().getUsers().keySet());
            for (String toKick : users) {
                if (server.campaign.CampaignMain.cm.getServer().isAdmin(toKick)) {continue;}
                if (players && !toKick.toLowerCase().startsWith("[dedicated]")) {
                    server.campaign.CampaignMain.cm.toUser("You have been forced to update by " + Username + "!",
                          toKick);
                    server.campaign.CampaignMain.cm.toUser("PL|FCU|Bye Bye", toKick, false);
                } else if (deds && toKick.toLowerCase().startsWith("[dedicated]")) {
                    try {
                        server.campaign.CampaignMain.cm.getServer().doStoreMail(toKick + ",update", Username);
                        Thread.sleep(120);
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                    }
                }
            }//end for
        }//end hasMore Commands
    }
}
