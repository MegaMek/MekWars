/*
 * MekWars - Copyright (C) 2004 
 * 
 * Original Author - Nathan Morris (urgru@users.sourceforge.net)
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

public class ModDeactivateCommand implements Command {
	
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
		
		String targetPlayerString = (String) command.nextElement();
		SPlayer targetPlayer = CampaignMain.cm.getPlayer(targetPlayerString);
		
		if (targetPlayer == null) {
			CampaignMain.cm.toUser(targetPlayerString + " cannot be found.",Username,true);
			return;
		}
		
		if (targetPlayer.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
			CampaignMain.cm.toUser(targetPlayerString + " is fighting. Cancel his game before deactivating.",Username,true);
			return;
		}
		
		if (targetPlayer.getDutyStatus() != SPlayer.STATUS_ACTIVE) {
			CampaignMain.cm.toUser("Target player is logged out or is already in reserve state. Nice try though.",Username,true);
			return;
		}
		
		//the above caught all non-active players, so can continure assuming activity.
		targetPlayer.setActive(false);
		CampaignMain.cm.toUser(Username + " forced you to return to reserve duty.",targetPlayerString,true);
		CampaignMain.cm.toUser("You forced " + targetPlayerString + " into reserve status.",Username,true);
		//server.MWLogger.modLog(Username + " force-deactivated " + targetPlayerString + ".");
		CampaignMain.cm.doSendModMail("NOTE",Username + " force-deactivated " + targetPlayerString + ".'");
		CampaignMain.cm.sendPlayerStatusUpdate(targetPlayer,!Boolean.parseBoolean(CampaignMain.cm.getConfig("HideActiveStatus")));	
		
	}//end process()
	
}//end ModCancelCommand