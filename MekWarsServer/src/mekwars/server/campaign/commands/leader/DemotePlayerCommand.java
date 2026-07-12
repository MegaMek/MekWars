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

package mekwars.server.campaign.commands.leader;

import common.SubFaction;
import mekwars.server.campaign.CampaignMain;

public class DemotePlayerCommand implements server.campaign.commands.Command {

    int accessLevel = CampaignMain.campaignMain.getIntegerConfig("factionLeaderLevel");
    String syntax = "";

    public String getSyntax() {
        return syntax;
    }

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

        // person doing the demoting
        server.campaign.SPlayer leader = CampaignMain.campaignMain.getPlayer(Username);
        // Person being demoted
        server.campaign.SPlayer grunt = null;
        String subFactionName;
        SubFaction subFaction = null;

        try {
            grunt = CampaignMain.campaignMain.getPlayer(command.nextToken());
            subFactionName = command.nextToken();
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("AM:Invalid Syntax: /demoteplayer Player#NewSubFactionName[none]",
                  Username);
            return;
        }

        if (grunt == null) {
            CampaignMain.campaignMain.toUser("AM:Unknown Player", Username);
            return;
        }

        if (!grunt.getMyHouse().getName().equalsIgnoreCase(leader.getMyHouse().getName()) &&
                  !CampaignMain.campaignMain.getServer().isModerator(Username)) {
            CampaignMain.campaignMain.toUser("AM:You can only demote players that within your same faction!",
                  Username);
            return;
        }

        if (subFactionName.equalsIgnoreCase("none")) {subFaction = new SubFaction();} else {
            subFaction = grunt.getMyHouse().getSubFactionList().get(subFactionName);
        }

        if (subFaction == null) {
            CampaignMain.campaignMain.toUser("AM:That SubFaction does not exist for faction " +
                                                   grunt.getMyHouse().getName() +
                                                   ".", Username);
            return;
        }

        int minELO = Integer.parseInt(subFaction.getConfig("MinELO"));
        int minEXP = Integer.parseInt(subFaction.getConfig("MinExp"));

        if (grunt.getSubFactionAccess() < Integer.parseInt(subFaction.getConfig("AccessLevel"))) {
            CampaignMain.campaignMain.toUser("AM:You cannot demote " +
                                                   grunt.getName() +
                                                   " to a subfaction with a higher access level", Username);
            return;
        }

        if (grunt.getExperience() < minEXP || grunt.getRating() < minELO) {
            CampaignMain.campaignMain.toUser("AM:Sorry but " +
                                                   grunt.getName() +
                                                   " is not skilled enough to join that SubFaction.", Username);
            return;
        }

        grunt.setSubFaction(subFactionName);
        CampaignMain.campaignMain.toUser("PL|SSN|" + subFactionName, grunt.getName(), false);
        CampaignMain.campaignMain.doSendToAllOnlinePlayers("PI|FT|" +
                                                                 grunt.getName() +
                                                                 "|" +
                                                                 grunt.getFluffText(), false);
        CampaignMain.campaignMain.toUser("HS|CA|0", grunt.getName(), false);// clear old data
        CampaignMain.campaignMain.toUser(grunt.getMyHouse().getCompleteStatus(), grunt.getName(), false);
        for (server.campaign.SArmy army : grunt.getArmies()) {
            CampaignMain.campaignMain.getOpsManager().checkOperations(army, true);
        }

        CampaignMain.campaignMain.toUser("AM:You have been demoted to SubFaction " + subFactionName + ".",
              grunt.getName());
        CampaignMain.campaignMain.doSendHouseMail(grunt.getMyHouse(),
              "NOTE",
              grunt.getName() + " has been demoted to subfaction " + subFactionName + " by " + leader.getName() + "!");
        CampaignMain.campaignMain.toUser("AM:You demoted " +
                                               grunt.getName() +
                                               " to SubFaction " +
                                               subFactionName +
                                               ".", Username);

        if (CampaignMain.campaignMain.getServer().isModerator(Username)) {
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  Username + " demoted " + grunt.getName() + " to SubFaction " + subFactionName + ".");
        }
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}// end RequestSubFactionPromotionCommand class
