/*
 * MekWars - Copyright (C) 2010
 * 
 * Original author - Bob Eldred (billypinhead@users.sourceforge.net)
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

package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;



public class AdminRestartTrackerThreadCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "AdminRestartTrackerThread";
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

		if ( !Boolean.parseBoolean(CampaignMain.cm.getConfig("UseTracker"))) {
			CampaignMain.cm.toUser("AM: Server is configured not to use the tracker.", Username, true);
			return;
		}
		CampaignMain.cm.getServer().startTracker();
		CampaignMain.cm.toUser("AM: Tracker Thread restarted.", Username, true);
	}
	
}