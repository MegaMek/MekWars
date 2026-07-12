/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


package mekwars.common.campaign;

import java.util.StringTokenizer;

import megamek.common.enums.Gender;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import mekwars.common.Unit;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.util.TokenReader;

/**
 * Client-side market unit. Represents a single lot on the "Black Market" auction/trading system: a unit listed for
 * sale (either by the server/faction or by another {@link CPlayer}) that this client can view and bid on. The market
 * uses the filenames and other data from the CBMUnit to generate temporary CUnits (to determine BV, etc.).
 * <p>
 * In the past, this class extended CUnit. This is no longer the case. Using minimal data (not sending ammo settings and
 * complete unit strings) saves bandwidth. Instead, we build a temporary CUnit and store it w/i the BMUnit.
 * <p>
 * Instances are owned/collected by {@link CCampaign} in its Black Market map (see {@link CCampaign#setBMData},
 * {@link CCampaign#addBMUnit}, {@link CCampaign#changeBMUnit}), keyed by auction ID. CBMUnit objects are immutable
 * value snapshots aside from the sales-tick countdown: whenever the server sends updated listing data, the old
 * CBMUnit is discarded and a new one built from the fresh string (see the class comment on the getters below).
 */
public class CBMUnit {

    //IVARS
    /** Whether this listing was put up for sale by another player (true) or is a server/faction-generated listing (false). */
    private final boolean soldByPlayer;
    private final String modelName;
    private final String fileName;
    /** Unique ID of this auction listing (used as the key in {@link CCampaign}'s Black Market map). */
    private final int auctionID;
    /** ID of the underlying unit being auctioned. */
    private final int unitID;
    private final int minBid;
    /** The current player's own bid amount on this listing, if any. */
    private final int playersBid;
    private final String unitWeight;
    private final String unitType;
    /**
     * A locally-constructed, throwaway {@link CUnit} built from this listing's filename, used purely to compute
     * derived display data (BV, weight class, etc.) via MegaMek's Entity APIs. Will be {@code null} whenever
     * {@code hiddenUnits} was {@code true} at construction time (server config hides BM unit identities), so callers
     * of {@link #getEmbeddedUnit()} must handle a null result.
     */
    CUnit embeddedUnit = null;
    /** Number of campaign ticks remaining before this auction closes; decremented externally via {@link #decrementSalesTicks()}. */
    private int salesTicksRemaining;

    //CONSTRUCTOR

