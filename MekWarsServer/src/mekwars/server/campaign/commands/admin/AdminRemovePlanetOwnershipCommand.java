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
 * @author jtighe This Command is used by server admins to remove owners from a planet.
 *
 */

package mekwars.server.campaign.commands.admin;

import common.House;

public class AdminRemovePlanetOwnershipCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#Faction Name";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        //vars
        server.campaign.SPlanet planet = null;
        House removingHouse = null;

        try {
            planet = server.campaign.CampaignMain.cm.getPlanetFromPartialString(command.nextToken(), Username);
            removingHouse = server.campaign.CampaignMain.cm.getHouseFromPartialString(command.nextToken(), Username);

        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("Improper command. Try: /c adminremoveplanetownership#planet#faction",
                  Username,
                  true);
            return;
        }

        if (planet == null) {
            server.campaign.CampaignMain.cm.toUser("Could not find a matching planet.", Username, true);
            return;
        }

        if (removingHouse == null) {
            server.campaign.CampaignMain.cm.toUser("Could not find a matching faction to remove.", Username, true);
            return;
        }

        planet.getInfluence().removeHouse(removingHouse);
        planet.updated();

        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " removed " + removingHouse.getName() + " as an owner of " + planet.getName() + ".");
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
