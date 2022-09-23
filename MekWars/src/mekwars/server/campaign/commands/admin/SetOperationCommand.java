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
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

//Syntax setoperation#optype#opname#data
public class SetOperationCommand implements Command {
    
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
            CampaignMain.cm.toUser("Syntax setoperation#optype#opname",Username,true);
            return;
        }
        
        File opFile = new File("./data/operations/"+opType+"/"+opName+".txt");

        try{
            FileOutputStream fos = new FileOutputStream(opFile);
            PrintStream ps = new PrintStream(fos);
            while (command.hasMoreTokens()){
                ps.println(command.nextToken().replaceAll("\\(pound\\)","#"));
            }
            ps.close();
            fos.close();
            
        }catch(Exception ex){
            CampaignMain.cm.toUser("Unable to write to "+opFile.getName(),Username,true);
            return;
        }
        
        CampaignMain.cm.doSendModMail("NOTE",Username+" has updated "+opFile.getName());
        // Delete md5 file so clients will refresh properly
        File md5File = new File("./data/operations/opsmd5.txt");
        if (md5File.exists()) {
        	md5File.delete();
        }
    }
}//end RetrieveShortOperation
