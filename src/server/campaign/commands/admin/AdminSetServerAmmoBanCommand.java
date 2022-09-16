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


import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

public class AdminSetServerAmmoBanCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Munition Number";
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
		
		String ammoName = "";
		try{
			ammoName = command.nextToken();
		}
		catch (Exception ex){
			CampaignMain.cm.toUser("Invalid syntax. Try: adminsetserveradmmoban#munitionnumber",Username,true);
		}
		
		if ( CampaignMain.cm.getServerBannedAmmo().get(ammoName)!= null ){
			CampaignMain.cm.getServerBannedAmmo().remove(ammoName);
            CampaignMain.cm.getData().setServerBannedAmmo(CampaignMain.cm.getServerBannedAmmo());
			ammoName = CampaignMain.cm.getData().getMunitionsByNumber().get(Long.parseLong(ammoName));
			CampaignMain.cm.toUser("Server-wide ban on " + ammoName + " lifted.",Username,true);
			CampaignMain.cm.doSendModMail("NOTE",Username + " lifted the server-wide ban on " + ammoName+ ".");
		}
		else {
			CampaignMain.cm.getServerBannedAmmo().put(ammoName,"banned");
            CampaignMain.cm.getData().setServerBannedAmmo(CampaignMain.cm.getServerBannedAmmo());
			ammoName = CampaignMain.cm.getData().getMunitionsByNumber().get(Long.parseLong(ammoName));
			CampaignMain.cm.toUser(ammoName + " banned server-wide.",Username,true);
			CampaignMain.cm.doSendModMail("NOTE",Username + " banned " + ammoName+ " server-wide.");
		}
		
        CampaignMain.cm.saveBannedAmmo();
	}
}