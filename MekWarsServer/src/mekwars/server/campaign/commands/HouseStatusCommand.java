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

package mekwars.server.campaign.commands;

import common.House;
import megamek.common.TechConstants;

public class HouseStatusCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }

        String result = "<h2>Faction Status: </h2>";
        java.util.Iterator<House> e = server.campaign.CampaignMain.cm.getData().getAllHouses().iterator();
        while (e.hasNext()) {
            server.campaign.SHouse h = (server.campaign.SHouse) e.next();
            if (h.getId() < 0) {continue;}
            result += "<FONT Color=\"" + h.getHouseColor() + "\">";
            result += h.getName() +
                            " Tech Level: " +
                            TechConstants.getLevelDisplayableName(h.getTechLevel()) +
                            " has " +
                            h.getPlanets().size();
            result += " Planets providing " +
                            h.getBaysProvided() +
                            " bays and " +
                            h.getSmallPlayers().size() +
                            " Members. The total economy value is: " +
                            h.getComponentProduction() +
                            "</font><br>";
        }
        result += "Note: Member numbers are based on members that have logged in since the last reboot of the server!";

        server.campaign.CampaignMain.cm.toUser("SM|" + result, Username, false);
    }
}
