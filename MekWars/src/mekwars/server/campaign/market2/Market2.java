/*
 * MekWars - Copyright (C) 2005
 * 
 * original author: nmorris (urgru@users.sourceforge.net)
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

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import common.CampaignData;
import common.Unit;
import common.util.MWLogger;
import common.util.UnitUtils;
import server.campaign.BuildTable;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.pilot.SPilot;


/**
 * A marketplace, where players may sell their units to one another. Admins may
 * configure the type of auction used, min/max list price, min/max list time,
 * and various other behaviours.
 * 
 * @urgru 12.29.05
 */
public class Market2 {
	
	// IVARS
	private IAuction auctionType;// set in constructor
	private TreeMap<Integer, MarketListing> currentAuctions;
	boolean hiddenBM = CampaignMain.cm.getBooleanConfig("HiddenBMUnits");
	
	// CONSTRUCTOR
	/**
	 * Market determines auction type at construction. Changing the declared
	 * auction type in the server configuration will NOT immediately change the
	 * type. The server must be restarted for this constructor to run and the
	 * change to take hold.
	 */
	public Market2() {
		/*
		 * Check which auction type to use based on server configs. we go in
		 * order Vickery, Highest Sealed bid, etc(as others are added) If none
		 * are selected then defaults to vickery
		 */
		
		if (CampaignMain.cm.getBooleanConfig("UseVickeryAuctionType"))
			auctionType = new VickreyAuction();
		else if (CampaignMain.cm.getBooleanConfig("UseHighestSealedBidAuctionType"))
			auctionType = new HighestSealedBidAuction();
		else
			auctionType = new VickreyAuction();
		
		currentAuctions = new TreeMap<Integer, MarketListing>();
	}
	
	// METHODS
	/**
	 * Add a listing to the market. This method assumes that a player's sale
	 * request has successfully passed checks in SellCommand (legal min bid,
	 * etc).
	 */
    public void addListing(String sellername, SUnit unit, int minbid, int salesticks) {
        addListing(sellername, unit, minbid, salesticks, false);

    }

	public void addListing(String sellername, SUnit unit, int minbid, int salesticks, boolean tickListing) {
		
		SPlayer sellingPlayer = null;
		
		if (!sellername.toLowerCase().startsWith("faction_") && CampaignMain.cm.getHouseFromPartialString(sellername, null) == null)
			sellingPlayer = CampaignMain.cm.getPlayer(sellername);
		
		if (unit.getModelName().startsWith("Error") || unit.getModelName().startsWith("OMG")) {
			MWLogger.errLog("OMG unit trying to be sold on the BM " + unit.getProducer());
			return;
		}
		
		if ((!UnitUtils.canStartUp(unit.getEntity())
				|| UnitUtils.hasArmorDamage(unit.getEntity())
				|| UnitUtils.hasCriticalDamage(unit.getEntity()))
				&& !CampaignMain.cm.getBooleanConfig("AllowDonatingOfDamagedUnits")) {
			MWLogger.errLog("Damaged unit trying to be sold on the BM " + unit.getProducer());
			
			if (sellingPlayer != null)
				CampaignMain.cm.toUser("You cannot sell damaged units on the Black Market!",sellername, true);
			
			return;
		}
		
		// get an ID for the auction
		int auctionID = this.getFreeID();
		
		// Set the unit's forSale boolean and send an update to the player.
		unit.setStatus(Unit.STATUS_FORSALE);
		
		// create the new listing and add it to the auction hashtable
		MarketListing newAuction = new MarketListing(sellername, unit, minbid, salesticks);
		currentAuctions.put(auctionID, newAuction);
		
		// send a BM addition command to all online/logged in players
        if ( tickListing ){
            newAuction.increaseSaleTicks();
            CampaignMain.cm.doSendToAllOnlinePlayers("BM|AU|" + newAuction.toString(auctionID, null), false);
            newAuction.decreaseSaleTicks();
        }
        else
            CampaignMain.cm.doSendToAllOnlinePlayers("BM|AU|" + newAuction.toString(auctionID, null), false);
		// send an accurate unit (with seller flag) to the listing player using BM|changeunit
		// also, send a PL|SUS| to change the unit in the player's HQ.
		if (sellingPlayer != null) {
			CampaignMain.cm.toUser("BM|CU|" + newAuction.toString(auctionID, sellingPlayer),sellername, false);
			CampaignMain.cm.toUser("PL|SUS|" + unit.getId() + "#" + Unit.STATUS_FORSALE, sellername, false);
		}
	}
	
