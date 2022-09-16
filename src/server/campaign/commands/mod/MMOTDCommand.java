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

package server.campaign.commands.mod;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

/**
 * @author Jason Tighe
 */
public class MMOTDCommand implements Command {

	/*
	 * @see server.Campaign.Commands.Command#process(java.util.StringTokenizer, java.lang.String, server.Campaign.CampaignMain)
	 */
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "";
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
		
		//CampaignMain.cm.doSendModMail(Username, CampaignMain.cm.getConfig("MMOTD"));
		CampaignMain.cm.toUser("(Moderator Mail) Mod MOTD: "+ CampaignMain.cm.getConfig("MMOTD"), Username);
		
	}//end process
	
}
