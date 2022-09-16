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

import java.util.ArrayList;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;
import server.campaign.operations.OperationManager;
import server.campaign.operations.ShortOperation;

public class AdminTerminateAllCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "";
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
		
		//get the player
		SPlayer tp = CampaignMain.cm.getPlayer(Username);
		if (tp == null) {
			CampaignMain.cm.toUser("Null player. Report this immediately!",Username,true);
			return;
		}
		
		//determine which should cancel
		ArrayList<ShortOperation> opsToCancel = new ArrayList<ShortOperation>();		
		for (ShortOperation currO : CampaignMain.cm.getOpsManager().getRunningOps().values()) {
			if (currO.getStatus() != ShortOperation.STATUS_REPORTING && currO.getStatus() != ShortOperation.STATUS_FINISHED)
				opsToCancel.add(currO);
		}
		
		//do the cancelling
		for (ShortOperation currO : opsToCancel) {
			CampaignMain.cm.getOpsManager().terminateOperation(currO, OperationManager.TERM_TERMCOMMAND, tp);
		}

		//MWLogger.modLog(Username + " terminated all unfinished games.");
		CampaignMain.cm.doSendToAllOnlinePlayers(Username + " terminated all unfinished games.", true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " terminated all unfinished games.");
		
	}//end process()
}//end AdminTerminateAllCommand