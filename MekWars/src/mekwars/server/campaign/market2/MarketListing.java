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

import java.util.TreeMap;

import common.Unit;
import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;

/**
 * MarketListing keeps track of all infomation related to a
 * sale and necessary for resolution. Bids and duration are
 * updated while the auction is running. All other info is
 * passed at construction time and may not be changed while
 * the auction is running.
 * 
 * @author urgru
 */
public final class MarketListing {
	
	//IVARS
	private String sellerName;
	private int listedUnitID;
	private int minimumBid;
	private int saleDuration;//listing length, in TICKS
    
	private TreeMap<String,MarketBid> bidsReceived;
	
	/*
	 * Keep some information about the unit being sold (name, chassis, etc)
	 * for direct use w/o fetching the unit from the SPlayer. Getting it from
	 * the player would be more proper; however, we want to let the whole player
	 * cycle out of memory if at all possible.
	 */
	String modelName;
	String fileName;

	String unitTypeString;
	String unitWeightString;
	int unitType;
	int unitWeight;

	
	//CONSTRUCTOR
	public MarketListing(String sellname, SUnit unit, int minbid, int duration) {
		sellerName = sellname;
		listedUnitID = unit.getId();
		minimumBid = minbid;
		saleDuration = duration;
		bidsReceived = new TreeMap<String,MarketBid>();
        
		/*
		 * Fill in the temp unit data.
		 */
		modelName = unit.getModelName();
		fileName = unit.getUnitFilename();

		unitTypeString = Unit.getTypeClassDesc(unit.getType());
		unitWeightString = Unit.getWeightClassDesc(unit.getWeightclass());
		unitType = unit.getType();
		unitWeight = unit.getWeightclass();

	}
	
	//METHODS
	/**
	 * Convert to client-readable string.
	 */
	public String toString(int id, SPlayer p) {
		
		StringBuilder toReturn = new StringBuilder();
		
		//data everyone gets - names, times, etc.
		toReturn.append(id + "*");
		toReturn.append(this.getListedUnitID() + "*");
		if (CampaignMain.cm.getBooleanConfig("HiddenBMUnits")) {
			toReturn.append(this.getListedHiddenModelName() + "*");
		} else {
			if (this.getListedModelName().length() > 0) {
				toReturn.append(this.getListedModelName() + "*");
			} else {
				toReturn.append(" *");//no blank model names!
			}			
		}
		if (CampaignMain.cm.getBooleanConfig("HiddenBMUnits")) {
			toReturn.append(" *");
		} else {
			toReturn.append(this.getListedFileName() + "*");
		}
		toReturn.append(this.getSaleTicks() + "*");
		toReturn.append(this.getMinBid() + "*");
		
		//seller check
		if (p != null && this.getSellerName().equalsIgnoreCase(p.getName())) {
			toReturn.append(true);
		} else {
			toReturn.append(false);
		}
		toReturn.append("*");
		
		//player's bid.
		if (p != null) {
			toReturn.append(this.getBidForPlayer(p));
		} else {
			toReturn.append(0);
		}
		
		toReturn.append("*");
		toReturn.append(unitTypeString + "*" + unitWeightString);
		
		return toReturn.toString();
	}
	
	/**
	 * Add a bid to the treemap. This overwrites any prior
	 * bid from a player of the same name.
	 */
	public void placeBid(String bidderName, int bidAmount) {
	
        try{
    		
        	String lowerName = bidderName.toLowerCase();
        	
        	//add the bid, or attempt to remove if the bid is negative
    		MarketBid currBid = new MarketBid(bidAmount, System.currentTimeMillis());
            currBid.setBidderName(lowerName);
    		if (bidAmount > 0) {
    			bidsReceived.put(lowerName, currBid);
    		} else if (bidAmount < 1 && bidsReceived.containsKey(lowerName)) {
    			bidsReceived.remove(lowerName);
    		}
        } catch(Exception ex) {
            MWLogger.errLog(ex);
        }
	}
	
	/**
	 * Return all bids. Uused when resolving auction and
	 * by RecallCommand.java
	 */
	public TreeMap<String,MarketBid> getAllBids() {
		return bidsReceived;
	}
	
	/**
	 * Return the bid from a given player. Used by Market2.getAutoMarketStatus()
	 * and RecallBidCommand.java.
	 */
	public int getBidForPlayer(SPlayer p) {
		
		//loop through all bids looking for the player
		for (MarketBid currBid : bidsReceived.values()) {
			if (p.getName().equalsIgnoreCase(currBid.getBidderName()))
				return currBid.getAmount();
		}
		
		//no bid from the player. return a 0.
		return 0;	
	}
	
	/**
	 * Return the minimim bid for this unit.
	 */
	public int getMinBid() {
		return minimumBid;
	}
	
	/**
	 * Return the seller's name.
	 */
	public String getSellerName() {
		return sellerName;
	}
	
	/**
	 * Return the ID# of the unit being sold. Note that the unit
	 * itself is NOT stored anywhere within the market, and must be
	 * accessed by using SPlayer's getUnit(int unitID) or equivalent
	 * method in SHouse.
	 */
	public int getListedUnitID() {
		return listedUnitID;
	}
	
	/**
	 * Return the saved model name.
	 */
	public String getListedModelName() {
		return modelName;
	}
	
	/**
	 * Returns the simulated model name (i.e., "Heavy Mek") for an auction where
	 * the units are hidden
	 */
	public String getListedHiddenModelName() {
		return unitWeightString + " " + unitTypeString;
	}
	
	/**
	 * Return the saved filename.
	 */
	public String getListedFileName() {
		return fileName;
	}
	
	/**
	 * Return the current sale duration, in ticks.
	 */
	public int getSaleTicks() {
		return saleDuration;
	}
	
	/**
	 * Decrease the remaining sale-ticks by one. This is called by
	 * Market.java during a tick. Don't worry about negative values
	 * here, as the Market will resolve any auction with a duration
	 * of less than 1 before decrementing. 
	 */
	public void decreaseSaleTicks() {
		saleDuration--;
	}
    
    /**
     * This is called for units that are placed on the market at the tick
     * due to the tick decrementing the sale after its listed causing
     * units to appear with -1 ticks left.
     *
     */ 
    public void increaseSaleTicks(){
        saleDuration++;
    }
    
    public int getUnitType() {
    	return unitType;
    }
    
    public int getUnitWeight() {
    	return unitWeight;
    }
}//end MarketListing.java