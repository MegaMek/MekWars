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

package server.campaign.commands.mod;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;


public class GooseCommand implements Command {
	
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
		
		try{
			String player = command.nextToken();
			SPlayer p = CampaignMain.cm.getPlayer(player);
			
			if ( p == null) {
				CampaignMain.cm.toUser("AM:Sorry you cannot find "+player+" to goose!", Username);
				return;
			}
			
			if ( p.getName().equalsIgnoreCase("torren") || p.getName().equalsIgnoreCase("spork") || userLevel < CampaignMain.cm.getServer().getUserLevel(p.getName()) ) {
				CampaignMain.cm.toUser(p.getName()+" grabs your hand and breaks it just before your able to goose 'em!", Username);
				CampaignMain.cm.toUser(Username+" tried to goose you but you deftly avoided it!",p.getName());
				CampaignMain.cm.doSendModMail("NOTE",Username + " tried to goose " + p.getName() + " and nearly lost their hand for it.");
				return;
			}
			
			
				
			CampaignMain.cm.toUser("AM:You goose " + p.getName() + ".",Username,true);
			CampaignMain.cm.toUser("AM:"+Username+" goosed you!", p.getName());
			CampaignMain.cm.doSendModMail("NOTE",Username + " goosed " + p.getName() + ".");
		}
		catch(Exception ex){
			CampaignMain.cm.toUser("AM:You really need to specify whom you would like to goose", Username);
			return;
		}
	}
}