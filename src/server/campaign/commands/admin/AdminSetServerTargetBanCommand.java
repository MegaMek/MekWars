/*
 * MekWars - Copyright (C) 2010 
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
import java.util.Vector;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

public class AdminSetServerTargetBanCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Banned TargetSystem String - list of integers separated by #";
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
		
		CampaignMain.cm.getData().getBannedTargetingSystems().clear();
		Vector<Integer> bans = new Vector<Integer>(1,1);
		while (command.hasMoreTokens()) {
			bans.add(Integer.parseInt(command.nextToken()));
		}
		CampaignMain.cm.getData().setBannedTargetingSystems(bans);
		
		// Send updates to everyone
		StringBuilder sb = new StringBuilder();
		sb.append("SBT|");
		for (int ban : bans) {
			sb.append(ban);
			sb.append("|");
		}
		CampaignMain.cm.saveBannedTargetSystems();
		CampaignMain.cm.doSendToAllOnlinePlayers(sb.toString(), false);
		CampaignMain.cm.toUser("AM: Server Target Bans set", Username, true);
	}
}