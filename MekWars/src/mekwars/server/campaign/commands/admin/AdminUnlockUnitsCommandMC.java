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
import server.campaign.SPlayer;
import server.campaign.commands.Command;

//@salient - unlocks all units - used with mini campaign 
public class AdminUnlockUnitsCommandMC implements Command 
{

	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "/c adminunlockunitsmc#name";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}

	public void process(StringTokenizer command,String Username) 
	{
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}

		SPlayer p = null;

		try {
			p = CampaignMain.cm.getPlayer(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminunlockunitsmc#name", Username, true);
			return;
		}

		if(p == null) {
			CampaignMain.cm.toUser("Couldn't find a player with that name.", Username, true);
			return;
		}


		p.unlockAllUnitsMC();
		
		CampaignMain.cm.toUser("You unlocked" + p.getName() + "'s units.", Username, true);
		CampaignMain.cm.toUser(Username + " unlocked your units.", p.getName(), true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " unlocked " + p.getName() + "'s units.");
	}
}
