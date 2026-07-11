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
 * Client-side market unit. The market uses the filenames and other data from the CBMUnit to generate temporary CUnits
 * (to determine BV, etc.).
 * <p>
 * In the past, this class extended CUnit. This is no longer the case. Using minimal data (not sending ammo settings and
 * complete unit strings) saves bandwidth. Instead, we build a temporary CUnit and store it w/i the BMUnit.
 */
public class CBMUnit {

    //IVARS
    private final boolean soldByPlayer;
    private final String modelName;
    private final String fileName;
    private final int auctionID;
    private final int unitID;
    private final int minBid;
    private final int playersBid;
    private final String unitWeight;
    private final String unitType;
    CUnit embeddedUnit = null;
    private int salesTicksRemaining;

    //CONSTRUCTOR

    /**
     * Constructor that takes a data string from the server. This String is generated in Market2's getAutoMarketStatus()
     * method.
     * <p>
     * Be sure that the token read-in order always matches the market's write-out order.
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
        return String.format("%s %s", unitWeight, unitType);
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
