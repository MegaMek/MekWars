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

//import java.util.Hashtable;

import common.AdvancedTerrain;
import common.Continent;
import common.PlanetEnvironments;
import mekwars.server.campaign.CampaignMain;

//import common.Terrain;
//import common.PlanetEnvironment;

public class SetAdvancedPlanetTerrainCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name$Terrain ID$AdvTerrain ID";

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

        server.campaign.SPlanet planet = (server.campaign.SPlanet) CampaignMain.campaignMain.getData()
                                                                         .getPlanetByName(command.nextToken());
        if (planet == null) {
            CampaignMain.campaignMain.toUser("Unknown Planet", Username, true);
            return;
        }

        int id = Integer.parseInt(command.nextToken());
        int aid = Integer.parseInt(command.nextToken());
        //		int conid = 0;

        AdvancedTerrain AT = CampaignMain.campaignMain.getData().getAdvancedTerrain(aid);
        PlanetEnvironments originalPe = planet.getEnvironments();
        PlanetEnvironments changedPe = new PlanetEnvironments();
        Continent[] Cont = originalPe.toArray();

        for (int x = 0; x < originalPe.size(); x++) {
            if (Cont[x].getEnvironment().getId() == id) {
                changedPe.add(new Continent(Cont[x].getSize(), Cont[x].getEnvironment(), AT));
            } else {
                changedPe.add(Cont[x]);
            }
        }

        planet.setEnvironments(changedPe);
        planet.updated();

        CampaignMain.campaignMain.toUser("Advanced Terrain set for terrain: " +
                                               CampaignMain.campaignMain.getData()
                                                     .getTerrain(id)
                                                     .getName() +
                                               "(" +
                                               AT.getName() +
                                               ") on planet " +
                                               planet.getName(), Username, true);
        //server.MWLogger.modLog(Username + " set Advanced Terrain for terrain: "+aTerrain.getDisplayName()+" on planet "+planet.getName());
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username +
                    " has set Advanced Terrain for terrain: " +
                    CampaignMain.campaignMain.getData().getTerrain(id).getName() +
                    "(" +
                    AT.getName() +
                    ") on planet " +
                    planet.getName());

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
