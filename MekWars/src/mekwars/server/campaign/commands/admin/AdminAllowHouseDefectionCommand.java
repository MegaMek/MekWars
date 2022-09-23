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

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.commands.Command;

// AdminAllowHouseDefection#House#true/false
public class AdminAllowHouseDefectionCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "factionname#to/from#true/false";
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
			SHouse faction = (SHouse)CampaignMain.cm.getData().getHouseByName(command.nextToken());
			if (faction == null){
				CampaignMain.cm.toUser("Unknown faction!",Username,true);
				return;   
			}
			String toFrom = command.nextToken();
			
			boolean lock; 
			if (command.hasMoreElements())
				lock = Boolean.parseBoolean(command.nextToken());
			else {
			    if ( toFrom.equalsIgnoreCase("to")){
					if (faction.getHouseDefectionTo())
						lock = false;
					else
						lock = true;
			    }
			    else{
					if (faction.getHouseDefectionFrom())
						lock = false;
					else
						lock = true;
			    }
			}
			
			if ( toFrom.equalsIgnoreCase("to"))
			    faction.setHouseDefectionTo(lock);
			else
			    faction.setHouseDefectionFrom(lock);
			
			if ( !lock ) {
				CampaignMain.cm.toUser("You've blocked defection "+toFrom.toLowerCase()+" "+ faction.getName(),Username,true);
				//server.MWLogger.modLog(Username + " has blocked defection "+toFrom.toLowerCase()+" "+ faction.getName());
				CampaignMain.cm.doSendModMail("NOTE",Username + " has blocked defection "+toFrom.toLowerCase()+" "+ faction.getName());
			}
			else {	
				CampaignMain.cm.toUser("You've allowed defection "+toFrom.toLowerCase()+" "+ faction.getName(),Username,true);
				//server.MWLogger.modLog(Username + " has allowed defection "+toFrom.toLowerCase()+" "+ faction.getName());
				CampaignMain.cm.doSendModMail("NOTE",Username + " has allowed defections "+toFrom.toLowerCase()+" "+ faction.getName());
			}
			
			faction.updated();
		} catch (Exception ex){
			CampaignMain.cm.toUser("Command failed. Make sure format was: /c adminallowhousedefection#factionname#to/from#true/false", Username, true);
		}
		
	}
}