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

import java.net.InetAddress;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


/**
 * Moving the UnBanIP command from MWServ into the normal command structure.
 *
 * Syntax  /c UnBanIP#Number
 */
public class UnBanIPCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Number";
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
    		//make sure that the an acceptably formatted ip was sent and checks if it is on the list and if yes then delete 
    		InetAddress ip = null;
    		String toUnBan = command.nextToken().trim();//ip to unban
    		ip = InetAddress.getByName(toUnBan);
    		if (!CampaignMain.cm.getServer().getBanIps().containsKey(ip)) {
    			CampaignMain.cm.toUser("AM:Value (" + ip + ") not found in banlist.", Username);
    			return;
    		}
            CampaignMain.cm.getServer().getBanIps().remove(ip);
            CampaignMain.cm.getServer().bansUpdate();
            CampaignMain.cm.toUser("AM:You unbanned: " + ip.toString(), Username);
            CampaignMain.cm.doSendModMail("NOTE",Username + " unbanned " + ip.toString());
        } catch(Exception ex) {
            CampaignMain.cm.toUser("AM:Syntax: unbanip (IPAddress)<br>Where IPAddress corresponds to the IPAddress in the ipban list like 12.12.12.12.", Username);
        }
	}
}