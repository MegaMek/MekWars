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

import java.io.File;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class AdminDestroyPlanetCommand implements Command {
	
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
		
		SPlanet p = CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),Username);
		
		//remove the world from its owner
		SHouse h = p.getOwner();
		if (h != null)
			h.removePlanet(p);
		
		//remove the world from the data in memory
		CampaignMain.cm.getData().removePlanet(p.getId());
		
		//finally, remove the world's flat file
		File fp = new File("./campaign/planets/" + p.getName().toLowerCase().trim() + ".dat");
		if (fp.exists())
			fp.delete();
		
		CampaignMain.cm.updateHousePlanetUpdate();
		//server.MWLogger.modLog(Username + " unleashed the Death Star on " + p.getName() + ". Planet destroyed!");
		CampaignMain.cm.doSendModMail("NOTE",Username + " unleashed the Death Star on " + p.getName() + ". Planet destroyed!");
		
	}
}