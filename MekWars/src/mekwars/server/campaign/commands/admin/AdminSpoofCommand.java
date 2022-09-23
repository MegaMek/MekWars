/*
 * MekWars - Copyright (C) 2004 
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)
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
 * AdminSpoof allows an admin to issue ANY command on a player's behalf. Unlike
 * other commands, this cannot be delegated to lower userlevels. Instead, Spoof
 * is locked IAuthenticator.ADMIN.
 * 
 * Format:
 * adminspoof#targetname#command#[command options]
 */
public class AdminSpoofCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Target Name#command#[command options]";
	public String getSyntax() { return syntax;}
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i){accessLevel = i;}//cannot be changed
	
	public void process(StringTokenizer command,String Username) {
		
		//access level check
		if(CampaignMain.cm.getServer().getUserLevel(Username) < IAuthenticator.ADMIN) {
			CampaignMain.cm.toUser("Only admins may use the spoof command.",Username,true);
			return;
		}
		
		String targetPlayerName;
		String targetCommandName;
		try {
			targetPlayerName = command.nextToken();
			targetCommandName = command.nextToken();
		} catch (NoSuchElementException e) {
			CampaignMain.cm.toUser("Improper format. Try: /c adminspoof#targetname#commandname#[command inputs]", Username, true);
    		return;
		}
		
		//ensure the player exixts
		if (CampaignMain.cm.getPlayer(targetPlayerName) == null) {
			CampaignMain.cm.toUser("Spoof failed. Could not find player: " + targetPlayerName, Username, true);
    		return;
		}
		
		//uppercase the command, and make sure it exists in CampaignMain tree.
		if (CampaignMain.cm.getServerCommands().get(targetCommandName.toUpperCase()) == null) {
			CampaignMain.cm.toUser("Spoof failed. Could not find command: " + targetCommandName, Username, true);
    		return;
		}
		
		if ( CampaignMain.cm.getServerCommands().get(targetCommandName.toUpperCase()).getExecutionLevel() > CampaignMain.cm.getServer().getUserLevel(targetPlayerName)){
			CampaignMain.cm.toUser(targetPlayerName+"'s access level is too low to use command "+targetCommandName, Username);
			return;
		}
		
		//build a new string tokenizer to pass to the command, and a commandstring to show in logs, etc.
		StringBuilder issuedCommand = new StringBuilder();
		while (command.hasMoreTokens())
			issuedCommand.append(command.nextToken() + "#");
		
		StringTokenizer newCommand = new StringTokenizer(issuedCommand.toString(),"#");//rebuild a tokenizer to pass
		
		//checks passed. we have a valid player and command name. tell everyone about the spoof ...
		//MWLogger.modLog(Username + " used spoof to send a command as if he were " + targetPlayerName + ": /c " + targetCommandName + "#" + issuedCommand);
		CampaignMain.cm.doSendModMail("WARNING",Username + " used spoof to send a command as if he were " + targetPlayerName + ": /c " + targetCommandName + "#" + issuedCommand);
		CampaignMain.cm.toUser(Username + " issued a command on your behalf: /c " + targetCommandName + "#" + issuedCommand,targetPlayerName,true);
		CampaignMain.cm.toUser("You issued a command as if you were " + targetPlayerName + ": /c " + targetCommandName + "#" + issuedCommand,Username,true);
		
		// ... then actually do it.
		CampaignMain.cm.getServerCommands().get(targetCommandName.toUpperCase()).process(newCommand,targetPlayerName);
		
	}//end process()
}//end AdminTerminateAllCommand