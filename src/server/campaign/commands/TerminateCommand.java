/*
 * MekWars - Copyright (C) 2005  
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
package server.campaign.commands;

import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.operations.OperationManager;
import server.campaign.operations.ShortOperation;

/**
 * Terminate command is analagous to the old
 * cancel command used for Tasks. Should be
 * mirrored in the CampaignMain command tree.
 */
public class TerminateCommand implements Command {
	
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
		
		//get the Op ID and the Army ID
		int opID = -1;
        ShortOperation so = null;
        
        //get the player
        SPlayer tp = CampaignMain.cm.getPlayer(Username);
        if (tp == null) {
            CampaignMain.cm.toUser("AM:Null player. Report this immediately!",Username,true);
            return;
        }
        
        if ( command.hasMoreTokens() ){
    		try {
    			opID = Integer.parseInt(command.nextToken());
                so = CampaignMain.cm.getOpsManager().getRunningOps().get(opID);
            } catch (Exception e) {
    			CampaignMain.cm.toUser("AM:Improper format. Try: /c terminate#attack number",Username,true);
    			return;
    		}
        }
        else{
            so = CampaignMain.cm.getOpsManager().getShortOpForPlayer(tp);
        }

        //check the attack
		
		if (so == null) {
			CampaignMain.cm.toUser("AM:Terminate failed. Attack #" + opID + " does not exist.",Username,true);
			return;
		}
		
		//if the player isnt in the game, reject
		if (!so.getAllPlayerNames().contains(tp.getName().toLowerCase())) {
			CampaignMain.cm.toUser("AM:Terminate failed. You must be a participant in order to terminate an Attack.",Username,true);
			return;
		}
		
		//don't cancel finished or reporting games
		if (so.getStatus() == ShortOperation.STATUS_FINISHED || so.getStatus() == ShortOperation.STATUS_REPORTING) {
			CampaignMain.cm.toUser("AM:Terminate failed. You may not terminate a completed game.",Username,true);
			return;
		}
		
		if ( so.getStatus() == ShortOperation.STATUS_WAITING ){
			CampaignMain.cm.toUser("AM:Terminate failed. You may not terminate a game that has yet to start!", Username);
			return;
		}
		
		so.getCancelledPlayers().add(Username.toLowerCase());
		
		// Check if opponents are offline.  Otherwise, this game cannot be cancelled by a user who has been disconnected on
		for (String currPlayerName : so.getAllPlayerNames()) {
			if(CampaignMain.cm.getPlayer(currPlayerName).getDutyStatus() == SPlayer.STATUS_LOGGEDOUT) {
				if (!so.getCancelledPlayers().contains(currPlayerName.toLowerCase())) {
						so.getCancelledPlayers().add(currPlayerName.toLowerCase());
				}
			}
		}
		
		if ( so.getCancelledPlayers().size() >= so.getAllPlayerNames().size() ){
			
			String msg = "Cancelling Operation "+so.getName();
			for (String currPlayerName : so.getAllPlayerNames()) {
				CampaignMain.cm.toUser(msg, currPlayerName);
			}
			//terminate
			CampaignMain.cm.getOpsManager().terminateOperation(so, OperationManager.TERM_TERMCOMMAND, tp);
		}else{
			CampaignMain.cm.toUser("AM:Informing all other participants of your wish to cancel the operation.", Username);

			String msg = "AM:"+Username+" wishes to cancel Operation #"+so.getShortID()+" "+so.getName()+ " <a href=\"MEKWARS/c terminate#"+so.getShortID()+"\">click here to confirm</a>";
			for (String currPlayerName : so.getAllPlayerNames()) {
				
				if ( !so.getCancelledPlayers().contains(currPlayerName.toLowerCase()) && !Username.equalsIgnoreCase(currPlayerName) )
					CampaignMain.cm.toUser(msg, currPlayerName);
			}
		}
	}//end process
	
}//end TerminateCommand