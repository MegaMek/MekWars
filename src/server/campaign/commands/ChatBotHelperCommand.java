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

package server.campaign.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
//import java.nio.charset.Charset;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
import java.util.StringTokenizer;

import common.util.MWLogger;
//import server.MWClientInfo;
import server.campaign.CampaignMain;
//import server.campaign.SPlayer;


/**
 * used for capturing chat to a file a discord bot can manipulate
 */
public class ChatBotHelperCommand implements Command {

	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}

	public void process(StringTokenizer command,String Username) {

		if(!accessChecks(Username))
			return;	

        StringBuilder buffer = new StringBuilder();

        //Should be all i need?
        if ( command.hasMoreTokens() )
        	buffer.append(command.nextToken());
        
        captureAllChatForBot(Username, buffer.toString());

	}
	
	private Boolean accessChecks(String Username)  
	{
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		
		if(userLevel < getExecutionLevel()) 
		{
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return false;
		}
		
		if(!Boolean.parseBoolean(CampaignMain.cm.getConfig("Enable_Bot_Chat"))) 
		{
			CampaignMain.cm.toUser("AM:This command is disabled on this server.",Username,true);
			return false;
		}
		
		return true;
	}

	private void captureAllChatForBot(String Username, String chatMsg)
	{
		if(!Boolean.parseBoolean(CampaignMain.cm.getConfig("Enable_Bot_Chat")))
			return;

		File file = new File(CampaignMain.cm.getConfig("Bot_Buffer_Location"));


		try(FileWriter fw = new FileWriter(CampaignMain.cm.getConfig("Bot_Buffer_Location"), true);
			    BufferedWriter bw = new BufferedWriter(fw);
			    PrintWriter out = new PrintWriter(bw))
		{

		    out.println(chatMsg);

		}
		catch (UnsupportedEncodingException e)
		{
			MWLogger.errLog(e);
			//CampaignMain.cm.toUser(e.toString(),Username,true);

		}
		catch (IOException e)
		{
			MWLogger.errLog(e);
			//CampaignMain.cm.toUser(e.toString(),Username,true);

		}
	}
}
