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

import common.CampaignData;
import common.House;
import common.Unit;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.commands.Command;

// AdminGrantComponents#Faction#Type#WeightClass#Components
public class AdminResetFactionComponentsCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "faction name or all#Full or a number";

	final int actionFill = 0;
	final int actionEmpty = 1;
	final int actionFillTo = 2;
	
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
		int action = -1;
		int amountToFillTo = 0;
		String fname = "";
		String requestedAmount = "";
		
		if (! command.hasMoreTokens()) {
			CampaignMain.cm.toUser("Improper command.  Try /adminResetFactionComponents " + syntax, Username);
			return;
		}
		fname = command.nextToken();
		
		if (! command.hasMoreTokens()) {
			CampaignMain.cm.toUser("Improper command.  Try /adminResetFactionComponents " + syntax, Username);
			return;
		}
		requestedAmount = command.nextToken();
		if (requestedAmount.trim().equalsIgnoreCase("full")) {
			action = actionFill;
		} else {
			try {
				amountToFillTo = Integer.parseInt(requestedAmount);
			} catch (Exception e) {
				CampaignMain.cm.toUser("Improper amount.  Could not parse " + requestedAmount + " into Integer.", Username);
				return;
			}
			if (amountToFillTo == 0) {
				action = actionEmpty;
			} else {
				action = actionFillTo;
			}
		}
		
		if (fname.trim().equalsIgnoreCase("all")) {
			// Cycle through the factions
			for (House faction : CampaignData.cd.getAllHouses()) {
				// Call the function that actually does all the work
				if (((SHouse)faction).isNewbieHouse() || faction.getName().equalsIgnoreCase("None") ) {
					continue;
				}
				reset((SHouse)faction, action, amountToFillTo);
				CampaignMain.cm.toUser("You reset components for " + faction.getName(), Username, true);
				CampaignMain.cm.doSendModMail("NOTE", Username + " reset components for " + faction.getName());				
			}
		} else {
			try {
				h = (SHouse)CampaignMain.cm.getData().getHouseByName(fname);
				if (h == null) {
					CampaignMain.cm.toUser("Unable to find faction " + fname, Username);
					return;
				}
			} catch (Exception e) {
				CampaignMain.cm.toUser("Unable to find faction " + fname, Username);
				return;
			}
			reset(h, action, amountToFillTo);
			CampaignMain.cm.toUser("You reset components for " + h.getName(), Username, true);
			CampaignMain.cm.doSendModMail("NOTE", Username + " reset components for " + h.getName());
			
		}
	}
	
	private void reset(SHouse h, int action, int fillTo) {
		for (int weight = Unit.LIGHT; weight <= Unit.ASSAULT; weight++) {
			for (int type = Unit.MEK; type < Unit.MAXBUILD; type++) {
				if (action == actionFill) {
					h.addPP(weight, type, h.getMaxAllowedPP(weight, type) - h.getPP(weight, type), true);
				} else {
					h.addPP(weight, type, fillTo - h.getPP(weight, type), true);
				}
			}
		}
	}
	
	public AdminResetFactionComponentsCommand() {
		
	}
}