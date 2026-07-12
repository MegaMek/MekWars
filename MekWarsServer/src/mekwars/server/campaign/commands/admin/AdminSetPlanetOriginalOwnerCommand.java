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

/*
 * Created on 10.27.2005
 *
 */
package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;

public class AdminSetPlanetOriginalOwnerCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#Faction Name";

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
        String originalOwner = CampaignMain.campaignMain.getConfig("NewbieHouseName");
        server.campaign.SPlanet planet = CampaignMain.campaignMain.getPlanetFromPartialString(PlanetName,
              Username);

        if (planet == null) {return;}

        try {
            if (command.hasMoreTokens()) {originalOwner = command.nextToken();}
        } catch (Exception ex) {}


        planet.setOriginalOwner(originalOwner);

        CampaignMain.campaignMain.toUser(planet.getName() + "'s original owner set to: " + originalOwner + ".",
              Username,
              true);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " changed " + PlanetName + " 's original owner to: " + originalOwner + ".");
        planet.updated();
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
