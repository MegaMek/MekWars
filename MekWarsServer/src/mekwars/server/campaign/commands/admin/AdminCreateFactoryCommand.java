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

/*
 * Created on 14.04.2004
 *
 */
package mekwars.server.campaign.commands.admin;

import common.UnitFactory;


/**
 * @author Helge Richter
 *
 */

public class AdminCreateFactoryCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "planet name#factory name#size#faction#type#subfolder#SubfactionAccessLevel";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

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
        server.campaign.SPlanet planet = server.campaign.CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),
              Username);
        String name = command.nextToken();
        String size = command.nextToken();
        String faction = command.nextToken();
        String buildTableFolder = "0";
        int accessLevel = 0;

        int type = Integer.parseInt(command.nextToken());

        if (command.hasMoreElements()) {buildTableFolder = command.nextToken();}
        if (command.hasMoreElements()) {accessLevel = Integer.parseInt(command.nextToken());}

        server.campaign.SUnitFactory fac = new server.campaign.SUnitFactory(name,
              planet,
              size,
              faction,
              0,
              100,
              type,
              buildTableFolder,
              accessLevel);

        fac.setID(java.util.UUID.randomUUID().toString());

        java.util.Vector<UnitFactory> uf = planet.getUnitFactories();
        uf.add(fac);
        fac.setPlanet(planet);
        if (planet.getOwner() != null) {
            planet.getOwner().removePlanet(planet);
            planet.getOwner().addPlanet(planet);
        }
        planet.updated();

        server.campaign.CampaignMain.cm.toUser("Factory created!", Username, true);

        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has created factory " + fac.getName() + " on planet " + planet.getName());

    }
}
