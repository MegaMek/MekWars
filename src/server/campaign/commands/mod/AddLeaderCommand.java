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
import server.util.MWPasswd;

/**
 * Add a Leader to a faction.
 */
public class AddLeaderCommand implements Command {
	
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
			player.getMyHouse().addLeader(player.getName());
			int level = CampaignMain.cm.getIntegerConfig("factionLeaderLevel");
			if ( player.getPassword().getAccess() < level ) {
				//CampaignMain.cm.updatePlayersAccessLevel(target,level);
				MWPasswd.getRecord(target).setAccess(level);
				CampaignMain.cm.getServer().getClient(target).setAccessLevel(level);
				CampaignMain.cm.getServer().getUser(target).setLevel(level);
				CampaignMain.cm.getServer().sendRemoveUserToAll(target, false);
				CampaignMain.cm.getServer().sendNewUserToAll(target, false);
				MWPasswd.writeRecord(player.getPassword(), target);
				if (player != null) {
					CampaignMain.cm.doSendToAllOnlinePlayers("PI|DA|" + CampaignMain.cm.getPlayerUpdateString(player), false);
				}
			}
			CampaignMain.cm.toUser("AM:You have been promoted to the faction leadership by "+Username+".", target);
			CampaignMain.cm.doSendHouseMail(player.getMyHouse(), "NOTE", player.getName()+" has been promoted to the faction leadership.");
			CampaignMain.cm.doSendModMail("NOTE",Username+" has added promoted "+target+" to faction leader.");		
			
		}catch(Exception ex){
			CampaignMain.cm.toUser("AM:Invalid syntax: /addleader UserName", Username);
		}
	}		
}