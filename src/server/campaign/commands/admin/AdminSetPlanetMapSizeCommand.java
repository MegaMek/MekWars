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

import java.awt.Dimension;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlanet;
import server.campaign.commands.Command;


public class AdminSetPlanetMapSizeCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Planet Name#X#Y";
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
		
		SPlanet planet =  (SPlanet) CampaignMain.cm.getData().getPlanetByName(command.nextToken());
		if ( planet == null ) {
			CampaignMain.cm.toUser("Unknown Planet",Username,true);
			return;
		}
		int x = Integer.parseInt(command.nextToken());
		int y = Integer.parseInt(command.nextToken());
		
		planet.setMapSize(new Dimension(x,y));
		planet.updated();
		
		CampaignMain.cm.toUser("Map size set for planet "+planet.getName(),Username,true);
		//server.MWLogger.modLog(Username + " set the map size for planet "+planet.getName());
		CampaignMain.cm.doSendModMail("NOTE",Username + " has set the mapsize for planet "+planet.getName());
		
	}
}