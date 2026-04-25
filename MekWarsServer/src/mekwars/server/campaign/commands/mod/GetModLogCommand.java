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

import common.util.MWLogger;

public class GetModLogCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }
        java.io.BufferedReader dis = null;
        try {
            java.io.File configFile = new java.io.File("./logs/modlog.0");
            java.io.FileInputStream fis = new java.io.FileInputStream(configFile);
            dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
            String total = "";
            while (dis.ready()) {
                String line = dis.readLine();
                total += line + "<br>";
            }
            server.campaign.CampaignMain.cm.toUser("SM|" + total, Username, false);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " read the modlog.");
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        } finally {
            try {
                dis.close();
            } catch (java.io.IOException e) {
                MWLogger.errLog(e);
            }
        }


    }
}
