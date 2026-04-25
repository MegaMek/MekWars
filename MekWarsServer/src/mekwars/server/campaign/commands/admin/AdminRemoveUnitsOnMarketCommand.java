/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Helge Richter (McWizard)
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

/*
 * Created on 17.04.2004
 *
 */
package mekwars.server.campaign.commands.admin;

/**
 * @author Helge Richter
 */
public class AdminRemoveUnitsOnMarketCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "[player][all][number]";

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

        //vars
        String toRemove = "";

        try {
            toRemove = command.nextToken();
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser(
                  "Improper command. Try: /c adminremoveunitsonmarker#[player][all][number]",
                  Username,
                  true);
            return;
        }

        if (toRemove.equalsIgnoreCase("all")) {
            server.campaign.CampaignMain.cm.getMarket().removeAllListings();
            server.campaign.CampaignMain.cm.toUser("You removed all units from the market.", Username, true);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " removed all units from the BM.");
            server.campaign.CampaignMain.cm.getServer().sendChat(Username + " removed all units from the BM.");
            return;
        }


        //check to see if a specific auction is given.
        int auctionNumber = -1;
        try {
            auctionNumber = Integer.parseInt(toRemove);
        } catch (Exception ex) {
            //do nothing
        }

        if (auctionNumber > -1) {
            server.campaign.CampaignMain.cm.getMarket().removeListing(auctionNumber);
            server.campaign.CampaignMain.cm.toUser("You removed auction #" + auctionNumber + " from the market.",
                  Username,
                  true);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username + " removed auction #" + auctionNumber + " from the market.");
        } else {

            server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(toRemove);
            if (p == null) {
                server.campaign.CampaignMain.cm.toUser("Couldn't find a player named " + toRemove + ".",
                      Username,
                      true);
                return;
            }

            if (!server.campaign.CampaignMain.cm.getMarket().hasActiveListings(p)) {
                server.campaign.CampaignMain.cm.toUser(p.getName() + " doesn't have any running auctions.",
                      Username,
                      true);
                return;
            }

            server.campaign.CampaignMain.cm.getMarket().removePlayerListings(p);

            server.campaign.CampaignMain.cm.toUser("You cancelled all of " + p.getName() + "'s auctions.",
                  Username,
                  true);
            server.campaign.CampaignMain.cm.toUser(Username + " cancelled all of your running auctions.",
                  p.getName(),
                  true);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username + " cancelled all of " + toRemove + "'s auctions.");
        }
    }
}
