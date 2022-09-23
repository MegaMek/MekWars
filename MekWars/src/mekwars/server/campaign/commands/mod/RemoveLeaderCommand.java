/*
 * MekWars - Copyright (C) 2007 
 *
 * Original author - jtighe (torren@users.sourceforge.net)
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

/**
 * Remove a part from a player.
 */
public class RemoveLeaderCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Player Name";
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
		SPlayer player;
		
		try{
			String target = command.nextToken();
			player = CampaignMain.cm.getPlayer(target);
			player.getMyHouse().removeLeader(player.getName());
			int level = CampaignMain.cm.getIntegerConfig("factionLeaderLevel");
			//if they where just a normal leader then send them back to level 2
			//if they where giving higher then normal access because of mods or admin
			//status or even a admin lacky then let them keep their level.
			if ( player.getPassword().getAccess() <= level ){
				level = 2;
				CampaignMain.cm.updatePlayersAccessLevel(target,level);
			}
			CampaignMain.cm.toUser("AM:You have been demoted as a faction leader by "+Username+".", target);
			CampaignMain.cm.doSendHouseMail(player.getMyHouse(), "Note", player.getName()+" has been demoted from the faction leadership.");
			CampaignMain.cm.doSendModMail("NOTE",Username+" has removed "+player.getName()+" as a faction leader.");
		}catch(Exception ex){
			CampaignMain.cm.toUser("Invalid syntax: /addleader UserName", Username);
		}

	}		
}