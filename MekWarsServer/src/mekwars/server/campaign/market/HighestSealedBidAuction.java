/*
 * MekWars - Copyright (C) 2005
 *
 * original author: N. Morris (urgru@users.sourceforge.net)
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

package mekwars.server.campaign.market;

import common.Unit;
import common.util.MWLogger;

/**
 * Classic MMNET-style sealed bid auction.
 *
 * @author urgru
 */
public final class HighestSealedBidAuction implements IAuction {

    /**
     * Winner is simply the highest offering person who can afford to pay. This, codewise, is a truncated Vickrey
     * Auction. Same mechanism to find highest bidder, but no downward adjustment.
     */
    public MarketBid getWinner(MarketListing listing, boolean hiddenBM) {

        MarketBid winningBid = null;

        /*
         * Assemble a TreeMap of the bids. This is a simple
         * inversion of the name/bid tree in the listing.
         */
        java.util.TreeMap<mekwars.server.campaign.market.MarketBid, String> orderedBids = new java.util.TreeMap<mekwars.server.campaign.market.MarketBid, String>();
        java.util.TreeMap<String, mekwars.server.campaign.market.MarketBid> placedBids = listing.getAllBids();
        for (String bidderName : placedBids.keySet()) {orderedBids.put(placedBids.get(bidderName), bidderName);}

        // Set up for checking for bay space.  Let's just do this once, instead of
        // every iteration through the loop
        int unitType;
        int unitWeightClass;
        server.campaign.SUnit u;
        if (listing.getSellerName().toLowerCase().startsWith("faction_") ||
                  (server.campaign.CampaignMain.cm.getHouseFromPartialString(listing.getSellerName()) != null)) {
            // It's coming from a house
            String sellingFaction = listing.getSellerName().replace("Faction_", "");
            u = server.campaign.CampaignMain.cm.getHouseFromPartialString(sellingFaction)
                      .getUnit(listing.getListedUnitID());
        } else {
            // It's coming from a player
            u = server.campaign.CampaignMain.cm.getPlayer(listing.getSellerName()).getUnit(listing.getListedUnitID());
        }
        unitType = u.getType();
        unitWeightClass = u.getWeightclass();


        /*
         * Now, loop through the ordered bids until we find someone
         * who can actually AFFORD to pay for the unit at this point.
         */
        java.util.Iterator<mekwars.server.campaign.market.MarketBid> i = orderedBids.keySet().iterator();

        while (i.hasNext()) {
            MarketBid currBid = i.next();
            IBuyer potentialWinner = server.campaign.CampaignMain.cm.getPlayer(currBid.getBidderName());
            if (potentialWinner == null) {
                potentialWinner = server.campaign.CampaignMain.cm.getHouseFromPartialString(currBid.getBidderName(),
                      null);
            }

            //if we get a null buyer (someone unenrolled?), continue to next.
            if (potentialWinner == null) {continue;}

            //if the buyer can no longer afford his bid, move on
            if (potentialWinner.getMoney() < currBid.getAmount()) {
                if (potentialWinner.isHuman() && !hiddenBM) {//let a human know ...
                    server.campaign.CampaignMain.cm.toUser("The " +
                                                                 listing.getListedModelName()
                                                                 +
                                                                 " from the BM could have been yours! Unfortunately, you don't have the "
                                                                 +
                                                                 server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                                       true,
                                                                       currBid.getAmount()) +
                                                                 " you "
                                                                 +
                                                                 "offered.", currBid.getBidderName(), true);
                }
                continue;
            }

            // Check to see if the SOs are allowing users to go into negative bays
            if (potentialWinner.isHuman() && server.campaign.CampaignMain.cm.isUsingAdvanceRepair() && (
                  server.campaign.CampaignMain.cm.getIntegerConfig("MaximumNegativeBaysFromBM") != -1)) {
                server.campaign.SPlayer p = (server.campaign.SPlayer) potentialWinner;
                int baysAvailable = p.getFreeBays() +
                                          server.campaign.CampaignMain.cm.getIntegerConfig("MaximumNegativeBaysFromBM");
                int baysNeeded;
                server.campaign.SHouse sellingFaction;
                //SUnit u;
                String sellerName = listing.getSellerName();
                if (sellerName.toLowerCase().startsWith("faction_") ||
                          (server.campaign.CampaignMain.cm.getHouseFromPartialString(sellerName) != null)) {
                    // Coming from a house bay
                    sellingFaction = server.campaign.CampaignMain.cm.getHouseFromPartialString(sellerName.replace(
                          "Faction_",
                          ""));
                    u = sellingFaction.getUnit(listing.getListedUnitID());
                } else {
                    // Coming from a player
                    server.campaign.SPlayer s = server.campaign.CampaignMain.cm.getPlayer(sellerName);
                    sellingFaction = s.getMyHouse();
                    u = s.getUnit(listing.getListedUnitID());
                }
                if (u != null) {
                    // OK, we've got a unit to work with
                    baysNeeded = server.campaign.SUnit.getHangarSpaceRequired(u, sellingFaction);
                } else {
                    MWLogger.errLog(
                          "Spork effed something up.  Unable to find unit in HighestSealedBidAuction.getWinner()");
                    server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                          "Spork effed something up.  Unable to find unit in HighestSealedBidAuction.getWinner()");
                    baysNeeded = 0;
                }
                if (baysNeeded > baysAvailable) {
                    // No can do
                    if (potentialWinner.isHuman() && !hiddenBM) {//let a human know ...
                        server.campaign.CampaignMain.cm.toUser("The " +
                                                                     listing.getListedModelName()
                                                                     +
                                                                     " from the BM could have been yours! Unfortunately, you don't have the "
                                                                     +
                                                                     baysNeeded +
                                                                     " bays you need to store this unit.",
                              currBid.getBidderName(),
                              true);
                    }
                    continue;
                }
            }


