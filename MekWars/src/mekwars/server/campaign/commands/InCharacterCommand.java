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

import server.MWClientInfo;
import server.campaign.CampaignMain;

public class InCharacterCommand implements Command {
	
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
		
		if (command.countTokens() < 1)
			return;
		
		String toSend = command.nextToken();
		while (command.hasMoreElements())
			toSend += command.nextToken();
		
		if (toSend.trim().length() == 0)
			return;
		
		toSend = "(In Character)"+Username+":" + toSend;
		
        //if client is somehow null, just send the message
        MWClientInfo client = CampaignMain.cm.getServer().getUser(Username);
        if (client == null) 
        	return;
        
        boolean generalMute = CampaignMain.cm.getServer().getIgnoreList().indexOf(client.getName()) > -1;
        boolean factionMute = CampaignMain.cm.getServer().getFactionLeaderIgnoreList().indexOf(client.getName()) > -1;
       
        if (generalMute || factionMute)
            CampaignMain.cm.toUser("AM:You've been set to ignore mode and cannot participate in chat.", Username,true);
        else
        	CampaignMain.cm.doSendToAllOnlinePlayers(toSend,true);
		
	}
}

