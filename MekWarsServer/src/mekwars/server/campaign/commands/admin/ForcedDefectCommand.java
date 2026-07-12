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


import mekwars.server.campaign.CampaignMain;

public class ForcedDefectCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Player Name#Faction Name";

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

        //vars
        server.campaign.SPlayer p = null;
        server.campaign.SHouse h = null;

        //see if the player is online
        boolean playerOnline = false;

        try {

            String name = command.nextToken();
            playerOnline = CampaignMain.campaignMain.isLoggedIn(name);

            p = CampaignMain.campaignMain.getPlayer(name);
            h = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), null);

        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper command. Try: /c forceddefect#player#faction",
                  Username,
                  true);
            return;
        }

        if (p == null) {
            CampaignMain.campaignMain.toUser("AM:Couldn't find a player with that name.", Username, true);
            return;
        }

        if (h == null) {
            CampaignMain.campaignMain.toUser("AM:Couldn't find a faction with that name.", Username, true);
            return;
        }

        //make the move
        String clientVersion = p.getPlayerClientVersion();
        p.getMyHouse().removeLeader(p.getName());
        p.getMyHouse().removePlayer(p, false);
        p.setMyHouse(h);
        p.setSubFaction(h.getZeroLevelSubFaction());

        //log the player into his new faction
        if (playerOnline) {
            CampaignMain.campaignMain.getPlayer(p.getName());
            CampaignMain.campaignMain.doLoginPlayer(p.getName());
        }

        //send appropraite messages
        CampaignMain.campaignMain.toUser("AM:" + Username + " forced you to defect to " + h.getName(),
              p.getName(),
              true);
        CampaignMain.campaignMain.toUser("AM:You forced " + p.getName() + " to defect to " + h.getName(),
              Username,
              true);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " forced " + p.getName() + " to defect to " + h.getName());
        p.setPlayerClientVersion(clientVersion);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
