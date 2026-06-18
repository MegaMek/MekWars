/*
 * MekWars - Copyright (C) 2005
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

/*
 * Created on 10.05.2005
 *
 */
package mekwars.server.campaign.commands;


import java.util.StringTokenizer;

import megamek.codeUtilities.MathUtility;
import megamek.common.CriticalSlot;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import mekwars.common.util.UnitUtils;
import mekwars.server.MWChatServer.auth.AccessRole;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SPlayer;
import mekwars.server.campaign.SUnit;
import mekwars.server.util.RepairTrackingThread;

/**
 * @author Torren (Jason Tighe) this parses out what the User wants reparied on thier unit and sends that data to the
 *       repair thread
 */
public class RepairUnitCommand implements Command {
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

        try {

            int unitID = MathUtility.parseInt(command.nextToken(), 0);
            int location = MathUtility.parseInt(command.nextToken(), 0);
            int slot = MathUtility.parseInt(command.nextToken(), 0);
            boolean armor = MathUtility.parseBoolean(command.nextToken(), false);
            int techType = MathUtility.parseInt(command.nextToken(), 0);
            int retries = MathUtility.parseInt(command.nextToken(), 0);
            int techWorkMod = MathUtility.parseInt(command.nextToken(), 0);
            boolean sendDialogUpdate = MathUtility.parseBoolean(command.nextToken(), false);

            retries = Math.max(0, retries);

            SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
            SUnit unit = player.getUnit(unitID);

            if (unit == null) {
                CampaignMain.campaignMain.toUser(STR."FSM|You do not have a unit with ID#\{unitID}.", Username, false);
                return;
            }

            Entity entity = unit.getEntity();

            if (entity == null) {
                CampaignMain.campaignMain.toUser(STR."FSM|You do not have a unit with an Entity \{unitID}.",
                      Username,
                      false);
                return;
            }
            String repairMessage = "";
            int tabLocation = location;
            int cost = CampaignMain.campaignMain.getRepairCost(entity,
                  location,
                  slot,
                  techType,
                  armor,
                  techWorkMod);

            if (unit.getType() == server.campaign.SUnit.INFANTRY) {
                CampaignMain.campaignMain.toUser("FSM|Infantry cannot be repaired.", Username, false);
                return;
            }

            if (CampaignMain.campaignMain.getRTT().isBeingRepaired(unitID, location, slot, armor)) {
                CampaignMain.campaignMain.toUser(
                      "FSM|That section is already being repaired wait for the work to finish before starting again.",
                      Username,
                      false);
                return;
            }

            if (player.isUnitInLockedArmy(unitID)) {
                CampaignMain.campaignMain.toUser(
                      "FSM|Sorry but that unit is currently in combat and may not be repaired.",
                      Username,
                      false);
                return;
            }

            if (techType != UnitUtils.TECH_REWARD_POINTS && cost > player.getMoney()) {
                CampaignMain.campaignMain.toUser("FSM|You do not have enough " +
                                                       CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                             false,
                                                             -cost) +
                                                       " to repair this location.", Username, false);
                return;
            }

            if (techType == UnitUtils.TECH_REWARD_POINTS && cost > player.getReward()) {
                CampaignMain.campaignMain.toUser("FSM|You do not have enough " +
                                                       CampaignMain.campaignMain.getConfig("RPLongName") +
                                                       " to repair this location.", Username, false);
                return;
            }

            if (player.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE &&
                      player.getAmountOfTimesUnitExistsInArmies(unitID) > 0) {
                CampaignMain.campaignMain.toUser("FSM|You may not repair that unit while it is in an active army.",
                      Username,
                      false);
                return;
            }

            int numberOfTechs = 1;

            if (techType < UnitUtils.TECH_PILOT) {
                numberOfTechs = player.getAvailableTechs().elementAt(techType);
            }

