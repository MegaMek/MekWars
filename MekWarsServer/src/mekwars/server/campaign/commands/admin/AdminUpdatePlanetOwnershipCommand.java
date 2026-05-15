/*
 * MekWars - Copyright (C) 2007
 *
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

/**
 * @author jtighe This Command is used by server admins to update owners for a planet.
 *
 */

package mekwars.server.campaign.commands.admin;

import common.House;
import mekwars.server.campaign.CampaignMain;

public class AdminUpdatePlanetOwnershipCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#Faction Name#amount";

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

        //vars
        server.campaign.SPlanet planet = null;
        House house = null;
        String houseName;
        int ownerShip = 0;
        int id = -1;

        try {
            planet = CampaignMain.campaignMain.getPlanetFromPartialString(command.nextToken(), Username);
            houseName = command.nextToken();
            house = CampaignMain.campaignMain.getHouseFromPartialString(houseName, null);
            ownerShip = Integer.parseInt(command.nextToken());

        } catch (Exception e) {
            CampaignMain.campaignMain.toUser(
                  "Improper command. Try: /c adminupdateplanetownership#planet#faction#amount",
                  Username,
                  true);
            return;
        }

        if (planet == null) {
            CampaignMain.campaignMain.toUser("Could not find a matching planet.", Username, true);
            return;
        }

        if (house == null && !houseName.equalsIgnoreCase("none")) {
            CampaignMain.campaignMain.toUser("Could not find a matching faction to remove.", Username, true);
            return;
        }

        if (ownerShip <= 0) {
            CampaignMain.campaignMain.toUser("Ownership cannot be less then or equal to 0", Username, true);
            return;
        }

        if (house != null) {id = house.getId();}

        if (ownerShip == planet.getInfluence().getInfluence(id)) {return;}

        planet.getInfluence().updateHouse(id, ownerShip);
        planet.updated();

        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " updated " + houseName + " ownership of " + planet.getName() + " to " + ownerShip + ".");
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
