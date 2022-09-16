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

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.mercenaries.MercHouse;


public class UnemployedMercsCommand implements Command {
	
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
		
		String s = "Unemployed Mercenaries: ";
		Vector<MercHouse> mh = CampaignMain.cm.getMercHouses();
		for (int i = 0; i < mh.size(); i++) {
			MercHouse searchHouse = mh.get(i);
			Enumeration<SPlayer> e = searchHouse.getAllOnlinePlayers().elements();
			
			boolean foundMerc = false;
			while (e.hasMoreElements()) {
				SPlayer mp = e.nextElement();
				if (mp.getMyHouse().getHouseFightingFor(mp).isMercHouse()){
					if ( !foundMerc ){
						s += mp.getName();
						foundMerc = true;
					}
					else
						s +=  ", "+mp.getName();
				}
			}//end while
		}//end for(all merc factions)
		
		CampaignMain.cm.toUser(s, Username, true);
	}
}