            if (techType == UnitUtils.TECH_PILOT && unit.getPilot() != null
                      && unit.getLastCombatPilot() != unit.getPilot().getPilotId()) {
                CampaignMain.campaignMain.toUser("FSM|" +
                                                       unit.getPilot().getName() +
                                                       " refuses to repair a unit he does not remember damaging himself!",
                      Username,
                      false);
                return;
            }

            if (numberOfTechs <= 0) {
                CampaignMain.campaignMain.toUser("FSM|You do not have any " +
                                                       UnitUtils.techDescription(techType) +
                                                       " techs to do this repair!", Username, false);
                return;
            }

            //if they are using RP to repair then it doesn't use parts from their stock pile
            if (techType != UnitUtils.TECH_REWARD_POINTS &&
                      CampaignMain.campaignMain.getBooleanConfig("UsePartsRepair")) {
                String crit = UnitUtils.getCritName(entity, slot, location, armor);
                int damagedCrits = UnitUtils.getNumberOfDamagedCrits(entity, slot, location, armor);
                //MWLogger.errLog(crit+" Crits: "+player.getUnitParts().getPartsCritCount(crit)+" Needed: "+damagedCrits);
                if (player.getPartsAmount(crit) < damagedCrits) {

                    if (player.getAutoReorder()) {

                        String newCommand = crit + "#" + damagedCrits;

                        CampaignMain.campaignMain.getServerCommands()
                              .get("BUYPARTS")
                              .process(new java.util.StringTokenizer(newCommand, "#"), Username);
                        if (player.getPartsAmount(crit) >= damagedCrits) {
                            newCommand = unitID +
                                               "#" +
                                               location +
                                               "#" +
                                               slot +
                                               "#" +
                                               armor +
                                               "#" +
                                               techType +
                                               "#" +
                                               retries +
                                               "#" +
                                               techWorkMod +
                                               "#" +
                                               sendDialogUpdate;
                            CampaignMain.campaignMain.getServerCommands()
                                  .get("REPAIRUNIT")
                                  .process(new java.util.StringTokenizer(newCommand, "#"), Username);
                            return;
                        }
                    }
                    String critPrettyname = UnitUtils.getCritExternalName(entity, slot, location, armor);
                    CampaignMain.campaignMain.toUser("FSM|You do not have enough " +
                                                           critPrettyname +
                                                           " crits to repair this.", Username, false);
                    return;
                }
            }

            repairMessage = UnitUtils.getRepairMessage(entity, tabLocation, slot, armor);
            if (repairMessage.length() > 0) {
                CampaignMain.campaignMain.toUser("FSM|" + repairMessage, Username, false);
                return;
            }

