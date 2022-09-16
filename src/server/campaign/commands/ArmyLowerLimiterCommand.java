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

package server.campaign.commands;

import java.util.StringTokenizer;

import common.Army;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;

public class ArmyLowerLimiterCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		//access check
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		if (command.hasMoreElements()) {
			
			boolean limitsAllowed = Boolean.parseBoolean(CampaignMain.cm.getConfig("AllowLimiters"));
			if (!limitsAllowed) {
				CampaignMain.cm.toUser("AM:Limits are disabled.",Username,true);
				return;
			}
			
			int armyid = Integer.parseInt((String)command.nextElement());
			if (command.hasMoreElements()) {
				int limit = Integer.parseInt(command.nextToken());
				SPlayer p = CampaignMain.cm.getPlayer(Username);
				if (p != null) {
					if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
						CampaignMain.cm.toUser("AM:You cannot change limits while active.",Username,true);
						return;
					}
					
					SArmy army = p.getArmy(armyid);
					
					if (army != null) {
						if (limit < Army.NO_LIMIT) {//-1 is NO_LIMIT
							CampaignMain.cm.toUser("AM:You may not set negative limits.",Username,true);
							return;	
						}
						
						//check to make sure buffer isnt violated
						int bufferAmt = CampaignMain.cm.getIntegerConfig("LowerLimitBuffer");
						if (limit < bufferAmt && limit != Army.NO_LIMIT) {
							CampaignMain.cm.toUser("AM:You must set a lower limit of " + bufferAmt + " or more.",Username,true);
							return;
						}
						
						army.setLowerLimiter(limit);
						
						if (limit == -1)
							CampaignMain.cm.toUser("AM:Army #" + armyid + "'s lower limit disabled.",Username,true);
						else	
							CampaignMain.cm.toUser("AM:Army #" + armyid + "'s lower limit set to " + limit + ".",Username,true);
						
						CampaignMain.cm.toUser("PL|SAB|"+army.getID()+"#"+army.getLowerLimiter()+"#"+ army.getUpperLimiter(),Username,false);
					}
				}
			}
		}
	}
}