/*
 * MekWars - Copyright (C) 2007
 *
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

/**
 * @author jtighe
 * This command is used to set Black Market Settings
 * for max/min cost and production.
 *
 */
package server.campaign.commands.admin;

import java.util.Enumeration;
import java.util.StringTokenizer;

import common.Equipment;
import common.util.UnitUtils;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.TechConstants;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

public class AutoFillBlackMarketSettingCommand implements Command {

    int accessLevel = IAuthenticator.ADMIN;
    String syntax = "Min Cost Modifer#Max Cost Modifer#Min Production#Max Production#Unit Weight(optional)";

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

    public void process(StringTokenizer command, String Username) {

        // access level check
        int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
            return;
        }

        // get config var and new setting
        double minCost = 1.0;
        double maxCost = 1.0;
        double maxCostMod = 1.0;
        double minCostMod = 1.0;
        double baseCost = 1.0;

        int year = CampaignMain.cm.getIntegerConfig("CampaignYear");

        Entity ent = UnitUtils.createOMG();

        int minProduction = 0;
        int maxProduction = 0;

        minCostMod = Double.parseDouble(command.nextToken());
        maxCostMod = Double.parseDouble(command.nextToken());
        minProduction = Integer.parseInt(command.nextToken());
        maxProduction = Integer.parseInt(command.nextToken());

        if (command.hasMoreElements()) {
            ent.setWeight(Float.parseFloat(command.nextToken()));
        }

        Enumeration<EquipmentType> list = EquipmentType.getAllTypes();
        double crits = 1;

        while (list.hasMoreElements()) {

            EquipmentType eq = list.nextElement();

            String key = eq.getInternalName();

            if (eq instanceof AmmoType) {
                crits = ((AmmoType) eq).getRackSize();
            } else if (isArmor(eq)) {
                crits = 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.getArmorType(eq));
            } else if (isStructure(eq)) {
                crits = 8;
            } else {
                try {
                    crits = eq.getCriticals(ent);
                } catch (Exception ex) {
                    crits = 1;
                }
            }

            crits = Math.max(crits, 1);
            baseCost = eq.getCost(ent, false, -1);

            if (isArmor(eq)) {
                baseCost = EquipmentType.getArmorCost(EquipmentType.getArmorType(eq));
            } else if (isStructure(eq)) {
                baseCost = EquipmentType.getStructureCost(EquipmentType.getStructureType(eq));
            } else if (eq instanceof MiscType) {
                if (eq.hasFlag(MiscType.F_HEAT_SINK) || eq.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                    if (eq.getName().equals("1 Compact Heat Sink")) {
                        baseCost = 3000;
                    } else if (eq.getName().equals("Heat Sink")) {
                        baseCost = 2000;
                    } else {
                        baseCost = 6000;
                    }
                } else if (eq.hasFlag(MiscType.F_JUMP_BOOSTER)) {
                    baseCost = 6.0 * ent.getWeight() * 150;
                } else if (eq.hasFlag(MiscType.F_JUMP_JET)) {
                    if (eq.getTechLevel(year) > TechConstants.T_IS_TW_ALL) {
                        baseCost = 6.0 * ent.getWeight() * 500;
                    } else {
                        baseCost = 6.0 * ent.getWeight() * 200;
                    }
                } else if (eq.hasFlag(MiscType.F_UMU)) {
                    baseCost = 6.0 * ent.getWeight() * 200;
                }
            }

            baseCost /= crits;
            baseCost = Math.max(0, baseCost);

            minCost = baseCost * minCostMod;
            maxCost = baseCost * maxCostMod;

            Equipment bme = CampaignMain.cm.getBlackMarketEquipmentTable().get(key);

            if (bme == null) {
                bme = new Equipment();
                bme.setEquipmentInternalName(eq.getInternalName());
            }

