/*
 * MekWars - Copyright (C) 2004 
 * 
 * Original Author: jtighe
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
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class AdminSetHousePlayerColorCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Faction Name#htmlhexcolor";
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
		
		String HouseName = "";
		SHouse faction = null;
		String houseColor;
		SPlayer player = CampaignMain.cm.getPlayer(Username);
		
		try {
			HouseName = command.nextToken();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminsethouseplayercolormod#faction#htmlhexcolor", Username, true);
			return;
		}
		
		faction = CampaignMain.cm.getHouseFromPartialString(HouseName);
		if (faction == null) {
			CampaignMain.cm.toUser("Couldn't find a faction with that name.", Username, true);
			return;
		}
		
		if ( userLevel < IAuthenticator.MODERATOR )
		    faction = player.getMyHouse();
		
		try {
			houseColor = command.nextToken();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminsethouseplayercolormod#faction#htmlhexcolor", Username, true);
			return;
		}
		
		faction.setHousePlayerColors(houseColor);
		faction.updated();
		
		CampaignMain.cm.doSendModMail("NOTE",Username + " changed the faction playerlist color for " + HouseName);
		//server.MWLogger.modLog(Username + " changed the faction playerlist color for " + HouseName);
		
		CampaignMain.cm.toUser(HouseName +" color changed.",Username,true);
		
	}
}