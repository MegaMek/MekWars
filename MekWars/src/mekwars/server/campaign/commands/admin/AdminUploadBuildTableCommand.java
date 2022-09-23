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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


public class AdminUploadBuildTableCommand implements Command {

	/*
	 * This command allows an Admin to upload a single build table
	 * from a directory on their local machine.  The directory structure
	 * on the local machine must match that on the server - i.e, ./buildtables/rare
	 * ./buildtables/reward and ./buildtables/standard.  The replacement build
	 * table is put into place and a backup of the original build table is created.
	 */
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "[rare/reward/standard]/Build Table File Name";
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
		String fileName = "./data/buildtables/" + command.nextToken();

		if (!command.hasMoreElements()){
			CampaignMain.cm.toUser("No file data found. This may be due to the fact that the command line was used intead of the GUI.", Username,true);
			return;
		}
		try {
			File newTable = new File(fileName);
			if(newTable.exists()) {
				// Back it up
				String newFileName = fileName + ".bak";
				File backupFile = new File(newFileName);
				if(backupFile.exists())
					backupFile.delete();
				newTable.renameTo(backupFile);
				newTable.delete();
			}
			FileOutputStream out = new FileOutputStream(fileName);
			PrintStream p = new PrintStream(out);
			while (command.hasMoreTokens()) {
				p.println(command.nextToken());
			}
			p.close();
			out.close();
			
			CampaignMain.cm.toUser(fileName + " Saved", Username, true);
		}
		catch ( Exception ex){
			CampaignMain.cm.toUser("File Not found",Username,true);
			return;
		}
		
	}
}