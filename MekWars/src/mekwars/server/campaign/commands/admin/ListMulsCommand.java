/*
 * MekWars - Copyright (C) 2006 
 *
 * Original author - jtighe (torren@users.sourceforge.net)
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

import java.io.File;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


public class ListMulsCommand implements Command {

	/*
	 * This command allows an Admin to upload a single build table
	 * from a directory on their local machine.  The directory structure
	 * on the local machine must match that on the server - i.e, ./buildtables/rare
	 * ./buildtables/reward and ./buildtables/standard.  The replacement build
	 * table is put into place and a backup of the original build table is created.
	 */
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Option Box[True/False]";
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

		File mulDir = new File("./data/armies");
		String clientCommand = null;
		
		if ( command.hasMoreTokens() )
			clientCommand = command.nextToken();
		
		if ( !mulDir.exists() ) {
			CampaignMain.cm.toUser("AM:./data/armies/ folder not found.", Username);
			return;
		}
		
		StringBuffer fileNames = new StringBuffer();
		
		if ( clientCommand != null ) {
			fileNames.append("PL|");
			fileNames.append(clientCommand);
			fileNames.append("|");
			for (File file : mulDir.listFiles()) {
				fileNames.append(file.getName());
				fileNames.append("#");
			}
		}else{
			fileNames.append("SM|");
			for (File file : mulDir.listFiles()) {
				fileNames.append("<a href=\"MEKWARS/c retrievemul#");
				fileNames.append(file.getName());
				fileNames.append("\">");
				fileNames.append(file.getName());
				fileNames.append("</a><br>");
			}
			
		}
			
		CampaignMain.cm.toUser(fileNames.toString(), Username,false);
	}
}