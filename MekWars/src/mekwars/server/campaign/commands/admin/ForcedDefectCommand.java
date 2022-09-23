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
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class ForcedDefectCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Player Name#Faction Name";
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
		SPlayer p = null;
		SHouse h = null;
		
		//see if the player is online
		boolean playerOnline = false;
		
		try {
			
			String name = command.nextToken();
			playerOnline = CampaignMain.cm.isLoggedIn(name);
			
			p = CampaignMain.cm.getPlayer(name);
			h = CampaignMain.cm.getHouseFromPartialString(command.nextToken(),null);
			
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper command. Try: /c forceddefect#player#faction", Username, true);
			return;
		}
		
		if (p == null) {
			CampaignMain.cm.toUser("AM:Couldn't find a player with that name.", Username, true);
			return;
		}
		
		if (h == null) {
			CampaignMain.cm.toUser("AM:Couldn't find a faction with that name.", Username, true);
			return;
		}
		
		//make the move
		String clientVersion = p.getPlayerClientVersion();
		p.getMyHouse().removeLeader(p.getName());
		p.getMyHouse().removePlayer(p,false);
		p.setMyHouse(h);
		p.setSubFaction(h.getZeroLevelSubFaction());
		
		//log the player into his new faction
		if (playerOnline) {
			CampaignMain.cm.getPlayer(p.getName());
			CampaignMain.cm.doLoginPlayer(p.getName());
		}

		//send appropraite messages
		CampaignMain.cm.toUser("AM:"+Username + " forced you to defect to " + h.getName(),p.getName(),true);
		CampaignMain.cm.toUser("AM:You forced " + p.getName() + " to defect to " + h.getName(),Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " forced " + p.getName() + " to defect to " + h.getName());
		p.setPlayerClientVersion(clientVersion);
		
	}
}