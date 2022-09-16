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

import server.MWChatServer.MWChatServer;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class DeleteAccountCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Name#ScrapUnits[true/false]";
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
		
		//vars
		SPlayer p = null;
		boolean scrapUnits;
		
		try {
			p = CampaignMain.cm.getPlayer(command.nextToken());
			scrapUnits = Boolean.parseBoolean(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c deleteaccount#Name#ScrapUnits", Username, true);
			return;
		}
		
		if (p == null) {
			CampaignMain.cm.toUser("Couldn't find a player with that name.", Username, true);
			return;
		}
		
		if (p.getDutyStatus() > SPlayer.STATUS_RESERVE) {
			CampaignMain.cm.toUser("Fighting and active players may not be deleted.", Username, true);
			return;
		}
		
		//non-null player, delete the account
		p.getMyHouse().removePlayer(p,!scrapUnits);
		
		//delete the pfile
		File fp = new File("./campaign/players/" + p.getName().toLowerCase() + ".dat");
		if (fp.exists())
			fp.delete();
		
		CampaignMain.cm.toUser("You deleted " + p.getName() + "'s account.",Username,true);
		CampaignMain.cm.toUser(Username + " deleted your account.",p.getName(),true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " deleted " + p.getName() + "'s account.");
		//server.MWLogger.modLog(Username + " deleted " + p.getName() + "'s account.");
		CampaignMain.cm.doLogoutPlayer(p.getName(),false);  //Baruk Khazad! 20151110
		if (CampaignMain.cm.getServer().getClient(MWChatServer.clientKey(p.getName())) != null)
			CampaignMain.cm.getServer().killClient(p.getName(),Username);
		
	}//end process()
	
}