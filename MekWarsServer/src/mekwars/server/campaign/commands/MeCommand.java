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

//import java.io.File;
//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;

//
//import common.CampaignData;


import mekwars.server.campaign.CampaignMain;

/**
 * Moving the Me command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c me blah
 */
public class MeCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        StringBuilder buffer = new StringBuilder();

        //grab the first command
        if (command.hasMoreTokens()) {buffer.append(command.nextToken());}

        while (command.hasMoreTokens()) {
            buffer.append("#");
            buffer.append(command.nextToken());
        }


        java.util.StringTokenizer channels = new java.util.StringTokenizer(buffer.toString(), "|");

        String toSend = "";
        String channel = "";

        if (channels.hasMoreTokens()) {toSend = channels.nextToken();} else {toSend = buffer.toString();}

        if (toSend.trim().length() == 0) {return;}
        if (channels.hasMoreTokens()) {channel = channels.nextToken();}

        //if client is somehow null, just send the message
        server.MWClientInfo client = CampaignMain.campaignMain.getServer().getUser(Username);
        if (client == null) {
            CampaignMain.campaignMain.doSendToAllOnlinePlayers(Username + "|#me " + toSend, true);
            return;
        }

        //check to see if the player is muted
        boolean generalMute = CampaignMain.campaignMain.getServer().getIgnoreList().indexOf(client.getName()) >
                                    -1;
        boolean factionMute = CampaignMain.campaignMain.getServer()
                                    .getFactionLeaderIgnoreList()
                                    .indexOf(client.getName()) > -1;

        if (generalMute || factionMute) {
            CampaignMain.campaignMain.toUser("AM:You've been set to ignore mode and cannot participate in chat.",
                  Username,
                  true);
        } else if (channel.equalsIgnoreCase("hm")) {
            server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
            CampaignMain.campaignMain.doSendHouseMail(player.getHouseFightingFor(), Username, "#me " + toSend);
        } else if (channel.equalsIgnoreCase("mm")) {
            CampaignMain.campaignMain.doSendModMail(Username, "#me " + toSend);
        } else if (channel.equalsIgnoreCase("ic")) {
            CampaignMain.campaignMain.doSendToAllOnlinePlayers("(In Character)" + Username + ":#me " + toSend,
                  true);
        } else if (channel.equalsIgnoreCase("mail")) {
            String reciever = channels.nextToken();
            CampaignMain.campaignMain.getServer().doStoreMail(reciever + ",#me " + toSend, Username);
        } else {
            CampaignMain.campaignMain.doSendToAllOnlinePlayers(Username + "|#me " + toSend, true);
            //captureAllChatForBot(Username, toSend);
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    //this works, but needs a bit of touch up, copy code from other command @salient
    //	private void captureAllChatForBot(String Username, String chatMsg)
    //	{
    //		if(!Boolean.parseBoolean(CampaignMain.cm.getConfig("Enable_Bot_Chat")))
    //			return;
    //
    //		File file = new File(CampaignMain.cm.getConfig("Bot_Buffer_Location"));
    //
    //		try
    //		{
    //			Files.write(Paths.get(file.toURI()), chatMsg.getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    //			CampaignMain.cm.toUser(chatMsg,Username,true);
    //
    //		}
    //		catch (UnsupportedEncodingException e)
    //		{
    //			MWLogger.errLog(e);
    //			CampaignMain.cm.toUser(e.toString(),Username,true);
    //
    //		}
    //		catch (IOException e)
    //		{
    //			MWLogger.errLog(e);
    //			CampaignMain.cm.toUser(e.toString(),Username,true);
    //
    //		}
    //	}
}