	/**
	 * Public rmeove listing. Any call from outside of the Market
	 * is going to be a simple remove without destruction.
	 */
	public void removeListing(int auctionID) {
		this.removeListing(auctionID,false);
	}
	
	/**
	 * Remove a listing from the market.
	 * 
	 * In most cases, will be triggered by a player-issued RecallCommand.
	 * May also be called from AdminRecallCommand. Note that any penalty
	 * or msg. to player(s) is handled in the command classes.
	 * 
	 * NOTE: removeListing is only called after a FAILED sale. If a unit
	 * changes hands, transfer and updates are handled in this.tick().
	 * 
	 * @param auctionID - ID of auction to terminate.
	 * @param destroyFaction - if false, return faction units to bays.
	 */
	private void removeListing(int auctionID, boolean destroyFactionUnits) {
		
		// get the auction and seller name
		MarketListing currAuction = currentAuctions.get(auctionID);
		String sellerName = currAuction.getSellerName();
		
		// rares are removed into the netherworld. faction units have forsale removed.
		if (!sellerName.equalsIgnoreCase("rare") && !sellerName.toLowerCase().startsWith("faction_")) {
			
			// get the ISeller (house or player) listing the unit
			ISeller seller = null;
			seller = CampaignMain.cm.getHouseFromPartialString(sellerName,null);
			
			//Check the house first first to avoid annoying "no pfile" errors
			if (seller == null)
				seller = CampaignMain.cm.getPlayer(sellerName);
			
			// if neither a house nor a player is selling this, bail?
			if (seller == null)
				return;
			
			// load the unit, return to STATUS_OK
			SUnit auctionU = seller.getUnit(currAuction.getListedUnitID());
			
			// Something bad happend now lets get rid of the evil NPE before
			// something even worse happens!
			if (auctionU == null) {
				currentAuctions.remove(auctionID);
				CampaignMain.cm.doSendToAllOnlinePlayers("BM|RU|" + auctionID, false);
				return;
			}
			
			auctionU.setStatus(Unit.STATUS_OK);
			
			/*
			 * If seller is a player, send a PL| and remove the for_sale flag in his HQ.
			 * 
			 * If seller is a house, we check the destroy boolean. We don't need to send
			 * HS|RU| commands for destroyed units, because they were removed from view
			 * when added to the BM. Send an HS|AU| to return the unit to plain view if it
			 * isn't being destroyed. 
			 */
			if (seller.isHuman()) {
				SPlayer p = (SPlayer) seller;
				CampaignMain.cm.toUser("PL|SUS|" + auctionU.getId() + "#" + Unit.STATUS_OK, sellerName, false);
				p.setSave();
			} else{
				SHouse h = (SHouse) seller;
				if (destroyFactionUnits) {
					h.removeUnit(auctionU,false);
				}
				else
					CampaignMain.cm.doSendToAllOnlinePlayers(h, "HS|" + h.getHSUnitAdditionString(auctionU), false);
			}
		} else {
            SHouse sellingFaction = CampaignMain.cm.getHouseFromPartialString(CampaignMain.cm.getConfig("NewbieHouseName"), null);
            SUnit auctionU = sellingFaction.getUnit(currAuction.getListedUnitID());
            if (auctionU != null) {
            	sellingFaction.removeUnit(auctionU,false);
            }
        }
		
		// Remove the auction from the hash
		currentAuctions.remove(auctionID);
		
		// send a removal command to all online/logged in players
		CampaignMain.cm.doSendToAllOnlinePlayers("BM|RU|" + auctionID, false);
	}
	
	/**
	 * Method which batch-terminates sales, making the units available to their
	 * original owners.
	 */
	public void removeAllListings() {
		
		//remove each listing using the standard, non=destructive, remove.
		ArrayList<Integer> listingsToRemove = new ArrayList<Integer>();
		for (Integer currAuctionID : currentAuctions.keySet())
			listingsToRemove.add(currAuctionID);
		for (Integer idToRemove : listingsToRemove)
			this.removeListing(idToRemove);
	}
	
