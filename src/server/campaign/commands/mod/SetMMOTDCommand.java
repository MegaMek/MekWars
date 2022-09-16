/*
 * MekWars - Copyright (C) 2006 
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

/**
 * @author Jason Tighe
 */

package server.campaign.commands.mod;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

/**
 * Set a faction's message of the day. Can be of arbitrary length and use HTML.
 * 
 * Syntax: /c setmmotd#text
 */
public class SetMMOTDCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Message[Clear to clear]";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		String motd = "";
		try {
			
			//there may be #'s in HTML. Use all tokens and restore #'s.
			motd = command.nextToken();
			while (command.hasMoreTokens())
				motd += "#" + command.nextToken();
			
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper syntax. Try: /setmmotd text or /setmmotd clear to clear",Username,true);
			return;
		}
		
		if (motd.trim().equals("") || motd.equalsIgnoreCase("clear")) {
			CampaignMain.cm.getConfig().setProperty("MMOTD", "");
			CampaignMain.cm.toUser("MMOTD cleared.",Username,true);
			return;
		}
		
		int size = motd.length();
		if (size > 7000) {
			CampaignMain.cm.toUser("MMOTD's may contain up to 7000 charachters. Your message was " + size + "chars long. Reduce its length and try again.",Username,true);
			return;
		}
		
		CampaignMain.cm.getConfig().setProperty("MMOTD",motd + "<br>- Set by " + Username);
		CampaignMain.cm.toUser("MMOTD set. Use /c mmotd to review.",Username,true);
					
	}//end process()
}//end setMMOTDCommand.java
