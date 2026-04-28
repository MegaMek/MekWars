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

package mekwars.server.campaign.commands.admin;

public class AdminResetPlayerCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Player Name/Faction Name/All#CONFIRM";

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

        //variables
        String resetType = "";
        String commandConfirmed = "";
        try {
            resetType = command.nextToken();
            commandConfirmed = command.nextToken();
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser(
                  "Improper command. Try: /c adminresetplayer#Player Name/Faction Name/All#CONFIRM",
                  Username,
                  true);
            return;
        }

        if (!commandConfirmed.equals("CONFIRM")) {
            server.campaign.CampaignMain.cm.toUser(
                  "Improper command. Try: /c adminresetplayer#Player Name/Faction Name/All#CONFIRM",
                  Username,
                  true);
            return;
        }

        if (resetType.equalsIgnoreCase("all")) {
            java.io.File[] playerList = new java.io.File("./campaign/players").listFiles();

            for (int i = 0; i < playerList.length; i++) {
                java.io.File playerFile = playerList[i];
                if (playerFile.isDirectory()) {continue;}

                server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(playerFile.getName()
                                                                                                 .substring(0,
                                                                                                       playerFile.getName()
                                                                                                             .indexOf(
                                                                                                                   ".dat")));

                if (player == null) {continue;}

                player.reset("CONFIRM");
                server.campaign.CampaignMain.cm.doLogoutPlayer(player.getName());

            }
            //server.MWLogger.modLog(Username + " has reset all player accounts.");
            server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " has reset all player accounts.");
        } else if (server.campaign.CampaignMain.cm.getHouseFromPartialString(resetType, null) != null) {
            java.io.File[] playerList = new java.io.File("./campaign/players").listFiles();

            for (int i = 0; i < playerList.length; i++) {
                java.io.File playerFile = playerList[i];
                if (playerFile.isDirectory()) {continue;}

                server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(playerFile.getName()
                                                                                                 .substring(0,
                                                                                                       playerFile.getName()
                                                                                                             .indexOf(
                                                                                                                   ".dat")));

                if (player == null) {continue;}

                if (player.getMyHouse().getName().equalsIgnoreCase(resetType)) {
                    player.reset("CONFIRM");
                    server.campaign.CampaignMain.cm.doLogoutPlayer(player.getName());
                }

            }
            //server.MWLogger.modLog(Username + " has reset all player accounts for faction "+resetType);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username + " has reset all player accounts for faction " + resetType);
        } else if (server.campaign.CampaignMain.cm.getPlayer(resetType) != null) {
            server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(resetType);
            player.reset("CONFIRM");
            server.campaign.CampaignMain.cm.doLogoutPlayer(player.getName());
            //server.MWLogger.modLog(Username + " has reset "+player.getName()+"'s account.");
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username + " has reset " + player.getName() + "'s account.");
        }

        server.campaign.CampaignMain.cm.forceSavePlayers(Username);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
