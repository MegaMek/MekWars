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
 * Created on 10.26.2005
 *
 */
package mekwars.server.campaign.commands.admin;

public class AdminSetHomeWorldCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#[true/false]";

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

        boolean homeworld = false;
        server.campaign.SPlanet planet = null;

        try {
            planet = server.campaign.CampaignMain.cm.getPlanetFromPartialString(command.nextToken(), Username);
            if (command.hasMoreTokens()) {homeworld = Boolean.parseBoolean(command.nextToken());} else {
                homeworld = !planet.isHomeWorld();
            }
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("Invalid syntax. Try: adminsethomeworld#Planet#[true/false]",
                  Username);
        }

        if (planet == null) {return;}

        planet.setHomeWorld(homeworld);

        server.campaign.CampaignMain.cm.toUser(planet.getName() + "'s homeworld status set to: " + homeworld + ".",
              Username,
              true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has set " + planet.getName() + "'s homeworld status to: " + homeworld + ".");
        planet.updated();
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
