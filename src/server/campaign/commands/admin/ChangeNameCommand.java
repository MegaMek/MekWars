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

import java.io.File;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class ChangeNameCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Old Name#New Name";
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
		
		//variables
		String oldName = "";
		String newName = "";
		
		try {
			oldName = command.nextToken();
			newName = command.nextToken();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c changename#oldname#newname", Username, true);
			return;
		}
		
		
		SPlayer p = CampaignMain.cm.getPlayer(oldName);
		if (p == null) {
			CampaignMain.cm.toUser("Couldn't find player with name " + oldName, Username, true);
			return;
		}
		
		if (p.getDutyStatus() != SPlayer.STATUS_RESERVE) {
			CampaignMain.cm.toUser("You may only rename players who are in reserve.", Username, true);
			return;
		}
		
		//old player exists. nuke him in the faction, then re-add with a new name.
		CampaignMain.cm.doLogoutPlayer(oldName);//logout, for safety ...
		p.getMyHouse().removePlayer(p,false);//delete account. dont dupe the units.
		
		//delete old pfile
		File fp = new File("./campaign/players/" + p.getName().toLowerCase() + ".dat");
		if (fp.exists())
			fp.delete();
		
		//change the name
		p.setName(newName);
        CampaignMain.cm.forceSavePlayer(p);

		CampaignMain.cm.toUser("You changed " + oldName + "'s name to '" + newName + "'.",Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " changed " + oldName + "'s name to '" + newName + "'.");
		CampaignMain.cm.toUser(Username + " changed your name from '" + oldName + "' name to '" + newName + "'. Quit and re-join " +
				"for the change to take full effect.",oldName,true);
		
	}//end process()
	
}