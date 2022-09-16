/*
 * MekWars - Copyright (C) 2007 
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

package server.campaign.commands.mod;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlanet;
import server.campaign.SUnitFactory;
import server.campaign.commands.Command;

//modrefreshfactory#planet#factory
public class ModRefreshFactoryCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Planet Name#Factory Name";
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
		
		String planetName;
		String factoryName;
		try {
			planetName = command.nextToken();
			factoryName = command.nextToken();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper format. Try: /c modrefreshfactory#planetname#factoryname",Username,true);
			return;   
		}
		
		SPlanet p = (SPlanet)CampaignMain.cm.getData().getPlanetByName(planetName);
		if (p == null){
			CampaignMain.cm.toUser("Could not find planet: " + planetName + ".",Username,true);
			return;   
		}
		
		SUnitFactory uf = (SUnitFactory)CampaignMain.cm.getData().getFactoryByName(p,factoryName);
		if (uf == null){
			CampaignMain.cm.toUser("Could not find factory: " + factoryName + ".",Username,true);
			return;               
		}
		
		int ticksToRemove = uf.getTicksUntilRefresh();
		String refresh = uf.addRefresh(-ticksToRemove, true);//use get and add instead of set b/c add sends HS update

		
		//send update to all players
		if ( p.getOwner() != null )
			CampaignMain.cm.doSendToAllOnlinePlayers(p.getOwner(), "HS|" + refresh, false);

		CampaignMain.cm.doSendModMail("NOTE",Username+" has refreshed factory "+uf.getName()+" on planet "+p.getName()+"!");	}
}