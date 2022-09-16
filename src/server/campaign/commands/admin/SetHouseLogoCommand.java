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

public class SetHouseLogoCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Faction Name#Logo URL";
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
		
		SHouse h = null;
		String logo = "";
		
		try {
			h = CampaignMain.cm.getHouseFromPartialString(command.nextToken(),Username);
			logo = command.nextToken();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c sethouselogo#Faction#Logo", Username, true);
			return;
		}
		
		if (h == null) {
			CampaignMain.cm.toUser("Couldn't find a faction with that name.", Username, true);
			return;
		}
		
		if (logo.equals("")) {
			CampaignMain.cm.toUser("Can't use a blank logo name.", Username, true);
			return;
		}
		
		//breaks passed. set the logo.
		h.setLogo(logo);
		h.updated();
		CampaignMain.cm.toUser("You set " + h.getName() + "'s logo to " + h.getLogo(),Username,true);
		
	}
}