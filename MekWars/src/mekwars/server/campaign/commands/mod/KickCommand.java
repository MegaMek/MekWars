/*
 * MekWars - Copyright (C) 2006
 * 
 * Original author - Jason Tighe (torren@users.sourceforge.net)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package server.campaign.commands.mod;

import java.util.StringTokenizer;

import common.util.MWLogger;
import server.MWChatServer.MWChatServer;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


/**
 * Moving the Kick command from MWServ into the normal command structure.
 *
 * Syntax  /c Kick#Player
 */
public class KickCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Player Name";
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
		
		String toKick = null;
		try {
			toKick = command.nextToken();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper format. Try: /c kick#NAME", Username);
        	return;
		}

		if (CampaignMain.cm.getServer().isAdmin(toKick) && !Username.startsWith("[Dedicated]")) {
			CampaignMain.cm.toUser("AM:You may not kick an admin.", Username);
			return;
		}
		
		CampaignMain.cm.toUser("AM:You were kicked by " + Username,toKick,true);
		CampaignMain.cm.toUser("PL|GBB|Bye Bye", toKick,false);
		
		//Use this to kick ghost players from the clients.
		CampaignMain.cm.getServer().sendRemoveUserToAll(toKick,false);
		CampaignMain.cm.getServer().sendChat("AM:"+Username + " kicked " + toKick);
		MWLogger.modLog(Username + " kicked " + toKick);
		
		/*try {
			Thread.sleep(100);//Why do we sleep here? Anyone?
		} catch (Exception ex) {
			MWLogger.errLog(ex);
		} */       
		
		try {
			
			CampaignMain.cm.getOpsManager().doDisconnectCheckOnPlayer(toKick);
			CampaignMain.cm.doLogoutPlayer(toKick);
			if (CampaignMain.cm.getServer().getClient(MWChatServer.clientKey(toKick)) != null)
				CampaignMain.cm.getServer().killClient(toKick,Username);
			
		} catch (Exception ex) {
			MWLogger.errLog(ex);
		}
       

	}
}