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

import megamek.client.generator.RandomGenderGenerator;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import mekwars.common.Unit;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.util.TokenReader;

/**
 * client-side market unit. The market uses the filenames and other data from the CBMUnit to generate temporary CUnits
 * (to determine BV, etc.).
 * <p>
 * In the past, this class extended CUnit. This is no longer the case. Using minimal data (not sending ammo settings and
 * complete unit strings) saves bandwidth. Instead, we build a temporary CUnit and store it w/i the BMUnit.
 */
public class CBMUnit {

    CUnit embeddedUnit = null;
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

    //CONSTRUCTOR

    /**
     * Constructor that takes a data string from the server. This String is generated in Market2's getAutoMarketStatus()
     * method.
     * <p>
     * Be sure that the token read-in order always matches the market's write-out order.
     */
    public CBMUnit(String listingData, CCampaign campaign, boolean hiddenUnits) {

        //read data
        java.util.StringTokenizer ST = new java.util.StringTokenizer(listingData, "*");
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
                        RandomGenderGenerator.generate(),
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
        return STR."\{unitWeight} \{unitType}";
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
