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

package common;

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * Unit Equipment Container
 */
public class BMEquipment {

    private String equipmentInternalName = "";
    private String equipmentName = "";
    private double cost = 0;
    private int amount = 0;
    private boolean costUp = false;
    private String equipmentType = "";
    private String tech = "";
    private int techLevel = TechConstants.T_ALL;

    static public String PART_AMMO = "Ammo";
    static public String PART_WEAPON = "Weapons";
    static public String PART_MISC = "Misc";
    static public String PART_ARMOR = "Armor";

    public void setEquipmentInternalName(String name) {
        equipmentInternalName = name;
    }

    public String getEquipmentInternalName() {
        return equipmentInternalName;
    }

    public void setEquipmentName(String name) {
        equipmentName = name;
    }

    public String getEquipmentName() {

        if (equipmentName.trim().length() < 1) {
            EquipmentType eq = EquipmentType.get(getEquipmentInternalName());

            // Armor,IS,Engines,Actuators,Cockpit,Sensors anything that doesn't
            // make a normal object in MM
            if (eq == null) {
                setEquipmentName(getEquipmentInternalName());

                if ((getEquipmentName().toLowerCase().indexOf("armor") > -1) || getEquipmentName().equalsIgnoreCase("IS (STD)") || (EquipmentType.getArmorType(eq) != EquipmentType.T_ARMOR_UNKNOWN) || (EquipmentType.getStructureType(eq) != EquipmentType.T_STRUCTURE_UNKNOWN)) {
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
                } else if ((getEquipmentName().toLowerCase().indexOf("armor") > -1) || (EquipmentType.getArmorType(eq) != EquipmentType.T_ARMOR_UNKNOWN) || (EquipmentType.getStructureType(eq) != EquipmentType.T_STRUCTURE_UNKNOWN)) {
                    setEquipmentType(BMEquipment.PART_ARMOR);
                } else {
                    setEquipmentType(BMEquipment.PART_MISC);
                }
            }

        }
        return equipmentName;
    }

    public void setEquipmentType(String type) {
        equipmentType = type;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public void setCost(double cost) {

        this.cost = cost;
    }

    public double getCost() {

        return cost;
    }

    public void setAmount(int amount) {

        this.amount = amount;
    }

    public int getAmount() {

        return amount;
    }

    public boolean isCostUp() {
        return costUp;
    }

    public void setCostUp(boolean update) {
        costUp = update;
    }

    public int getTechLevel() {
        return techLevel;
    }

    public String getTech(int year) {
    	if (tech.trim().length() > 0) {
            return tech;
        }

        EquipmentType eq = EquipmentType.get(getEquipmentInternalName());

        if (eq == null) {
            if ((getEquipmentInternalName().indexOf("Engine") > 0) && getEquipmentInternalName().startsWith("Clan")) {
                tech = "Clan";
                techLevel = TechConstants.T_CLAN_TW;
            } else if ((getEquipmentInternalName().indexOf("Engine") > 0) && getEquipmentInternalName().startsWith("IS")) {
                tech = "IS";
                techLevel = TechConstants.T_IS_TW_ALL;
            } else {
                tech = "All";
                techLevel = TechConstants.T_ALL;
            }
        } else {
            if ((eq.getTechLevel(year) == TechConstants.T_CLAN_ADVANCED) || (eq.getTechLevel(year) == TechConstants.T_CLAN_EXPERIMENTAL) || (eq.getTechLevel(year) == TechConstants.T_CLAN_TW) || (eq.getTechLevel(year) == TechConstants.T_CLAN_UNOFFICIAL)) {
                tech = "Clan";
            } else if ((eq.getTechLevel(year) == TechConstants.T_ALL) || (eq.getTechLevel(year) < TechConstants.T_INTRO_BOXSET)) {
                tech = "All";
            } else {
                tech = "IS";
            }
            techLevel = eq.getTechLevel(year);
        }

        return tech;
    }


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
