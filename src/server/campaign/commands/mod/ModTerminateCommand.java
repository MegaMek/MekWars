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
import server.campaign.operations.OperationManager;
import server.campaign.operations.ShortOperation;

public class ModTerminateCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Game Number";
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
		
		//get the Op ID and the Army ID
		int opID = -1;
		
		try {
			opID = Integer.parseInt(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper format. Try: /c modterminate#game number",Username,true);
			return;
		}
		
		//get the player
		SPlayer tp = CampaignMain.cm.getPlayer(Username);
		if (tp == null) {
			CampaignMain.cm.toUser("Null player. Report this immediately!",Username,true);
			return;
		}
		
		//check the attack
		ShortOperation so = CampaignMain.cm.getOpsManager().getRunningOps().get(opID);
		if (so == null) {
			CampaignMain.cm.toUser("AM:Terminate failed. Attack #" + opID + " does not exist.",Username,true);
			return;
		}
		
		//don't cancel finished or reporting games
		if (so.getStatus() == ShortOperation.STATUS_FINISHED) {
			CampaignMain.cm.toUser("AM:Terminate failed. You may not terminate a completed game.",Username,true);
			return;
		}
		
		//terminate
		CampaignMain.cm.getOpsManager().terminateOperation(so, OperationManager.TERM_TERMCOMMAND, tp);
		
		//Make a string which holds involved players names. Use
		//an incrementing ocunter to make sure that formatting is
		//correct for a list of any given size.
		String players = "Players were ";
		int playerCounter = 1;
		int totalPlayers = so.getAllPlayerNames().size();
		for (String currPlayerName : so.getAllPlayerNames()) {
			playerCounter++;
			players += currPlayerName;
			if (playerCounter == totalPlayers)
				players += " and ";
			else if (playerCounter > totalPlayers)
				players += ".";
			else
				players += ", ";
		}
		
		CampaignMain.cm.toUser("AM:You terminated Attack #" + opID + ". " + players,Username,true);
		//server.MWLogger.modLog(Username + " terminated Attack #" + opID + ". " + players);
		CampaignMain.cm.doSendModMail("NOTE",Username + " terminated Attack #" + opID + ". " + players);
		
	}//end process()
	
}//end ModCancelCommand