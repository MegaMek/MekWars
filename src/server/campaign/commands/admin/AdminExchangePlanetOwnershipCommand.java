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

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class AdminExchangePlanetOwnershipCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "planet#winner#loser#amount";
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
		
		//vars
		SPlanet planet = null;
		SHouse winningHouse = null;
		SHouse losingHouse = null;
		int amount = 0;
		
		try {
			planet = CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),Username);
			winningHouse = CampaignMain.cm.getHouseFromPartialString(command.nextToken(),Username);
			losingHouse = CampaignMain.cm.getHouseFromPartialString(command.nextToken(),Username);
			amount = Integer.parseInt(command.nextToken());
			
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminexchangeplanetownership#planet#winner#loser#amount", Username, true);
			return;
		}
		
		if (planet == null) {
			CampaignMain.cm.toUser("Could not find a matching planet.",Username,true);
			return;
		}
		
		if ( winningHouse == null ){
			CampaignMain.cm.toUser("Could not find a matching faction for the winner.",Username,true);
			return;
		}
		
		if ( losingHouse == null ){
			CampaignMain.cm.toUser("Could not find a matching faction for the loser.",Username,true);
			return;
		}
		
		if (amount <= 0){
			CampaignMain.cm.toUser("Get real try a number above 0!",Username,true);
			return;
		}
		
		//breaks passed
		int newAmount = planet.doGainInfluence(winningHouse,losingHouse, amount, true);
		
		//server.MWLogger.modLog(Username + " took " + newAmount + "% of "+ planet.getName() + " from " + losingHouse.getName() + " and gave it to " + winningHouse.getName() + ".");
		CampaignMain.cm.toUser("You took " + newAmount + "% of "+ planet.getName() + " from " + losingHouse.getName() + " and gave it to " + winningHouse.getName() + ".",Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " took " + newAmount + "% of "+ planet.getName() + " from " + losingHouse.getName() + " and gave it to " + winningHouse.getName() + ".");
	}
}