	/**
	 * Method that removes all of a player's listings. Called by
	 * AdminRemoveUnitsOnMarketCommand.java.
	 */
	public void removePlayerListings(SPlayer p) {
		
		// decide which listings to remove
		ArrayList<Integer> listingsToRemove = new ArrayList<Integer>();
		for (Integer currAuctionID : currentAuctions.keySet()) {
			MarketListing currL = currentAuctions.get(currAuctionID);
			if (currL.getSellerName().equalsIgnoreCase(p.getName()))
				listingsToRemove.add(currAuctionID);
		}
		
		// remove each using the standard removeListing
		for (Integer idToRemove : listingsToRemove)
			this.removeListing(idToRemove);
	}
	
	/**
	 * Method which returns a MarketListing which contains a given unique unit
	 * ID. Returns null if no matching listing exists.
	 * 
	 * NOTE: When simply checking whether or not a unit is being sold, use
	 * SUnit's .getStatus() to see if Unit.STATUS_FORSALE is set.
	 */
	public MarketListing getListingForUnit(int unitID) {
		
		for (MarketListing currList : currentAuctions.values()) {
			if (currList.getListedUnitID() == unitID)
				return currList;// match.
		}
		return null;// no match.
	}
	
	/**
	 * Method which returns a MarketListing with a given auction ID. Used by
	 * BidCommand, RecallCommand and other classes which can identify auctions.
	 * 
	 * Use this instead of getListingForUnit whenever possible, as no iteration
	 * is required.
	 * 
	 * @param auctionID - ID of auction to fetch.
	 * @return listing. null if no listing has given ID.
	 */
	public MarketListing getListingByID(int auctionID) {
		return currentAuctions.get(auctionID);
	}
	
	/**
	 * Method which determines whether or not a player has units on the market.
	 * Used by DefectCommand and UnenrollCommand to ensure that a player who is
	 * moving/leaving isn't selling anything.
	 */
	public boolean hasActiveListings(SPlayer player) {
		for (MarketListing currList : currentAuctions.values()) {
			if (currList.getSellerName().equalsIgnoreCase(player.getName()))
				return true;
		}
		return false;
	}
	
	/**
	 * Method which gets a player's bid on a given auction. Generally used when
	 * a player first connects to send him accurate BM info.
	 */
	public int getPlayerBidOnAuction(String playerName, int auctionID) {
		
		// return 0 if the requested auction doesn't exist
		MarketListing currList = currentAuctions.get(auctionID);
		if (currList == null)
			return 0;
		
		// return 0 if the player hasn't bid
		MarketBid playersBid = currList.getAllBids().get(playerName.toLowerCase());
		if (playersBid == null)
			return 0;
		
		// a real bid exists. return its amount.
		return playersBid.getAmount();
	}
	
	/**
	 * Method which returns all bids made by a player, in a formatted string.
	 * This is used by MyBidsCommand.java.
	 * 
	 * One could argue that the command should iterate through the listings and
	 * prep the return string itself; however, that would require giving the
	 * hash to the command. As is, the hash is touched only in this class, and
	 * I'd like to keep it that way.
	 * 
	 * @urgru 12.29.05
	 */
	public String getPlayerBidsString(String playerName) {
		String result = "Current Bids:<br> ";
		for (Integer currListID : currentAuctions.keySet()) {
			MarketListing currList = currentAuctions.get(currListID);
			Integer currBid = currList.getAllBids().get(playerName.toLowerCase()).getAmount();
			if (currBid != null && currBid > 0)
				result += "You bid " + currBid + " for " + currList.getListedModelName() + " (Auction #" + currListID + ")<br>";
		}
		
		return result;
	}
	
	/**
	 * Method which returns all listings, in a formatted string. Used by
	 * BMStatusCommand.java.
	 * 
	 * See comments in this.getPlayerBidsString for more details.
	 */
	public String getMarketInfoString() {
		String result = "<font color=\"black\">Black Market: <br>";
		for (Integer currListID : currentAuctions.keySet()) {
			MarketListing currList = currentAuctions.get(currListID);
			result += "#" + currListID + " " + 
			(CampaignMain.cm.getBooleanConfig("HiddenBMUnits") ? currList.getListedHiddenModelName() : currList.getListedModelName())+ 
			". Minimum Bid: ";
			result += currList.getMinBid() + ", Remaining Ticks: " + currList.getSaleTicks();
			result += ", Comment: " + "<br>";
		}
		return result += "</font>";
	}
	
	/**
	 * Private method which determines the lowest available auction ID. Used by
	 * the market when a new listing is added. Lame brute force approach, but it
	 * doesn't really matter here.
	 */
	private int getFreeID() {
		boolean foundID = false;
		int potentialID = 0;
		while (!foundID) {
			if (currentAuctions.get(potentialID) == null)
				foundID = true;
			else
				potentialID++;
		}
		return potentialID;
	}
	
