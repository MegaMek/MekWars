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

import common.Continent;
import common.util.MWLogger;


public class AdminCreateTerrainCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#TerrainType#AdvancedTerrain#Chance";

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

        try {
            server.campaign.SPlanet p = server.campaign.CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),
                  Username);
            String terraintype = command.nextToken();
            String advTerrainType = command.nextToken();
            int chance = Integer.parseInt(command.nextToken());

            if (p == null) {
                server.campaign.CampaignMain.cm.toUser("Planet not found:", Username, true);
                return;
            }

            Continent cont = new Continent(chance,
                  server.campaign.CampaignMain.cm.getData().getTerrainByName(terraintype),
                  server.campaign.CampaignMain.cm.getData().getAdvancedTerrainByName(advTerrainType));
            p.getEnvironments().add(cont);
            p.updated();

            //server.MWLogger.modLog(Username + " added terrain to " + p.getName() + " (" + terraintype + ").");
            server.campaign.CampaignMain.cm.toUser("Terrain added to " +
                                                         p.getName() +
                                                         "(" +
                                                         terraintype +
                                                         "-" +
                                                         advTerrainType +
                                                         ").", Username, true);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username +
                        " added terrain to planet " +
                        p.getName() +
                        "(" +
                        terraintype +
                        "-" +
                        advTerrainType +
                        ").");
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
