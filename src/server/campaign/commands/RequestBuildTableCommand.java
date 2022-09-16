	/*
	 * MekWars - Copyright (C) 2008
	 * 
	 * Original author - Bob Eldred (billypinhead@users.sourceforge.net)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import common.util.MWLogger;
import server.campaign.CampaignMain;


public class RequestBuildTableCommand implements Command {

	/*
	 * This command allows an Admin to upload a single build table
	 * from a directory on their local machine.  The directory structure
	 * on the local machine must match that on the server - i.e, ./buildtables/rare
	 * ./buildtables/reward and ./buildtables/standard.  The replacement build
	 * table is put into place and a backup of the original build table is created.
	 */
	
	int accessLevel = 2;
	String syntax = "list";
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
		String subcommand = command.nextToken();
		if(subcommand.equalsIgnoreCase("list")) {
			StringBuilder toReturn = new StringBuilder();
			String folderDelimiter="?";
			String fileDelimiter="*";
			String[] folderList = {"standard"};
			boolean viewer = false;
			
			if ( command.hasMoreTokens() ) {
			    viewer = Boolean.parseBoolean(command.nextToken());
			}
			
			for (int i = 0; i < folderList.length; i++) {
				File currF = new File("./data/buildtables/" + folderList[i]);
				toReturn.append(folderList[i]);
				toReturn.append(folderDelimiter);
				if (!currF.exists() || !currF.isDirectory())
					continue;
				File fileNames[] = currF.listFiles();
				
				for (File currFile : fileNames) {
					if(currFile.getName().endsWith("txt")) { 
						toReturn.append(currFile.getName());
						toReturn.append(fileDelimiter);
					}
				}
				toReturn.append(folderDelimiter);
			}
			CampaignMain.cm.toUser("BT|PLS|"+ toReturn.toString()+"|"+viewer, Username, false);
			
			return;
		} else if (subcommand.equalsIgnoreCase("get")) {
			StringBuilder toReturn = new StringBuilder();
			String folder = "";
			String table = "";
			long time = 0;
			
			if(command.hasMoreTokens())
				folder = command.nextToken();
			if(command.hasMoreTokens())
				table = command.nextToken();
			
			if ( command.hasMoreTokens() ) {
			    time = Long.parseLong(command.nextToken());
			}
			
			if(folder.length() == 0 || table.length() == 0) {
				CampaignMain.cm.toUser("Bad Build Table Request: " + (folder.length()==0 ? "Empty folder name" : "Empty file name"), Username, true);
				return;
			}
			File file = new File("./data/buildtables/" + folder + "/" + table);
			if(!file.exists()) {
				CampaignMain.cm.toUser("Bad Build Table Request: " + folder + "/" + table + " does not exist.", Username, true);
				return;
			}
			
			if ( time >= file.lastModified() ) {
			    return;
			}
			
			// The request is good, so send it.
			try {
				FileInputStream in = new FileInputStream(file);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				try {
					while(br.ready()) {
						toReturn.append("|" + br.readLine());
					}
					br.close();
					in.close();
				} catch (IOException ex) {
					
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				MWLogger.errLog(e);
			}
			CampaignMain.cm.toUser("BT|BT|" + folder + "|" + table + toReturn.toString(), Username, false);
			
		} else if ( subcommand.equalsIgnoreCase("view") ) {
		    CampaignMain.cm.toUser("BT|VS|DONE#DONE", Username, false);
		}
	}
}