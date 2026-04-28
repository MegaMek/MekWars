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

public class CreateMercFactionCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#htmlhexcolor#abbreviation";

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

        //vars
        String name = "";
        String color = "";
        String abb = "";

        try {
            name = command.nextToken();
            color = "#" + command.nextToken();
            abb = command.nextToken();
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser(
                  "Improper command. Try: /c creatmercfactioon#name#htmlhexcolor#abbreviation",
                  Username,
                  true);
            return;
        }

        //hope the strings are right and make the faction
        server.campaign.mercenaries.MercHouse m = new server.campaign.mercenaries.MercHouse(
              server.campaign.CampaignMain.cm.getData().getUnusedHouseID(), name, color, 4, 5, abb);
        server.campaign.CampaignMain.cm.addHouse(m);
        server.campaign.CampaignMain.cm.toUser("You created a new mercenary faction [" + name + "]", Username, true);

        //trigger a data refresh for logging in players.
        m.updated();

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
