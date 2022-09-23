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
import java.util.TreeSet;

import server.MWClientInfo;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


/**
 * Moving the IPList command from MWServ into the normal command structure.
 *
 * Syntax  /c IPList
 */
public class IPListCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
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
        String result = "AM:Current IP's:<br>";
        TreeSet<MWClientInfo> sorted = new TreeSet<MWClientInfo>();
        sorted.addAll(CampaignMain.cm.getServer().getIPHelp().keySet());
        for (MWClientInfo m: sorted)
            result += m.getName() + ": " + m.getAdr() + "<br>";
        CampaignMain.cm.toUser(result, Username);
	}
}