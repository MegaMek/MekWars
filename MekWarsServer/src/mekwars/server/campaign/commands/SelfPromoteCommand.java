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

public class SelfPromoteCommand implements Command {

    int accessLevel = 2;
    String syntax = "/selfpromote SubFactionName";

    public void process(java.util.StringTokenizer command, String Username) {
        if (!CampaignMain.campaignMain.getBooleanConfig("Self_Promote_Subfaction")) {
            CampaignMain.campaignMain.toUser("AM: Self Promotion is Disabled " + ".", Username, true);
            return;
        }

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
            //should make it so you can only use this once... i hope (unless you are a mod)
            if (userLevel > getExecutionLevel() && userLevel < 100) {
                CampaignMain.campaignMain.toUser("AM:Access level is too high for this command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        server.campaign.SPlayer user = CampaignMain.campaignMain.getPlayer(Username);
        String subFactionName;
        SubFaction subFaction = null;

        if (user.getSubFactionAccess() > 0) {
            CampaignMain.campaignMain.toUser("AM:You have already chosen a subfaction.", Username, true);
            return;
        }

        try {
            subFactionName = command.nextToken();
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("AM:Invalid Syntax: /selfpromote SubFactionName", Username);
            return;
        }

        subFaction = user.getMyHouse().getSubFactionList().get(subFactionName);

        if (subFaction == null) {
            CampaignMain.campaignMain.toUser("AM:That SubFaction does not exist for faction " +
                                                   user.getMyHouse().getName() +
                                                   ".", Username);
            return;
        }

        int minELO = Integer.parseInt(subFaction.getConfig("MinELO"));
        int minEXP = Integer.parseInt(subFaction.getConfig("MinExp"));


        if (user.getExperience() < minEXP || user.getRating() < minELO) {
            CampaignMain.campaignMain.toUser("AM:Sorry but " +
                                                   user.getName() +
                                                   " is not skilled enough to join that SubFaction.", Username);
            return;
        }

        user.setSubFaction(subFactionName);
        CampaignMain.campaignMain.toUser("PL|SSN|" + subFactionName, user.toString(), false);
        CampaignMain.campaignMain.doSendToAllOnlinePlayers("PI|FT|" + user.getName() + "|" + user.getFluffText(),
              false);
        CampaignMain.campaignMain.doSendToAllOnlinePlayers("PI|SSN|" + user.getName() + "|" + subFactionName,
              false);
        CampaignMain.campaignMain.toUser("HS|CA|0", user.getName(), false);// clear old data
        CampaignMain.campaignMain.toUser(user.getMyHouse().getCompleteStatus(), user.getName(), false);
        for (server.campaign.SArmy army : user.getArmies()) {
            CampaignMain.campaignMain.getOpsManager().checkOperations(army, true);
        }
        CampaignMain.campaignMain.toUser("AM:Congratulations you have been promoted to SubFaction " +
                                               subFactionName +
                                               ".", user.getName());
        CampaignMain.campaignMain.doSendHouseMail(user.getMyHouse(),
              "NOTE",
              user.getName() + " has been promoted to subfaction " + subFactionName + "!");

        CampaignMain.campaignMain.toUser("AM:You've promoted " +
                                               user.getName() +
                                               " to SubFaction " +
                                               subFactionName +
                                               ".", Username);

        CampaignMain.campaignMain.doSendModMail("NOTE",
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
