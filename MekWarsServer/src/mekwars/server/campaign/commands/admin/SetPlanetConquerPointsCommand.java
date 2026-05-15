/*
 * MekWars - Copyright (C) 2007
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


import mekwars.server.campaign.CampaignMain;

public class SetPlanetConquerPointsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#Amount";

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

        server.campaign.SPlanet p = null;
        int points = 0;
        try {
            p = CampaignMain.campaignMain.getPlanetFromPartialString(command.nextToken(), Username);
            points = Integer.parseInt(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("Improper command. Try: /c SetPlanetConquerPoints#planet#amount",
                  Username,
                  true);
            return;
        }

        if (p == null) {
            CampaignMain.campaignMain.toUser("Couldn't find a planet with that name.", Username, true);
            return;
        }

        p.setConquestPoints(points);
        p.updated();

        CampaignMain.campaignMain.toUser("You set " + p.getName() + "'s conquer points to " + points,
              Username,
              true);
        CampaignMain.campaignMain.doSendModMail("PLANETARY CHANGE",
              Username + " has changed " + p.getName() + "'s conquer points to " + points);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
