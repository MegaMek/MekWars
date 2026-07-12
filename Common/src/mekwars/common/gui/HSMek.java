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

/**
 * Lightweight, read-only summary of a single unit belonging to a faction/house, used by the House Status screen
 * (see {@code CHSPanel}) to display a faction's roster (name, type, battle value, damage state) without needing
 * the full army/pilot data the server holds. Instances are constructed directly from a tokenized network message
 * (see {@link #HSMek(StringTokenizer)}), and internally wrap a {@link CUnit} built from the referenced unit file so
 * that MegaMek's own battle-value calculation ({@link Entity#calculateBattleValue()}) can be reused instead of
 * relying on a BV value serialized by the server.
 */
public class HSMek {

    /** Filename (relative unit data file) describing the unit's chassis/loadout. */
    String MekFile;
    /** Server-assigned unique ID of this unit. */
    int unitID;

    /** Display name derived from the unit (chassis/model, or short name for non-Mek unit types). */
    String name;
    /** Coarse unit-type label: one of "Mek", "ProtoMek", "BattleArmor", "Infantry", or "Vehicle" (the default). */
    String type;
    /** Free-form battle damage description, if the server supplied one; empty string if not. */
    String battleDamage = "";

    /** The actual MegaMek entity, wrapped in a {@link CUnit}, used to answer BV/entity queries. Buried here the
     * same way BMUnit embeds a CUnit elsewhere in the codebase. */
    CUnit embeddedUnit;//bury a CUnit in HSMek, a la BMUnit

    /**
     * Parses one unit's worth of House Status data out of a tokenized network message, builds the corresponding
     * {@link CUnit}/{@link Entity}, and assigns it a generic single-person crew using the faction's default
     * gunnery/piloting skill values (the server does not send per-pilot data for this screen). Also derives the
     * display {@link #name} and {@link #type} from the resulting entity.
     * <p>
     * Skill handling: ProtoMeks always get piloting skill 5 regardless of the supplied {@code factionPiloting}.
     * Infantry get the supplied piloting skill only if the unit {@link Infantry#canMakeAntiMekAttacks()}; otherwise
     * they too are forced to piloting skill 5. All other unit types use the supplied gunnery/piloting values as-is.
     *
     * @param tokenizer tokenized message containing, in order: unit filename, unit ID, faction gunnery skill,
     *                  faction piloting skill, and optionally (if more tokens remain) a battle-damage description
     */
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
        // NOTE: despite the comment above, this branch actually only special-cases Meks (chassis+model for
        // omni units, otherwise model-or-chassis); everything else (including vehicles/infantry) falls through
        // to the else branch and uses getShortNameRaw() instead. The comment appears stale/inaccurate.
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

    /**
     * @return the underlying MegaMek {@link Entity} constructed from this unit's data file.
     */
    public Entity getEntity() {
        return embeddedUnit.getEntity();
    }

    /**
     * @return the relative unit data filename this summary was built from.
     */
    public String getMekFile() {
        return MekFile;
    }

    /**
     * @return the display name for this unit (see constructor for derivation rules).
     */
    public String getName() {
        return name;
    }

    /**
     * @return the coarse unit-type label ("Mek", "ProtoMek", "BattleArmor", "Infantry", or "Vehicle").
     */
    public String getType() {
        return type;
    }

    /**
     * @return the server-assigned unique ID of this unit.
     */
    public int getUnitID() {
        return unitID;
    }

    /**
     * @return a free-form description of battle damage sustained, or an empty string if none was supplied.
     */
    public String getBattleDamage() {
        return battleDamage;
    }

    /**
     * Computes the unit's battle value on demand using MegaMek's own calculation (rather than trusting any BV
     * value the server might have sent), based on the generic faction-default crew assigned in the constructor.
     *
     * @return the calculated battle value of the underlying entity
     */
    public int getBV() {
        return embeddedUnit.getEntity().calculateBattleValue();
    }

}
