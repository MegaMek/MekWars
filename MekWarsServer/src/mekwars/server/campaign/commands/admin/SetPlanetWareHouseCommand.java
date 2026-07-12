/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - Torren (torren@users.sourceforge.net)
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
 * @author Torren (Jason Tighe) Created on 11.08.2005 Allows the SO to change the number of bays/techs a planet
 *       offers the owning faction on the fly.
 */

package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;

public class SetPlanetWareHouseCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#Number Of Warehouses";

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

        String PlanetName = command.nextToken();
        int warehouses = 0;
        server.campaign.SPlanet planet = CampaignMain.campaignMain.getPlanetFromPartialString(PlanetName,
              Username);

        if (planet == null) {
            CampaignMain.campaignMain.toUser(PlanetName + " not found.", Username, true);
            return;
        }

        try {
            if (command.hasMoreTokens()) {warehouses = Integer.parseInt(command.nextToken());}
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("Invalid Syntax: SetPlanetWareHouse#PlanetName#NumberOfWareHouses",
                  Username,
                  true);
            return;
        }


        planet.setBaysProvided(warehouses);

        if (planet.getOwner() != null) {
            server.campaign.SHouse house = planet.getOwner();
            house.removePlanet(planet);
            house.addPlanet(planet);
            house.updated();
        }

        CampaignMain.campaignMain.toUser(planet.getName() +
                                               " has had its number of warehouses set to " +
                                               planet.getBaysProvided(), Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " has set planet " + PlanetName + " warehouses to " + planet.getBaysProvided());
        planet.updated();
    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}//end class
