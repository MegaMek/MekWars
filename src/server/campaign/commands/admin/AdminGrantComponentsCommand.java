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

import common.Unit;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.commands.Command;

// AdminGrantComponents#Faction#Type#WeightClass#Components
public class AdminGrantComponentsCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "faction#type#weight#numcomponents";
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
		
		//vars
		SHouse h = null;
		String typestring = "";
		String weightstring = "";
		int comps = -1;
		int unitType = Unit.MEK;
		int unitWeight = Unit.LIGHT;
		
		try {
			h = (SHouse)CampaignMain.cm.getData().getHouseByName(command.nextToken());
			typestring = command.nextToken();
			weightstring = command.nextToken();
			comps = Integer.parseInt(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c admingrancomponents#faction#type#weight#numcomponents", Username, true);
			return;
		}
		
		if (h == null) {
			CampaignMain.cm.toUser("Couldn't find a faction with that name.", Username, true);
			return;
		}
		
		try {
			unitType = Integer.parseInt(typestring);
		} catch (Exception ex) {
			unitType = Unit.getTypeIDForName(typestring);
		}
		
		try {
			unitWeight = Integer.parseInt(weightstring);
		} catch (Exception ex) {
			unitWeight = Unit.getWeightIDForName(weightstring.toUpperCase());
		}
		
		h.addPP(unitWeight,unitType,comps,true);
		CampaignMain.cm.toUser("You granted " + comps + " Comps to " + h.getName(),Username,true);
		//server.MWLogger.modLog(Username + " granted " + comps+ " Comps to " + h.getName());
		CampaignMain.cm.doSendModMail("NOTE",Username + " granted " + comps+ " Comps to " + h.getName());
		
	}
}