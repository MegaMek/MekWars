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

public class SetFactionToFactionRewardPointMultiplierCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#Faction Name#Multipler";

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

        server.campaign.SHouse faction1 = null;
        server.campaign.SHouse faction2 = null;
        double multiplier = 0.0;
        try {
            faction1 = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), Username);
            faction2 = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), Username);
            multiplier = Double.parseDouble(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("Improper command. Try: /c SetFactionToFactionRewardPointMultiplier#" +
                                                   syntax, Username, true);
            return;
        }

        if (faction1 == null || faction2 == null) {
            return;
        }

        String rewardMultiplier = faction1.getName() + "To" + faction2.getName() + "RewardPointMultiplier";

        CampaignMain.campaignMain.getConfig().setProperty(rewardMultiplier, Double.toString(multiplier));

        CampaignMain.campaignMain.toUser("You set the " +
                                               CampaignMain.campaignMain.getConfig("RPShortName") +
                                               " multipler for " +
                                               faction1.getName() +
                                               " to " +
                                               faction2.getName() +
                                               " to " +
                                               multiplier, Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username +
                    " has set " +
                    CampaignMain.campaignMain.getConfig("RPShortName") +
                    " multipler for " +
                    faction1.getName() +
                    " to " +
                    faction2.getName() +
                    " to " +
                    multiplier);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
