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

package server.campaign.commands.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


public class AdminViewLogCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "/path/file";
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
		
		String fileName = command.nextToken();
		if  ( fileName.startsWith("../") ) {
			CampaignMain.cm.doSendModMail("NOTE",Username + " tried to viewed file " + fileName+" but was denied!");
			CampaignMain.cm.toUser("Sorry but you are not allowed to backout of the root directory!",Username,true);
			return;
		}
		try {
			File logFile = new File("./"+fileName);
			if ( logFile.length() > 3072000) {
				CampaignMain.cm.toUser("The file you are trying to open is over 3 megs that is not allowed!",Username,true);
				return;
			}
			FileInputStream fis = new FileInputStream(logFile);
			BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
			while (dis.ready()) {
				CampaignMain.cm.toUser("SM|"+dis.readLine(),Username,false);
			}
			fis.close();
			dis.close();
			CampaignMain.cm.doSendModMail("NOTE",Username + " has viewed file" + logFile);
		}
		catch ( Exception ex){
			CampaignMain.cm.toUser("File Not found",Username,true);
			return;
		}
		
	}
}