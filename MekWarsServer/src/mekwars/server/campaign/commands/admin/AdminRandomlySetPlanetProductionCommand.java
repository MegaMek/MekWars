/*
 * MekWars - Copyright (C) 2008
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

import common.House;
import common.Planet;
import mekwars.server.campaign.CampaignMain;

public class AdminRandomlySetPlanetProductionCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "min#max[set to 0 to reset all planets to 0]";

    public String getSyntax() {
        return syntax;
    }

    public void process(java.util.StringTokenizer command, String Username) {

        // access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        int min = Integer.parseInt(command.nextToken());
        int max = Integer.parseInt(command.nextToken());

        if (max == 0) {min = 0;}
        // look for confirmation
        if (!command.hasMoreTokens() || !command.nextToken().equalsIgnoreCase("confirm")) {
            CampaignMain.campaignMain.toUser(
                  "Do you want to change all of the planets production? If so, [<a href=\"MEKWARS/c AdminRandomlySetPlanetProduction#" +
                        min +
                        "#" +
                        max +
                        "#confirm\">click to confirm.</a>",
                  Username,
                  true);
            return;
        }

        // check for double confirmation
        if (!command.hasMoreTokens() || !command.nextToken().equalsIgnoreCase("confirm")) {
            CampaignMain.campaignMain.toUser(
                  "Are you *ABSOLUTELY SURE* you want to change all of the planets production? This cannot be easily reversed. If so, [<a href=\"MEKWARS/c AdminRandomlySetPlanetProduction#" +
                        min +
                        "#" +
                        max +
                        "#confirm#confirm\">click to re-confirm.</a>",
                  Username,
                  true);
            return;
        }

        // doubly confirmed. loop through every planet and restore it to the
        // original owner
        for (Planet currP : CampaignMain.campaignMain.getData().getAllPlanets()) {

            // cast to planet
            server.campaign.SPlanet p = (server.campaign.SPlanet) currP;

            if (p.getCompProduction() > 0 && max > 0) {continue;}

            int production = CampaignMain.campaignMain.getRandomNumber(max);

            production = Math.max(production, min);
            // change production
            p.setCompProduction(production);

            // set updated flag so players' maps refresh
            p.updated();
        }

        if (max == 0) {
            for (House currH : CampaignMain.campaignMain.getData().getAllHouses()) {
                server.campaign.SHouse h = (server.campaign.SHouse) currH;
                h.setComponentProduction(0);
                int productionAmount = 0;
                for (server.campaign.SPlanet planet : h.getPlanets().values()) {
                    productionAmount = planet.getCompProduction();
                }
                h.setComponentProduction(productionAmount);
            }
        }

        CampaignMain.campaignMain.toUser("You have set production for all of the planets.", Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " has set production for all of the planets.");

    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}