            bme.setMinCost(minCost);
            bme.setMaxCost(maxCost);
            if (maxCost <= 0) {
                bme.setMinProduction(0);
                bme.setMaxProduction(0);
            } else {
                bme.setMinProduction(minProduction);
                bme.setMaxProduction(maxProduction);
            }
            CampaignMain.cm.getBlackMarketEquipmentTable().put(key, bme);
        }

        Equipment bme = new Equipment();
        bme.setEquipmentInternalName("Armor (STD)");
        baseCost = EquipmentType.getStructureCost(EquipmentType.T_ARMOR_STANDARD);
        baseCost /= 16;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName("IS (STD)");
        baseCost = EquipmentType.getStructureCost(EquipmentType.T_STRUCTURE_STANDARD) * ent.getWeight();
        baseCost /= 8;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(Mech.systemNames[Mech.SYSTEM_LIFE_SUPPORT]);
        baseCost = 25000;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(Mech.systemNames[Mech.SYSTEM_SENSORS]);
        baseCost = 1000 * ent.getWeight();
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(Mech.getCockpitTypeString(Mech.COCKPIT_TORSO_MOUNTED));
        baseCost = 750000;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(Mech.getCockpitTypeString(Mech.COCKPIT_COMMAND_CONSOLE));
        baseCost = 700000;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(Mech.getCockpitTypeString(Mech.COCKPIT_DUAL));
        baseCost = 700000;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(Mech.getCockpitTypeString(Mech.COCKPIT_STANDARD));
        baseCost = 200000;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(Mech.getCockpitTypeString(Mech.COCKPIT_SMALL));
        baseCost = 175000;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(Mech.getCockpitTypeString(Mech.COCKPIT_SMALL));
        baseCost = 175000;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName("Actuator");
        baseCost = 100 * ent.getWeight();
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(Mech.getGyroTypeString(Mech.GYRO_STANDARD));
        baseCost = 300000 * (int) Math.ceil((ent.getOriginalWalkMP() * ent.getWeight()) / 100f);
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(Mech.getGyroTypeString(Mech.GYRO_HEAVY_DUTY));
        baseCost = 500000 * (int) Math.ceil((ent.getOriginalWalkMP() * ent.getWeight()) / 100f) * 2;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(Mech.getGyroTypeString(Mech.GYRO_XL));
        baseCost = 750000 * (int) Math.ceil((ent.getOriginalWalkMP() * ent.getWeight()) / 100f) * 0.5;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(Mech.getGyroTypeString(Mech.GYRO_COMPACT));
        baseCost = 400000 * (int) Math.ceil((ent.getOriginalWalkMP() * ent.getWeight()) / 100f) * 1.5;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(UnitUtils.ENGINE_TECH_STRING[UnitUtils.STANDARD_ENGINE]);
        baseCost = (5000 * ent.getEngine().getRating() * ent.getWeight()) / 75.0;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(UnitUtils.ENGINE_TECH_STRING[UnitUtils.IS_LIGHT_ENGINE]);
        baseCost = (15000 * ent.getEngine().getRating() * ent.getWeight()) / 75.0;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(UnitUtils.ENGINE_TECH_STRING[UnitUtils.IS_XL_ENGINE]);
        baseCost = (20000 * ent.getEngine().getRating() * ent.getWeight()) / 75.0;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(UnitUtils.ENGINE_TECH_STRING[UnitUtils.IS_XXL_ENGINE]);
        baseCost = (100000 * ent.getEngine().getRating() * ent.getWeight()) / 75.0;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(UnitUtils.ENGINE_TECH_STRING[UnitUtils.CLAN_XL_ENGINE]);
        baseCost = (20000 * ent.getEngine().getRating() * ent.getWeight()) / 75.0;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName(UnitUtils.ENGINE_TECH_STRING[UnitUtils.CLAN_XXL_ENGINE]);
        baseCost = (100000 * ent.getEngine().getRating() * ent.getWeight()) / 75.0;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName("ISTargeting Computer");
        baseCost = 10000;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        bme = new Equipment();
        bme.setEquipmentInternalName("CLTargeting Computer");
        baseCost = 10000;
        minCost = baseCost * minCostMod;
        maxCost = baseCost * maxCostMod;
        bme.setMinCost(minCost);
        bme.setMaxCost(maxCost);
        bme.setMinProduction(minProduction);
        bme.setMaxProduction(maxProduction);
        CampaignMain.cm.getBlackMarketEquipmentTable().put(bme.getEquipmentInternalName(), bme);

        CampaignMain.cm.toUser("AM:Done setting equipment costs for the black market.", Username);
    }// end process

    private boolean isArmor(EquipmentType eq) {
        for (String armor : EquipmentType.armorNames) {
            if (eq.getName().equalsIgnoreCase(armor)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStructure(EquipmentType eq) {
        for (String IS : EquipmentType.structureNames) {
            if (eq.getName().equalsIgnoreCase(IS)) {
                return true;
            }
        }
        return false;
    }
}