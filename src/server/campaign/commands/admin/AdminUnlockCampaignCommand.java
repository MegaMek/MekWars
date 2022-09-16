/*
 * MekWars - Copyright (C) 2004 
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
import server.campaign.commands.Command;

public class AdminUnlockCampaignCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "";
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
		
		if (Boolean.parseBoolean(CampaignMain.cm.getConfig("CampaignLock")) != true) {
			CampaignMain.cm.toUser("AM:Campaign is already unlocked.",Username,true);
			return;
		}
		
		//reset the lock property so players can activate
		CampaignMain.cm.getConfig().setProperty("CampaignLock","false");
		
		//tell the admin he has unlocked the campaign
        CampaignMain.cm.doSendToAllOnlinePlayers("AM:"+Username+" unlocked the campaign!", true);
		CampaignMain.cm.toUser("AM:You unlocked the campaign. Players may now activate.",Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " unlocked the campaign");
		
	}//end Process()
	
}