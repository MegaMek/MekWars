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

import common.Planet;

public class AutoPlanetStatusCommand implements Command {

    //conforming methods
    public int getExecutionLevel() {return 0;}

    public void setExecutionLevel(int i) {}

    String syntax = "";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //Send all SPlanet Info to the user
        java.util.Iterator<Planet> e = server.campaign.CampaignMain.cm.getData().getAllPlanets().iterator();
        String result = "PL|";
        while (e.hasNext()) {
            server.campaign.SPlanet p = (server.campaign.SPlanet) e.next();
            result += p.toString();
            result += "|";
        }
        server.campaign.CampaignMain.cm.toUser(result, Username, false);

    }
}
