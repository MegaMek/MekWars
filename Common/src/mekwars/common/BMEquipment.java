/*
 * MekWars - Copyright (C) 2007
 *
 * Original author - Torren (torren@users.sourceforge.net)
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

package mekwars.common;

import megamek.common.TechConstants;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;

/**
 * Represents a single equipment item as listed on the "black market" (BM) — i.e. a part salvaged/available for
 * sale, derived from an actual MegaMek {@link EquipmentType} (or a non-equipment part such as armor/structure/an
 * engine that has no proper {@code EquipmentType} in MegaMek).
 * <p>
 * Unlike {@link Equipment} (a factory build-table catalog entry with a configurable min/max cost range), this
 * class represents one concrete listing with a single cost/amount, and lazily classifies itself into one of the
 * {@code PART_*} categories and a tech base ("Clan"/"IS"/"All") the first time that information is requested. See
 * {@link #getEquipmentName()} and {@link #getTech(int)} for the classification logic.
 *
 * @see CBMUnit
 */
public class BMEquipment {

    /** Category label for ammunition parts. */
    static public String PART_AMMO = "Ammo";
    /** Category label for weapon parts. */
    static public String PART_WEAPON = "Weapons";
    /** Category label for anything that isn't ammo/weapon/armor (actuators, cockpit, sensors, engines, etc.). */
    static public String PART_MISC = "Misc";
    /** Category label for armor and internal structure parts. */
    static public String PART_ARMOR = "Armor";
    /** Internal (MegaMek) name used to look up the corresponding {@link EquipmentType}, if one exists. */
    private String equipmentInternalName = "";
    /** Display name; computed lazily by {@link #getEquipmentName()} if left blank. */
    private String equipmentName = "";
    /** Listed cost of this part. */
    private double cost = 0;
    /** Quantity of this part available/listed. */
    private int amount = 0;
    /** Flag indicating the cost has increased (e.g. due to repeated purchase, market fluctuation, etc.). */
    private boolean costUp = false;
    /** One of the {@code PART_*} constants; computed lazily alongside {@link #equipmentName}. */
    private String equipmentType = "";
    /** Tech base label ("Clan", "IS" or "All"); computed lazily by {@link #getTech(int)}. */
    private String tech = "";
    /** MegaMek tech level constant corresponding to {@link #tech}; computed lazily by {@link #getTech(int)}. */
    private int techLevel = TechConstants.T_ALL;

    /**
     * @return the internal (MegaMek) equipment name.
     */
    public String getEquipmentInternalName() {
        return equipmentInternalName;
    }

    /**
     * @param name the internal (MegaMek) equipment name to set.
     */
    public void setEquipmentInternalName(String name) {
        equipmentInternalName = name;
    }

    /**
     * Returns the display name, computing (and caching, as a side effect) it and {@link #equipmentType} the first
     * time this is called if {@link #equipmentName} is still blank.
     * <p>
     * Resolution logic: looks up {@link #equipmentInternalName} via {@link EquipmentType#get(String)}. If no
     * {@code EquipmentType} exists (true for armor, internal structure, engines, actuators, cockpits, sensors —
     * anything that isn't a "normal" MegaMek equipment object), the internal name is used as-is for the display
     * name, and the part is classified as {@link #PART_ARMOR} if its name mentions "armor"/"IS (STD)" or (always
     * false here, since {@code eq} is {@code null}) {@code EquipmentType.getArmorType(eq)}/
     * {@code getStructureType(eq)} report a known type, otherwise {@link #PART_MISC}.
     * <p>
     * If an {@code EquipmentType} is found, its proper name is used, and the part is classified as
     * {@link #PART_AMMO} for {@link AmmoType}, {@link #PART_WEAPON} for {@link WeaponType} (appending
     * {@code " (BA)"} to the name if it is a battle-armor weapon), otherwise {@link #PART_ARMOR} or
     * {@link #PART_MISC} following the same "contains armor" / armor-or-structure-type heuristic as above.
     * <p>
     * Note: the {@code EquipmentType.getArmorType(eq)} / {@code getStructureType(eq)} checks in the {@code eq ==
     * null} branch are always evaluated with a {@code null} argument, so they can never actually detect an armor
     * or structure type there — that branch effectively only classifies by the "armor"/"IS (STD)" name check.
     *
     * @return the (possibly just-computed) display name.
     */
    public String getEquipmentName() {

        if (equipmentName.trim().isEmpty()) {
            EquipmentType eq = EquipmentType.get(getEquipmentInternalName());

            // Armor,IS,Engines,Actuators,Cockpit,Sensors anything that doesn't
            // make a normal object in MM
            if (eq == null) {
                setEquipmentName(getEquipmentInternalName());

                if ((getEquipmentName().toLowerCase().contains("armor")) ||
                          getEquipmentName().equalsIgnoreCase("IS (STD)") ||
                          (EquipmentType.getArmorType(eq) != EquipmentType.T_ARMOR_UNKNOWN) ||
                          (EquipmentType.getStructureType(eq) != EquipmentType.T_STRUCTURE_UNKNOWN)) {
                    setEquipmentType(BMEquipment.PART_ARMOR);
                } else {
                    setEquipmentType(BMEquipment.PART_MISC);
                }
            } else {

                setEquipmentName(eq.getName());

                if (eq instanceof AmmoType) {
                    setEquipmentType(BMEquipment.PART_AMMO);
                } else if (eq instanceof WeaponType) {
                    setEquipmentType(BMEquipment.PART_WEAPON);
                    if (eq.hasFlag(WeaponType.F_BA_WEAPON)) {
                        setEquipmentName(eq.getName() + " (BA)");
                    }
                } else if ((getEquipmentName().toLowerCase().contains("armor")) ||
                                 (EquipmentType.getArmorType(eq) != EquipmentType.T_ARMOR_UNKNOWN) ||
                                 (EquipmentType.getStructureType(eq) != EquipmentType.T_STRUCTURE_UNKNOWN)) {
                    setEquipmentType(BMEquipment.PART_ARMOR);
                } else {
                    setEquipmentType(BMEquipment.PART_MISC);
                }
            }

        }
        return equipmentName;
    }

