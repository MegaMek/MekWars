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

/**
 * Simple object which stores a bid's two variables: amount
 * and time placed. Bidder name is saved here for use in the
 * Market's auctionType; however, the MarketListing used a 
 * player name key in lieu of the stored bidder name.
 * 
 * The bidAmount may be changed after construction. This is 
 * used by some auction types (Dutch, Vickrey) to modify the
 * price paid by the winner.
 */
public final class MarketBid implements Comparable<Object> {
	
	//IVARS
	private int bidAmount;
	private long bidTime;
	private String bidderName;
	
	//CONSTRUCTOR
	public MarketBid(int amount, long time) {
		bidAmount = amount;
		bidTime = time;
	}
	
	//METHDOS
	public int getAmount() {
		return bidAmount;
	}

	/**
	 * Some auction types (eg - Vickrey) modify the amount
	 * a winner should pay. ONLY a MekWarsAuction should
	 * change the bid amount.
	 */
	public void setAmount(int newAmount) {
		bidAmount = newAmount;
	}
	
	public long getTime() {
		return bidTime;
	}
	
    public void setBidderName(String bidder) {
        bidderName = bidder;
    }
    
	public String getBidderName() {
		return bidderName;
	}
	
	/**
	 * Implement comparable in order to make
	 * post-auction bid sorting simpler.
	 */
	public int compareTo(Object o) {
		
		if (o instanceof MarketBid == false)
			return 0;
		
		MarketBid compareBid = (MarketBid)o;
		if (this.bidAmount > compareBid.getAmount())
			return -1;
		
		else if (this.bidAmount < compareBid.getAmount())
			return 1;
		
		if (this.bidTime > compareBid.getTime())
			return -1;
		
		else if (this.bidTime < compareBid.getTime())
			return 1;
		
		/*
		 * In the event of an exact time-of-submission tie, pick
		 * a winner by alpha-order. This will probably NEVER happen.
		 */
		return this.bidderName.compareTo(compareBid.getBidderName());
	}
	
	/**
	 * Implement .equals(Object o) because it's good
	 * practice when something is comparable; however,
	 * this probably won't ever be used ...
	 */
	@Override
	public boolean equals(Object o) {
		
		if (o instanceof MarketBid == false)
			return false;
		
		MarketBid compareBid = (MarketBid)o;
		if (this.bidAmount == compareBid.getAmount() && this.bidTime == compareBid.getAmount())
			return true;
		
		return false;
	}
}
