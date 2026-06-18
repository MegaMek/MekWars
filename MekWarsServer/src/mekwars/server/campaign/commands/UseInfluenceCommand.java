/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Jason Tighe (Torren)
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

package mekwars.server.campaign.commands;

import java.util.StringTokenizer;
import java.util.Vector;

import megamek.codeUtilities.MathUtility;
import megamek.common.CriticalSlot;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;
import mekwars.common.Unit;
import mekwars.common.util.StringUtils;
import mekwars.common.util.UnitUtils;
import mekwars.server.MWChatServer.auth.AccessRole;
import mekwars.server.campaign.BuildTable;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SHouse;
import mekwars.server.campaign.SPlayer;
import mekwars.server.campaign.SUnit;
import mekwars.server.campaign.SUnitFactory;
import mekwars.server.campaign.pilot.SPilot;

/**
 *
 * @author Salient allows users to redeem influence, copy of userewardpoints
 *
 */
public class UseInfluenceCommand implements Command {
    private final static MMLogger LOGGER = MMLogger.getLogger(UseInfluenceCommand.class);

    AccessRole accessLevel = AccessRole.NONE;
    String syntax = "";

    public void process(StringTokenizer command, String Username) {
        if (accessLevel != AccessRole.NONE) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser(STR."AM:Insufficient access level for command. Level: \{userLevel}. Required: \{accessLevel}.",
                      Username,
                      true);
                return;
            }
        }

        /*
         * rewardSelections:
         * 0 Techs
         * 1 Influence
         * 2 Units
         * 3 Repair
         * 4 C-bills
         */

        int influence = 0;

        int rewardSelection = MathUtility.parseInt(command.nextToken(), -1);
        int rewardPoints;
        String techs = "";
        String rewards = "";

        SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
        SHouse house = player.getMyHouse();
        // Salient - added additional case for RP for CBills

        if (rewardSelection < 0 || rewardSelection > 4) {
            CampaignMain.campaignMain.toUser(
                  "AM:Invalid reward selection. 0 for techs, 1 for influence, 2 for units, 3 for repair, 4 for CBills.",
                  Username,
                  true);
            return;
        }

        switch (rewardSelection) {
            case 0:  //buying techs.
                rewardPoints = MathUtility.parseInt(command.nextToken(), -1);

                if (rewardPoints < 0) {
                    CampaignMain.campaignMain.toUser(STR."AM:Invalid input - negative \{CampaignMain.campaignMain.getConfig(
                          "RPLongName")}.", Username, true);
                    return;
                }

                if (!(MathUtility.parseBoolean(house.getConfig("AllowTechsForRewards"), false))) {
                    CampaignMain.campaignMain.toUser(STR."AM:Sorry but you are not allowed to buy techs with \{CampaignMain.campaignMain.getConfig(
                          "RPLongName")}.", Username, true);
                    return;
                }

                if (rewardPoints > player.getReward()) {
                    if (player.getReward() == 1) {
                        CampaignMain.campaignMain.toUser(STR."AM:You only have 1 \{CampaignMain.campaignMain.getConfig(
                              "RPShortName")}. Try again later.", Username, true);
                    } else {
                        CampaignMain.campaignMain.toUser(STR."AM:You only have \{player.getReward()} \{CampaignMain.campaignMain.getConfig(
                              "RPShortName")} . Try again later.", Username, true);
                    }
                    return;
                }
                if (CampaignMain.campaignMain.isUsingAdvanceRepair()) {
                    int techCost =
                          MathUtility.parseInt(house.getConfig(STR."RewardPointsFor\{UnitUtils.techDescription(
                                rewardPoints)}"), 0);

                    if (player.getReward() < techCost) {
                        CampaignMain.campaignMain.toUser(STR."AM:You do not have enough \{CampaignMain.campaignMain.getConfig(
                              "RPLongName")} to buy this tech. You need \{techCost}", Username, true);
                        return;
                    }

                    player.addReward(-techCost);
                    player.addTotalTechs(rewardPoints, 1);
                    player.addAvailableTechs(rewardPoints, 1);

                    if (techCost > 1) {
                        rewards = "s";
                    }

                    CampaignMain.campaignMain.toUser(STR."AM:You hired \{StringUtils.aOrAn(UnitUtils.techDescription(
                          rewardPoints), true)} tech for \{techCost}RP\{rewards}.", Username, true);

                } else {
                    int numOfTechBought = MathUtility.parseInt(house.getConfig("TechsForARewardPoint"), 0);
                    numOfTechBought *= rewardPoints;

                    if (numOfTechBought > 1) {
                        techs = "s";
                    }

                    if (rewardPoints > 1) {
                        rewards = "s";
                    }

                    CampaignMain.campaignMain.toUser(STR."AM:You hired \{numOfTechBought} tech\{techs} for \{rewardPoints} \{CampaignMain.campaignMain.getConfig(
                          "RPLongName")}\{rewards}.", Username, true);
                    player.addReward(-rewardPoints);
                    player.addTechnicians(numOfTechBought);
                }
                break;

            case 1: //buying influence
                rewardPoints = MathUtility.parseInt(command.nextToken(), -1);

                if (rewardPoints < 0) {
                    CampaignMain.campaignMain.toUser(STR."AM:Invalid input - negative \{CampaignMain.campaignMain.getConfig(
                          "RPLongName")}.", Username, true);
                    return;
                }

                if (!(MathUtility.parseBoolean(house.getConfig("AllowInfluenceForRewards"), false))) {
                    CampaignMain.campaignMain.toUser(STR."Sorry but you are not allowed to buy influence with \{CampaignMain.campaignMain.getConfig(
                          "RPLongName")}.", Username, true);
                    return;
                }

                if (rewardPoints > player.getReward()) {
                    if (player.getReward() == 0) {
                        CampaignMain.campaignMain.toUser(STR."AM:You don't have any \{CampaignMain.campaignMain.getConfig(
                              "RPLongName")}. Purchase fails.", Username, true);
                    } else {
                        String toSend = STR."AM:You only have \{player.getReward()}\{CampaignMain.campaignMain.getConfig(
                              "RPLongName")}\{StringUtils.addAnS(player.getReward())}. Try again.";
                        CampaignMain.campaignMain.toUser(toSend, Username, true);
                    }

                    return;
                }

                int amountOfInfluenceBought = MathUtility.parseInt(house.getConfig("InfluenceForARewardPoint"), 0);
                amountOfInfluenceBought *= rewardPoints;
                CampaignMain.campaignMain.toUser(STR."AM:You've bought \{CampaignMain.campaignMain.moneyOrFluMessage(
                      false,
                      true,
                      amountOfInfluenceBought)} for \{rewardPoints} \{CampaignMain.campaignMain.getConfig("RPLongName")}\{StringUtils.addAnS(
                      rewardPoints)}.", Username, true);

                player.addReward(-rewardPoints);
                player.addInfluence(amountOfInfluenceBought);
                break;

            case 2: //buying units
                if (!(MathUtility.parseBoolean(house.getConfig("AllowUnitsForRewards"), false))) {
                    CampaignMain.campaignMain.toUser(STR."AM:Sorry but you are not allowed to buy units with \{CampaignMain.campaignMain.getConfig(
                          "RPLongName")}.", Username, true);
                    return;
                }

                int rewardPointsAvailable = player.getReward();
                int unitTotalRewardPointCost = 0;
                String typestring = command.nextToken();
                String weightstring = command.nextToken();

                int unitType;
                int unitWeight;

                SHouse faction = player.getHouseFightingFor();
                double rareCost = 1;
                boolean buyRareUnit = false;
                Vector<SUnit> newUnits = new Vector<>(1, 1);
                SPilot newPilot;
                String factionstring = "common";

                if (house.getBooleanConfig("AllowRareUnitsForRewards")) {
                    rareCost = (house.getDoubleConfig("RewardPointMultiplierForRare"));
                }

                unitType = MathUtility.parseInt(typestring, 0);
                unitWeight = MathUtility.parseInt(weightstring, 0);

                if (command.hasMoreElements()) {
                    factionstring = command.nextToken();
                    if (factionstring.equalsIgnoreCase("rare")) {

                        if (!(MathUtility.parseBoolean(house.getConfig("AllowRareUnitsForRewards"), false))) {
                            CampaignMain.campaignMain.toUser(
                                  STR."AM:Sorry. You are not allowed to buy rare units with your \{CampaignMain.campaignMain.getConfig(
                                        "RPLongName")}.",
                                  Username,
                                  true);
                            return;
                        }

                        //else
                        buyRareUnit = true;
                        factionstring = house.getConfig("RewardsRareBuildTable");

                    } else if (!factionstring.equalsIgnoreCase("common")) {
                        faction = CampaignMain.campaignMain.getHouseFromPartialString(factionstring, Username);
                    }

                    if (faction == null) {
                        faction = player.getHouseFightingFor();
                        if (faction == null) {
                            factionstring = "Common";
                        }
                    }
                }

                String configName = "";
                if (unitType == Unit.MEK) {
                    configName = STR."\{Unit.getWeightClassDesc(unitWeight)}RP";
                } else {
                    configName = STR."\{Unit.getWeightClassDesc(unitWeight)}\{Unit.getTypeClassDesc(unitType)}RP";
                }
                unitTotalRewardPointCost = MathUtility.parseInt(house.getConfig(configName), 0);
                //unitTotalRewardPointCost = weightCost + typeCost;

                if (faction != null && !player.getHouseFightingFor().equals(faction)) {
                    double nonHouseUnitMod = MathUtility.parseDouble(house.getConfig(STR."\{player.getHouseFightingFor()
                                                                                                  .getName()}To\{faction.getName()}RewardPointMultiplier"),
                          0.0);
                    if (nonHouseUnitMod < 0) {
                        nonHouseUnitMod = Double.parseDouble(house.getConfig("RewardPointNonHouseMultiplier"));
                    }
                    if (nonHouseUnitMod > 0) {
                        unitTotalRewardPointCost = (int) Math.round(unitTotalRewardPointCost * nonHouseUnitMod);
                    }
                }

                if (buyRareUnit) {
                    unitTotalRewardPointCost = (int) Math.round(unitTotalRewardPointCost * rareCost);
                }

                if (unitTotalRewardPointCost > rewardPointsAvailable) {
                    CampaignMain.campaignMain.toUser(STR."AM:Sorry. You need more \{CampaignMain.campaignMain.getConfig(
                          "RPLongName")} to buy that kind of unit.", Username, true);
                    return;
                }

                try {
                    //Lets get us a pilot and a unit
                    if (MathUtility.parseBoolean(house.getConfig("AllowPersonalPilotQueues"), false) &&
                              (unitType == Unit.MEK || unitType == Unit.PROTOMEK)) {
                        newPilot = new SPilot("Vacant", 99, 99);
                    } else {
                        newPilot = player.getMyHouse().getNewPilot(unitType);
                    }

                    newUnits.addAll(getUnitProduced(unitType,
                          unitWeight,
                          newPilot,
                          factionstring,
                          player.getMyHouse()));

                    for (SUnit newUnit : newUnits) {
                        player.addUnit(newUnit, true);
                        CampaignMain.campaignMain.toUser(STR."AM:You've bought a \{newUnit.getModelName()} for \{unitTotalRewardPointCost} \{CampaignMain.campaignMain.getConfig(
                              "RPLongName")}.", Username, true);
                    }
                    player.addReward(-unitTotalRewardPointCost);
                } catch (Exception ex) {
                    CampaignMain.campaignMain.toUser(
                          STR."AM:An error has occured while trying to create your requested unit. Please contact an admin. Faction: \{factionstring} Type: \{unitType} Class: \{unitWeight}",
                          Username,
                          true);
                    LOGGER.error(ex, STR."Error creating unit in \{this.getClass().getName()}");
                }
                break;

            case 3://repairs
                rewardPoints = MathUtility.parseInt(house.getConfig("RewardPointsForRepair"), 0);

                if (rewardPoints > player.getReward()) {
                    CampaignMain.campaignMain.toUser(STR."AM:You need more \{CampaignMain.campaignMain.getConfig(
                          "RPLongName")} to repair this unit (requires \{rewardPoints} RP)", Username, true);
                    return;
                }

                int unitID = MathUtility.parseInt(command.nextToken(), 0);
                SUnit unit = player.getUnit(unitID);

                //break out if the player doesn't have a unit with that id
                if (unit == null) {
                    CampaignMain.campaignMain.toUser(STR."AM:You don't have a unit with ID# \{unitID}.",
                          Username,
                          true);
                    return;
                }

                Entity entity = unit.getEntity();

                if (entity.getInternal(Mek.LOC_CENTER_TORSO) < 1) {
                    CampaignMain.campaignMain.toUser(STR."AM:Sorry but cored units cannot be repaired with \{CampaignMain.campaignMain.getConfig(
                          "RPLongName")}!", Username);
                    return;
                }

                for (int x = 0; x < entity.locations(); x++) {
                    entity.setArmor(entity.getOArmor(x), x);
                    if (entity.hasRearArmor(x)) {entity.setArmor(entity.getOArmor(x, true), x, true);}
                    entity.setInternal(entity.getOInternal(x), x);
                    for (int y = 0; y < entity.getNumberOfCriticalSlots(x); y++) {
                        CriticalSlot criticalSlot = entity.getCritical(x, y);

                        if (criticalSlot == null) {
                            continue;
                        }

                        if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                            Mounted<?> mounted = criticalSlot.getMount();
                            UnitUtils.repairEquipment(mounted, entity, x);
                        }// end CS type if
                        else {
                            if (UnitUtils.isEngineCrit(criticalSlot)) {
                                UnitUtils.repairDamagedEngine(entity);
                            } else {
                                if (entity instanceof Mek) {
                                    //Fix both breached and damaged crits.
                                    UnitUtils.fixCriticalSlot(criticalSlot, true);
                                    UnitUtils.fixCriticalSlot(criticalSlot, false);
                                }
                                entity.setCritical(x, y, criticalSlot);
                            }
                        }//end CS type else

                    }
                }

                //Fill up ammo.
                for (AmmoMounted ammoMounted : entity.getAmmo()) {
                    if (ammoMounted.isByShot()) {
                        ammoMounted.setShotsLeft(ammoMounted.getOriginalShots());
                    } else {
                        ammoMounted.setShotsLeft(ammoMounted.getType().getShots());
                    }
                }

                CampaignMain.campaignMain.toUser(STR."AM:Unit #\{unitID} \{unit.getModelName()} is now fully repaired.",
                      Username,
                      true);
                CampaignMain.campaignMain.toUser(STR."PL|UU|\{unit.getId()}|\{unit.toString(true)}", Username, false);
                player.addReward(-rewardPoints);
                player.checkAndUpdateArmies(unit);
                player.setSave();
                break;

            // @Author Salient (mwosux@gmail.com) , Add CBills per Flu
            case 4: //buying CBills
                influence = MathUtility.parseInt(command.nextToken(), -1);

                if (influence < 0) {
                    CampaignMain.campaignMain.toUser(STR."AM:Invalid input - negative \{CampaignMain.campaignMain.getConfig(
                          "FluLongName")}.", Username, true);
                    return;
                }

                if ((MathUtility.parseInt(house.getConfig("C_bills_Per_Flu"), -1)) <= 0) {
                    CampaignMain.campaignMain.toUser(STR."Sorry but you are not allowed to buy CBills with \{CampaignMain.campaignMain.getConfig(
                          "FluLongName")}.", Username, true);
                    return;
                }

                if (influence > player.getInfluence()) {
                    if (player.getInfluence() == 0) {
                        CampaignMain.campaignMain.toUser(STR."AM:You don't have any \{CampaignMain.campaignMain.getConfig(
                              "FluLongName")}. Purchase fails.", Username, true);
                    } else {
                        String toSend = STR."AM:You only have \{player.getInfluence()}\{CampaignMain.campaignMain.getConfig(
                              "FluLongName")}\{StringUtils.addAnS(player.getInfluence())}. Try again.";
                        CampaignMain.campaignMain.toUser(toSend, Username, true);
                    }

                    return;
                }

                int amountOfCBillsBought = MathUtility.parseInt(house.getConfig("Cbills_Per_Flu"), 0);
                amountOfCBillsBought *= influence;
                CampaignMain.campaignMain.toUser(STR."AM:You've bought \{CampaignMain.campaignMain.moneyOrFluMessage(
                            true,
                            false,
                            amountOfCBillsBought)} for \{influence} \{CampaignMain.campaignMain.getConfig("FluLongName")}.",
                      Username,
                      true);

                player.addInfluence(-influence);
                player.addMoney(amountOfCBillsBought);
                break;
        }

    }

    public AccessRole getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(AccessRole accessRole) {
        accessLevel = accessRole;
    }

    public String getSyntax() {
        return syntax;
    }

    /**
     * Build a unit. Derived from SUnitFactory.java's getUnitProduced()
     *
     * @return the Mek Produced
     */
    private Vector<SUnit> getUnitProduced(int type_id, int weightClass, SPilot pilot, String faction, SHouse house) {
        SUnitFactory factory = new SUnitFactory();
        String unitSize = Unit.getWeightClassDesc(weightClass);
        factory.setFounder(faction);
        Vector<SUnit> units = new Vector<>(1, 1);
        String Filename;

        //Use special RP-build fluff text for the unit
        String producer = "Reward Unit";

        if (MathUtility.parseBoolean(house.getConfig("UseOnlyOneVehicleSize"), false) && type_id == Unit.VEHICLE) {
            unitSize = Unit.getWeightClassDesc(CampaignMain.campaignMain.getRandomNumber(4));
        }

        Filename = BuildTable.getUnitFilename(faction, unitSize, type_id, BuildTable.REWARD);//build from rewards dir.

        if (Filename.toLowerCase().endsWith(".mul")) {
            units.addAll(SUnit.createMULUnits(Filename, producer));
        } else {
            SUnit sUnit = new SUnit(producer, Filename, weightClass);
            sUnit.setPilot(pilot);
            units.add(sUnit);
        }
        // clear this out of memory
        return units;
    }
}
