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

import common.Unit;
import common.util.MWLogger;
import common.util.StringUtils;
import common.util.UnitUtils;
import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Mounted;
import server.campaign.pilot.SPilot;

/**
 *
 * @author Torren Aug 28, 2004 allows users to redeem award points. they can redeem for techs, influence, or units
 *       syntax for techs and influence: /c userewardpoints#typeofreward#amountofrewardpointstouse syntax for units /c
 *       userewardpoints#typeofreward#unittype#unitweight#[faction]/[rare] items in brackets are optional. purchasing a
 *       rare unit will cost more rewardpoints
 *
 */
public class UseRewardPointsCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }

        /*
         * rewardSelections:
         * 0 Techs
         * 1 Influence
         * 2 Units
         * 3 Repair
         * 4 Cbills
         */

        int rewardSelection = Integer.parseInt(command.nextToken());
        int rewardPoints = 0;
        String techs = "";
        String rewards = "";

        server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);
        server.campaign.SHouse house = player.getMyHouse();
        // Salient - added additional case for RP for CBills
        if (rewardSelection < 0 || rewardSelection > 4) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Invalid reward selection. 0 for techs, 1 for influence, 2 for units, 3 for repair, 4 for CBills.",
                  Username,
                  true);
            return;
        }
        switch (rewardSelection) {
            case 0:  //buying techs.
                rewardPoints = Integer.parseInt(command.nextToken());

                if (rewardPoints < 0) {
                    server.campaign.CampaignMain.cm.toUser("AM:Invalid input - negative " +
                                                                 server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                                 ".", Username, true);
                    return;
                }

                if (!(Boolean.parseBoolean(house.getConfig("AllowTechsForRewards")))) {
                    server.campaign.CampaignMain.cm.toUser("AM:Sorry but you are not allowed to buy techs with " +
                                                                 server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                                 ".", Username, true);
                    return;
                }

                if (rewardPoints > player.getReward()) {
                    if (player.getReward() == 1) {
                        server.campaign.CampaignMain.cm.toUser("AM:You only have 1 " +
                                                                     server.campaign.CampaignMain.cm.getConfig(
                                                                           "RPShortName") +
                                                                     ". Try again later.", Username, true);
                    } else {
                        server.campaign.CampaignMain.cm.toUser("AM:You only have " +
                                                                     player.getReward() +
                                                                     " " +
                                                                     server.campaign.CampaignMain.cm.getConfig(
                                                                           "RPShortName") +
                                                                     " . Try again later.", Username, true);
                    }
                    return;
                }
                if (server.campaign.CampaignMain.cm.isUsingAdvanceRepair()) {
                    int typeOfTechToBuy = rewardPoints;
                    int techCost = Integer.parseInt(house.getConfig("RewardPointsFor" +
                                                                          UnitUtils.techDescription(typeOfTechToBuy)));

                    if (player.getReward() < techCost) {
                        server.campaign.CampaignMain.cm.toUser("AM:You do not have enough " +
                                                                     server.campaign.CampaignMain.cm.getConfig(
                                                                           "RPLongName") +
                                                                     " to buy this tech. You need " +
                                                                     techCost, Username, true);
                        return;
                    }

                    player.addReward(-techCost);
                    player.addTotalTechs(typeOfTechToBuy, 1);
                    player.addAvailableTechs(typeOfTechToBuy, 1);
                    if (techCost > 1) {rewards = "s";}

                    server.campaign.CampaignMain.cm.toUser("AM:You hired " +
                                                                 StringUtils.aOrAn(UnitUtils.techDescription(
                                                                       typeOfTechToBuy), true) +
                                                                 " tech for " +
                                                                 techCost +
                                                                 "RP" +
                                                                 rewards +
                                                                 ".", Username, true);

                } else {
                    int numOfTechBought = (Integer.parseInt(house.getConfig("TechsForARewardPoint")));
                    numOfTechBought *= rewardPoints;
                    if (numOfTechBought > 1) {techs = "s";}
                    if (rewardPoints > 1) {rewards = "s";}
                    server.campaign.CampaignMain.cm.toUser("AM:You hired " +
                                                                 numOfTechBought +
                                                                 " tech" +
                                                                 techs +
                                                                 " for " +
                                                                 rewardPoints +
                                                                 " " +
                                                                 server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                                 rewards +
                                                                 ".", Username, true);
                    player.addReward(-rewardPoints);
                    player.addTechnicians(numOfTechBought);
                }
                break;

            case 1: //buying influence
                rewardPoints = Integer.parseInt(command.nextToken());

                if (rewardPoints < 0) {
                    server.campaign.CampaignMain.cm.toUser("AM:Invalid input - negative " +
                                                                 server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                                 ".", Username, true);
                    return;
                }

                if (!(Boolean.parseBoolean(house.getConfig("AllowInfluenceForRewards")))) {
                    server.campaign.CampaignMain.cm.toUser("Sorry but you are not allowed to buy influence with " +
                                                                 server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                                 ".", Username, true);
                    return;
                }

                if (rewardPoints > player.getReward()) {

                    if (player.getReward() == 0) {
                        server.campaign.CampaignMain.cm.toUser("AM:You don't have any " +
                                                                     server.campaign.CampaignMain.cm.getConfig(
                                                                           "RPLongName") +
                                                                     ". Purchase fails.", Username, true);
                    } else {
                        String toSend = "AM:You only have " +
                                              player.getReward() +
                                              server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                              StringUtils.addAnS(player.getReward()) +
                                              ". Try again.";
                        server.campaign.CampaignMain.cm.toUser(toSend, Username, true);
                    }

                    return;
                }

                int amountOfInfluenceBought = (Integer.parseInt(house.getConfig("InfluenceForARewardPoint")));
                amountOfInfluenceBought *= rewardPoints;
                server.campaign.CampaignMain.cm.toUser("AM:You've bought " +
                                                             server.campaign.CampaignMain.cm.moneyOrFluMessage(false,
                                                                   true,
                                                                   amountOfInfluenceBought) +
                                                             " for " +
                                                             rewardPoints +
                                                             " " +
                                                             server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                             StringUtils.addAnS(rewardPoints) +
                                                             ".", Username, true);

                player.addReward(-rewardPoints);
                player.addInfluence(amountOfInfluenceBought);
                break;

            case 2: //buying units
                if (!(Boolean.parseBoolean(house.getConfig("AllowUnitsForRewards")))) {
                    server.campaign.CampaignMain.cm.toUser("AM:Sorry but you are not allowed to buy units with " +
                                                                 server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                                 ".", Username, true);
                    return;
                }
                int rewardPointsAvailable = player.getReward();
                int unitTotalRewardPointCost = 0;
                String typestring = command.nextToken();
                String weightstring = command.nextToken();
                int unitType = Unit.MEK;
                int unitWeight = Unit.LIGHT;
                server.campaign.SHouse faction = player.getHouseFightingFor();
                double rareCost = 1;
                boolean buyRareUnit = false;
                java.util.Vector<server.campaign.SUnit> newUnits = new java.util.Vector<server.campaign.SUnit>(1, 1);
                SPilot newPilot = null;
                String factionstring = "common";

                if (house.getBooleanConfig("AllowRareUnitsForRewards")) {
                    rareCost = (house.getDoubleConfig("RewardPointMultiplierForRare"));
                }

                try {
                    unitType = Integer.parseInt(typestring);
                } catch (Exception ex) {
                    unitType = Unit.getTypeIDForName(typestring);
                }

                try {
                    unitWeight = Integer.parseInt(weightstring);
                } catch (Exception ex) {
                    unitWeight = Unit.getWeightIDForName(weightstring.toUpperCase());
                }

                if (command.hasMoreElements()) {
                    factionstring = command.nextToken();
                    if (factionstring.equalsIgnoreCase("rare")) {

                        if (!(Boolean.parseBoolean(house.getConfig("AllowRareUnitsForRewards")))) {
                            server.campaign.CampaignMain.cm.toUser(
                                  "AM:Sorry. You are not allowed to buy rare units with your " +
                                        server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                        ".",
                                  Username,
                                  true);
                            return;
                        }

                        //else
                        buyRareUnit = true;
                        factionstring = house.getConfig("RewardsRareBuildTable");

                    } else if (!factionstring.equalsIgnoreCase("common")) {
                        faction = server.campaign.CampaignMain.cm.getHouseFromPartialString(factionstring, Username);
                    }

                    if (faction == null) {
                        faction = player.getHouseFightingFor();
                        if (faction == null) {factionstring = "Common";}
                    }
                }

                String configName = "";
                if (unitType == Unit.MEK) {
                    configName = Unit.getWeightClassDesc(unitWeight) + "RP";
                } else {
                    configName = Unit.getWeightClassDesc(unitWeight) + Unit.getTypeClassDesc(unitType) + "RP";
                }
                unitTotalRewardPointCost = Integer.parseInt(house.getConfig(configName));
                //unitTotalRewardPointCost = weightCost + typeCost;

                if (!player.getHouseFightingFor().equals(faction)) {
                    double nonHouseUnitMod = Double.parseDouble(house.getConfig(player.getHouseFightingFor().getName() +
                                                                                      "To" +
                                                                                      faction.getName() +
                                                                                      "RewardPointMultiplier"));
                    if (nonHouseUnitMod < 0) {
                        nonHouseUnitMod = Double.parseDouble(house.getConfig("RewardPointNonHouseMultiplier"));
                    }
                    if (nonHouseUnitMod > 0) {unitTotalRewardPointCost *= nonHouseUnitMod;}
                }

                if (buyRareUnit) {unitTotalRewardPointCost *= rareCost;}

                if (unitTotalRewardPointCost > rewardPointsAvailable) {
                    server.campaign.CampaignMain.cm.toUser("AM:Sorry. You need more " +
                                                                 server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                                 " to buy that kind of unit.", Username, true);
                    return;
                }

                try {
                    //Lets get us a pilot and a unit
                    if (Boolean.parseBoolean(house.getConfig("AllowPersonalPilotQueues")) &&
                              (unitType == Unit.MEK || unitType == Unit.PROTOMEK)) {
                        newPilot = new SPilot("Vacant", 99, 99);
                    } else {newPilot = player.getMyHouse().getNewPilot(unitType);}

                    newUnits.addAll(getUnitProduced(unitType,
                          unitWeight,
                          newPilot,
                          factionstring,
                          player.getMyHouse()));

                    for (server.campaign.SUnit newUnit : newUnits) {
                        player.addUnit(newUnit, true);
                        server.campaign.CampaignMain.cm.toUser("AM:You've bought a " +
                                                                     newUnit.getModelName() +
                                                                     " for " +
                                                                     unitTotalRewardPointCost +
                                                                     " " +
                                                                     server.campaign.CampaignMain.cm.getConfig(
                                                                           "RPLongName") +
                                                                     ".", Username, true);
                    }
                    player.addReward(-unitTotalRewardPointCost);
                } catch (Exception ex) {
                    server.campaign.CampaignMain.cm.toUser(
                          "AM:An error has occured while trying to create your requested unit. Please contact an admin. Faction: " +
                                factionstring +
                                " Type: " +
                                unitType +
                                " Class: " +
                                unitWeight,
                          Username,
                          true);
                    MWLogger.errLog(ex);
                    MWLogger.errLog("Error creating unit in " + this.getClass().getName());
                }
                break;

            case 3://repairs
                rewardPoints = Integer.parseInt(house.getConfig("RewardPointsForRepair"));

                if (rewardPoints > player.getReward()) {
                    server.campaign.CampaignMain.cm.toUser("AM:You need more " +
                                                                 server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                                 " to repair this unit (requires " +
                                                                 rewardPoints +
                                                                 " RP)", Username, true);
                    return;
                }

                int unitID = Integer.parseInt(command.nextToken());
                server.campaign.SUnit unit = player.getUnit(unitID);

                //break out if the player doesn't have a unit with that id
                if (unit == null) {
                    server.campaign.CampaignMain.cm.toUser("AM:You don't have a unit with ID# " + unitID + ".",
                          Username,
                          true);
                    return;
                }

                Entity entity = unit.getEntity();

                if (entity.getInternal(Mech.LOC_CT) < 1) {
                    server.campaign.CampaignMain.cm.toUser("AM:Sorry but cored units cannot be repaired with " +
                                                                 server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                                 "!", Username);
                    return;
                }

                for (int x = 0; x < entity.locations(); x++) {
                    entity.setArmor(entity.getOArmor(x), x);
                    if (entity.hasRearArmor(x)) {entity.setArmor(entity.getOArmor(x, true), x, true);}
                    entity.setInternal(entity.getOInternal(x), x);
                    for (int y = 0; y < entity.getNumberOfCriticals(x); y++) {
                        CriticalSlot cs = entity.getCritical(x, y);

                        if (cs == null) {continue;}

                        if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                            Mounted mounted = cs.getMount();
                            UnitUtils.repairEquipment(mounted, entity, x);
                        }// end CS type if
                        else {
                            if (UnitUtils.isEngineCrit(cs)) {
                                UnitUtils.repairDamagedEngine(entity);
                            } else {
                                if (entity instanceof Mech) {
                                    //Fix both breached and damaged crits.
                                    UnitUtils.fixCriticalSlot(cs, entity, true);
                                    UnitUtils.fixCriticalSlot(cs, entity, false);
                                }
                                entity.setCritical(x, y, cs);
                            }
                        }//end CS type else

                    }
                }

                //Fill up ammo.
                for (Mounted weap : entity.getAmmo()) {
                    if (weap.byShot()) {
                        weap.setShotsLeft(weap.getOriginalShots());
                    } else {
                        weap.setShotsLeft(((AmmoType) weap.getType()).getShots());
                    }
                }

                server.campaign.CampaignMain.cm.toUser("AM:Unit #" +
                                                             unitID +
                                                             " " +
                                                             unit.getModelName() +
                                                             " is now fully repaired.", Username, true);
                server.campaign.CampaignMain.cm.toUser("PL|UU|" + unit.getId() + "|" + unit.toString(true),
                      Username,
                      false);
                player.addReward(-rewardPoints);
                player.checkAndUpdateArmies(unit);
                player.setSave();
                break;

            // @Author Salient (mwosux@gmail.com) , Add RP for CBills
            case 4: //buying CBills
                rewardPoints = Integer.parseInt(command.nextToken());

                if (rewardPoints < 0) {
                    server.campaign.CampaignMain.cm.toUser("AM:Invalid input - negative " +
                                                                 server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                                 ".", Username, true);
                    return;
                }

                if (!(Boolean.parseBoolean(house.getConfig("AllowCBillsForRewards")))) {
                    server.campaign.CampaignMain.cm.toUser("Sorry but you are not allowed to buy CBills with " +
                                                                 server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                                 ".", Username, true);
                    return;
                }

                if (rewardPoints > player.getReward()) {

                    if (player.getReward() == 0) {
                        server.campaign.CampaignMain.cm.toUser("AM:You don't have any " +
                                                                     server.campaign.CampaignMain.cm.getConfig(
                                                                           "RPLongName") +
                                                                     ". Purchase fails.", Username, true);
                    } else {
                        String toSend = "AM:You only have " +
                                              player.getReward() +
                                              server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                              StringUtils.addAnS(player.getReward()) +
                                              ". Try again.";
                        server.campaign.CampaignMain.cm.toUser(toSend, Username, true);
                    }

                    return;
                }

                int amountOfCBillsBought = (Integer.parseInt(house.getConfig("CBillsForARewardPoint")));
                amountOfCBillsBought *= rewardPoints;
                server.campaign.CampaignMain.cm.toUser("AM:You've bought " +
                                                             server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                                   false,
                                                                   amountOfCBillsBought) +
                                                             " for " +
                                                             rewardPoints +
                                                             " " +
                                                             server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                             ".", Username, true);

                player.addReward(-rewardPoints);
                player.addMoney(amountOfCBillsBought);
                break;
        }

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    /**
     * Build a unit. Derived from SUnitFactory.java's getUnitProduced()
     *
     * @return the Mek Produced
     */
    private java.util.Vector<server.campaign.SUnit> getUnitProduced(int type_id, int weightClass, SPilot pilot,
          String faction, server.campaign.SHouse house) {

        server.campaign.SUnitFactory factory = new server.campaign.SUnitFactory();
        String unitSize = Unit.getWeightClassDesc(weightClass);
        factory.setFounder(faction);
        java.util.Vector<server.campaign.SUnit> units = new java.util.Vector<server.campaign.SUnit>(1, 1);
        String Filename = "";

        //Use special RP-build fluff text for the unit
        String producer = "Reward Unit";

        if (Boolean.parseBoolean(house.getConfig("UseOnlyOneVehicleSize")) && type_id == Unit.VEHICLE) {
            unitSize = Unit.getWeightClassDesc(server.campaign.CampaignMain.cm.getRandomNumber(4));
        }

        Filename = server.campaign.BuildTable.getUnitFilename(faction,
              unitSize,
              type_id,
              server.campaign.BuildTable.REWARD);//build from rewards dir.

        if (Filename.toLowerCase().endsWith(".mul")) {
            units.addAll(server.campaign.SUnit.createMULUnits(Filename, producer));
        } else {
            server.campaign.SUnit cm = new server.campaign.SUnit(producer, Filename, weightClass);
            cm.setPilot(pilot);
            units.add(cm);
        }
        factory = null;  // clear this out of memory
        return units;
    }
}
