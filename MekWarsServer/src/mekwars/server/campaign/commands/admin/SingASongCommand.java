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

import common.House;
import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;

public class SingASongCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Song Name";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        String startHouse = "";
        String request = "";
        server.campaign.SHouse faction = null;

        if (command.hasMoreTokens()) {request = command.nextToken();}

        if (command.hasMoreTokens()) {startHouse = command.nextToken();}

        if (request.equalsIgnoreCase("list")) {
            listSongs(Username);
            return;
        }

        String song = getSong(request);

        if (song == null) {
            listSongs(Username);
            return;
        }

        if (!startHouse.equals("")) {
            faction = CampaignMain.campaignMain.getHouseFromPartialString(startHouse, null);
        }

        CampaignMain.campaignMain.doSendToAllOnlinePlayers("AM:" +
                                                                 Username +
                                                                 " forces you all to sing " +
                                                                 request, true);
        java.util.StringTokenizer songLyrics = new java.util.StringTokenizer(song, "#");

        try {
            while (songLyrics.hasMoreTokens()) {
                String songLine = "";

                if (faction != null) {
                    for (server.campaign.SPlayer player : faction.getAllOnlinePlayers().values()) {
                        if (player.getDutyStatus() < server.campaign.SPlayer.STATUS_RESERVE) {continue;}
                        if (CampaignMain.campaignMain.getServer().isAdmin(player.getName())) {continue;}
                        if (player.getName().equalsIgnoreCase("Spork")) {continue;}
                        songLine = songLyrics.nextToken();
                        CampaignMain.campaignMain.doSendToAllOnlinePlayers(player.getName() + "|" + songLine,
                              true);
                        Thread.sleep(1000);
                    }
                }
                java.util.Iterator<House> factions = CampaignMain.campaignMain.getData()
                                                           .getAllHouses()
                                                           .iterator();
                while (factions.hasNext()) {
                    faction = (server.campaign.SHouse) factions.next();
                    for (server.campaign.SPlayer player : faction.getAllOnlinePlayers().values()) {
                        if (player.getDutyStatus() < server.campaign.SPlayer.STATUS_RESERVE) {continue;}
                        if (CampaignMain.campaignMain.getServer().isAdmin(player.getName())) {continue;}
                        if (player.getName().equalsIgnoreCase("Spork")) {continue;}
                        songLine = songLyrics.nextToken();
                        CampaignMain.campaignMain.doSendToAllOnlinePlayers(player.getName() + "|" + songLine,
                              true);
                        Thread.sleep(1000);
                    }
                }

            }
        } catch (Exception ex) {

        }

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public void listSongs(String user) {
        java.io.File songList = new java.io.File("./data/songs.txt");
        CampaignMain.campaignMain.toUser("SM|Current song List", user, false);
        CampaignMain.campaignMain.toUser("SM|teapot", user, false);
        java.io.BufferedReader dis = null;
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(songList);
            dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
            while (dis.ready()) {
                java.util.StringTokenizer song = new java.util.StringTokenizer(dis.readLine(), "|");
                CampaignMain.campaignMain.toUser("SM|" + song.nextToken(), user, false);
            }
        } catch (Exception ex) {
            //No song list found;
            return;
        } finally {
            try {
                dis.close();
            } catch (java.io.IOException e) {
                MWLogger.errLog(e);
            }
        }

    }

    public String getSong(String songName) {

        //default song of torcher --Torren
        String Song = "I'm a little teapot!#Short and Stout#Here is my handle#Here is my Spout#When I get all steamed up#Hear me shout!#Tip me over#And pour me out!";
        if (songName.equalsIgnoreCase("teapot")) {return Song;}
        java.io.File songList = new java.io.File("./data/songs.txt");
        java.io.BufferedReader dis = null;
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(songList);
            dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
            while (dis.ready()) {
                java.util.StringTokenizer song = new java.util.StringTokenizer(dis.readLine(), "|");
                if (song.nextToken().equalsIgnoreCase(songName)) {
                    Song = song.nextToken();
                    return Song;
                }
            }
        } catch (Exception ex) {
            return null;
        } finally {
            try {
                dis.close();
            } catch (java.io.IOException e) {
                MWLogger.errLog(e);
            }
        }

        return null;
    }

}
