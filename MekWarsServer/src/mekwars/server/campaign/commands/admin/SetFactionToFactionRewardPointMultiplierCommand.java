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


public class SetFactionToFactionRewardPointMultiplierCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#Faction Name#Multipler";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
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
            faction1 = server.campaign.CampaignMain.cm.getHouseFromPartialString(command.nextToken(), Username);
            faction2 = server.campaign.CampaignMain.cm.getHouseFromPartialString(command.nextToken(), Username);
            multiplier = Double.parseDouble(command.nextToken());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("Improper command. Try: /c SetFactionToFactionRewardPointMultiplier#" +
                                                         syntax, Username, true);
            return;
        }

        if (faction1 == null || faction2 == null) {
            return;
        }

        String rewardMultiplier = faction1.getName() + "To" + faction2.getName() + "RewardPointMultiplier";

        server.campaign.CampaignMain.cm.getConfig().setProperty(rewardMultiplier, Double.toString(multiplier));

        server.campaign.CampaignMain.cm.toUser("You set the " +
                                                     server.campaign.CampaignMain.cm.getConfig("RPShortName") +
                                                     " multipler for " +
                                                     faction1.getName() +
                                                     " to " +
                                                     faction2.getName() +
                                                     " to " +
                                                     multiplier, Username, true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username +
                    " has set " +
                    server.campaign.CampaignMain.cm.getConfig("RPShortName") +
                    " multipler for " +
                    faction1.getName() +
                    " to " +
                    faction2.getName() +
                    " to " +
                    multiplier);

    }
}
