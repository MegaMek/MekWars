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


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.commands.Command;

public class AdminListHouseBannedAmmoCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Faction Name";
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
        
        String faction = null;
        if ( command.hasMoreTokens() )
            faction = command.nextToken();
        else{
            CampaignMain.cm.toUser("Unkown House. Syntax: /c AdminListHouseBannedAmmo#HouseName",Username,true);
            return;
        }
        
		SHouse h = CampaignMain.cm.getHouseFromPartialString(faction,Username);
		
		if ( h == null || h.getBannedAmmo().size() <= 0 )
			CampaignMain.cm.toUser("That faction is not currently banning any ammo.",Username,true);
		else {
		    CampaignMain.cm.toUser("Banned ammo for Faction "+h.getName(),Username,true);
			Enumeration<String> ammoBan = h.getBannedAmmo().keys();
			Hashtable<Long,String> munitions = CampaignMain.cm.getData().getMunitionsByNumber();
			while (ammoBan.hasMoreElements()) {
				String ammoName = ammoBan.nextElement();
				CampaignMain.cm.toUser(munitions.get(Long.parseLong(ammoName)),Username,true);
			}
		}
		
	}//end process
}