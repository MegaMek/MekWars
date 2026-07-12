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

import mekwars.server.campaign.CampaignMain;
import server.campaign.market2.MarketListing;

public class RecallCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

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

        //load the SPlayer
        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
        if (p == null) {
            CampaignMain.campaignMain.toUser("AM:Null SPlayer while recalling unit. Report immediately!",
                  Username,
                  true);
            return;
        }

        //get the auction ID and the amount to bid
        int auctionID = -1;
        try {
            auctionID = Integer.parseInt(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper format. Try: /c recall#AuctionID", Username, true);
            return;
        }

        //check the auction ID
        MarketListing auction = CampaignMain.campaignMain.getMarket().getListingByID(auctionID);
        if (auction == null) {
            CampaignMain.campaignMain.toUser("AM:There is no auction with ID#" + auctionID + ".", Username, true);
            return;
        }

        //make sure the requestor is the seller
        if (!auction.getSellerName().equalsIgnoreCase(Username)) {
            CampaignMain.campaignMain.toUser("AM:Only the selling player may terminate an auction.",
                  Username,
                  true);
            return;
        }

        //if the auction has received bids, it cant be killed
        if (auction.getAllBids().size() > 0) {
            CampaignMain.campaignMain.toUser("AM:There are bids on the " + auction.getListedModelName()
                                                   + ". Sale may not be stopped.", Username, true);
            return;
        }

        /*
         * All checks cleared. Remove the listing. No need to remove
         * the FOR_SALE, send PL|SUS to the player, or set save. All
         * are handled by .removeListing().
         */
        CampaignMain.campaignMain.getMarket().removeListing(auctionID);

        //let the player know the unit was recalled
        CampaignMain.campaignMain.toUser("The " +
                                               auction.getListedModelName() +
                                               " is no longer for sale on the Market.", Username, true);
        CampaignMain.campaignMain.doSendHouseMail(p.getMyHouse(),
              "NOTE",
              p.getName() + " cancelled an auction [" + auction.getListedModelName() + "].");

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

}
