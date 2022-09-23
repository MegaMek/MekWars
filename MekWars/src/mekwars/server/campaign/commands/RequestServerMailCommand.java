/*
 * MekWars - Copyright (C) 2005
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

/**
 * @author Torren (Jason Tighe)
 * sends server stored mail to the user!
 * 
 */
package server.campaign.commands;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;

// refreshfactory#planet#factory
public class RequestServerMailCommand implements Command {
	
	int accessLevel = IAuthenticator.GUEST;
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
		
        if (CampaignMain.cm.getServer().getServerMail().get(Username.toLowerCase()) != null) {
            CampaignMain.cm.toUser("PM|SERVER|" + (CampaignMain.cm.getServer().getServerMail().get(Username.toLowerCase())), Username,false);
    		CampaignMain.cm.getServer().getServerMail().remove(Username.toLowerCase());
            CampaignMain.cm.getServer().doWriteMailFile();
        }
        else
            CampaignMain.cm.toUser("AM:Sorry but you do not have any mail waiting for you.",Username,true);
	}
}