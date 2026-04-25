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

package mekwars.server.campaign.commands;

import server.campaign.market2.MarketListing;

public class RecallCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

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

        //load the SPlayer
        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Null SPlayer while recalling unit. Report immediately!",
                  Username,
                  true);
            return;
        }

        //get the auction ID and the amount to bid
        int auctionID = -1;
        try {
            auctionID = Integer.parseInt(command.nextToken());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("AM:Improper format. Try: /c recall#AuctionID", Username, true);
            return;
        }

        //check the auction ID
        MarketListing auction = server.campaign.CampaignMain.cm.getMarket().getListingByID(auctionID);
        if (auction == null) {
            server.campaign.CampaignMain.cm.toUser("AM:There is no auction with ID#" + auctionID + ".", Username, true);
            return;
        }

        //make sure the requestor is the seller
        if (!auction.getSellerName().equalsIgnoreCase(Username)) {
            server.campaign.CampaignMain.cm.toUser("AM:Only the selling player may terminate an auction.",
                  Username,
                  true);
            return;
        }

        //if the auction has received bids, it cant be killed
        if (auction.getAllBids().size() > 0) {
            server.campaign.CampaignMain.cm.toUser("AM:There are bids on the " + auction.getListedModelName()
                                                         + ". Sale may not be stopped.", Username, true);
            return;
        }

        /*
         * All checks cleared. Remove the listing. No need to remove
         * the FOR_SALE, send PL|SUS to the player, or set save. All
         * are handled by .removeListing().
         */
        server.campaign.CampaignMain.cm.getMarket().removeListing(auctionID);

        //let the player know the unit was recalled
        server.campaign.CampaignMain.cm.toUser("The " +
                                                     auction.getListedModelName() +
                                                     " is no longer for sale on the Market.", Username, true);
        server.campaign.CampaignMain.cm.doSendHouseMail(p.getMyHouse(),
              "NOTE",
              p.getName() + " cancelled an auction [" + auction.getListedModelName() + "].");

    }//end process()

}