    /**
     * Constructor that takes a data string from the server. This String is generated in Market2's getAutoMarketStatus()
     * method.
     * <p>
     * Be sure that the token read-in order always matches the market's write-out order.
     * <p>
     * When {@code hiddenUnits} is {@code false}, this also builds a throwaway {@link CUnit} from the listing's
     * filename (to compute BV/weight/etc. for display) and assigns it a generic pilot using the current player's
     * faction base gunnery/piloting skills ({@code getMyHouse().getBaseGunner()/getBasePilot()}); non-Mek,
     * non-Vehicle units (e.g. infantry, ProtoMeks) get a hardcoded piloting skill of 5 since it is not meaningful for
     * them. A matching MegaMek {@link Crew} is then attached to the embedded unit's Entity so that
     * {@code calculateBV()} can be used downstream (see BlackMarketModel.java) instead of trusting a stale
     * server-sent BV string, since the server's listed BV reflects the seller's actual pilot, not a generic one.
     * When {@code hiddenUnits} is {@code true}, none of this happens and {@link #embeddedUnit} stays {@code null}.
     *
     * @param listingData a single unit's "*"-delimited listing fields, extracted from the larger market status string.
     * @param campaign the client's campaign, used to look up the current player's faction base skills.
     * @param hiddenUnits if true, suppresses building the embedded CUnit/Entity/Crew (server is configured to hide unit identities on the BM).
     */
    public CBMUnit(String listingData, CCampaign campaign, boolean hiddenUnits) {

        //read data
        StringTokenizer stringTokenizer = new StringTokenizer(listingData, "*");
        auctionID = TokenReader.readInt(stringTokenizer);
        unitID = TokenReader.readInt(stringTokenizer);
        modelName = TokenReader.readString(stringTokenizer);
        fileName = TokenReader.readString(stringTokenizer);
        salesTicksRemaining = TokenReader.readInt(stringTokenizer);
        minBid = TokenReader.readInt(stringTokenizer);
        soldByPlayer = TokenReader.readBoolean(stringTokenizer);
        playersBid = TokenReader.readInt(stringTokenizer);
        unitType = TokenReader.readString(stringTokenizer);
        unitWeight = TokenReader.readString(stringTokenizer);

        //bury a CUnit
        if (!hiddenUnits) {
            embeddedUnit = new CUnit();
            embeddedUnit.setUnitFilename(fileName);
            embeddedUnit.createEntity();
        }
        /*
         * CUnit.createEntity sets type. Now that we've bootstrapped the type in, we know if we need to set piloting
         * and gunnery (meks, vehicles) or just gunnery (misc. infantry types).
         */
        if (!hiddenUnits) {
            int factionGunnery = campaign.getPlayer().getMyHouse().getBaseGunner();
            int factionPiloting = campaign.getPlayer().getMyHouse().getBasePilot();

            if ((embeddedUnit.getType() == Unit.MEK) || (embeddedUnit.getType() == Unit.VEHICLE)) {
                embeddedUnit.setPilot(new Pilot("BM Unit", factionGunnery, factionPiloting));
            } else {
                embeddedUnit.setPilot(new Pilot("BM Unit", factionGunnery, 5));
            }

            /*
             * BlackMarketModel uses MegaMek's calculateBV() function instead of pulling the stringed CUnit BV. This
             * is because the server sends over values which reflect the current BV of a unit, not the BV it would
             * have with a generic faction pilot.
             *
             * As such, we need to set the crew. See BlackMarketModel.java for usage.
             */
            embeddedUnit.getEntity()
                  .setCrew(new Crew(CrewType.SINGLE,
                        "Generic Pilot",
                        1,
                        factionGunnery,
                        factionGunnery,
                        factionGunnery,
                        factionPiloting,
                        Gender.RANDOMIZE,
                        false,
                        null));
        }
    }

    /*
     * Methods are all simple get()'s used by the various BM classes (BMPanel, etc.) to draw misc. info out of the
     * CBMUnit.
     *
     * BMUnits are completely replaced whenever data is refreshed. No need for setters or any way to change the
     * stored values.
     */
    /** @return the unique ID of this auction listing. */
    public int getAuctionID() {
        return auctionID;
    }

    /** @return the ID of the underlying unit being auctioned. */
    public int getUnitID() {
        return unitID;
    }

    /** @return the unit's data/blueprint filename, used to build the embedded temporary CUnit. */
    public String getFileName() {
        return fileName;
    }

    /** @return the display model name of the unit (e.g. chassis/model). */
    public String getModelName() {
        return modelName;
    }

    /** @return the number of campaign ticks remaining before this auction closes. */
    public int getTicks() {
        return salesTicksRemaining;
    }

    /** Decrements the remaining sales-tick countdown by one; called once per campaign tick from {@link CCampaign}. */
    public void decrementSalesTicks() {
        salesTicksRemaining--;
    }

    /** @return the minimum bid required for this listing. */
    public int getMinBid() {
        return minBid;
    }

    /**
     * @return a human-readable weight/type description (e.g. "Heavy Mek") for use when unit identities are hidden
     *       by server config and the full embedded unit isn't available.
     */
    public String getHiddenUnitDescription() {
        return String.format("%s %s", unitWeight, unitType);
    }

    /** @return true if this listing was put up for sale by another player rather than the server/faction. */
    public boolean playerIsSeller() {
        return soldByPlayer;
    }

    /** @return the current player's own bid amount on this listing (0/undefined semantics defined by the server). */
    public int getBid() {
        return playersBid;
    }

    /**
     * @return the throwaway {@link CUnit} built from this listing's filename for BV/stat display purposes, or
     *       {@code null} if this listing was constructed with {@code hiddenUnits == true}.
     */
    public CUnit getEmbeddedUnit() {
        return embeddedUnit;
    }

}//end CBMUnit.java
