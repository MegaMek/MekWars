/*
 * MekWars - Copyright (C) 2007
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

package mekwars.server.campaign.commands.admin;

import common.House;
import common.Planet;
import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;

public class AdminReturnPlanetsToOriginalOwnersCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

    public String getSyntax() {return syntax;}

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

        //look for confirmation
        if (!command.hasMoreTokens() || !command.nextToken().equalsIgnoreCase("confirm")) {
            CampaignMain.campaignMain.toUser(
                  "Do you want to return all planets to their original owners? If so, [<a href=\"MEKWARS/c adminreturnplanetstooriginalowners#confirm\">click to confirm.</a>]",
                  Username,
                  true);
            return;
        }

        //check for double confirmation
        if (!command.hasMoreTokens() || !command.nextToken().equalsIgnoreCase("confirm")) {
            CampaignMain.campaignMain.toUser(
                  "Are you *ABSOLUTELY SURE* you want to return planets to their original owners? This cannot be easily reversed. If so, [<a href=\"MEKWARS/c adminreturnplanetstooriginalowners#confirm#confirm\">click to re-confirm.</a>]",
                  Username,
                  true);
            return;
        }

        for (House currH : CampaignMain.campaignMain.getData().getAllHouses()) {
            server.campaign.SHouse h = (server.campaign.SHouse) currH;
            h.getPlanets().clear();
            h.setComponentProduction(0);

        }

        //doubly confirmed. loop through every planet and restore it to the original owner
        for (Planet currP : CampaignMain.campaignMain.getData().getAllPlanets()) {

            //cast to planet
            server.campaign.SPlanet p = (server.campaign.SPlanet) currP;
            MWLogger.mainLog("Returning planet " + p.getName() + " to original owner");
            int totalCP = p.getConquestPoints();
            //get original owner
            server.campaign.SHouse origOwner = CampaignMain.campaignMain.getHouseFromPartialString(p.getOriginalOwner(),
                  Username);
            //change the ownership in the respective SHouses
            p.setOwner(p.getOwner(), origOwner, true);

            //change the planet's influence table

            java.util.HashMap<Integer, Integer> flu = new java.util.HashMap<Integer, Integer>();
            flu.put(origOwner.getId(), totalCP);
            p.getInfluence().setInfluence(flu);

            //set updated flag so players' maps refresh
            p.updated();
        }

        //server.MWLogger.modLog(Username + " restored all plants to their original owners.");
        CampaignMain.campaignMain.toUser("You restored all plants to their original owners.", Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " restored all plants to their original owners.");

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
