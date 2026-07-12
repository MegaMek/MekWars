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


import mekwars.server.campaign.CampaignMain;

public class AdminViewLogCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "/path/file";

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

        String fileName = command.nextToken();
        if (fileName.startsWith("../")) {
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  Username + " tried to viewed file " + fileName + " but was denied!");
            CampaignMain.campaignMain.toUser("Sorry but you are not allowed to backout of the root directory!",
                  Username,
                  true);
            return;
        }
        try {
            java.io.File logFile = new java.io.File("./" + fileName);
            if (logFile.length() > 3072000) {
                CampaignMain.campaignMain.toUser(
                      "The file you are trying to open is over 3 megs that is not allowed!",
                      Username,
                      true);
                return;
            }
            java.io.FileInputStream fis = new java.io.FileInputStream(logFile);
            java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
            while (dis.ready()) {
                CampaignMain.campaignMain.toUser("SM|" + dis.readLine(), Username, false);
            }
            fis.close();
            dis.close();
            CampaignMain.campaignMain.doSendModMail("NOTE", Username + " has viewed file" + logFile);
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("File Not found", Username, true);
            return;
        }

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
