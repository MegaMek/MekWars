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


import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;

import common.Planet;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.util.PlanetNameComparator;

public class ISStatusCommand implements Command {
	
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
		
		if (command.hasMoreElements()) {
			String HouseString = (String)command.nextElement();
			//Check if the factionname was only partial and complete it..
			SHouse theone = CampaignMain.cm.getHouseFromPartialString(HouseString,Username);
			if (theone == null)
				return;
			if (command.hasMoreElements()){
				String next = command.nextToken();
				boolean owner = false;
				if (next.equalsIgnoreCase("own")){
					owner = true;
					if ( command.hasMoreElements())
						next = command.nextToken();
				}
				
				doShowISStatus(Username,theone.getName(),next,owner);
			}
			else
				doShowISStatus(Username,theone.getName(),"null",false);
		}
	}
	
	public void doShowISStatus(String User, String h, String h2, boolean onlyOwner) {
		String result = "<h2>Universe Status";
		TreeSet<SPlanet> Sorted = new TreeSet<SPlanet>(new PlanetNameComparator());
		int hID=  CampaignMain.cm.getData().getHouseByName(h).getId();
		int hID2 = -1;
		if ( CampaignMain.cm.getData().getHouseByName(h2) != null )
			hID2 = CampaignMain.cm.getData().getHouseByName(h2).getId();
		
		if (h != null) {
			result += " for Faction " + h;
			if (onlyOwner)
				result += " (only planets owned are shown)";
			if ( hID2 != -1 )
				result += " and Faction " + h2;
		}
		result += ":</h2>";
		Iterator<Planet> e = CampaignMain.cm.getData().getAllPlanets().iterator();
		while (e.hasNext()) {
			SPlanet p = (SPlanet)e.next();
			boolean show = false;
			if (h != null) {
				if (onlyOwner) {
					if (p.getOwner() != null) {
						if ( hID2 != -1 
								&& p.getOwner().getName().equals(h)
								&& p.getInfluence().getInfluence(hID2) > 0)
							show = true;
						else if (p.getOwner().getName().equals(h))
							show = true;
					}
				}
				else if ( hID2 != -1){
					if (p.getInfluence().getInfluence(hID2) > 0
							&& p.getInfluence().getInfluence(hID) > 0)
						show = true;
				}
				else
				{
					if (p.getInfluence().getInfluence(hID) > 0)
						show = true;
				} 
			}
			else
				show = true;
			
			if (show)
				Sorted.add(p);
		}
		
		Iterator<SPlanet> it = Sorted.iterator();
		while (it.hasNext()) {
			result += it.next().getSmallStatus(true);
		}
		CampaignMain.cm.toUser("SM|" + result,User,false);
	}
	
}
