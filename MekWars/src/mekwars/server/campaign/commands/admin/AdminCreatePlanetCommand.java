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

/*
 * Created on 14.04.2004
 *
 */
package server.campaign.commands.admin;

import java.util.HashMap;
import java.util.StringTokenizer;

import common.Influences;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.commands.Command;


/**
 * @author Helge Richter
 *
 */
public class AdminCreatePlanetCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Planet Name#Xcood#YCoord#";
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
		
		SHouse faction = CampaignMain.cm.getHouseFromPartialString(CampaignMain.cm.getConfig("NewbieHouseName"),Username);
		String PlanetName = command.nextToken();
		double xcood = Double.parseDouble(command.nextToken());
		double ycood = Double.parseDouble(command.nextToken());
		if (faction == null || PlanetName == null)
			return;
		HashMap<Integer,Integer> flu = new HashMap<Integer,Integer>();
		flu.put(faction.getId(),100);
		SPlanet planet = new SPlanet(CampaignMain.cm.getData().getUnusedPlanetID(),PlanetName, new Influences(flu), 0, 0, xcood, ycood);
		CampaignMain.cm.addPlanet(planet);
		planet.setOwner(null,faction,true);
		planet.setOriginalOwner(faction.getName());
        planet.updated();
        
		CampaignMain.cm.toUser("Planet created!",Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " has created planet " + PlanetName);
		
	}
}
