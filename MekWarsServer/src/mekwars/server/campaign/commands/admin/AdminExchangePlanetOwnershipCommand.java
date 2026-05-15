/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

public class AdminExchangePlanetOwnershipCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "planet#winner#loser#amount";

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

        //vars
        server.campaign.SPlanet planet = null;
        server.campaign.SHouse winningHouse = null;
        server.campaign.SHouse losingHouse = null;
        int amount = 0;

        try {
            planet = CampaignMain.campaignMain.getPlanetFromPartialString(command.nextToken(), Username);
            winningHouse = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), Username);
            losingHouse = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), Username);
            amount = Integer.parseInt(command.nextToken());

        } catch (Exception e) {
            CampaignMain.campaignMain.toUser(
                  "Improper command. Try: /c adminexchangeplanetownership#planet#winner#loser#amount",
                  Username,
                  true);
            return;
        }

        if (planet == null) {
            CampaignMain.campaignMain.toUser("Could not find a matching planet.", Username, true);
            return;
        }

        if (winningHouse == null) {
            CampaignMain.campaignMain.toUser("Could not find a matching faction for the winner.", Username, true);
            return;
        }

        if (losingHouse == null) {
            CampaignMain.campaignMain.toUser("Could not find a matching faction for the loser.", Username, true);
            return;
        }

        if (amount <= 0) {
            CampaignMain.campaignMain.toUser("Get real try a number above 0!", Username, true);
            return;
        }

        //breaks passed
        int newAmount = planet.doGainInfluence(winningHouse, losingHouse, amount, true);

        //server.MWLogger.modLog(Username + " took " + newAmount + "% of "+ planet.getName() + " from " + losingHouse.getName() + " and gave it to " + winningHouse.getName() + ".");
        CampaignMain.campaignMain.toUser("You took " +
                                               newAmount +
                                               "% of " +
                                               planet.getName() +
                                               " from " +
                                               losingHouse.getName() +
                                               " and gave it to " +
                                               winningHouse.getName() +
                                               ".", Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username +
                    " took " +
                    newAmount +
                    "% of " +
                    planet.getName() +
                    " from " +
                    losingHouse.getName() +
                    " and gave it to " +
                    winningHouse.getName() +
                    ".");
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
