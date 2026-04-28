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


/**
 * Syntax  /CreateSubFaction SubFactionName#SubFactionAccessLevel#FactionName
 */

public class CreateSubFactionCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "SubFaction Name#SubFaction AccessLevel#Faction Name";

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

        String factionName = "";
        String subFactionName = "";
        int access = 0;
        server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);

        try {
            subFactionName = command.nextToken();
            access = Integer.parseInt(command.nextToken());
            if (command.hasMoreTokens() && server.campaign.CampaignMain.cm.getServer().isModerator(Username)) {
                factionName = command.nextToken();
            } else {factionName = player.getMyHouse().getName();}
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Invalid syntax: /CreateSubFaction SubFactionName#SubFactionAccessLevel#[FactionName]",
                  Username);
            return;
        }

        server.campaign.SHouse faction = server.campaign.CampaignMain.cm.getHouseFromPartialString(factionName,
              Username);

        if (faction == null) {return;}

        if (faction.getSubFactionList().containsKey(subFactionName)) {return;}

        SubFaction subFaction = new SubFaction(subFactionName, Integer.toString(access));

        faction.getSubFactionList().put(subFactionName, subFaction);

        faction.updated();

        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has created subfaction " + subFactionName + " for faction " + faction.getName());
        server.campaign.CampaignMain.cm.toUser("AM:You have created subfaction " +
                                                     subFactionName +
                                                     " for faction " +
                                                     faction.getName(), Username);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
