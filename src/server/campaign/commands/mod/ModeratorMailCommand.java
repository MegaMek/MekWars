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

package server.campaign.commands.mod;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

public class ModeratorMailCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Message";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		
		if (Username.startsWith("[Dedicated]"))
			userLevel = getExecutionLevel();
		
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		if (command.countTokens() < 1)
			return;
		
		String toSend = command.nextToken();
		while (command.hasMoreElements())
			toSend += "#"+command.nextToken();
		
		if (toSend.trim().length() == 0)
			return;
		
		CampaignMain.cm.doSendModMail(Username, toSend);
		//MWLogger.modLog("[MM] " + Username + ": " + toSend);
		
	}
}
