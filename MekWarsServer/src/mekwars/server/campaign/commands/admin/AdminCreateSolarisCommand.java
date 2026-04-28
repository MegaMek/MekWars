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

import common.Influences;

public class AdminCreateSolarisCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

    public String getSyntax() {
        return syntax;
    }

    public void process(java.util.StringTokenizer command, String Username) {

        // access level check
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        // Add the Newbie-SHouse
        server.campaign.SHouse solaris = new server.campaign.NewbieHouse(server.campaign.CampaignMain.cm.getData()
                                                                               .getUnusedHouseID(),
              server.campaign.CampaignMain.cm.getConfig("NewbieHouseName"),
              "#33CCCC",
              4,
              5,
              "SOL");

        server.campaign.CampaignMain.cm.addHouse(solaris);
        java.util.HashMap<Integer, Integer> solFlu = new java.util.HashMap<Integer, Integer>();
        solFlu.put(server.campaign.CampaignMain.cm.getHouseFromPartialString(server.campaign.CampaignMain.cm.getConfig(
              "NewbieHouseName"), null).getId(), 100);
        server.campaign.SPlanet newbieP = new server.campaign.SPlanet(0,
              "Solaris VII",
              new Influences(solFlu),
              0,
              0,
              -3,
              -2);
        server.campaign.CampaignMain.cm.addPlanet(newbieP);

        solaris.addPlanet(newbieP);
        server.campaign.CampaignMain.cm.toUser(server.campaign.CampaignMain.cm.getConfig("NewbieHouseName"),
              Username,
              true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has created " + server.campaign.CampaignMain.cm.getConfig("NewbieHouseName"));

    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}
