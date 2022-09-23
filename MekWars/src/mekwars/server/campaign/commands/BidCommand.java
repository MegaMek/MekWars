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

package server.campaign.commands;

import java.util.StringTokenizer;

import common.Unit;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.market2.MarketListing;

public class BidCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		//access check
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}

		//load the SPlayer
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		if (p == null) {
			CampaignMain.cm.toUser("AM:Null SPlayer while bidding. Report immediately!",Username,true);
			return;
		}
		
		//get the auction ID and the amount to bid
		int auctionID = -1;
		int bidAmount = -1;
		try {
			auctionID = Integer.parseInt(command.nextToken());
			bidAmount = Integer.parseInt(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper format. Try: /c bid#AuctionID#Amount",Username,true);
			return;
		}
		
		//players whose factions don't have market buying access cannot bid
		if (!p.getMyHouse().mayBuyFromBM()) {
			CampaignMain.cm.toUser("AM:You are not allowed to buy units from the market. Your faction forbids it!", Username, true);
			return;
		}
		
		// check xp
		int minBMEXP = CampaignMain.cm.getIntegerConfig("MinEXPforBMBuying");
		if (p.getExperience() < minBMEXP) {
			CampaignMain.cm.toUser("AM:You are not allowed to buy units from the Market. Required Experience: " + minBMEXP + ".", Username, true);
			return;
		}
		
		//check the auction ID
		MarketListing auction = CampaignMain.cm.getMarket().getListingByID(auctionID);
		if (auction == null) {
			CampaignMain.cm.toUser("AM:There is no auction with ID#" + auctionID + ".",Username,true);
			return;
		}
		
		//make sure the bid meets the minimum requirement
		int minBid = auction.getMinBid();
		if (bidAmount < minBid) {
			if (!CampaignMain.cm.getBooleanConfig("HiddenBMUnits")) {
				CampaignMain.cm.toUser("AM:Minimum bid for the " + auction.getListedModelName()
					+ " is " + CampaignMain.cm.moneyOrFluMessage(true,false,minBid) + ". Nice try.",Username,true);
			} else {
				CampaignMain.cm.toUser("AM:Minimum bid for the " + auction.getListedHiddenModelName()
						+ " is " + CampaignMain.cm.moneyOrFluMessage(true,false,minBid) + ". Nice try.",Username,true);
			}
			return;
		}
		
		//check the player's flu
		int bidFluCost = CampaignMain.cm.getIntegerConfig("BMBidFlu");
		if (p.getInfluence() < bidFluCost) {
			CampaignMain.cm.toUser("AM:You need " +CampaignMain.cm.moneyOrFluMessage(false,true,bidFluCost)+" to place a bid.", Username, true);
			return;
		}
		
		// Check that the house is allowed to bid on this type of unit
		int uType = auction.getUnitType();
		int uWeight = auction.getUnitWeight();
		boolean canBuy = CampaignMain.cm.getHouseForPlayer(p.getName()).canBuyFromBM(uType, uWeight);
//		boolean canBuy = p.getMyHouse().canBuyFromBM(uType, uWeight);
		if (!canBuy) {
			CampaignMain.cm.toUser("AM:Your faction is not allowed to purchase " + 
					Unit.getWeightClassDesc(uWeight) + " " + 
					Unit.getTypeClassDesc(uType) + " from the Black Market.", Username, true);
			return;
		}
		
		/*
		 * All checks cleared. Decrease the player's influence and add his bid.
		 */
		auction.placeBid(Username, bidAmount);
		p.addInfluence(-bidFluCost);
		
		CampaignMain.cm.toUser("AM:You bid "+CampaignMain.cm.moneyOrFluMessage(true,false,bidAmount)+" for the "
				+ (CampaignMain.cm.getBooleanConfig("HiddenBMUnits") ? auction.getListedHiddenModelName() : auction.getListedModelName()) 
				+ " (-" + CampaignMain.cm.moneyOrFluMessage(false,true,bidFluCost)
				+").", Username, true);
		
		//send BM|CU to bidder
		CampaignMain.cm.toUser("BM|CU|" + auction.toString(auctionID,p),Username,false);
		
	}//end process(string)
}