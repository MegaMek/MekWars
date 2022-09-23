/*
 * MekWars - Copyright (C) 2007 
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

import java.util.HashMap;
import java.util.StringTokenizer;

import common.House;
import common.Planet;
import common.util.MWLogger;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class AdminReturnPlanetsToOriginalOwnersCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "";
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
		
		//look for confirmation
		if (!command.hasMoreTokens() || !command.nextToken().equalsIgnoreCase("confirm")) {
			CampaignMain.cm.toUser("Do you want to return all planets to their original owners? If so, [<a href=\"MEKWARS/c adminreturnplanetstooriginalowners#confirm\">click to confirm.</a>]",Username,true);
			return;
		}
		
		//check for double confirmation
		if (!command.hasMoreTokens() || !command.nextToken().equalsIgnoreCase("confirm")) {
			CampaignMain.cm.toUser("Are you *ABSOLUTELY SURE* you want to return planets to their original owners? This cannot be easily reversed. If so, [<a href=\"MEKWARS/c adminreturnplanetstooriginalowners#confirm#confirm\">click to re-confirm.</a>]",Username,true);
			return;
		}
		
		for ( House currH : CampaignMain.cm.getData().getAllHouses() ){
		    SHouse h = (SHouse)currH;
		    h.getPlanets().clear();
		    h.setComponentProduction(0);
		    
		}
		
		//doubly confirmed. loop through every planet and restore it to the original owner
		for (Planet currP : CampaignMain.cm.getData().getAllPlanets()) {
			
			//cast to planet
			SPlanet p = (SPlanet)currP;
			MWLogger.mainLog("Returning planet " + p.getName() + " to original owner");
			int totalCP	= p.getConquestPoints();
			//get original owner
			SHouse origOwner = CampaignMain.cm.getHouseFromPartialString(p.getOriginalOwner(), Username);
			//change the ownership in the respective SHouses
			p.setOwner(p.getOwner(), origOwner, true);
			
			//change the planet's influence table
			
			HashMap<Integer,Integer> flu = new HashMap<Integer,Integer>();
			flu.put(origOwner.getId(), totalCP);
			p.getInfluence().setInfluence(flu);
			
			//set updated flag so players' maps refresh
			p.updated();
	}
		
		//server.MWLogger.modLog(Username + " restored all plants to their original owners.");
		CampaignMain.cm.toUser("You restored all plants to their original owners.",Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " restored all plants to their original owners.");
		
	}
}