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

package mekwars.server.campaign.commands;

import common.House;


/**
 * Syntax  /ListSubFaction FactionName
 */

public class ListSubFactionCommand implements Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "";

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
        server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);

        try {
            if (command.hasMoreTokens() && server.campaign.CampaignMain.cm.getServer().isModerator(Username)) {
                factionName = command.nextToken();
            } else {factionName = player.getMyHouse().getName();}
        } catch (Exception ex) {
            factionName = player.getMyHouse().getName();
        }

        if (factionName.equalsIgnoreCase("all")) {

            for (House faction : server.campaign.CampaignMain.cm.getData().getAllHouses()) {
                StringBuffer result = new StringBuffer("SM|Subfaction list for faction ");
                result.append(faction.getName());
                for (String subFactionName : faction.getSubFactionList().keySet()) {
                    result.append("<BR>");
                    result.append(subFactionName);
                }

                server.campaign.CampaignMain.cm.toUser(result.toString(), Username, false);
                result.setLength(0);
            }
            return;
        }

        server.campaign.SHouse faction = server.campaign.CampaignMain.cm.getHouseFromPartialString(factionName,
              Username);

        if (faction == null) {return;}

        StringBuffer result = new StringBuffer("SM|Subfaction list for faction ");
        result.append(faction.getName());
        for (String subFactionName : faction.getSubFactionList().keySet()) {
            result.append("<BR>");
            result.append(subFactionName);
        }

        server.campaign.CampaignMain.cm.toUser(result.toString(), Username, false);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
