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

import common.UnitFactory;
import common.util.MWLogger;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class AdminDestroyFactoryCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN, factoryID;
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	String syntax = "Planet Name#Factory Name";
	public String getSyntax() { return syntax;}

	
	public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		try {
			SPlanet p = CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),Username);
			String factoryname = command.nextToken();
			if ( p == null ) {
				CampaignMain.cm.toUser("Planet not found:",Username,true);
				return;
			}
			
			if (  p.getUnitFactories().size() < 1) {
				CampaignMain.cm.toUser("This planet does not have any factories!",Username,true);
				return;
			}
			
			UnitFactory foundFactory = null;
			for (UnitFactory UF : p.getUnitFactories()){
				if ( UF.getName().equalsIgnoreCase(factoryname)) {
					foundFactory = UF; 
					break;
				}
			}
			
			if ( foundFactory == null){
				CampaignMain.cm.toUser("Factory " + factoryname + " not found",Username,true);
				return;
			}
			
			p.getUnitFactories().removeElement(foundFactory);
			p.getUnitFactories().trimToSize();
			

			p.updated();
			//server.MWLogger.modLog(Username + "  removed " + factoryname + " from " + p.getName() + ".");
			CampaignMain.cm.toUser(factoryname + " removed from " + p.getName() + ".",Username,true);
			CampaignMain.cm.doSendModMail("NOTE",Username + "  removed " + factoryname + " from " + p.getName() + ".");
		} catch (Exception ex){
			MWLogger.errLog(ex);
		}//end catch
		
	}
}