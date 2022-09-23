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
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class InvisCommand implements Command {
	
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
		
        SPlayer player = CampaignMain.cm.getPlayer(Username);
        if (player == null)
            return;
        
        if (player.getDutyStatus() != SPlayer.STATUS_RESERVE) {
        	CampaignMain.cm.toUser("AM:You must be in reserve to change visibility.",Username,true);
        	return;
        }
        
        player.setInvisible(!player.isInvisible());

        CampaignMain.cm.getServer().sendRemoveUserToAll(Username,false);

        CampaignMain.cm.getServer().getUser(Username).setInvis(player.isInvisible());
        CampaignMain.cm.getServer().sendNewUserToAll(Username,false);
        
        //Fix for BUG 1491951: post-invisibility status
        CampaignMain.cm.sendPlayerStatusUpdate(player,true);

        CampaignMain.cm.toUser("AM:You have become "+ (player.isInvisible()?"invisible":"visible"),Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " has become " + (player.isInvisible()?"invisible":"visible"));		
	}
}