	/**
	 * Market.tick() decrements counters on unfinished auction and resolves
	 * completed auctions. Amounts to be paid and actual winners are determined
	 * by the Market's auctionType, but the tick itself removes units from one
	 * player, adds to new owner.
	 */
	public void tick() {
		
		/*
		 * Tick every auction. Resolve those which have negative sales times
		 * after decrementing. Store completed auctions in an array list for
		 * removal after the finishing loop.
		 */
		ArrayList<Integer> listingsToRemove = new ArrayList<Integer>();
		Lock lock = new ReentrantLock();
		
		lock.lock();
		for (Integer currAuctionID : currentAuctions.keySet()) {
			
			// get the ID and actual listing
			MarketListing currList = currentAuctions.get(currAuctionID);
			
			// don't deal with null's
			if (currList == null) {
				listingsToRemove.add(currAuctionID);
				continue;
			}
			
			try {
				
				// if the sales ticks have gone negative, resolve the auction
				currList.decreaseSaleTicks();
				if (currList.getSaleTicks() < 0) {
					
					/*
					 * Pass the MarketListing to an auctionType to determine the
					 * winner, and how much he should pay. Fetch the SUnit and
					 * SPlayer, as we need them whether the unit sold or not.
					 */
					MarketBid winningBid = auctionType.getWinner(currList, hiddenBM);
					ISeller sellingActor = null;
					
					// try all possible ISellers.
					if (currList.getSellerName().toLowerCase().startsWith("faction_"))
						sellingActor = CampaignMain.cm.getHouseFromPartialString(currList.getSellerName().substring(8), null);
					
					// check house first.
					if (sellingActor == null)
						sellingActor = CampaignMain.cm.getHouseFromPartialString(currList.getSellerName(), null);
					if (sellingActor == null)
						sellingActor = CampaignMain.cm.getPlayer(currList.getSellerName());
					
					SUnit unitForSale = sellingActor.getUnit(currList.getListedUnitID());
					
					if (unitForSale == null) {
						MWLogger.errLog("Unable to get unit for sale "+ currList.getListedModelName() + " seller " + currList.getSellerName());
						listingsToRemove.add(currAuctionID);
						continue;
					}
					
					/*
					 * No one wanted the unit. Updates and removes will be handled through removeListing().
					 */
					if (winningBid == null) {
						if (sellingActor.isHuman()){
							CampaignMain.cm.toUser("No one purchased your " + currList.getListedModelName() + ".", currList.getSellerName(), true);
                            SPlayer p = (SPlayer) sellingActor;
                            CampaignMain.cm.toUser("PL|SUS|" + currAuctionID + "#" + Unit.STATUS_OK, p.getName(), false);
                            p.setSave();
                        }
						listingsToRemove.add(currAuctionID);
					}
					
					/*
					 * Someone purchased the unit. Move the money and the unit.
					 * No need to set SPlayer.setSave() here b/c addMoney and
					 * addUnit both trigger player saves.
					 */
					else {
						
						// load the winning IBuyer.
						IBuyer buyingActor = CampaignMain.cm.getPlayer(winningBid.getBidderName());
						if (buyingActor == null)
							buyingActor = CampaignMain.cm.getHouseFromPartialString(winningBid.getBidderName(), null);
						
						/*
						 * Subtract the bid amount from the winner. The winner
						 * may not get the full amount. Check to see what %
						 * should be taken as an auction fee, and give the
						 * remainder to the selling player.
						 */
						int winningBidAmt = winningBid.getAmount();
						float auctionFee = CampaignMain.cm.getFloatConfig("AuctionFee");
						int auctionFeeAmt = Math.round(winningBidAmt * auctionFee);
						int winnerMoneyGain = winningBidAmt - auctionFeeAmt;
						buyingActor.addMoney(-winningBidAmt);
						sellingActor.addMoney(winnerMoneyGain);
						
						/*
						 * Inform the seller and buyer of the outcome. Also,
						 * send the BM sound trigger to the winner. NOTE: only
						 * human actors are so informed.
						 * 
						 * Players who lost their bids are told (or, at least,
						 * should be told) of the result, including final price,
						 * by the auctionType.
						 */
						unitForSale.setStatus(Unit.STATUS_OK);
						if (sellingActor.isHuman()) {
							CampaignMain.cm.toUser("The " + currList.getListedModelName() + " sold for "
									+ CampaignMain.cm.moneyOrFluMessage(true,true, winningBidAmt) + ". Auction fees were "
									+ CampaignMain.cm.moneyOrFluMessage(true,true, auctionFeeAmt) + ", leaving "
									+ CampaignMain.cm.moneyOrFluMessage(true,true, winnerMoneyGain) + " as your take from "
									+ "the sale.", currList.getSellerName(), true);
						}
						
						if (buyingActor.isHuman()) {
							CampaignMain.cm.toUser("PL|BMW|1", winningBid.getBidderName(), false);
							CampaignMain.cm.toUser("You purchased the " + currList.getListedModelName() + " for " 
									+ CampaignMain.cm.moneyOrFluMessage(true,true, winningBidAmt) + ".", winningBid.getBidderName(), true);
						}
						
						/*
						 * Move the unit. Strip the pilot out and put him in the
						 * seller's personal queue (if PPQ's are enabled) or the
						 * faction queue.
						 * 
						 * IF PPQ's are turned on, the recipient gets a vacant
						 * mek or proto. If no PPQ's, or unit is BA/Veh, return
						 * the pilot to the faction queue and pull a random
						 * pilot from the faction stack.
						 */
						if (Boolean.parseBoolean(CampaignMain.cm.getConfig("AllowPersonalPilotQueues"))
								&& unitForSale.isSinglePilotUnit()) {
							
							// If the old owner is human, he keeps the pilot and gets an updated PPQ. If a
							// faction, just eat the pilot. They'll make more. =)
							if (sellingActor.isHuman()) {
								SPlayer p = (SPlayer) sellingActor;
								p.getPersonalPilotQueue().addPilot(unitForSale.getPilot(),unitForSale.getWeightclass());
								CampaignMain.cm.toUser("PL|AP2PPQ|" + unitForSale.getType() + "|" + unitForSale.getWeightclass() + "|" + ((SPilot) unitForSale.getPilot()).toFileFormat("#", true), p.getName(), false);
					    	    p.getPersonalPilotQueue().checkQueueAndWarn(p.getName(), unitForSale.getType(), unitForSale.getWeightclass());
							}
							
							// vacate the unit
							unitForSale.setPilot(new SPilot("Vacant", 99, 99));
						}
						
						else {// no PPQs, or not a PPQ-using unit type
							
							// add to the faction queue
							if (sellingActor.isHuman()) {
								SPlayer p = (SPlayer) sellingActor;
								p.getMyHouse().addDispossessedPilot(unitForSale, false);
							} else {
								SHouse h = (SHouse) sellingActor;
								h.addDispossessedPilot(unitForSale, false);
							}
							
							// get a new pilot from the future owner's faction pool
							if (buyingActor.isHuman()) {
								SPlayer p = (SPlayer) buyingActor;
								unitForSale.setPilot(p.getMyHouse().getNewPilot(unitForSale.getType()));
							} else {
								SHouse h = (SHouse) buyingActor;
								unitForSale.setPilot(h.getNewPilot(unitForSale.getType()));
							}
						}
						
						// send the unit to its new owner, and log the transition
						sellingActor.removeUnit(unitForSale, false);//BM units already removed from the SHouse display
						buyingActor.addUnit(unitForSale, true, true);
						MWLogger.bmLog(winningBid.getBidderName()+ " bought a " + currList.getListedModelName()+ " from " + currList.getSellerName() + " for "+ winningBidAmt);
					
						/*
						 * Add to listings to remove. The seller will be non-null, but the unit
						 * will be null (because seller no longer has it). This will trigger an
						 * auction removal, send an update, then exit removeListing().
						 */
                        listingsToRemove.add(currAuctionID);
						
					}
					
				}// end if(auction is over)
				
			} catch (Exception ex) {
				MWLogger.errLog("Error during Market Tick for unit " + currList.getListedModelName());
				MWLogger.errLog(ex);
			}
		}// end for(all auctions)

		lock.unlock();
       // remove failed auction using the standard removeListing, which will send client updates
		for (Integer idToRemove : listingsToRemove)
			this.removeListing(idToRemove,true);
		
		/*
		 * If the admins choose to enable raresales, roll a 1d10000/100 and see
		 * if a new rare should be listed at this point. This gets us a nice 2
		 * decimal double to compare to the config.
		 */
		double rareChance = (CampaignMain.cm.getRandomNumber(100001)) / 1000;// 0.000-100.000
		if (rareChance < CampaignMain.cm.getDoubleConfig("RareChance")) {
			
			/*
			 * We're going to be creating a RARE unit. Joyous day. First, lets
			 * pick a weightclass ...
			 */
			int weightClass = 0;
			if (CampaignMain.cm.getBooleanConfig("UseBMWeightingTables")) {
				weightClass = getSkewedWeightClass();
			} else {
				weightClass = CampaignMain.cm.getRandomNumber(4);// 0-3
			}
			
			// get a filename from the static build table
			String factionName = "Rare";
			String unitFluff = "Sold by mysterious figures";
			
			// get a filename for a unit (ex: Wahrammer WHM-6R.MTF) from a build table
			String unitFilename = BuildTable.getUnitFilename(factionName, Unit.getWeightClassDesc(weightClass), Unit.MEK, BuildTable.RARE);//rare units onto BM
			Vector<SUnit> rareUnits = new Vector<SUnit>(1,1);
			
			if ( unitFilename.toLowerCase().trim().endsWith(".mul")){
				rareUnits.addAll(SUnit.createMULUnits(unitFilename,unitFluff));
			}else
				rareUnits.add(new SUnit(unitFluff, unitFilename, weightClass));
			// build the new unit
			for ( SUnit rareUnit : rareUnits){ 
				
				/*
				 * Have the newbie-house sell the unit. Although this can lead to
				 * nasty rare unit buildup, we're lazy and don't want to handle null
				 * sellers.
				 * 
				 * List it for the stock factory purchase price.
				 */
				SHouse sellingFaction = CampaignMain.cm.getHouseFromPartialString(CampaignMain.cm.getConfig("NewbieHouseName"), null);
				int priceForUnit = sellingFaction.getPriceForUnit(rareUnit.getWeightclass(), rareUnit.getType());
				
				// Create the listing
	            //add 1 to the sale tick due to a quirk with the BM autoupdate.
	            //The the unit is sent to the player before the new tick counter so the clients
	            //are a tick ahead of the server.
				int rareSalesTime = CampaignMain.cm.getIntegerConfig("RareMinSaleTime");
				this.addListing("Faction_" + sellingFaction.getName(), rareUnit,priceForUnit, rareSalesTime, true);
				sellingFaction.addUnit(rareUnit, true);
				rareUnit.setStatus(Unit.STATUS_FORSALE);
			}
		}
	}
	
