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

import mekwars.server.campaign.CampaignMain;

//Syntax retrieveoperation#optype#opname
public class RetrieveOperationCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Op Type[Short/Long/Special]#Op Name";

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

        String opType;
        String opName;

        try {
            opType = command.nextToken();
            opName = command.nextToken();
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("Syntax retrieveoperation#optype#opname", Username, true);
            return;
        }

        java.io.File opFile = new java.io.File("./data/operations/" + opType + "/" + opName + ".txt");


        if (!opFile.exists()) {
            CampaignMain.campaignMain.toUser("No file found for Op " + opName, Username, true);
            return;
        }

        StringBuilder opData = new StringBuilder();

        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(opFile);
            java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
            opData.append(opName + "#");
            while (dis.ready()) {
                opData.append(dis.readLine().replaceAll("#", "(pound)") + "#");
            }
            dis.close();
            fis.close();

        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("Unable to read " + opFile, Username, true);
            return;
        }

        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " has retrieved " + opFile);

        CampaignMain.campaignMain.toUser("PL|RSOD|" + opData.toString(), Username, false);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}//end RetrieveOperation
