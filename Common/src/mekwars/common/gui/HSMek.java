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

package mekwars.common.gui;

import java.util.StringTokenizer;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.Gender;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import mekwars.common.Unit;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.util.TokenReader;

public class HSMek {

    String MekFile;
    int unitID;

    String name;
    String type;
    String battleDamage = "";

    CUnit embeddedUnit;//bury a CUnit in HSMek, a la BMUnit

    public HSMek(StringTokenizer tokenizer) {

        MekFile = TokenReader.readString(tokenizer);
        unitID = TokenReader.readInt(tokenizer);

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
        if (embeddedUnit.getType() != Unit.PROTOMEK) {
            if (embeddedUnit.getType() == Unit.INFANTRY) {
                if (((Infantry) embeddedUnit.getEntity()).canMakeAntiMekAttacks()) {
                    embeddedUnit.setPilot(new Pilot("BM Unit", factionGunnery, factionPiloting));
                } else {
                    embeddedUnit.setPilot(new Pilot("BM Unit", factionGunnery, 5));
                }
            } else {
                embeddedUnit.setPilot(new Pilot("BM Unit", factionGunnery, factionPiloting));
            }
        } else {
            embeddedUnit.setPilot(new Pilot("BM Unit", factionGunnery, 5));
        }

        /*
         * HSMek.getBV() uses MegaMek's calculateBV() function instead of pulling the
         * stringed CUnit BV. The server sends over units without pilot data, so we set
         * a faction-default crew. See CHSPanel.java for usage.
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

        //set type
        Entity entity = embeddedUnit.getEntity();
        switch (entity) {
            case Mek ignored -> type = "Mek";
            case ProtoMek ignored -> type = "ProtoMek";
            case BattleArmor ignored -> type = "BattleArmor";
            case Infantry ignored -> type = "Infantry";
            case null, default -> type = "Vehicle";
        }

        //vehicles and inf prepend chassis
        if (type.equalsIgnoreCase("Mek")) {
            if (entity.isOmni()) {
                name = String.format("%s %s", entity.getChassis(), entity.getModel());
            } else {
                if (!entity.getModel().trim().isEmpty()) {
                    name = entity.getModel().trim();
                } else {
                    name = entity.getChassis().trim();
                }
            }
        } else {
            name = entity.getShortNameRaw();
        }
    }

    public Entity getEntity() {
        return embeddedUnit.getEntity();
    }

    public String getMekFile() {
        return MekFile;
    }

    public String getName() {
        return name;
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
