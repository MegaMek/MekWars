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

package mekwars.server.campaign.commands;

import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;

//import java.nio.charset.Charset;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import server.MWClientInfo;
//import server.campaign.SPlayer;


/**
 * used for capturing chat to a file a discord bot can manipulate
 */
public class ChatBotHelperCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (!accessChecks(Username)) {return;}

        StringBuilder buffer = new StringBuilder();

        //Should be all i need?
        if (command.hasMoreTokens()) {buffer.append(command.nextToken());}

        captureAllChatForBot(Username, buffer.toString());

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    private Boolean accessChecks(String Username) {
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);

        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return false;
        }

        if (!Boolean.parseBoolean(CampaignMain.campaignMain.getConfig("Enable_Bot_Chat"))) {
            CampaignMain.campaignMain.toUser("AM:This command is disabled on this server.", Username, true);
            return false;
        }

        return true;
    }

    private void captureAllChatForBot(String Username, String chatMsg) {
        if (!Boolean.parseBoolean(CampaignMain.campaignMain.getConfig("Enable_Bot_Chat"))) {return;}

        java.io.File file = new java.io.File(CampaignMain.campaignMain.getConfig("Bot_Buffer_Location"));


        try (java.io.FileWriter fw = new java.io.FileWriter(CampaignMain.campaignMain.getConfig(
              "Bot_Buffer_Location"), true);
              java.io.BufferedWriter bw = new java.io.BufferedWriter(fw);
              java.io.PrintWriter out = new java.io.PrintWriter(bw)) {

            out.println(chatMsg);

        } catch (java.io.UnsupportedEncodingException e) {
            MWLogger.errLog(e);
            //CampaignMain.cm.toUser(e.toString(),Username,true);

        } catch (java.io.IOException e) {
            MWLogger.errLog(e);
            //CampaignMain.cm.toUser(e.toString(),Username,true);

        }
    }
}
