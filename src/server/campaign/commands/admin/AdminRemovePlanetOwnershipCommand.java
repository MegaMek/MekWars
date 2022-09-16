/*
 * MekWars - Copyright (C) 2007 
 * 
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
 * @author jtighe
 * This Command is used by server admins to remove owners from a planet.
 * 
 */

package server.campaign.commands.admin;

import java.util.StringTokenizer;

import common.House;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class AdminRemovePlanetOwnershipCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Planet Name#Faction Name";
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
		SPlanet planet = null;
		House removingHouse = null;
		
		try {
			planet = CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),Username);
			removingHouse = CampaignMain.cm.getHouseFromPartialString(command.nextToken(),Username);
			
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminremoveplanetownership#planet#faction", Username, true);
			return;
		}
		
		if (planet == null) {
			CampaignMain.cm.toUser("Could not find a matching planet.",Username,true);
			return;
		}
		
		if ( removingHouse == null ){
			CampaignMain.cm.toUser("Could not find a matching faction to remove.",Username,true);
			return;
		}
		
		planet.getInfluence().removeHouse(removingHouse);
		planet.updated();
		
		CampaignMain.cm.doSendModMail("NOTE",Username + " removed "+removingHouse.getName()+" as an owner of "+ planet.getName()+".");
	}
}