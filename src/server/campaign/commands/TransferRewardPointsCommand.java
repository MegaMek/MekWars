/*
 * MekWars - Copyright (C) 200 
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

/**
 * @author Targ
 */

package server.campaign.commands;

import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;


public class TransferRewardPointsCommand implements Command {
	
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
		
		SPlayer player = CampaignMain.cm.getPlayer(Username);
		SHouse house = player.getMyHouse();
		
		if (player.getMyHouse().isNewbieHouse()) {
			CampaignMain.cm.toUser("AM:You may not transfer " + CampaignMain.cm.getConfig("RPLongName") + " while in a training faction.",Username,true);
			return;
		}
		
		//Acquire needed Data
		String targetPlayer;
		int amount;
		try {
			targetPlayer = (String) command.nextElement();
			amount = Integer.parseInt((String) command.nextElement());
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper format. Try: /c transferrewardpoints#TargetPlayer#amount",Username,true);
			return;
		}
		
		SPlayer targetplayer = CampaignMain.cm.getPlayer(targetPlayer);
		if (targetplayer == null) {
			CampaignMain.cm.toUser("AM:Could not find a player named " + targetPlayer + ".",Username,true);
			return;
		}
		
		//no negative amounts
		if (amount < 1) {
			CampaignMain.cm.toUser("AM:You must transfer at least 1 " + CampaignMain.cm.getConfig("RPLongName") + ".",Username,true);
			return;
		}
		
		// check for same-ip interaction
		boolean ipcheck = Boolean.parseBoolean(house.getConfig("IPCheck"));
		if (ipcheck && CampaignMain.cm.getServer().getIP(player.getName()).toString().equals(CampaignMain.cm.getServer().getIP(targetplayer.getName()).toString())) {
			CampaignMain.cm.toUser("AM:"+targetplayer.getName() + " has the same IP as you do. You can't send them " + CampaignMain.cm.getConfig("RPLongName") + ".", Username, true);
			return;
		}
		
		// if the player is neither in the faction of the target, nor fighting for that faction
		if (!targetplayer.getHouseFightingFor().equals(player.getMyHouse()) && !targetplayer.getMyHouse().equals(player.getMyHouse())) {
			CampaignMain.cm.toUser("AM:"+targetplayer.getName() + " is not from your faction! You can't send them " + CampaignMain.cm.getConfig("RPLongName") + ".", Username, true);
			return;
		}

	if (!Boolean.parseBoolean(CampaignMain.cm.getConfig("AllowRPTransfer")))
	{
		CampaignMain.cm.toUser("AM:This feature has been disabled by the server operators.", Username, true);
		return;
	}
		
		//do the transfer
		player.addReward(-amount);
		targetplayer.addReward(amount);
		CampaignMain.cm.toUser("AM:You've transferred " + amount + " " + CampaignMain.cm.getConfig("RPLongName") + " to " + targetplayer.getName(), Username, true);
		CampaignMain.cm.toUser("AM:"+player.getName() + " sends you " + amount + " " + CampaignMain.cm.getConfig("RPLongName") + ".", targetPlayer, true);

	}
}
