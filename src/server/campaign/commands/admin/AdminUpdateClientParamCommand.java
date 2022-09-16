/*
 * MekWars - Copyright (C) 2006
 * 
 * Original author - jtighe (torren@sourceforge.net)
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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

/**
 * AdminUpdateClientParam allows an admin to set a client param for a single player 
 * or all players online. Unlike other commands, this cannot be delegated to lower 
 * userlevels. Instead, it is locked IAuthenticator.ADMIN.
 * 
 * Format:
 * adminupdateclientparam#targetname[ALL]#param#param value
 */
public class AdminUpdateClientParamCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Player Name[ALL]#param#param value";
	public String getSyntax() { return syntax;}

	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i){accessLevel = i;}//cannot be changed
	
	public void process(StringTokenizer command,String Username) {
		
		//access level check. hard check'ed to admin.
		if(CampaignMain.cm.getServer().getUserLevel(Username) < IAuthenticator.ADMIN) {
			CampaignMain.cm.toUser("Only admins may use the update client param command.",Username,true);
			return;
		}
		
		String playerName;
		String param;
        String paramValue;
		try {
			playerName = command.nextToken();
			param = command.nextToken();
            paramValue = command.nextToken();
		} catch (NoSuchElementException e) {
			CampaignMain.cm.toUser("Improper format. Try: /adminupdateclientparam player Name[ALL]#param#param value", Username, true);
    		return;
		}
		
		//ensure the player exits
		if (!playerName.equalsIgnoreCase("all") && CampaignMain.cm.getPlayer(playerName) == null) {
			CampaignMain.cm.toUser("update client param failed. Could not find player: " + playerName, Username, true);
    		return;
		}

        if ( !playerName.equalsIgnoreCase("all") ){
            CampaignMain.cm.toUser("PL|UCP|"+param+"|"+paramValue, playerName, false);
        }else{
            CampaignMain.cm.doSendToAllOnlinePlayers("PL|UCP|"+param+"|"+paramValue, false);
        }
        
		//checks passed. we have a valid player and command name. tell everyone about the spoof ...
		//MWLogger.modLog(Username + " used update client param to update " + playerName + "'s " + param + " to " + paramValue);
		CampaignMain.cm.doSendModMail("WARNING",Username + " used update client param to update " + playerName + "'s " + param + " to " + paramValue);
		CampaignMain.cm.toUser("you updated " + playerName + "'s " + param + " to " + paramValue,Username,true);
		
	}//end process()
}//end AdminTerminateAllCommand