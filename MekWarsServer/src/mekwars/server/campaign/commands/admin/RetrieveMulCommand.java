/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - jtighe (torren@users.sourceforge.net)
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

import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;


public class RetrieveMulCommand implements server.campaign.commands.Command {

    /*
     * This command allows an Admin to upload a single build table
     * from a directory on their local machine.  The directory structure
     * on the local machine must match that on the server - i.e, ./buildtables/rare
     * ./buildtables/reward and ./buildtables/standard.  The replacement build
     * table is put into place and a backup of the original build table is created.
     */

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "FileName";

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
        String fileName = "./data/armies/" + command.nextToken();
        java.io.BufferedReader br = null;
        try {
            java.io.File mul = new java.io.File(fileName);
            if (!mul.exists()) {
                CampaignMain.campaignMain.toUser("Unable to find file " + fileName, Username);
            }
            StringBuffer sendData = new StringBuffer("PL|RMF|");
            java.io.FileInputStream fis = new java.io.FileInputStream(mul);
            br = new java.io.BufferedReader(new java.io.InputStreamReader(fis));

            sendData.append(mul.getName());
            sendData.append("#");
            while (br.ready()) {
                sendData.append(br.readLine());

                if (sendData.lastIndexOf("#") == sendData.length() - 1) {sendData.append(" ");}
                sendData.append("#");
            }
            CampaignMain.campaignMain.toUser(sendData.toString(), Username, false);

        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("File Not found", Username, true);
            if (br != null) {
                try {
                    br.close();
                } catch (java.io.IOException e) {
                    MWLogger.errLog(e);
                }
            }
            return;
        } finally {
            try {
                br.close();
            } catch (java.io.IOException e) {
                MWLogger.errLog(e);
            }
        }

        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " has retrived mul file " + fileName);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
