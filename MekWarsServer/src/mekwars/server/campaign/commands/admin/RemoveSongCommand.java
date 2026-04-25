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

//Syntax removesong#songname
public class RemoveSongCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    String syntax = "Song Name";

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

        java.io.File songList = new java.io.File("./data/songs.txt");

        //no song file no reason to use it.
        if (!songList.exists()) {
            return;
        }

        String songName = command.nextToken();
        StringBuilder songBuffer = new StringBuilder();

        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(songList);
            java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
            while (dis.ready()) {
                String song = dis.readLine();
                if (!song.toLowerCase().startsWith(songName.toLowerCase()) && song.trim().length() > 0) {
                    songBuffer.append(song + "\n");
                }
            }
            fis.close();
            dis.close();
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("Unable to remove song!", Username, true);
            return;
        }

        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(songList);
            java.io.PrintStream ps = new java.io.PrintStream(fos);
            ps.print(songBuffer.toString());

            ps.close();
            fos.close();
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("Unable to create new songs.txt", Username, true);
            return;
        }

        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has removed " + songName + " from the song list!");
    }
}
