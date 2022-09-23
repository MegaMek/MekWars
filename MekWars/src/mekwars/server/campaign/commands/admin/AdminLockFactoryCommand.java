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

package server.campaign.commands.admin;

import java.util.StringTokenizer;

import common.util.MWLogger;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlanet;
import server.campaign.SUnitFactory;
import server.campaign.commands.Command;

// AdminLockPlanet#Planet#factory#true/false
public class AdminLockFactoryCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Planet#factory#true/false";
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
		
		try{
			SPlanet p = (SPlanet) CampaignMain.cm.getData().getPlanetByName(command.nextToken());
			if (p == null){
				CampaignMain.cm.toUser("Unknown planet!",Username,true);
				return;   
			}
			
			SUnitFactory uf = (SUnitFactory)CampaignMain.cm.getData().getFactoryByName(p,command.nextToken());
			if ( uf == null ){
				CampaignMain.cm.toUser("Unknown factory!",Username,true);
				return;               
			}
			
			boolean lock; 
			if (command.hasMoreElements())
				lock = Boolean.parseBoolean(command.nextToken());
			else {
				if (uf.isLocked())
					lock = false;
				else
					lock = true;
			}
			uf.setLock(lock);
			
			if (lock) {
				
				//set 9999 miniticks so factory can be used. use add instead of set so HSUpdates are sent.
				int currMiniTicks = uf.getTicksUntilRefresh();
				uf.addRefresh(9999 - currMiniTicks, true);
				
				//send messages
				CampaignMain.cm.toUser("You locked "+ uf.getName()+" on planet "+p.getName(),Username,true);
				//server.MWLogger.modLog(Username + " has locked "+ uf.getName()+" on planet "+p.getName());
				CampaignMain.cm.doSendModMail("NOTE",Username + " has locked "+ uf.getName()+" on planet "+p.getName());
				
			}
			else {
				
				//reset miniticks so factory can be used. use add instead of set so HSUpdates are sent.
				int miniTicksToRemove = uf.getTicksUntilRefresh();
				uf.addRefresh(-miniTicksToRemove, true);
				
				//send messages
				CampaignMain.cm.toUser("You unlocked "+ uf.getName()+" on planet "+p.getName(),Username,true);
				//server.MWLogger.modLog(Username + " has unlocked "+ uf.getName()+" on planet "+p.getName());
				CampaignMain.cm.doSendModMail("NOTE",Username + " has unlocked "+ uf.getName()+" on planet "+p.getName());
			}

		} catch (Exception ex){
			CampaignMain.cm.toUser("Command failed. Make sure format was: /c adminlockfactory#planetname#factoryname", Username, true);
			MWLogger.errLog(ex);
		}
		
	}
}