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
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

//Syntax retrieveoperation#optype#opname
public class RetrieveOperationCommand implements Command {
    
    int accessLevel = IAuthenticator.ADMIN;
    public int getExecutionLevel(){return accessLevel;}
    public void setExecutionLevel(int i) {accessLevel = i;}
	String syntax = "Op Type[Short/Long/Special]#Op Name";
	public String getSyntax() { return syntax;}

    public void process(StringTokenizer command,String Username) {
        
        //access level check
        int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
        if(userLevel < getExecutionLevel()) {
            CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
            return;
        }
        
        String opType;
        String opName;
        
        try{
            opType = command.nextToken();
            opName = command.nextToken();
        }catch (Exception ex){
            CampaignMain.cm.toUser("Syntax retrieveoperation#optype#opname",Username,true);
            return;
        }
        
        File opFile = new File("./data/operations/"+opType+"/"+opName+".txt");

        
        if ( !opFile.exists() ){
            CampaignMain.cm.toUser("No file found for Op "+opName,Username,true);
            return;
        }
            
        StringBuilder opData = new StringBuilder();
        
        try{
            FileInputStream fis = new FileInputStream(opFile);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
            opData.append(opName+"#");
            while (dis.ready()){
                opData.append(dis.readLine().replaceAll("#","(pound)")+"#");
            }
            dis.close();
            fis.close();
            
        }catch(Exception ex){
            CampaignMain.cm.toUser("Unable to read "+opFile,Username,true);
            return;
        }
        
        CampaignMain.cm.doSendModMail("NOTE",Username+" has retrieved "+opFile);
        
        CampaignMain.cm.toUser("PL|RSOD|"+opData.toString(),Username,false);
    }
}//end RetrieveOperation
