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
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class SetPlanetConquerCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Planet Name#[true/false]";
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
		
		SPlanet p = null;
		boolean conquer = true;
		
		try {
			p = CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),Username);
			conquer = Boolean.parseBoolean(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c setplanetconquer#planet#true/false", Username, true);
			return;
		}
		
		if (p == null) {
			CampaignMain.cm.toUser("Couldn't find a planet with that name.", Username, true);
			return;
		}
		
		p.setConquerable(conquer);
		p.updated();

		CampaignMain.cm.toUser("You set " + p.getName() + "'s conquer status to " + conquer,Username,true);
		//server.MWLogger.modLog(Username + " has changed the infaction conquer for " + p.getName()+" to "+conquer);
		CampaignMain.cm.doSendModMail("NOTE",Username + " has changed the conquer status for " + p.getName()+" to "+conquer);

	}
}