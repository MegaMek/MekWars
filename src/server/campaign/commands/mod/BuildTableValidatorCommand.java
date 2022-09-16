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

package server.campaign.commands.mod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import common.util.MWLogger;
import megamek.common.Entity;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SUnit;
import server.campaign.commands.Command;


/**
 * This command just goes through the build lists and makes sure all of the 
 * files on the build list will load with what is in the servers zip files.
 * @author Torren Oct 21, 2005
 *
 * Syntax  /c buildtablevalidator#era
 */
public class BuildTableValidatorCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "[Standard/Reward/Rare]";
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
		
		//Syntax BuildTableList#Folder (standard/reward/rare/all)
		String era = "standard";
        
		if ( command.hasMoreTokens() )
		    era = command.nextToken();
		
		String filePath = "./data/buildtables/";
		
		StringBuffer results = new StringBuffer();
		if (era.equalsIgnoreCase("all")) {
			
			results.append("<b>Bad filenames in "+ filePath+ "standard:</b><br>");
			results.append(this.validate("standard",Username) + "<br>");
			
			results.append("<b>Bad filenames in "+ filePath + "reward:</b><br>");
			results.append(this.validate("reward",Username) + "<br>");
			
			results.append("<b>Bad filenames in "+ filePath + "rare:</b><br>");
			results.append(this.validate("rare",Username));
			
		}
		
		else {
			results.append("<b>Bad filenames in "+ filePath + era + ":</b><br>");
			results.append(this.validate(era,Username));
		}
		
		CampaignMain.cm.toUser("SM|"+results.toString(),Username,false);
	}
	
	private String validate(String folderName, String Username) {
		
		File file = new File("./data/buildtables/" + folderName);
		if (!file.exists())
			return file.getPath() + " does not exist. Try again.";
		if (!file.isDirectory())
			return file.getPath() + " is not a directory. Try again.";

		StringBuffer toReturn = new StringBuffer();

		//check all files within the dir. do not recurse directories.
		File[] fileList = file.listFiles();
		for(int i = 0; i < fileList.length; i++) {

			if (!fileList[i].isFile())
				continue;
			BufferedReader dis = null;
			try {
				
				FileReader fis = new FileReader(fileList[i]);
				dis = new BufferedReader(fis);
				while (dis.ready()) {

					//check for blank lines, which can throw off counts
					String line = dis.readLine();
					if (line.trim().length() == 0) {
						toReturn.append("Empty line! [Table: "+fileList[i].getPath()+"]<br>");
						continue;
					}

					if (line.startsWith(" ")) {
						toReturn.append("Leading space! " + line + "[Table: "+fileList[i].getPath()+"]<br>");
						continue;
					}
					
					//filter out named factions, but this will still show false positives
					if (line.indexOf(".") == -1 && CampaignMain.cm.getHouseFromPartialString(line, null) != null) {
						toReturn.append("Missing file extension? " + line + "[Table: "+fileList[i].getPath()+"]<br>");
						continue;
					}
					
					String lowerLine = line.toLowerCase();
					if (!lowerLine.endsWith("mtf")
							&& !line.endsWith("blk")
							&& !line.endsWith("hmp")
							&& !line.endsWith("hmv")
							&& !line.endsWith("xml")) {
						toReturn.append("Unknown file extension? " + line + "[Table: "+fileList[i].getPath()+"]<br>");
						continue;
					}
					
					
					String unitToBuild = line.substring(line.indexOf(" ")).trim();
					Entity ent = SUnit.loadMech(unitToBuild);
					if (ent.getModel().equals("OMG-UR-FD"))
						toReturn.append("Error loading: " + unitToBuild.trim()+" [Table: "+fileList[i].getPath()+"]<br>");

				}
			} catch (Exception e) {
				MWLogger.errLog(e);
				return "Error from FileReader of BufferedReader while opening files. Check permissions.";
			} finally {
				try {
					dis.close();
				} catch (IOException e) {
					MWLogger.errLog(e);
				}
			}
		}

		return toReturn.toString();
		
	}
}