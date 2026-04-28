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

//Syntax addsong#songname#lyric1#lyric2....
public class AddSongCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "addsong#songname#lyric1#lyric2....";

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


        if (!songList.exists()) {
            try {
                songList.createNewFile();
            } catch (Exception ex) {
                return;
            }
        }

        String songName = command.nextToken();

        StringBuilder lyrics = new StringBuilder();

        while (command.hasMoreTokens()) {
            lyrics.append(command.nextToken());
            lyrics.append("#");
        }

        //get rid of that last #
        lyrics.deleteCharAt(lyrics.length() - 1);
        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(songList, true);
            java.io.PrintStream ps = new java.io.PrintStream(fos);
            ps.println(songName + "|" + lyrics.toString().trim());

            ps.close();
            fos.close();
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("Unable to append to songs.txt", Username, true);
            return;
        }

        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has added " + songName + " to the song list!");
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
