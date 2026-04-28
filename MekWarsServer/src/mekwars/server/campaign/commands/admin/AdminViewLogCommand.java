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


public class AdminViewLogCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "/path/file";

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

        String fileName = command.nextToken();
        if (fileName.startsWith("../")) {
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username + " tried to viewed file " + fileName + " but was denied!");
            server.campaign.CampaignMain.cm.toUser("Sorry but you are not allowed to backout of the root directory!",
                  Username,
                  true);
            return;
        }
        try {
            java.io.File logFile = new java.io.File("./" + fileName);
            if (logFile.length() > 3072000) {
                server.campaign.CampaignMain.cm.toUser(
                      "The file you are trying to open is over 3 megs that is not allowed!",
                      Username,
                      true);
                return;
            }
            java.io.FileInputStream fis = new java.io.FileInputStream(logFile);
            java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
            while (dis.ready()) {
                server.campaign.CampaignMain.cm.toUser("SM|" + dis.readLine(), Username, false);
            }
            fis.close();
            dis.close();
            server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " has viewed file" + logFile);
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("File Not found", Username, true);
            return;
        }

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
