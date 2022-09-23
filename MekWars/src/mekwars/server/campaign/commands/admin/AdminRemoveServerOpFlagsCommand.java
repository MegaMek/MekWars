/*
 * MekWars - Copyright (C) 2006 
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

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


public class AdminRemoveServerOpFlagsCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "";
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
		
        if ( !command.hasMoreTokens() ){
            CampaignMain.cm.toUser("Syntax AdminRemoveServerOpFlags#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times." , Username);
            return;
        }
        
        try{
            while (command.hasMoreTokens()){
                String key = command.nextToken();
                if ( CampaignMain.cm.getData().getPlanetOpFlags().remove(key) != null ){
                    CampaignMain.cm.toUser("Op flag "+key+" removed from the server.",Username,true);
                    //server.MWLogger.modLog(Username + " removed op flag "+key+".");
                    CampaignMain.cm.doSendModMail("NOTE",Username + " removed op flag "+key+".");
                }
            }
        }catch (Exception ex){
            CampaignMain.cm.toUser("Syntax AdminRemoveServerOpFlags#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times." , Username);
            return;
        }
        
	}
}