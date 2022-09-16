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
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package server.campaign.commands;

import java.util.StringTokenizer;

import server.campaign.CampaignMain;


public class SendToMiscCommand implements Command {
	
	int accessLevel = 200;
	public int getExecutionLevel(){return 200;}
	public void setExecutionLevel(int i) {accessLevel = 200;}
	String syntax = "";
	public String getSyntax() { return syntax;}

	public void process(StringTokenizer command,String Username) {
		
		String endUser = command.nextToken();
		
		if ( !Username.startsWith("[Dedicated]"))
		    return;
		
		if ( command.hasMoreTokens() ){
		    StringBuilder result = new StringBuilder();
		    while( command.hasMoreElements() ){
		        result.append(command.nextToken());
		        result.append(" ");
		    }
			CampaignMain.cm.toUser("DMML|"+Username+"|"+result.toString(),endUser,false);
		}
	}
}
	