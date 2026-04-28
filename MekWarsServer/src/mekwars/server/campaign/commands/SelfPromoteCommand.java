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

public class SelfPromoteCommand implements Command {

    int accessLevel = 2;
    String syntax = "/selfpromote SubFactionName";

    public void process(java.util.StringTokenizer command, String Username) {
        if (!server.campaign.CampaignMain.cm.getBooleanConfig("Self_Promote_Subfaction")) {
            server.campaign.CampaignMain.cm.toUser("AM: Self Promotion is Disabled " + ".", Username, true);
            return;
        }

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
            //should make it so you can only use this once... i hope (unless you are a mod)
            if (userLevel > getExecutionLevel() && userLevel < 100) {
                server.campaign.CampaignMain.cm.toUser("AM:Access level is too high for this command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }

        server.campaign.SPlayer user = server.campaign.CampaignMain.cm.getPlayer(Username);
        String subFactionName;
        SubFaction subFaction = null;

        if (user.getSubFactionAccess() > 0) {
            server.campaign.CampaignMain.cm.toUser("AM:You have already chosen a subfaction.", Username, true);
            return;
        }

        try {
            subFactionName = command.nextToken();
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("AM:Invalid Syntax: /selfpromote SubFactionName", Username);
            return;
        }

        subFaction = user.getMyHouse().getSubFactionList().get(subFactionName);

        if (subFaction == null) {
            server.campaign.CampaignMain.cm.toUser("AM:That SubFaction does not exist for faction " +
                                                         user.getMyHouse().getName() +
                                                         ".", Username);
            return;
        }

        int minELO = Integer.parseInt(subFaction.getConfig("MinELO"));
        int minEXP = Integer.parseInt(subFaction.getConfig("MinExp"));


        if (user.getExperience() < minEXP || user.getRating() < minELO) {
            server.campaign.CampaignMain.cm.toUser("AM:Sorry but " +
                                                         user.getName() +
                                                         " is not skilled enough to join that SubFaction.", Username);
            return;
        }

        user.setSubFaction(subFactionName);
        server.campaign.CampaignMain.cm.toUser("PL|SSN|" + subFactionName, user.toString(), false);
        server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers("PI|FT|" + user.getName() + "|" + user.getFluffText(),
              false);
        server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers("PI|SSN|" + user.getName() + "|" + subFactionName,
              false);
        server.campaign.CampaignMain.cm.toUser("HS|CA|0", user.getName(), false);// clear old data
        server.campaign.CampaignMain.cm.toUser(user.getMyHouse().getCompleteStatus(), user.getName(), false);
        for (server.campaign.SArmy army : user.getArmies()) {
            server.campaign.CampaignMain.cm.getOpsManager().checkOperations(army, true);
        }
        server.campaign.CampaignMain.cm.toUser("AM:Congratulations you have been promoted to SubFaction " +
                                                     subFactionName +
                                                     ".", user.getName());
        server.campaign.CampaignMain.cm.doSendHouseMail(user.getMyHouse(),
              "NOTE",
              user.getName() + " has been promoted to subfaction " + subFactionName + "!");

        server.campaign.CampaignMain.cm.toUser("AM:You've promoted " +
                                                     user.getName() +
                                                     " to SubFaction " +
                                                     subFactionName +
                                                     ".", Username);

        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " promoted " + user.getName() + " to SubFaction " + subFactionName + ".");

    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }
}// end RequestSubFactionPromotionCommand class
