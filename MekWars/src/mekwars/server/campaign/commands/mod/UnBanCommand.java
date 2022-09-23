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

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


/**
 * Moving the unban command from MWServ into the normal command structure.
 *
 * Syntax  /c unban#name
 */
public class UnBanCommand implements Command {
	
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

        try{
            String account = command.nextToken().toLowerCase();
            if (CampaignMain.cm.getServer().getBanAccounts().get(account) != null) {
                CampaignMain.cm.getServer().getBanAccounts().remove(account);
                CampaignMain.cm.getServer().bansUpdate();
                CampaignMain.cm.doSendModMail("NOTE",Username + " unbanned " + account);
                //MWLogger.modLog(Username + " unbanned " + account);

                CampaignMain.cm.toUser("AM:You unbanned " + account, Username);
                CampaignMain.cm.toUser("AM:Don't forget to unban any assotiated IP's as well with the unbanip command", Username);
            } else {
            	CampaignMain.cm.toUser("AM:Unban failed for " + account, Username);
            	CampaignMain.cm.doSendModMail("NOTE",Username + " tried to uban " + account + ", but failed.");
                //MWLogger.modLog(Username + " tried to uban " + account + ", but failed.");
            }
        }catch(Exception ex){
            CampaignMain.cm.toUser("AM:Syntax: unban (Username)<br>Don't forget to unban any assotiated IP's as well with the unbanip command", Username);
        }
	}
}