            if (armor) {
                boolean rear = false;

                //External armor
                if (slot < UnitUtils.LOC_INTERNAL_ARMOR) {

                    if (entity instanceof Tank) {rear = false;} else {
                        switch (location) {
                            case UnitUtils.LOC_CTR:
                                tabLocation = UnitUtils.LOC_CT;
                                rear = true;
                                break;
                            case UnitUtils.LOC_LTR:
                                tabLocation = UnitUtils.LOC_LT;
                                rear = true;
                                break;
                            case UnitUtils.LOC_RTR:
                                tabLocation = UnitUtils.LOC_RT;
                                rear = true;
                                break;
                            default:
                                if (slot == UnitUtils.LOC_REAR_ARMOR) {rear = true;} else {rear = false;}
                                break;
                        }
                    }

                    if (rear) {
                        repairMessage = "Repairs have begun on the external armor(" +
                                              entity.getLocationAbbr(tabLocation) +
                                              "r) of your " +
                                              entity.getShortNameRaw() +
                                              ".  <b>At a Cost of " +
                                              CampaignMain.campaignMain.moneyOrFluMessage(true, true, cost) +
                                              "</b>";
                    } else {
                        repairMessage = "Repairs have begun on the external armor(" +
                                              entity.getLocationAbbr(tabLocation) +
                                              ") of your " +
                                              entity.getShortNameRaw() +
                                              ".  <b>At a Cost of " +
                                              CampaignMain.campaignMain.moneyOrFluMessage(true, true, cost) +
                                              "</b>";
                    }
                }//Internal armor
                else {
                    repairMessage = "Repairs have begun on the internal structure(" +
                                          entity.getLocationAbbr(location) +
                                          ") of your " +
                                          entity.getShortNameRaw() +
                                          ".  <b>At a Cost of " +
                                          CampaignMain.campaignMain.moneyOrFluMessage(true, true, cost) +
                                          "</b>";
                }

            } else {
                CriticalSlot cs = entity.getCritical(location, slot);

                if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted mounted = cs.getMount();
                    repairMessage = "Work has begun on the " +
                                          mounted.getName() +
                                          "(" +
                                          entity.getLocationAbbr(location) +
                                          ") for your " +
                                          entity.getShortNameRaw() +
                                          ".  <b>At a Cost of " +
                                          CampaignMain.campaignMain.moneyOrFluMessage(true, true, cost) +
                                          "</b>";
                }// end CS type if
                else {
                    if (UnitUtils.isEngineCrit(cs)) {
                        repairMessage = "Work on your " +
                                              entity.getShortNameRaw() +
                                              "'s engine has begun.  <b>At a Cost of " +
                                              CampaignMain.campaignMain.moneyOrFluMessage(true, true, cost) +
                                              "</b>";
                    } else {
                        if (entity instanceof Mech) {
                            repairMessage = "Work has begun on the " +
                                                  ((Mech) entity).getSystemName(cs.getIndex()) +
                                                  "(" +
                                                  entity.getLocationAbbr(location) +
                                                  ") for your " +
                                                  entity.getShortName() +
                                                  ".  <b>At a Cost of " +
                                                  CampaignMain.campaignMain.moneyOrFluMessage(true, true, cost) +
                                                  "</b>";
                        }
                    }
                }//end CS type else

            }

            if (CampaignMain.campaignMain.getRTT().getState() == java.lang.Thread.State.TERMINATED) {
                CampaignMain.campaignMain.toUser(
                      "FSM|Sorry your repair order could not be processed, and the repair thread terminated. Staff was notified.",
                      Username,
                      false);
                MWLogger.errLog(
                      "NOTE: Repair Thread terminated! Use the restartrepairthread command to restart. If all else fails, reboot.");
                return;
            }
            if (techType == UnitUtils.TECH_PILOT) {unit.setPilotIsRepairing(true);}
            //charge them for the repair now.
            if (techType == UnitUtils.TECH_REWARD_POINTS) {
                player.addReward(-cost);
            } else {
                player.addMoney(-cost);
                unit.addRepairCost(cost);
                if (CampaignMain.campaignMain.getBooleanConfig("UsePartsRepair")) {
                    String crit = UnitUtils.getCritName(entity, slot, location, armor);
                    int damagedCrits = UnitUtils.getNumberOfDamagedCrits(entity, slot, location, armor);

                    if (crit.indexOf("Ammo") > -1) {
                        crit = "Ammo Bin";
                        damagedCrits = 1;
                    }
                    player.updatePartsCache(crit, -damagedCrits);
                }
            }
            player.setSave();
            CampaignMain.campaignMain.getRTT()
                  .getRepairList()
                  .add(RepairTrackingThread.Repair(player,
                        unitID,
                        armor,
                        location,
                        slot,
                        techType,
                        retries,
                        techWorkMod,
                        false));
            CampaignMain.campaignMain.toUser("FSM|" + repairMessage, Username, false);
            CampaignMain.campaignMain.toUser("PL|UU|" + unitID + "|" + unit.toString(true), Username, false);

            //call the repair dialog again witht he new unit info set.
            if (sendDialogUpdate) {CampaignMain.campaignMain.toUser("ARD|" + unitID, Username, false);}
        } catch (Exception ex) {
            MWLogger.errLog("Unable to Process Repair Unit Command!");
            MWLogger.errLog(ex);
        }

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}


}//end RepairUnitCommand
