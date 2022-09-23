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

package server.campaign.commands.mod;

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

public class GetModLogCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		BufferedReader dis = null;
		try {
			File configFile = new File("./logs/modlog.0");
			FileInputStream fis = new FileInputStream(configFile);
			dis = new BufferedReader(new InputStreamReader(fis));
			String total = "";
			while (dis.ready()) {
				String line = dis.readLine();
				total += line + "<br>";
			}
			CampaignMain.cm.toUser("SM|" + total,Username,false);
			CampaignMain.cm.doSendModMail("NOTE",Username + " read the modlog.");
		} catch (Exception ex) {
		    MWLogger.errLog(ex);
		} finally {
			try {
				dis.close();
			} catch (IOException e) {
				MWLogger.errLog(e);
			}
		}
		
		
		
	}
}