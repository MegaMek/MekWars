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

package server.campaign.commands;

import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;


public class MercStatusCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		String targetPlayer = "";
		try {
			targetPlayer = command.nextToken();
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper format. Try: /c mercstatus#name",Username,true);
			return;
		}
		
		SPlayer merc = CampaignMain.cm.getPlayer(targetPlayer);
		if (merc == null)
			CampaignMain.cm.toUser("AM:No player named " + targetPlayer + "!",Username,true);
		else if ((merc.getMyHouse()).isMercHouse()) {
			String s = merc.getReadableMercStatus();
			CampaignMain.cm.toUser(s,Username,true);
		} else
			CampaignMain.cm.toUser("AM:Target player is not a mercenary",Username,true);
		
	}
}