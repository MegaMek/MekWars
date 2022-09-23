/*
 * MekWars - Copyright (C) 2005  
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

import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;

public class DeclineAttackFromReserveCommand implements Command {
	
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

        SPlayer ap = null;
        
        try{
            ap = CampaignMain.cm.getPlayer(command.nextToken());
        }catch(Exception ex){
            return;
        }
        
        Long launchTime = ap.getLastAttackFromReserve();
        
        if ( launchTime + (Long.parseLong(CampaignMain.cm.getConfig("AttackFromReserveResponseTime"))*60000) < System.currentTimeMillis() ){
            CampaignMain.cm.toUser("AM:Sorry but this offer has already expired.",Username,true);
            return;
        }
        
        //else
        
        ap.setLastAttackFromReserve(launchTime-(Long.parseLong(CampaignMain.cm.getConfig("AttackFromReserveResponseTime"))*60000));
        
        CampaignMain.cm.toUser("AM:"+Username+" has declined your proposal.",ap.getName(),true);
        CampaignMain.cm.toUser("AM:You have declined "+ap.getName()+"'s proposal.",Username,true);
        
	}//end process
	
}//end AttackFromReserveCommand