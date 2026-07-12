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
 * @author Torren (Jason Tighe) Created on 11.08.2005 Allows SO's to set a planets component production base on
 *       the fly.
 */
package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;

public class SetPlanetCompProductionCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#Number Of Components";

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
        int compProduction = 0;
        server.campaign.SPlanet planet = CampaignMain.campaignMain.getPlanetFromPartialString(PlanetName,
              Username);

        if (planet == null) {
            CampaignMain.campaignMain.toUser(PlanetName + " not found.", Username, true);
            return;
        }

        try {
            if (command.hasMoreTokens()) {compProduction = Integer.parseInt(command.nextToken());}
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser(
                  "Invalid Syntax: SetPlanetCompProduction#PlanetName#NumberOfComponents",
                  Username,
                  true);
            return;
        }

        if (planet.getOwner() != null) {
            planet.getOwner()
                  .setComponentProduction(planet.getOwner().getComponentProduction() - planet.getCompProduction());
            planet.getOwner().setComponentProduction(planet.getOwner().getComponentProduction() + compProduction);
        }

        planet.setCompProduction(compProduction);

        CampaignMain.campaignMain.toUser(planet.getName() +
                                               " has had its component production set to " +
                                               planet.getCompProduction(), Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " has set planet " + PlanetName + "'s component production to " + planet.getCompProduction());
        planet.updated();
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
