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

public class AdminExchangePlanetOwnershipCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "planet#winner#loser#amount";

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

        //vars
        server.campaign.SPlanet planet = null;
        server.campaign.SHouse winningHouse = null;
        server.campaign.SHouse losingHouse = null;
        int amount = 0;

        try {
            planet = server.campaign.CampaignMain.cm.getPlanetFromPartialString(command.nextToken(), Username);
            winningHouse = server.campaign.CampaignMain.cm.getHouseFromPartialString(command.nextToken(), Username);
            losingHouse = server.campaign.CampaignMain.cm.getHouseFromPartialString(command.nextToken(), Username);
            amount = Integer.parseInt(command.nextToken());

        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser(
                  "Improper command. Try: /c adminexchangeplanetownership#planet#winner#loser#amount",
                  Username,
                  true);
            return;
        }

        if (planet == null) {
            server.campaign.CampaignMain.cm.toUser("Could not find a matching planet.", Username, true);
            return;
        }

        if (winningHouse == null) {
            server.campaign.CampaignMain.cm.toUser("Could not find a matching faction for the winner.", Username, true);
            return;
        }

        if (losingHouse == null) {
            server.campaign.CampaignMain.cm.toUser("Could not find a matching faction for the loser.", Username, true);
            return;
        }

        if (amount <= 0) {
            server.campaign.CampaignMain.cm.toUser("Get real try a number above 0!", Username, true);
            return;
        }

        //breaks passed
        int newAmount = planet.doGainInfluence(winningHouse, losingHouse, amount, true);

        //server.MWLogger.modLog(Username + " took " + newAmount + "% of "+ planet.getName() + " from " + losingHouse.getName() + " and gave it to " + winningHouse.getName() + ".");
        server.campaign.CampaignMain.cm.toUser("You took " +
                                                     newAmount +
                                                     "% of " +
                                                     planet.getName() +
                                                     " from " +
                                                     losingHouse.getName() +
                                                     " and gave it to " +
                                                     winningHouse.getName() +
                                                     ".", Username, true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
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
