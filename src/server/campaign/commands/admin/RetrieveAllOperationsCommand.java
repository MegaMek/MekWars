/*
 * MekWars - Copyright (C) 2007 
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
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

//Syntax retrievealloperations#optype
public class RetrieveAllOperationsCommand implements Command {
    
    int accessLevel = IAuthenticator.ADMIN;
    public int getExecutionLevel(){return accessLevel;}
    public void setExecutionLevel(int i) {accessLevel = i;}
	String syntax = "optype[Short/Long/Speical]";
	public String getSyntax() { return syntax;}

    public void process(StringTokenizer command,String Username) {
        
        //access level check
        int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
        if(userLevel < getExecutionLevel()) {
            CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
            return;
        }
        
        String opType;
        
        try{
            opType = command.nextToken();
        }catch (Exception ex){
            CampaignMain.cm.toUser("Syntax retrievealloperations#optype",Username,true);
            return;
        }
        
        File opFiles = new File("./data/operations/"+opType);

        
        if ( !opFiles.exists() ){
            CampaignMain.cm.toUser("No files found for Op type "+opType,Username,true);
            return;
        }
            
        StringBuilder opData = new StringBuilder();
        
    	for (File opFile : opFiles.listFiles()){
    		try{
	            FileInputStream fis = new FileInputStream(opFile);
	            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
	            opData.append(opFile.getName().substring(0,opFile.getName().lastIndexOf(".txt"))+"#");
	            while (dis.ready()){
	                opData.append(dis.readLine().replaceAll("#","(pound)")+"#");
	            }
	            dis.close();
	            fis.close();
	            CampaignMain.cm.doSendModMail("NOTE",Username+" has retrieved "+opFile.getName());
	            
	            CampaignMain.cm.toUser("PL|RSOD|"+opData.toString(),Username,false);
	            //Clean it out for use again.
	            opData.setLength(0);
	        }catch(Exception ex){
	            CampaignMain.cm.toUser("Unable to read "+opFile.getName(),Username,true);
	            return;
	        }
    	}
        
    }
}//end RetrieveAllOperations
