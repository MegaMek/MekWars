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
import server.campaign.SPlanet;

public class CalcDistCommand implements Command {
	
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
		
		if (!command.hasMoreElements()) {
			CampaignMain.cm.toUser("SM|You need to enter 2 Planet Names!", Username, false);
			return;
		}
		
		SPlanet p1 = CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),Username);
		if (!command.hasMoreElements()) {
			CampaignMain.cm.toUser("SM|You need to enter 2 Planet Names!", Username, false);
			return;
		}
		
		SPlanet p2 = CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),Username);
		if (p1 != null && p2 != null) {
			int xdiff = (int)(Math.pow(p1.getPosition().getX() - p2.getPosition().getX(),2));
			int ydiff = (int)(Math.pow(p1.getPosition().getY() - p2.getPosition().getY(),2));
			int newdist = (int)Math.sqrt(xdiff+ydiff);
			CampaignMain.cm.toUser("SM|The distance between " + p1.getName() + " and " + p2.getName() + " is " + newdist + " LY",Username,false);
		}
		
	}//end process()
}
