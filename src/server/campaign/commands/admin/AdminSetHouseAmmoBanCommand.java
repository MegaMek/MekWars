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

public class AdminSetHouseAmmoBanCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Faction Name#Munition Number";
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
		
		String faction = "";
		String ammoName = "";
		try{
		    faction = command.nextToken();
			ammoName = command.nextToken();
		}
		catch (Exception ex){
			CampaignMain.cm.toUser("Invalid syntax. Try: adminsethouseammoban#faction#munitionnumber",Username,true);
		}
		
		SHouse h = CampaignMain.cm.getHouseFromPartialString(faction,Username);
		
		if ( h == null )
		    return;
		 
		//Hashtable munitions = CampaignMain.cm.getData().getMunitionsByNumber();
		
		if ( h.getBannedAmmo().get(ammoName)!= null ){
		    h.getBannedAmmo().remove(ammoName);
			ammoName = CampaignMain.cm.getData().getMunitionsByNumber().get(Long.parseLong(ammoName));
			CampaignMain.cm.toUser("Ban on " + ammoName + " lifted for "+h.getName() + ".",Username,true);
			CampaignMain.cm.doSendModMail("NOTE",Username + " lifted the ban on " + ammoName+ " for " + h.getName() + ".");
		}
		else {
		    h.getBannedAmmo().put(ammoName,"banned");
			ammoName = CampaignMain.cm.getData().getMunitionsByNumber().get(Long.parseLong(ammoName));
			CampaignMain.cm.toUser("Banned " + ammoName + " for "+h.getName() + ".",Username,true);
			CampaignMain.cm.doSendModMail("NOTE",Username + " banned " + ammoName+ " for " + h.getName() + ".");
		}
	
		h.updated();
        CampaignMain.cm.saveBannedAmmo();
	}
}