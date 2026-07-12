/*
 * MekWars - Copyright (C) 2007
 *
 * Original author - Jason Tighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package mekwars.server.campaign.commands.mod;

import common.SubFaction;
import mekwars.server.campaign.CampaignMain;


/**
 * Syntax  /CreateSubFaction SubFactionName#SubFactionAccessLevel#FactionName
 */

public class CreateSubFactionCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "SubFaction Name#SubFaction AccessLevel#Faction Name";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        String factionName = "";
        String subFactionName = "";
        int access = 0;
        server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);

        try {
            subFactionName = command.nextToken();
            access = Integer.parseInt(command.nextToken());
            if (command.hasMoreTokens() && CampaignMain.campaignMain.getServer().isModerator(Username)) {
                factionName = command.nextToken();
            } else {factionName = player.getMyHouse().getName();}
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser(
                  "AM:Invalid syntax: /CreateSubFaction SubFactionName#SubFactionAccessLevel#[FactionName]",
                  Username);
            return;
        }

        server.campaign.SHouse faction = CampaignMain.campaignMain.getHouseFromPartialString(factionName,
              Username);

        if (faction == null) {return;}

        if (faction.getSubFactionList().containsKey(subFactionName)) {return;}

        SubFaction subFaction = new SubFaction(subFactionName, Integer.toString(access));

        faction.getSubFactionList().put(subFactionName, subFaction);

        faction.updated();

        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " has created subfaction " + subFactionName + " for faction " + faction.getName());
        CampaignMain.campaignMain.toUser("AM:You have created subfaction " +
                                               subFactionName +
                                               " for faction " +
                                               faction.getName(), Username);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
