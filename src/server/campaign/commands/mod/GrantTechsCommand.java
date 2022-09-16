/*
 * MekWars - Copyright (C) 2008 
 * 
 * Original author - Jason Tighe (torren@users.sourceforge.net)
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

import common.util.UnitUtils;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class GrantTechsCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Player Name#Tech Type#Amount";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		SPlayer p = CampaignMain.cm.getPlayer(command.nextToken());
		int techType = Integer.parseInt(command.nextToken());
		int amount = Integer.parseInt(command.nextToken());
		
		if ( amount < 0 && p.getTotalTechs().elementAt(techType) < Math.abs(amount) ) {
		    amount = -p.getTotalTechs().elementAt(techType);
		}
		
		p.addTotalTechs(techType, amount);
		p.addAvailableTechs(techType, amount);
		CampaignMain.cm.toUser("AM:"+Username+" has granted you "+amount+" "+UnitUtils.techDescription(techType)+ " techs.", p.getName());
		CampaignMain.cm.doSendModMail("NOTE", Username +" has granted "+p.getName()+" "+ amount +" "+UnitUtils.techDescription(techType)+ " techs.");
	}
}