	/**
	 * Method which returns a client-readable string detailing the current
	 * contents of the market. Sent when a player first logs in to the
	 */
	public void sendCompleteMarketStatus(SPlayer p) {
		
		StringBuilder marketData = new StringBuilder();
		for (Integer currListID : currentAuctions.keySet()) {
			MarketListing currL = currentAuctions.get(currListID);
			marketData.append(currL.toString(currListID, p) + "$");
		}
		
		// return data to SHouse send data to player
		String result = marketData.toString();
		if (result.trim().length() > 0)
			CampaignMain.cm.toUser("BM|AD|" + result, p.getName(), false);
	}
	
	private int getSkewedWeightClass() {
		int lightEnd = CampaignMain.cm.getIntegerConfig("BMLightMekWeight");
		int mediumEnd = lightEnd + CampaignMain.cm.getIntegerConfig("BMMediumMekWeight");
		int heavyEnd = mediumEnd + CampaignMain.cm.getIntegerConfig("BMHeavyMekWeight");
		int assaultEnd = heavyEnd + CampaignMain.cm.getIntegerConfig("BMAssaultMekWeight");
		int totalWeight = assaultEnd;
		int roll = CampaignMain.cm.getRandomNumber(totalWeight);
		
		if(roll < lightEnd) {
			return 0;
		} else if (roll < mediumEnd) {
			return 1;
		} else if (roll < heavyEnd) {
			return 2;
		} else if (roll < assaultEnd) {
			return 3;
		} else {
			MWLogger.errLog("Error in getSkewedWeightClass().");
			MWLogger.errLog("lightEnd: " + lightEnd);
			MWLogger.errLog("mediumEnd: " + mediumEnd);
			MWLogger.errLog("heavyEnd: " + heavyEnd);
			MWLogger.errLog("assaultEnd: " + assaultEnd);
			MWLogger.errLog("Roll: " + roll);
			return 0;
		}
	}
}// end Market.java
