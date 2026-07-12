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

package mekwars.server.campaign.commands.admin;

import common.House;
import common.Planet;
import mekwars.server.campaign.CampaignMain;
import server.campaign.util.HouseRankingHelpContainer;
import server.campaign.util.Statistics;

public class AdminCalculateHouseRankingsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

    public String getSyntax() {return syntax;}

    //calculate faction rankings by comparing with original owner
    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        //do the calculations...
        java.util.HashMap<Integer, Integer> original = new java.util.HashMap<Integer, Integer>();
        java.util.HashMap<Integer, Integer> current = new java.util.HashMap<Integer, Integer>();
        for (House h : CampaignMain.campaignMain.getData().getAllHouses()) {
            original.put(h.getId(), 0);
            current.put(h.getId(), 0);
        }
        java.util.Collection<Planet> planets = CampaignMain.campaignMain.getData().getAllPlanets();
        for (Planet planet : planets) {
            int originalHouseId = CampaignMain.campaignMain.getData()
                                        .getHouseByName(planet.getOriginalOwner())
                                        .getId();
            original.put(originalHouseId, original.get(originalHouseId) + 100);
            for (House h : planet.getInfluence().getHouses()) {
                current.put(h.getId(), current.get(h.getId()) + planet.getInfluence().getInfluence(h.getId()));
            }
        }

        java.util.TreeSet<HouseRankingHelpContainer> s = CampaignMain.campaignMain.getHouseRanking();
        for (HouseRankingHelpContainer h : s) {
            h.getHouse().setInitialHouseRanking(original.get(h.getHouse().getId()));
            h.setAmount(current.get(h.getHouse().getId()));
        }

        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " has recalculated the faction rankings");
        CampaignMain.campaignMain.toUser("You have recalculated the faction rankings", Username, true);

        String result = "SM|" + Statistics.getReadableHouseRanking(true);
        CampaignMain.campaignMain.toUser(result, Username, false);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
