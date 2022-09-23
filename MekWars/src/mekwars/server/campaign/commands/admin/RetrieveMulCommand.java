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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import common.util.MWLogger;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


public class RetrieveMulCommand implements Command {

	/*
	 * This command allows an Admin to upload a single build table
	 * from a directory on their local machine.  The directory structure
	 * on the local machine must match that on the server - i.e, ./buildtables/rare
	 * ./buildtables/reward and ./buildtables/standard.  The replacement build
	 * table is put into place and a backup of the original build table is created.
	 */
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "FileName";
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
		String fileName = "./data/armies/" + command.nextToken();
		BufferedReader br = null;
		try {
			File mul = new File(fileName);
			if(!mul.exists()) {
				CampaignMain.cm.toUser("Unable to find file "+fileName, Username);
			}
			StringBuffer sendData = new StringBuffer("PL|RMF|");
			FileInputStream fis = new FileInputStream(mul);
			br = new BufferedReader(new InputStreamReader(fis));
			
			sendData.append(mul.getName());
			sendData.append("#");
			while ( br.ready() ){
				sendData.append(br.readLine());
				
				if ( sendData.lastIndexOf("#") == sendData.length()-1)
					sendData.append(" ");
				sendData.append("#");
			}
			CampaignMain.cm.toUser(sendData.toString(), Username,false);
			
		}
		catch ( Exception ex){
			CampaignMain.cm.toUser("File Not found",Username,true);
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					MWLogger.errLog(e);
				}
			}
			return;
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				MWLogger.errLog(e);
			}
		}
		
		CampaignMain.cm.doSendModMail("NOTE", Username+" has retrived mul file "+fileName);
		
	}
}