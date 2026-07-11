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
import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;


public class AdminCreateTerrainCommand implements server.campaign.commands.Command {
    private static final MMLogger LOGGER = MMLogger.create(AdminCreateTerrainCommand.class);

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#TerrainType#AdvancedTerrain#Chance";

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

        try {
            server.campaign.SPlanet p = CampaignMain.campaignMain.getPlanetFromPartialString(command.nextToken(),
                  Username);
            String terraintype = command.nextToken();
            String advTerrainType = command.nextToken();
            int chance = Integer.parseInt(command.nextToken());

            if (p == null) {
                CampaignMain.campaignMain.toUser("Planet not found:", Username, true);
                return;
            }

            Continent cont = new Continent(chance,
                  CampaignMain.campaignMain.getData().getTerrainByName(terraintype),
                  CampaignMain.campaignMain.getData().getAdvancedTerrainByName(advTerrainType));
            p.getEnvironments().add(cont);
            p.updated();

            //server.MWLogger.modLog(Username + " added terrain to " + p.getName() + " (" + terraintype + ").");
            CampaignMain.campaignMain.toUser("Terrain added to " +
                                                   p.getName() +
                                                   "(" +
                                                   terraintype +
                                                   "-" +
                                                   advTerrainType +
                                                   ").", Username, true);
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  Username +
                        " added terrain to planet " +
                        p.getName() +
                        "(" +
                        terraintype +
                        "-" +
                        advTerrainType +
                        ").");
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
