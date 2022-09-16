/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

package client.gui;

import java.util.StringTokenizer;

import client.MWClient;
import client.campaign.CUnit;
import common.Unit;
import common.campaign.pilot.Pilot;
import common.util.TokenReader;
import megamek.client.generator.RandomGenderGenerator;
import megamek.common.BattleArmor;
import megamek.common.CrewType;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.QuadMech;

public class HSMek {

	String MekFile;
	int unitID;

	String name;
	String type;
	String battleDamage = "";

	CUnit embeddedUnit;//bury a CUnit in HSMek, a la BMUnit

	public HSMek(MWClient mwclient, StringTokenizer tokenizer) {

		MekFile = TokenReader.readString(tokenizer);
		unitID =  TokenReader.readInt(tokenizer);

        int factionGunnery = TokenReader.readInt(tokenizer);
        int factionPiloting = TokenReader.readInt(tokenizer);

        if (tokenizer.hasMoreTokens()) {
            battleDamage = TokenReader.readString(tokenizer);
        }

		//bury a CUnit
		embeddedUnit = new CUnit();
		embeddedUnit.setUnitFilename(MekFile);
		embeddedUnit.createEntity();

        /*
		 * CUnit.createEntity sets type. Now that we've bootstrapped the
		 * type in, we know if we need to set piloting and gunnery (meks,
		 * vehicles) or just gunnery (misc. infantry types).
		 */
		if (embeddedUnit.getType() != Unit.PROTOMEK ) {
            if ( embeddedUnit.getType() == Unit.INFANTRY  ){
		        if ( ((Infantry)embeddedUnit.getEntity()).canMakeAntiMekAttacks() ) {
                    embeddedUnit.setPilot(new Pilot("BM Unit",factionGunnery,factionPiloting));
                } else {
                    embeddedUnit.setPilot(new Pilot("BM Unit",factionGunnery,5));
                }
		    } else {
                embeddedUnit.setPilot(new Pilot("BM Unit",factionGunnery,factionPiloting));
            }
        } else {
            embeddedUnit.setPilot(new Pilot("BM Unit",factionGunnery,5));
        }

		/*
		 * HSMek.getBV() uses MegaMek's calculateBV() function instead of pulling the
		 * stringed CUnit BV. The server sends over units without pilot data, so we set
		 * a faction-default crew. See CHSPanel.java for usage.
		 */
		embeddedUnit.getEntity().setCrew(new megamek.common.Crew(CrewType.SINGLE, "Generic Pilot", 1, factionGunnery, factionGunnery, factionGunnery, factionPiloting, RandomGenderGenerator.generate(), null));

		//set type
		Entity e = embeddedUnit.getEntity();
		if ((e instanceof Mech) || (e instanceof QuadMech)) {
            type = "Mek";
        } else if (e instanceof Protomech) {
            type = "Protomek";
        } else if (e instanceof BattleArmor) {
            type = "BattleArmor";
        } else if (e instanceof Infantry) {
            type = "Infantry";
        } else {
            type = "Vehicle";
        }

		//vehicles and inf prepend chassis
		if (type.equalsIgnoreCase("Mek")) {
            if (e.isOmni()) {
		        name = e.getChassis() + " " +  e.getModel();
		    }
		    else {
	            if ( e.getModel().trim().length() > 0 ){
	                name = e.getModel().trim();
	            }
	            else{
	                name = e.getChassis().trim();
	            }
		    }
        } else {
            name = e.getShortNameRaw();
        }
	}

	public Entity getEntity() {
		return embeddedUnit.getEntity();
	}

	public String getMekFile() {
		return MekFile;
	}

	public String getName() {
		return name.toString();
	}

	public String getType() {
		return type;
	}

	public int getUnitID() {
		return unitID;
	}

    public String getBattleDamage() {
        return battleDamage;
    }

    public int getBV() {
    	return embeddedUnit.getEntity().calculateBattleValue();
    }

}
