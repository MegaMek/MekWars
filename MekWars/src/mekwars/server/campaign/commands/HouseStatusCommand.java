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

package server.campaign.commands;

import java.util.Iterator;
import java.util.StringTokenizer;

import common.House;
import megamek.common.TechConstants;
import server.campaign.CampaignMain;
import server.campaign.SHouse;

public class HouseStatusCommand implements Command {
	
	int accessLevel = 0;
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
		
		String result = "<h2>Faction Status: </h2>";
		Iterator<House> e = CampaignMain.cm.getData().getAllHouses().iterator();
		while (e.hasNext()) {
			SHouse h = (SHouse)e.next();
			if ( h.getId() < 0)
			    continue;
			result += "<FONT Color=\"" + h.getHouseColor() + "\">";
			result += h.getName() + " Tech Level: "+TechConstants.getLevelDisplayableName(h.getTechLevel()) + " has " + h.getPlanets().size(); 
			result += " Planets providing " + h.getBaysProvided() + " bays and " + h.getSmallPlayers().size() + " Members. The total economy value is: "+ h.getComponentProduction() + "</font><br>";
		}
		result += "Note: Member numbers are based on members that have logged in since the last reboot of the server!";
		
		CampaignMain.cm.toUser("SM|" + result,Username,false);
	}
}