    /**
     * @param name the display name to set (also used as a cache by {@link #getEquipmentName()}).
     */
    public void setEquipmentName(String name) {
        equipmentName = name;
    }

    /**
     * @return the part category, one of the {@code PART_*} constants. Empty until {@link #getEquipmentName()} (or
     *       {@link #setEquipmentType(String)}) has been called at least once.
     */
    public String getEquipmentType() {
        return equipmentType;
    }

    /**
     * @param type the part category to set, normally one of the {@code PART_*} constants.
     */
    public void setEquipmentType(String type) {
        equipmentType = type;
    }

    /**
     * @return the listed cost of this part.
     */
    public double getCost() {

        return cost;
    }

    /**
     * @param cost the cost to set.
     */
    public void setCost(double cost) {

        this.cost = cost;
    }

    /**
     * @return the quantity of this part listed/available.
     */
    public int getAmount() {

        return amount;
    }

    /**
     * @param amount the quantity to set.
     */
    public void setAmount(int amount) {

        this.amount = amount;
    }

    /**
     * @return {@code true} if this part's cost is flagged as having gone up.
     */
    public boolean isCostUp() {
        return costUp;
    }

    /**
     * @param update the cost-up flag to set.
     */
    public void setCostUp(boolean update) {
        costUp = update;
    }

    /**
     * @return the MegaMek tech-level constant for this part, valid only after {@link #getTech(int)} has been
     *       called (otherwise still the default {@link TechConstants#T_ALL}).
     */
    public int getTechLevel() {
        return techLevel;
    }

    /**
     * Returns the tech base label for this part ("Clan", "IS" or "All"), computing and caching it (along with
     * {@link #techLevel}) on first call.
     * <p>
     * If {@link #equipmentInternalName} does not resolve to a real {@link EquipmentType} (armor, structure,
     * engines, etc.), tech is inferred from the internal name: engine names starting with "Clan" are classified
     * Clan-tech, engine names starting with "IS" are classified IS-tech, everything else defaults to "All".
     * Otherwise the tech base is derived from {@code eq.getTechLevel(year)} for the given in-universe {@code
     * year}: any Clan tech-level variant maps to "Clan", {@code T_ALL} or anything below
     * {@code T_INTRO_BOX_SET} maps to "All", and everything else maps to "IS".
     * <p>
     * Once {@link #tech} has been set (non-blank), subsequent calls return the cached value regardless of the
     * {@code year} argument passed in — i.e. the tech base is effectively "frozen" to whichever year it was first
     * computed for.
     *
     * @param year the in-universe year used to resolve the equipment's tech level, only used on first call.
     *
     * @return the cached or newly computed tech base label.
     */
    public String getTech(int year) {
        if (!tech.trim().isEmpty()) {
            return tech;
        }

        EquipmentType eq = EquipmentType.get(getEquipmentInternalName());

        if (eq == null) {
            if ((getEquipmentInternalName().indexOf("Engine") > 0) && getEquipmentInternalName().startsWith("Clan")) {
                tech = "Clan";
                techLevel = TechConstants.T_CLAN_TW;
            } else if ((getEquipmentInternalName().indexOf("Engine") > 0) &&
                             getEquipmentInternalName().startsWith("IS")) {
                tech = "IS";
                techLevel = TechConstants.T_IS_TW_ALL;
            } else {
                tech = "All";
                techLevel = TechConstants.T_ALL;
            }
        } else {
            if ((eq.getTechLevel(year) == TechConstants.T_CLAN_ADVANCED) ||
                      (eq.getTechLevel(year) == TechConstants.T_CLAN_EXPERIMENTAL) ||
                      (eq.getTechLevel(year) == TechConstants.T_CLAN_TW) ||
                      (eq.getTechLevel(year) == TechConstants.T_CLAN_UNOFFICIAL)) {
                tech = "Clan";
            } else if ((eq.getTechLevel(year) == TechConstants.T_ALL) ||
                             (eq.getTechLevel(year) < TechConstants.T_INTRO_BOX_SET)) {
                tech = "All";
            } else {
                tech = "IS";
            }
            techLevel = eq.getTechLevel(year);
        }

        return tech;
    }


    /**
     * Creates a deep-enough copy of this listing: a new {@code BMEquipment} with the same amount, cost, cost-up
     * flag, internal/display name and part type, with {@link #getTech(int)} invoked for the given {@code year} so
     * the clone's tech base/level are (re)computed independently rather than copied verbatim.
     *
     * @param year the in-universe year to resolve the clone's tech base for.
     *
     * @return the newly created clone.
     */
    public BMEquipment clone(int year) {
        BMEquipment clone = new BMEquipment();

        clone.setAmount(getAmount());
        clone.setCost(getCost());
        clone.setCostUp(isCostUp());
        clone.setEquipmentInternalName(getEquipmentInternalName());
        clone.setEquipmentName(getEquipmentName());
        clone.setEquipmentType(getEquipmentType());
        clone.getTech(year);

        return clone;
    }

}
