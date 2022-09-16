/*
 * MekWars - Copyright (C) 2007 
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
import server.campaign.commands.Command;

public class AdminSetHouseAbbreviationCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Faction Name#Shortname";
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
		String houseAbbreviation;
		
		try {
			HouseName = command.nextToken();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminsethouseabbreviation#faction#shortname", Username, true);
			return;
		}
		
		faction = CampaignMain.cm.getHouseFromPartialString(HouseName,Username);
		if (faction == null) {
			CampaignMain.cm.toUser("Couldn't find a faction with that name.", Username, true);
			return;
		}
		
		try {
			houseAbbreviation = command.nextToken();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminsethouseabbreviation#faction#shortname", Username, true);
			return;
		}
		
		faction.setAbbreviation(houseAbbreviation);
		faction.updated();
		
		CampaignMain.cm.doSendModMail("NOTE",Username + " changed the faction abbreviation for " + HouseName);
		//server.MWLogger.modLog(Username + " changed the faction playerlist color for " + HouseName);
		
		CampaignMain.cm.toUser(HouseName +" abbreviation changed.",Username,true);
		
	}
}