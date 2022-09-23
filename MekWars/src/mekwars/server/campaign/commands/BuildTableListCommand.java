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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import common.util.MWLogger;
import megamek.common.Entity;
import server.campaign.CampaignMain;
import server.campaign.SUnit;


public class BuildTableListCommand implements Command {
	
	int accessLevel = 2;
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	String syntax = "";
	public String getSyntax() { return syntax;}

	
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		//Syntax BuildTableList#Path
		
		String filePath = "./data/buildtables";
		
		if ( command.hasMoreTokens() )
		    filePath = command.nextToken();
		
	    BufferedReader dis = null;
		try{
		    File file = new File(filePath);
		    String results = "Files in "+filePath+"<br>";
		    if ( file.isDirectory() ){
		        String[] fileList = file.list();
		        for( int i = 0; i < fileList.length; i++)
		            results += "<a href=\"MEKWARS/c buildtablelist#"+filePath+"/"+fileList[i]+"\">"+fileList[i]+"</a><br>";
		    }
		    if ( file.isFile() ){
		        FileInputStream fis = new FileInputStream(file);
		        dis = new BufferedReader(new InputStreamReader(fis));
		        while (dis.ready()) {
					String line = dis.readLine();
					if ( line.length() <= 1 )
					    continue;
					if ( line.indexOf(".") != -1 ){
						String fileWithOutChance = line.substring(line.indexOf(" ")).trim();
					    Entity ent = SUnit.loadMech(fileWithOutChance);
						if ( ent.getModel().equals("OMG-UR-FD") ){
						    MWLogger.errLog(fileWithOutChance+" errored in Build Table: "+filePath);
						    results += fileWithOutChance.trim()+"<br>";
						}
						else
						    results += "<a href=\"MEKINFO" +ent.getChassis() + " " +  ent.getModel()+ "#" + ent.calculateBattleValue() + "#4#5\">"+line.trim()+"</a><br>";
					}
					else if ( file.getName().indexOf("_") >= 0){
					    String typeWeight = file.getName().substring(file.getName().indexOf("_"));
					    line = line+typeWeight.trim();
					    String fileWithOutChance;
					    try {
							fileWithOutChance = line.substring(line.indexOf(" "));
						} catch (Exception ex) {
							fileWithOutChance = line;
						}
						String tempFilePath = filePath.substring(0,filePath.lastIndexOf("/"));
					    results += "<a href=\"MEKWARS/c buildtablelist#"+tempFilePath+"/"+fileWithOutChance.trim()+"\">"+line+"</a><br>";
					}
					
		        }
		    }
		    CampaignMain.cm.toUser("SM|"+results,Username,false);
		}
		catch(Exception ex){
		    CampaignMain.cm.toUser("AM:Unknown path try again!",Username,true);
		    MWLogger.errLog("Error with build table list");
		    MWLogger.errLog(ex);
		    return;
		} finally {
			try {
				if (dis != null) {
					dis.close();
				}
			} catch (IOException e) {
				MWLogger.errLog(e);
			}
		}
		

	}
}