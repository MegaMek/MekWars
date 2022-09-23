/*
 * MekWars - Copyright (C) 2006 
 * 
 * Original author - coelocanth
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

import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeSet;

import common.House;
import common.Planet;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;
import server.campaign.util.HouseRankingHelpContainer;
import server.campaign.util.Statistics;

public class AdminCalculateHouseRankingsCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}

	//calculate faction rankings by comparing with original owner
	public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		//do the calculations...
		HashMap<Integer,Integer> original = new HashMap<Integer,Integer>();
		HashMap<Integer,Integer> current = new HashMap<Integer,Integer>();
		for(House h:CampaignMain.cm.getData().getAllHouses()) {
			original.put(h.getId(), 0);
			current.put(h.getId(), 0);
		}
		Collection<Planet> planets = CampaignMain.cm.getData().getAllPlanets();
		for (Planet planet:planets) {
			int originalHouseId = CampaignMain.cm.getData().getHouseByName(planet.getOriginalOwner()).getId();
			original.put(originalHouseId, original.get(originalHouseId)+100);
			for(House h:planet.getInfluence().getHouses()) {
				current.put(h.getId(), current.get(h.getId())+planet.getInfluence().getInfluence(h.getId()));
			}
		}
		
		TreeSet<HouseRankingHelpContainer> s = CampaignMain.cm.getHouseRanking();
		for (HouseRankingHelpContainer h : s) {
			h.getHouse().setInitialHouseRanking(original.get(h.getHouse().getId()));
			h.setAmount(current.get(h.getHouse().getId()));
		}
		
		CampaignMain.cm.doSendModMail("NOTE",Username + " has recalculated the faction rankings");
		CampaignMain.cm.toUser("You have recalculated the faction rankings",Username,true);
		
		String result = "SM|" + Statistics.getReadableHouseRanking(true);
		CampaignMain.cm.toUser(result,Username,false);
	}
}
