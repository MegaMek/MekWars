/*
 * MekWars - Copyright (C) 2007 
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


import java.io.File;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.commands.Command;

public class AdminPurgeHouseConfigsCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Faction Name";
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
        
		try{
		    faction = command.nextToken();
		}
		catch (Exception ex){
		    CampaignMain.cm.toUser("Invalid syntax. Try: AdminPurgeHouseConfig#faction",Username,true);
		    return;
		}
		
		SHouse h = CampaignMain.cm.getHouseFromPartialString(faction,Username);
		
		if ( h == null )
		    return;

		h.getConfig().clear();
		File fp = new File("./campaign/factions/" + h.getName().toLowerCase() + "_configs.dat");
		
		if ( fp.exists() )
			fp.delete();
		
		h.saveConfigFile();
		
		h.updated();
        CampaignMain.cm.doSendModMail("NOTE",Username+" has purged campaign configs for "+h.getName());
	}
}// end AdminPurgeHouseConfigsCommand