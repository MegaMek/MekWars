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

import server.MWClientInfo;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


/**
 * Moving the Ignore command from MWServ into the normal command structure.
 *
 * Syntax  /c Ignore#Player
 */
public class IgnoreCommand implements Command {
	
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
        
        String user = command.nextToken();
        MWClientInfo client = CampaignMain.cm.getServer().getUser(user);
        
        //Offline users may only be de-listed.
        if (client.getName().equals("Nobody")) {
        	   CampaignMain.cm.getServer().getIgnoreList().remove(user);
               CampaignMain.cm.getServer().getFactionLeaderIgnoreList().remove(client.getName());
               //MWLogger.modLog(Username + " unmuted " + client.getName());
               CampaignMain.cm.toUser("AM:You set " + user + " to be ignored to: false. He/She is currently not in the channel.", Username);
               return;
        }
        
        //standard mute/unmute
        if (CampaignMain.cm.getServer().getIgnoreList().indexOf(client.getName()) == -1) {
        	CampaignMain.cm.getServer().getIgnoreList().add(client.getName());
        	//MWLogger.modLog(Username + " muted " + client.getName());
        	CampaignMain.cm.getServer().sendChat("AM:"+Username + " muted " + client.getName());
        } else {
        	CampaignMain.cm.getServer().getIgnoreList().remove(client.getName());
        	CampaignMain.cm.getServer().getFactionLeaderIgnoreList().remove(client.getName());
        	CampaignMain.cm.getServer().sendChat("AM:"+Username + " unmuted " + client.getName());
        	//MWLogger.modLog(Username + " unmuted " + client.getName());
        }

	}
}