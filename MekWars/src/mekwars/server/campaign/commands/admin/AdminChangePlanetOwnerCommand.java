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

import java.util.HashMap;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class AdminChangePlanetOwnerCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "planet#newfaction";
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
		
		//vars
		SPlanet p = null;
		SHouse h = null;
		
		try {
			p = CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),Username);
			h = CampaignMain.cm.getHouseFromPartialString(command.nextToken(),Username);
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminchangeplanetowner#planet#newfaction", Username, true);
			return;
		}
		
		if (p == null) {
			CampaignMain.cm.toUser("Could not find a matching planet.",Username,true);
			return;
		}
		
		if ( h == null ){
			CampaignMain.cm.toUser("Could not find a matching faction.",Username,true);
			return;
		}
		
		//breaks passed
		p.setOwner(p.getOwner(), h, true);//pass in old owner using p.getOwner so people get status updates 
		
		HashMap<Integer,Integer> flu = new HashMap<Integer,Integer>();
		flu.put(h.getId(),p.getConquestPoints());
		p.getInfluence().setInfluence(flu);
		p.updated();
		
		//server.MWLogger.modLog(Username + " gave ownership of " + p.getName() + " to " + h.getName() + ".");
		CampaignMain.cm.toUser("You gave ownership of " + p.getName() + " to " + h.getName()+ ".",Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " gave ownership of " + p.getName() + " to " + h.getName()+ ".");
		
	}
}