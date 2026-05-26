/*
 * Copyright (C) 2007 Torren (torren@users.sourceforge.net)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


package mekwars.common.threads;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import megamek.codeUtilities.MathUtility;
import megamek.common.CriticalSlot;
import megamek.common.equipment.Mounted;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.skills.PilotSkill;
import mekwars.common.util.UnitUtils;

public class SalvageManagmentThread extends Thread {
    private final static MMLogger LOGGER = MMLogger.create(SalvageManagmentThread.class);
    private final Vector<ConcurrentLinkedQueue<String>> workOrders = new Vector<>(5, 1);
    private final IClient client;
    private long averageRepairTime = 1000;

    //Set the repair time and init the work order queue
    public SalvageManagmentThread(Long repairTime, IClient client) {
        if (repairTime > 1000) {
            averageRepairTime = repairTime;
        }

        this.client = client;

        for (int x = 0; x <= UnitUtils.TECH_PILOT; x++) {
            ConcurrentLinkedQueue<String> tempVector = new ConcurrentLinkedQueue<>();
            workOrders.add(tempVector);
        }

    }

    @Override
    public synchronized void run() {
        while (true) {
            try {
                this.wait(averageRepairTime);
                processWorkOrders();
            } catch (Exception ex) {
                client.systemMessage(
                      "Error processing Salvage Management queue. Alert an SO and check your ./logs/error.0 for the error");
                LOGGER.error(ex, "Error in Salvage Management Queue");
            }
        }
    }

    private void processWorkOrders() {
        int availableTechs = 1;

        synchronized (workOrders) {
            for (int pos = UnitUtils.TECH_GREEN; pos <= UnitUtils.TECH_PILOT; pos++) {

                //no work orders for these techs on to the next one
                if (workOrders.elementAt(pos).size() <= 0) {
                    continue;
                }

                //No techs for this type what so ever! buy more!
                if (pos != UnitUtils.TECH_PILOT && client.getPlayer().getTotalTechs().get(pos) <= 0) {
                    client.systemMessage(STR."You have pending work orders for \{UnitUtils.techDescription(pos)} techs, but have none on your pay roll.");
                    continue;
                }

                if (pos != UnitUtils.TECH_PILOT) {
                    availableTechs = client.getPlayer().getAvailableTechs().get(pos);
                }

                //all techs are busy to keep it moving.
                if (availableTechs <= 0) {
                    continue;
                }


                //Lets start to process work orders.
                Iterator<String> workQueue = workOrders.elementAt(pos).iterator();

                while (workQueue.hasNext()) {

                    //no more techs can't continue;
                    if (availableTechs <= 0) {
                        break;
                    }

                    StringTokenizer order = new StringTokenizer(workQueue.next(), "#");

                    CUnit unit = client.getPlayer().getUnit(MathUtility.parseInt(order.nextToken(), -1));
                    int location = MathUtility.parseInt(order.nextToken(), -1);
                    int slot = MathUtility.parseInt(order.nextToken(), -1);

                    boolean armor = (slot >= UnitUtils.LOC_FRONT_ARMOR);

                    if (unit == null) {
                        LOGGER.debug("Unable to find unit to salvage. removing salvage job");
                        client.systemMessage("Unable to find unit to salvage. removing salvage job");
                        workQueue.remove();
                        continue;
                    }

                    synchronized (unit) {
                        if (pos == UnitUtils.TECH_PILOT && !unit.getPilot().getSkills().has(PilotSkill.AstechSkillID)) {
                            client.systemMessage(STR."Work order found for the pilot of \{unit.getModelName()} however the pilot cannot salvage this unit.<br>The work order has been terminated.");
                            workQueue.remove();
                            continue;
                        }

                        //Pilot is busy repairing wait for the next round.
                        if (pos == UnitUtils.TECH_PILOT && unit.getPilotIsRepairing()) {
                            continue;
                        }

                        //check to see if CS are viable before anything else.
                        if (!armor) {
                            CriticalSlot criticalSlot = unit.getEntity().getCritical(location, slot);

                            if (criticalSlot == null) {
                                client.systemMessage(STR."\{UnitUtils.techDescription(pos)} tech work order canceled because the critical doesn't exist.");
                                workQueue.remove();
                                continue;
                            }

                            if (criticalSlot.isDamaged()) {
                                client.systemMessage(STR."\{UnitUtils.techDescription(pos)} tech work order canceled because the critical was damaged.");
                                workQueue.remove();
                                continue;
                            }
                        } else {
                            if (slot == UnitUtils.LOC_FRONT_ARMOR) {
                                int tempLocation = location;
                                if (location >= UnitUtils.LOC_CENTER_TORSO) {
                                    tempLocation -= 7;
                                }

                                if (unit.getEntity().getArmor(tempLocation) <= 0) {
                                    client.systemMessage(STR."\{UnitUtils.techDescription(pos)} tech work order canceled, that sections armor has already been salvaged.");
                                    workQueue.remove();
                                    continue;
                                }

                            } else if (slot == UnitUtils.LOC_REAR_ARMOR) {
                                if (location >= UnitUtils.LOC_CENTER_TORSO) {
                                    location -= 7;
                                }

                                if (unit.getEntity().getArmor(location, true) <= 0) {
                                    client.systemMessage(STR."\{UnitUtils.techDescription(pos)} tech work order canceled, that sections armor has already been salvaged.");
                                    workQueue.remove();
                                    continue;
                                }
                            } else {//Internal!
                                //Check that everything is gone before you try and salvage internals.
                                if (!UnitUtils.getSalvageMessage(unit.getEntity(), location, slot, armor).isEmpty()) {
                                    continue;
                                }

                                if (unit.getEntity().getInternal(location) <= 0) {
                                    client.systemMessage(STR."\{UnitUtils.techDescription(pos)} tech work order canceled, that sections Internal Structure has already been salvaged.");
                                    workQueue.remove();
                                    continue;
                                }
                            }
                        }

                        client.sendChat(STR."/c salvageunit#\{unit.getId()}#\{location}#\{slot}#\{armor}#\{pos}#false");
                        workQueue.remove();
                        availableTechs--;
                    }
                }

            }
        }
    }

    public void addWorkOrder(int techType, String workOrder) {
        workOrders.elementAt(techType).add(workOrder);
    }

    public void removeAllWorkOrders(int unitID) {
        String id = Integer.toString(unitID);

        for (int tech = UnitUtils.TECH_GREEN; tech <= UnitUtils.TECH_PILOT; tech++) {
            workOrders.elementAt(tech).removeIf(repair -> repair.startsWith(STR."\{id}#"));
        }
    }

    public void removeWorkOrder(int techType, String data) {
        Iterator<String> repairs = workOrders.elementAt(techType).iterator();
        while (repairs.hasNext()) {
            String repair = repairs.next();
            if (repair.equals(data)) {
                repairs.remove();
                break;
            }
        }
        client.systemMessage(STR."Removed work orders for for \{UnitUtils.techDescription(techType)} techs.");
    }

    public boolean isQueued(int Location, int slot, int unitId) {
        for (int tech = UnitUtils.TECH_GREEN; tech <= UnitUtils.TECH_PILOT; tech++) {
            for (String repair : workOrders.elementAt(tech)) {
                if (repair.indexOf(Integer.toString(unitId)) == 0) {
                    StringTokenizer order = new StringTokenizer(repair, "#");
                    order.nextToken();//unit id Already Verified it.

                    int locationid = MathUtility.parseInt(order.nextToken(), -1);
                    int slotid = MathUtility.parseInt(order.nextToken(), -1);

                    if (slotid == UnitUtils.LOC_REAR_ARMOR) {
                        locationid -= 7;
                    }

                    if (locationid == Location && slotid == slot) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean hasQueuedOrders(int unitID) {
        String id = Integer.toString(unitID);

        for (int tech = UnitUtils.TECH_GREEN; tech <= UnitUtils.TECH_PILOT; tech++) {
            for (String repair : workOrders.elementAt(tech)) {
                if (repair.startsWith(STR."\{id}#")) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getSalvageQueue(int unitID) {
        StringBuilder data = new StringBuilder("None.");
        CUnit unit = client.getPlayer().getUnit(unitID);

        for (int tech = UnitUtils.TECH_GREEN; tech <= UnitUtils.TECH_PILOT; tech++) {
            for (String repair : workOrders.elementAt(tech)) {
                if (repair.indexOf(Integer.toString(unitID)) == 0) {
                    StringTokenizer order = new StringTokenizer(repair, "#");
                    order.nextToken();//unit id Already Verified it.
                    int locationID = MathUtility.parseInt(order.nextToken(), -1);
                    int slotID = MathUtility.parseInt(order.nextToken(), -1);

                    if (data.toString().equals("None.")) {
                        data = new StringBuilder();
                    }

                    if (slotID == UnitUtils.LOC_FRONT_ARMOR) {
                        data.append(STR."\{UnitUtils.techDescription(tech)} tech queued for external armor salavage \{unit.getEntity()
                                                                                                                            .getLocationAbbr(
                                                                                                                                  locationID)}.");
                    } else if (slotID == UnitUtils.LOC_REAR_ARMOR) {
                        data.append(UnitUtils.techDescription(tech))
                              .append(" tech queued for external armor salvage ")
                              .append(unit.getEntity().getLocationAbbr(locationID - 7))
                              .append("(r).");
                    } else if (slotID == UnitUtils.LOC_INTERNAL_ARMOR) {
                        data.append(UnitUtils.techDescription(tech))
                              .append(" tech queued for internal structure salvage ")
                              .append(unit.getEntity().getLocationAbbr(locationID))
                              .append(".");
                    } else {
                        CriticalSlot criticalSlot = unit.getEntity().getCritical(locationID, slotID);

                        if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                            Mounted<?> mounted = criticalSlot.getMount();
                            data.append(UnitUtils.techDescription(tech))
                                  .append(" tech queued for salvage of ")
                                  .append(mounted.getName())
                                  .append("(")
                                  .append(unit.getEntity().getLocationAbbr(locationID))
                                  .append(").");
                        }// end CS type if
                        else {
                            if (unit.getEntity() instanceof Mek mek) {
                                data.append(UnitUtils.techDescription(tech))
                                      .append(" tech queued for salvage of ")
                                      .append(mek.getSystemName(criticalSlot.getIndex()))
                                      .append("(")
                                      .append(mek.getLocationAbbr(locationID))
                                      .append(").");
                            }
                        }//end CS type else

                    }
                    data.append(" <a href=\"REMOVESALVAGEQUEUEDWORKORDER|")
                          .append(tech)
                          .append("|")
                          .append(repair)
                          .append("\">click here to remove work order</a>.<br>");
                }

            }
        }
        return data.toString();
    }
}