            // if the buyer doesn't have room, move on as well.

            if (potentialWinner.isHuman() &&
                      !hiddenBM &&
                      !((server.campaign.SPlayer) potentialWinner).hasRoomForUnit(unitType, unitWeightClass)) {
                MWLogger.errLog(currBid.getBidderName() +
                                      " has no room for a " +
                                      Unit.getWeightClassDesc(unitWeightClass) +
                                      " " +
                                      Unit.getTypeClassDesc(unitType) +
                                      " from the BM");
                server.campaign.CampaignMain.cm.toUser("The " +
                                                             listing.getListedModelName()
                                                             +
                                                             " from the BM could have been yours! Unfortunately, you don't have room for another "
                                                             +
                                                             Unit.getWeightClassDesc(unitWeightClass) +
                                                             " "
                                                             +
                                                             Unit.getTypeClassDesc(unitType) +
                                                             ".", currBid.getBidderName(), true);
                continue;
            }

            //we found someone who can afford the unit. joy!
            winningBid = currBid;
            break;
        }

        /*
         * If winningBid is still null, we had no valid winner. Just
         * return a null, and let Market.java sort it out from there.
         */
        if (winningBid == null) {return null;}

        /*
         * Let everyone else know they they lost, and what the winner paid.
         */
        while (i.hasNext()) {
            MarketBid losingBid = i.next();
            IBuyer loser = server.campaign.CampaignMain.cm.getPlayer(losingBid.getBidderName());
            if (loser == null) {
                loser = server.campaign.CampaignMain.cm.getHouseFromPartialString(losingBid.getBidderName(), null);
            }
            if (loser != null && loser.isHuman() && !hiddenBM) {
                server.campaign.CampaignMain.cm.toUser("You didn't get the  " +
                                                             listing.getListedModelName()
                                                             +
                                                             " for " +
                                                             server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                                   true,
                                                                   losingBid.getAmount()) +
                                                             ". The "
                                                             +
                                                             "winner paid " +
                                                             server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                                   true,
                                                                   winningBid.getAmount())
                                                             +
                                                             ".", losingBid.getBidderName(), true);
            }
        }//end while(losers remain)

        //return the winner.
        return winningBid;

    }//end getWinner

}//end HighestSealedBidAuction.java
