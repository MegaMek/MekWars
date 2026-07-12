/*
 * MekWars - Copyright (C) 2007
 *
 * Original author - jtighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package mekwars.server.campaign.commands;

import common.SubFaction;
import mekwars.server.campaign.CampaignMain;

public class RequestSubFactionPromotionCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

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

        server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
        server.campaign.SHouse faction = player.getMyHouse();
        String subFactionName;
        SubFaction subFaction = null;

        try {
            subFactionName = command.nextToken();
            subFaction = faction.getSubFactionList().get(subFactionName);
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("AM:Invalid Syntax: /RequestSubFactionPromotion subFactionname",
                  Username);
            return;
        }

        if (subFaction == null) {
            CampaignMain.campaignMain.toUser("AM:That SubFaction does not exist for faction " +
                                                   faction.getName() +
                                                   ".", Username);
            return;
        }

        int minELO = Integer.parseInt(subFaction.getConfig("MinELO"));
        int minEXP = Integer.parseInt(subFaction.getConfig("MinExp"));

        if (player.getExperience() < minEXP || player.getRating() < minELO) {
            CampaignMain.campaignMain.toUser("AM:Sorry but you are not skilled enough to join that SubFaction.",
                  Username);
            return;
        }

        if (player.getSubFactionAccess() > Integer.parseInt(subFaction.getConfig("AccessLevel"))) {
            CampaignMain.campaignMain.toUser("AM:Sorry but you cannot demote yourself", Username);
            return;
        }

        if (!player.canBePromoted()) {
            CampaignMain.campaignMain.toUser("AM:Sorry but you've already been premoted once within a " +
                                                   player.getMyHouse().getIntegerConfig("daysbetweenpromotions") +
                                                   " day period.", Username);
            return;
        }

        if (CampaignMain.campaignMain.getBooleanConfig("autoPromoteSubFaction")) {
            player.setSubFaction(subFactionName);
            CampaignMain.campaignMain.toUser("PL|SSN|" + subFactionName, Username, false);
            CampaignMain.campaignMain.toUser("HS|CA|0", player.getName(), false);//clear old data
            CampaignMain.campaignMain.toUser(player.getMyHouse().getCompleteStatus(), player.getName(), false);

            CampaignMain.campaignMain.toUser("AM:Congratulations you have been promoted to SubFaction " +
                                                   subFactionName +
                                                   ".", Username);
            CampaignMain.campaignMain.doSendHouseMail(player.getMyHouse(),
                  "NOTE",
                  player.getName() + " has been promoted to subfaction " + subFactionName + "!");
        } else {
            CampaignMain.campaignMain.toUser(
                  "AM:Your request for a promotion has been noted. Someone will be in touch.",
                  Username);
            String msg = Username +
                               " has requested for a promtion to subfaction " +
                               subFactionName +
                               " <a href=\"MEKWARS/c promoteplayer#" +
                               Username +
                               "#" +
                               subFactionName +
                               "\">Click here to promote.</a>";
            faction.sendMessageToHouseLeaders(msg);
            CampaignMain.campaignMain.doSendModMail("NOTE", msg);

        }

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}//end RequestSubFactionPromotionCommand class
