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

package server.campaign.market2;

import java.util.Iterator;
import java.util.TreeMap;

import common.Unit;
import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;

/**
 * Classic MMNET-style sealed bid auction.
 * @author urgru
 */
public final class HighestSealedBidAuction implements IAuction {
	
	/**
	 * Winner is simply the highest offering person who can
	 * afford to pay. This, codewise, is a truncated Vickrey
	 * Auction. Same mechanism to find highest bidder, but no
	 * downward adjustment.
	 */
	public MarketBid getWinner(MarketListing listing, boolean hiddenBM) {
		
		MarketBid winningBid = null;
		
		/*
		 * Assemble a TreeMap of the bids. This is a simple
		 * inversion of the name/bid tree in the listing.
		 */
		TreeMap<MarketBid,String> orderedBids = new TreeMap<MarketBid,String>();
		TreeMap<String, MarketBid> placedBids = listing.getAllBids();
        for (String bidderName : placedBids.keySet())
			orderedBids.put(placedBids.get(bidderName), bidderName);
		
        // Set up for checking for bay space.  Let's just do this once, instead of
        // every iteration through the loop        
        int unitType;
        int unitWeightClass;
        SUnit u;
        if (listing.getSellerName().toLowerCase().startsWith("faction_") || (CampaignMain.cm.getHouseFromPartialString(listing.getSellerName()) != null)) {
        	// It's coming from a house
        	String sellingFaction = listing.getSellerName().replace("Faction_", "");
        	u = CampaignMain.cm.getHouseFromPartialString(sellingFaction).getUnit(listing.getListedUnitID());
        } else {
        	// It's coming from a player
        	u = CampaignMain.cm.getPlayer(listing.getSellerName()).getUnit(listing.getListedUnitID());
        }
    	unitType = u.getType();
    	unitWeightClass = u.getWeightclass();
        
        
        /*
		 * Now, loop through the ordered bids until we find someone
		 * who can actually AFFORD to pay for the unit at this point.
		 */
		Iterator<MarketBid> i = orderedBids.keySet().iterator();
        
		while (i.hasNext()) {
			MarketBid currBid = i.next();
			IBuyer potentialWinner = CampaignMain.cm.getPlayer(currBid.getBidderName());
			if (potentialWinner == null)
				potentialWinner = CampaignMain.cm.getHouseFromPartialString(currBid.getBidderName(),null);
			
			//if we get a null buyer (someone unenrolled?), continue to next.
			if (potentialWinner == null)
				continue;
			
			//if the buyer can no longer afford his bid, move on
			if (potentialWinner.getMoney() < currBid.getAmount()) {
				if (potentialWinner.isHuman() && !hiddenBM) {//let a human know ...
				CampaignMain.cm.toUser("The " + listing.getListedModelName() 
						+ " from the BM could have been yours! Unfortunately, you don't have the "
						+ CampaignMain.cm.moneyOrFluMessage(true,true,currBid.getAmount())+" you "
						+ "offered.",currBid.getBidderName(),true);
				}
				continue;
			}
			
			// Check to see if the SOs are allowing users to go into negative bays
			if (potentialWinner.isHuman() && CampaignMain.cm.isUsingAdvanceRepair() && (CampaignMain.cm.getIntegerConfig("MaximumNegativeBaysFromBM") != -1)) {
				SPlayer p = (SPlayer) potentialWinner;
				int baysAvailable = p.getFreeBays() + CampaignMain.cm.getIntegerConfig("MaximumNegativeBaysFromBM");
				int baysNeeded;
				SHouse sellingFaction;
				//SUnit u;
				String sellerName = listing.getSellerName();
				if (sellerName.toLowerCase().startsWith("faction_") || (CampaignMain.cm.getHouseFromPartialString(sellerName) != null)) {
					// Coming from a house bay
					sellingFaction = CampaignMain.cm.getHouseFromPartialString(sellerName.replace("Faction_", ""));
					u = sellingFaction.getUnit(listing.getListedUnitID());
				} else {
					// Coming from a player
					SPlayer s = CampaignMain.cm.getPlayer(sellerName);
					sellingFaction = s.getMyHouse();
					u = s.getUnit(listing.getListedUnitID());
				}
				if (u != null) {
					// OK, we've got a unit to work with
					baysNeeded = SUnit.getHangarSpaceRequired(u, sellingFaction);
				} else {
					MWLogger.errLog("Spork effed something up.  Unable to find unit in HighestSealedBidAuction.getWinner()");
					CampaignMain.cm.doSendModMail("NOTE", "Spork effed something up.  Unable to find unit in HighestSealedBidAuction.getWinner()");
					baysNeeded = 0;
				}
				if (baysNeeded > baysAvailable) {
					// No can do
					if (potentialWinner.isHuman() && !hiddenBM) {//let a human know ...
						CampaignMain.cm.toUser("The " + listing.getListedModelName() 
								+ " from the BM could have been yours! Unfortunately, you don't have the "
								+ baysNeeded + " bays you need to store this unit.",currBid.getBidderName(),true);
						}
					continue;
				}
			}

			
			// if the buyer doesn't have room, move on as well.
			
			if (potentialWinner.isHuman() && !hiddenBM && !((SPlayer) potentialWinner).hasRoomForUnit(unitType, unitWeightClass)) {
				MWLogger.errLog(currBid.getBidderName() + " has no room for a " + Unit.getWeightClassDesc(unitWeightClass) + " " + Unit.getTypeClassDesc(unitType) + " from the BM");
				CampaignMain.cm.toUser("The " + listing.getListedModelName() 
						+ " from the BM could have been yours! Unfortunately, you don't have room for another "
						+ Unit.getWeightClassDesc(unitWeightClass) + " " 
						+ Unit.getTypeClassDesc(unitType) + ".", currBid.getBidderName(), true);
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
		if (winningBid == null)
			return null;
		
		/*
		 * Let everyone else know they they lost, and what the winner paid.
		 */
		while (i.hasNext()) {
			MarketBid losingBid = i.next();
			IBuyer loser = CampaignMain.cm.getPlayer(losingBid.getBidderName());
			if (loser == null)
				loser = CampaignMain.cm.getHouseFromPartialString(losingBid.getBidderName(),null);
			if (loser != null && loser.isHuman() && !hiddenBM) {
				CampaignMain.cm.toUser("You didn't get the  " + listing.getListedModelName()
						+ " for " + CampaignMain.cm.moneyOrFluMessage(true,true,losingBid.getAmount())+". The "
						+ "winner paid " + CampaignMain.cm.moneyOrFluMessage(true,true,winningBid.getAmount())
						+".",losingBid.getBidderName(),true);
			}	
		}//end while(losers remain)
		
		//return the winner.
		return winningBid;
		
	}//end getWinner

}//end HighestSealedBidAuction.java