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

import common.util.Position;
import mekwars.server.campaign.CampaignMain;

public class AdminMovePlanetCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#X Coord#Y Coord";

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

        server.campaign.SPlanet p = CampaignMain.campaignMain.getPlanetFromPartialString(command.nextToken(),
              Username);

        if (p == null) {return;}
        double x = 0;
        double y = 0;

        try {
            x = Double.parseDouble(command.nextToken());
            y = Double.parseDouble(command.nextToken());
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("Invalid Syntax: adminmoveplanet#name#x#y", Username);
            return;
        }

        p.setPosition(new Position(x, y));
        p.updated();

        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " has moved planet " + p.getName() + " to " + x + "," + y);
        CampaignMain.campaignMain.toUser("Planet Moved", Username);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
