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

package server.campaign.commands.admin;


import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.commands.Command;

public class AdminHouseStatusCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "faction";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		SHouse h = null;
		
		try {
			h = CampaignMain.cm.getHouseFromPartialString(command.nextToken(),Username);
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminhousestatus#faction", Username, true);
			return;
		}
		
		if (h == null) {
			CampaignMain.cm.toUser("Couldn't find a faction with that name.", Username, true);
			return;
		}
		
		//feed back the faction status
		CampaignMain.cm.toUser("HS|CA|0", Username, false);//clear old data
		CampaignMain.cm.toUser(h.getCompleteStatus(),Username,false);
		//server.MWLogger.modLog(Username + " checked " + h.getName());
		CampaignMain.cm.doSendModMail("NOTE",Username + " checked " + h.getName());
		
	}
}