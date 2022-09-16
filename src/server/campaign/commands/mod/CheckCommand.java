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

package server.campaign.commands.mod;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class CheckCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Player Name";
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
		
		SPlayer p = null;
		try {
			p = CampaignMain.cm.getPlayer(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper format. Try: /c check#name",Username,true);
			return;
		}
		
		if (p == null) {
			CampaignMain.cm.toUser("AM:Couldn't find a user with that name.",Username,true);
			return;
		}
		
		//String to return
		String toMod = p.getColoredName() + " is currently ";
		
		//player's status
		int status = p.getDutyStatus();
		if (status == SPlayer.STATUS_FIGHTING)
			toMod += "fighting. ";
		else if (status == SPlayer.STATUS_ACTIVE)
			toMod += "active. ";
		else
			toMod += "inactive. ";
		
		//player's resources and levels
		toMod += "He has " + CampaignMain.cm.moneyOrFluMessage(true,true, p.getMoney())+", ";
		toMod += p.getExperience() + " EXP, ";
		toMod += CampaignMain.cm.moneyOrFluMessage(false,true, p.getInfluence()) + " and ";
		toMod += p.getReward() + " " + CampaignMain.cm.getConfig("RPShortName") + "s.<br>";
		toMod += " - Client version is " + p.getPlayerClientVersion() + ".<br>";
		toMod += " - IP addess is " + CampaignMain.cm.getServer().getIP(p.getName()) + ".<br>";
		toMod += " - Userlevel is " + CampaignMain.cm.getServer().getUserLevel(p.getName()) + ".";
		toMod += " - Multiplayer group is " + p.getGroupAllowance() + " (0 == no group).";
		
		//send messages and log use
		CampaignMain.cm.toUser(toMod,Username,true);
		//server.MWLogger.modLog(Username + " checked " + p.getName());
		CampaignMain.cm.doSendModMail("NOTE",Username + " checked " + p.getName());
		
	}
}