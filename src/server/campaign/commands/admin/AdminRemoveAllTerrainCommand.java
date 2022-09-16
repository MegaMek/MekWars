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


package server.campaign.commands.admin;

import java.util.StringTokenizer;

import common.util.MWLogger;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class AdminRemoveAllTerrainCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Planet Name";
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
		
		try{
			SPlanet p = CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),Username);
			
			if ( p == null ) {
				CampaignMain.cm.toUser("Planet not found:",Username,true);
				return;
			}
			
			p.getEnvironments().removeAll();
			p.updated();

			//server.MWLogger.modLog(Username + " removed terrain from " + p.getName() + "(#" + placeToDelete + ").");
			CampaignMain.cm.doSendModMail("NOTE",Username + " removed all terrain from " + p.getName() + ".");
		}
		catch (Exception ex){
			MWLogger.errLog(ex);
		}
		
	}
}