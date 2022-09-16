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
 * This command is used to set the tech level of a faction.
 */
package server.campaign.commands.admin;

import java.util.StringTokenizer;

import megamek.common.TechConstants;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.commands.Command;


// comand /c AdminSetHouseTechLevel#House#TechLevel
public class AdminSetHouseTechLevelCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Faction Name#TechLevel";
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
		
		SHouse house = null;
		int techLevel;
		
		try {
			house = CampaignMain.cm.getHouseFromPartialString(command.nextToken());
			techLevel = Integer.parseInt(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminsethousetechlevel#faction#techlevel", Username, true);
			return;
		}
		
		house.setTechLevel(techLevel);
		house.updated();
		
		CampaignMain.cm.doSendModMail("NOTE",Username + " has set " + house.getName()+ "'s tech level to " + TechConstants.getLevelDisplayableName(techLevel)+".");
		
	}
}