/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - Torren (torren@users.sourceforge.net)
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

/**
 * 
 * @author Torren (Jason Tighe) 10.13.05 
 * 
 */

package server.campaign.commands;

import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;

public class DisplayUnitRepairJobsCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "";
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
		
        try{
            int unitid = Integer.parseInt(command.nextToken());
            String data = CampaignMain.cm.getRTT().unitRepairTimes(unitid);
            if ( data != null )
                CampaignMain.cm.toUser("FSM|"+data,Username,false);
            else{
                SPlayer player = CampaignMain.cm.getPlayer(Username);
                SUnit unit = player.getUnit(unitid);
                
                CampaignMain.cm.toUser("FSM|#"+unitid+" "+unit.getEntity().getShortNameRaw()+" has the following repair jobs pending:<br><b>None.</b><br>",Username,false);
            }
        }catch(Exception ex){}
	}
}