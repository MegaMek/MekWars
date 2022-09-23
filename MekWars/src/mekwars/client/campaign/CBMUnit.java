/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
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

package client.campaign;

import java.util.StringTokenizer;

import common.Unit;
import common.campaign.pilot.Pilot;
import common.util.TokenReader;
import megamek.client.generator.RandomGenderGenerator;
import megamek.common.CrewType;

/**
 * Client-side market unit. The market uses the filenames and other data
 * from the CBMUnit to generate temporary CUnits (to determine BV, etc).
 *
 * In the past, this class extended CUnit. This is no longer the case. Using
 * minimal data (not sending ammo settings and complete unit strings) saves
 * bandwidth. Instead, we build temporary CUnit and store it w/i the BMUnit.
 */
public class CBMUnit {

	//IVARS
	private boolean soldByPlayer = false;
	private String modelName = "";
	private String fileName = "";
	private int auctionID = -1;
	private int unitID = -1;
	private int salesTicksRemaining = -1;
	private int minBid = -1;
	private int playersBid = -1;
	private String unitWeight = "";
	private String unitType = "";
	private boolean hidden = false;

	CUnit embeddedUnit = null;

	//CONSTRUCTOR
	/**
	 * Constructor which takes a data string from the server. This
	 * String is generated in Market2's getAutoMarketStatus() method.
	 *
	 * Be sure that the token read-in order always matches the market's
	 * write-out order.
	 */
	public CBMUnit(String listingData, CCampaign campaign, boolean hiddenUnits) {
		hidden = hiddenUnits;

		//read data
		StringTokenizer ST = new StringTokenizer(listingData,"*");
		auctionID = TokenReader.readInt(ST);
		unitID = TokenReader.readInt(ST);
		modelName = TokenReader.readString(ST);
		fileName = TokenReader.readString(ST);
		salesTicksRemaining = TokenReader.readInt(ST);
		minBid = TokenReader.readInt(ST);
		soldByPlayer = TokenReader.readBoolean(ST);
		playersBid = TokenReader.readInt(ST);
		unitType = TokenReader.readString(ST);
		unitWeight = TokenReader.readString(ST);


		//bury a CUnit
		if (!hiddenUnits) {
			embeddedUnit = new CUnit();
			embeddedUnit.setUnitFilename(fileName);
			embeddedUnit.createEntity();
		}
        /*
		 * CUnit.createEntity sets type. Now that we've bootstrapped the
		 * type in, we know if we need to set piloting and gunnery (meks,
		 * vehicles) or just gunnery (misc. infantry types).
		 */
		if (!hidden) {
			int factionGunnery = campaign.getPlayer().getMyHouse().getBaseGunner();
			int factionPiloting = campaign.getPlayer().getMyHouse().getBasePilot();
			if ((embeddedUnit.getType() == Unit.MEK) || (embeddedUnit.getType() == Unit.VEHICLE)) {
                embeddedUnit.setPilot(new Pilot("BM Unit",factionGunnery,factionPiloting));
            } else {
                embeddedUnit.setPilot(new Pilot("BM Unit",factionGunnery,5));
            }

			/*
			 * BlackMarketModel uses MegaMek's calculateBV() function instead of pulling the
			 * stringed CUnit BV. This is because the server sends over values which reflect
			 * the current BV of a unit, not the BV it would have with a generic faction pilot.
			 *
			 * As such, we need to set the crew. See BlackMarketModel.java for usage.
			 */
			embeddedUnit.getEntity().setCrew(new megamek.common.Crew(CrewType.SINGLE, "Generic Pilot", 1, factionGunnery, factionGunnery, factionGunnery, factionPiloting, RandomGenderGenerator.generate(), null));
		}
	}

	//METHODS
	/*
	 * Methods are all simple get()'s used by the various BM classes
	 * (BMPanel, etc) to draw misc. info out of the CBMUnit.
	 *
	 * BMUnits are completely replaced whenever data is refreshed. No
	 * need for setters or oany way to change the stored values.
	 */
	public int getAuctionID() {
		return auctionID;
	}

	public int getUnitID() {
		return unitID;
	}

	public String getFileName() {
		return fileName;
	}

	public String getModelName() {
		return modelName;
	}

	public int getTicks() {
		return salesTicksRemaining;
	}

	public void decrementSalesTicks() {
		salesTicksRemaining--;
	}

	public int getMinBid() {
		return minBid;
	}

	public String getHiddenUnitDescription() {
		return unitWeight + " " + unitType;
	}

	public boolean playerIsSeller() {
		return soldByPlayer;
	}

	public int getBid() {
		return playersBid;
	}

	public CUnit getEmbeddedUnit() {
		return embeddedUnit;
	}

}//end CBMUnit.java