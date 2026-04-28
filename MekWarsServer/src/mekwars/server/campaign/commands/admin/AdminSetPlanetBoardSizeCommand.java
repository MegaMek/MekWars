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

package mekwars.server.campaign.commands.admin;

public class AdminSetPlanetBoardSizeCommand implements server.campaign.commands.Command {
    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#X#Y";

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

        server.campaign.SPlanet planet = (server.campaign.SPlanet) server.campaign.CampaignMain.cm.getData()
                                                                         .getPlanetByName(command.nextToken());
        if (planet == null) {
            server.campaign.CampaignMain.cm.toUser("Unknown Planet", Username, true);
            return;
        }
        int x = Integer.parseInt(command.nextToken());
        int y = Integer.parseInt(command.nextToken());

        planet.setBoardSize(new java.awt.Dimension(x, y));
        planet.updated();

        server.campaign.CampaignMain.cm.toUser("Board size set for planet " + planet.getName(), Username, true);
        //server.MWLogger.modLog(Username + " set the board size for planet "+planet.getName());
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has set the board size for planet " + planet.getName());

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
