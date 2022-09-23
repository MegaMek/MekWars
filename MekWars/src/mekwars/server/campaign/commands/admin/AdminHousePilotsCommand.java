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

import java.util.LinkedList;
import java.util.StringTokenizer;

import common.Unit;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.PilotQueues;
import server.campaign.SHouse;
import server.campaign.commands.Command;
import server.campaign.pilot.SPilot;

public class AdminHousePilotsCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "faction";
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
		SHouse h = null;
		
		try {
			h = CampaignMain.cm.getHouseFromPartialString(command.nextToken(),Username);
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminhousepilots#faction", Username, true);
			return;
		}
		
		if (h == null) {
			CampaignMain.cm.toUser("Couldn't find a faction with that name.", Username, true);
			return;
		}
		
		//input looks good. fetch queue from house
		PilotQueues currQ = h.getPilotQueues();
		StringBuilder toReturn = new StringBuilder();
		
		//loop through queue and put results in a StringBuilder
		for (int type = 0; type < Unit.MAXBUILD; type++) {
            toReturn.append("Faction Base Pilot: " + currQ.getBaseGunnery(type) + "/" + currQ.getBasePiloting(type)+"<br>");
            toReturn.append("Faction Base Pilot Skills: " + currQ.getBasePilotSkill(type)+"<br>");
			toReturn.append("<b>Queue for " + Unit.getTypeClassDesc(type) + ":</b><OL>");
			LinkedList<SPilot> l = currQ.getPilotQueue(type);
			for (SPilot currP : l)
				toReturn.append("<LI>"+currP.getName()+"("+currP.getGunnery()+"/"+currP.getPiloting()+") ["+currP.getSkillString(true)+"]</LI>");
			toReturn.append("</OL>");
		}
		
		h.updated();
		//send to caller and notify mod channel
		CampaignMain.cm.toUser(toReturn.toString(),Username,true);
		//server.MWLogger.modLog(Username + " checked House " + h.getName() + " Pilots");
		CampaignMain.cm.doSendModMail("NOTE",Username + " checked House "+ h.getName() + " Pilots");
		
		
		
	}
}