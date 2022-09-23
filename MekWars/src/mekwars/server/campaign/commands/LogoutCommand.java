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

package server.campaign.commands;

import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;


public class LogoutCommand implements Command {

	//conforming methods
	public int getExecutionLevel(){return 0;}
	public void setExecutionLevel(int i) {}
	String syntax = "";
	public String getSyntax() { return syntax;}

	
	public void process(StringTokenizer command,String Username) {

        SPlayer p = CampaignMain.cm.getPlayer(Username);
        
        if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE
        		&& System.currentTimeMillis() - p.getActiveSince() < Long.parseLong(CampaignMain.cm.getConfig("MinActiveTime")) * 1000) {
        	CampaignMain.cm.toUser("AM:You can't log out yet (must meet minimum activity time).",Username,true);
        	return;
        }
        
        if (p.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
			CampaignMain.cm.toUser("AM:You cannot log out until your game is over.",Username,true);
        	return;
        }
        
        CampaignMain.cm.doLogoutPlayer(Username);
    }
}