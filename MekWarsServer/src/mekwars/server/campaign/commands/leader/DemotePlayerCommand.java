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

public class DemotePlayerCommand implements server.campaign.commands.Command {

    int accessLevel = server.campaign.CampaignMain.cm.getIntegerConfig("factionLeaderLevel");
    String syntax = "";

    public String getSyntax() {
        return syntax;
    }

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

        // person doing the demoting
        server.campaign.SPlayer leader = server.campaign.CampaignMain.cm.getPlayer(Username);
        // Person being demoted
        server.campaign.SPlayer grunt = null;
        String subFactionName;
        SubFaction subFaction = null;

        try {
            grunt = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());
            subFactionName = command.nextToken();
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("AM:Invalid Syntax: /demoteplayer Player#NewSubFactionName[none]",
                  Username);
            return;
        }

        if (grunt == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Unknown Player", Username);
            return;
        }

        if (!grunt.getMyHouse().getName().equalsIgnoreCase(leader.getMyHouse().getName()) &&
                  !server.campaign.CampaignMain.cm.getServer().isModerator(Username)) {
            server.campaign.CampaignMain.cm.toUser("AM:You can only demote players that within your same faction!",
                  Username);
            return;
        }

        if (subFactionName.equalsIgnoreCase("none")) {subFaction = new SubFaction();} else {
            subFaction = grunt.getMyHouse().getSubFactionList().get(subFactionName);
        }

        if (subFaction == null) {
            server.campaign.CampaignMain.cm.toUser("AM:That SubFaction does not exist for faction " +
                                                         grunt.getMyHouse().getName() +
                                                         ".", Username);
            return;
        }

        int minELO = Integer.parseInt(subFaction.getConfig("MinELO"));
        int minEXP = Integer.parseInt(subFaction.getConfig("MinExp"));

        if (grunt.getSubFactionAccess() < Integer.parseInt(subFaction.getConfig("AccessLevel"))) {
            server.campaign.CampaignMain.cm.toUser("AM:You cannot demote " +
                                                         grunt.getName() +
                                                         " to a subfaction with a higher access level", Username);
            return;
        }

        if (grunt.getExperience() < minEXP || grunt.getRating() < minELO) {
            server.campaign.CampaignMain.cm.toUser("AM:Sorry but " +
                                                         grunt.getName() +
                                                         " is not skilled enough to join that SubFaction.", Username);
            return;
        }

        grunt.setSubFaction(subFactionName);
        server.campaign.CampaignMain.cm.toUser("PL|SSN|" + subFactionName, grunt.getName(), false);
        server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers("PI|FT|" +
                                                                       grunt.getName() +
                                                                       "|" +
                                                                       grunt.getFluffText(), false);
        server.campaign.CampaignMain.cm.toUser("HS|CA|0", grunt.getName(), false);// clear old data
        server.campaign.CampaignMain.cm.toUser(grunt.getMyHouse().getCompleteStatus(), grunt.getName(), false);
        for (server.campaign.SArmy army : grunt.getArmies()) {
            server.campaign.CampaignMain.cm.getOpsManager().checkOperations(army, true);
        }

        server.campaign.CampaignMain.cm.toUser("AM:You have been demoted to SubFaction " + subFactionName + ".",
              grunt.getName());
        server.campaign.CampaignMain.cm.doSendHouseMail(grunt.getMyHouse(),
              "NOTE",
              grunt.getName() + " has been demoted to subfaction " + subFactionName + " by " + leader.getName() + "!");
        server.campaign.CampaignMain.cm.toUser("AM:You demoted " +
                                                     grunt.getName() +
                                                     " to SubFaction " +
                                                     subFactionName +
                                                     ".", Username);

        if (server.campaign.CampaignMain.cm.getServer().isModerator(Username)) {
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
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
