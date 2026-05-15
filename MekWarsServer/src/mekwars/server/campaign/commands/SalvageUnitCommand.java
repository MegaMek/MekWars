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

/*
 * Created on 03.28.2007
 *
 */
package mekwars.server.campaign.commands;

import common.Unit;
import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Tank;
import mekwars.server.campaign.CampaignMain;
import server.util.RepairTrackingThread;

/**
 * @author Torren (Jason Tighe)
 * @author Kestrel this parses out what the User wants salvaged on thier unit and sends that data to the repair thread
 */
public class SalvageUnitCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        try {

            int unitID = Integer.parseInt(command.nextToken());
            int location = Integer.parseInt(command.nextToken());
            int slot = Integer.parseInt(command.nextToken());
            boolean armor = Boolean.parseBoolean(command.nextToken());
            int techType = Integer.parseInt(command.nextToken());
            boolean sendDialogUpdate = Boolean.parseBoolean(command.nextToken());

            server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
            server.campaign.SUnit unit = player.getUnit(unitID);
            Entity entity = unit.getEntity();
            String salvageMessage = "";
            int tabLocation = location;
            int cost = CampaignMain.campaignMain.getRepairCost(entity, location, slot, techType, armor, 0, true);

            if ((unit.getType() != Unit.MEK) && (unit.getType() != Unit.VEHICLE)) {
                CampaignMain.campaignMain.toUser(
                      "AM:Sorry you can only salvage components from meks and vehicles.",
                      Username);
                return;
            }

            if (player.isUnitInLockedArmy(unitID)) {
                CampaignMain.campaignMain.toUser(
                      "FSM|Sorry but that unit is currently in combat and may not be worked on.",
                      Username,
                      false);
                return;
            }

            if (cost > player.getMoney()) {
                CampaignMain.campaignMain.toUser("FSM|You do not have enough " +
                                                       CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                             false,
                                                             -cost) +
                                                       " to salvage this location.", Username, false);
                return;
            }

            if ((player.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE) &&
                      (player.getAmountOfTimesUnitExistsInArmies(unitID) > 0)) {
                CampaignMain.campaignMain.toUser(
                      "FSM|You may not work on that unit while it is in an active army.",
                      Username,
                      false);
                return;
            }

            /*
             * by Kestrel Do not allow users to salvage fresh units if the
             * option is enabled.
             */
            if (CampaignMain.campaignMain.getBooleanConfig("DisallowFreshUnitSalvage") &&
                      !(UnitUtils.hasArmorDamage(entity)) &&
                      !UnitUtils.hasCriticalDamage(entity)) {
                CampaignMain.campaignMain.toUser("FSM|You may not salvage undamaged units.", Username, false);
                return;
            }

            int numberOfTechs = 1;

            if (techType < UnitUtils.TECH_PILOT) {
                numberOfTechs = player.getAvailableTechs().elementAt(techType);
            }

            if ((techType == UnitUtils.TECH_PILOT) &&
                      (unit.getPilot() != null) &&
                      (unit.getLastCombatPilot() != unit.getPilot().getPilotId())) {
                CampaignMain.campaignMain.toUser("FSM|" +
                                                       unit.getPilot().getName() +
                                                       " refuses to work on a unit he does not remember damaging himself!",
                      Username,
                      false);
                return;
            }

            if (numberOfTechs <= 0) {
                CampaignMain.campaignMain.toUser("FSM|You do not have any " +
                                                       UnitUtils.techDescription(techType) +
                                                       " techs to do this salvage job!", Username, false);
                return;
            }

            salvageMessage = UnitUtils.getSalvageMessage(entity, tabLocation, slot, armor);
            if (salvageMessage.length() > 0) {
                CampaignMain.campaignMain.toUser("FSM|" + salvageMessage, Username, false);
                return;
            }

            if (armor) {
                boolean rear = false;

                // External armor
                if (slot < UnitUtils.LOC_INTERNAL_ARMOR) {

                    if (entity instanceof Tank) {
                        rear = false;
                    } else {
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
                                if (slot == UnitUtils.LOC_REAR_ARMOR) {
                                    rear = true;
                                } else {
                                    rear = false;
                                }
                                break;
                        }
                    }

                    if (rear) {
                        salvageMessage = "Work has begun on the external armor(" +
                                               entity.getLocationAbbr(tabLocation) +
                                               "r) of your " +
                                               entity.getShortNameRaw() +
                                               ".  <b>At a Cost of " +
                                               CampaignMain.campaignMain.moneyOrFluMessage(true, true, cost) +
                                               "</b>";
                    } else {
                        salvageMessage = "Work has begun on the external armor(" +
                                               entity.getLocationAbbr(tabLocation) +
                                               ") of your " +
                                               entity.getShortNameRaw() +
                                               ".  <b>At a Cost of " +
                                               CampaignMain.campaignMain.moneyOrFluMessage(true, true, cost) +
                                               "</b>";
                    }
                }// Internal armor
                else {
                    salvageMessage = "Work has begun on the internal structure(" +
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
                    salvageMessage = "Work has begun on the " +
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
                        salvageMessage = "Work on your " +
                                               entity.getShortNameRaw() +
                                               "'s engine has begun.  <b>At a Cost of " +
                                               CampaignMain.campaignMain.moneyOrFluMessage(true, true, cost) +
                                               "</b>";
                    } else {
                        if (entity instanceof Mech) {
                            salvageMessage = "Work has begun on the " +
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
                }// end CS type else

            }

            if (CampaignMain.campaignMain.getRTT().isBeingRepaired(unitID, location, slot, armor)) {
                CampaignMain.campaignMain.toUser(
                      "FSM|That section is already being worked on wait for the work to finish before starting again.",
                      Username,
                      false);
                return;
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
            if (techType == UnitUtils.TECH_PILOT) {
                unit.setPilotIsRepairing(true);
            }
            // charge them for the repair now.
            player.addMoney(-cost);
            player.setSave();
            CampaignMain.campaignMain.getRTT()
                  .getRepairList()
                  .add(RepairTrackingThread.Repair(player, unitID, armor, location, slot, techType, 0, 0, true));
            CampaignMain.campaignMain.toUser("FSM|" + salvageMessage, Username, false);
            CampaignMain.campaignMain.toUser("PL|UU|" + unitID + "|" + unit.toString(true), Username, false);

            // call the repair dialog again witht he new unit info set.
            if (sendDialogUpdate) {
                CampaignMain.campaignMain.toUser("ARD|" + unitID + "|true", Username, false);
            }
        } catch (Exception ex) {
            MWLogger.errLog("Unable to Process Salvage Unit Command!");
            MWLogger.errLog(ex);
        }

    }// end process()

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

}// end SalvageUnitCommand
