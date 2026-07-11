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

public class RepairManagmentThread extends Thread {
    private final static MMLogger LOGGER = MMLogger.create(RepairManagmentThread.class);
    private final Vector<ConcurrentLinkedQueue<String>> workOrders = new Vector<>(5, 1);
    private final IClient client;
    private long averageRepairTime = 1000;

    //Set the repair time and init the work order queue
    public RepairManagmentThread(Long repairTime, IClient client) {
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
                      "Error processing Repair Management queue. Alert an SO and check your ./logs/error.0 for the error");
                LOGGER.error(ex, "Error in Repair Management Queue");
            }
        }
    }

    private void processWorkOrders() {
        int availableTechs = 1;

        synchronized (workOrders) {
            for (int pos = UnitUtils.TECH_GREEN; pos <= UnitUtils.TECH_PILOT; pos++) {

                //no work orders for these techs on to the next one
                if (workOrders.elementAt(pos).isEmpty()) {
                    continue;
                }

                //No techs for this type whatsoever! buy more!
                if (pos != UnitUtils.TECH_PILOT && client.getPlayer().getTotalTechs().get(pos) <= 0) {
                    client.systemMessage(String.format("You have pending work orders for %s techs, but have none on your pay roll.", UnitUtils.techDescription(pos)));
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
                    //no more techs, can't continue;
                    if (availableTechs <= 0) {
                        break;
                    }

                    StringTokenizer order = new StringTokenizer(workQueue.next(), "#");

                    CUnit unit = client.getPlayer().getUnit(MathUtility.parseInt(order.nextToken(), -1));
                    int location = MathUtility.parseInt(order.nextToken(), -1);
                    int slot = MathUtility.parseInt(order.nextToken(), -1);
                    int roll = MathUtility.parseInt(order.nextToken(), -1);
                    int retries = MathUtility.parseInt(order.nextToken(), -1);

                    boolean armor = (slot >= UnitUtils.LOC_FRONT_ARMOR);

                    if (unit == null) {
                        LOGGER.debug("Unable to find unit to repair. removing repair job");
                        client.systemMessage("Unable to find unit to repair. removing repair job");
                        workQueue.remove();
                        continue;
                    }

                    synchronized (unit) {
                        if (pos == UnitUtils.TECH_PILOT && !unit.getPilot().getSkills().has(PilotSkill.AsTechSkillID)) {
                            client.systemMessage(new StringBuilder("Work order found for the pilot of ")
                                  .append(unit.getModelName())
                                  .append(" however the pilot cannot repair this unit.<br>The work order has been terminated.")
                                  .toString());
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
                                client.systemMessage(String.format("%s tech work order canceled because the critical doesn't exist.", UnitUtils.techDescription(pos)));
                                workQueue.remove();
                                continue;
                            }

                            if (!criticalSlot.isDamaged() && !criticalSlot.isBreached()) {
                                client.systemMessage(String.format("%s tech work order canceled because the critical was not damaged.", UnitUtils.techDescription(pos)));
                                workQueue.remove();
                                continue;
                            }
                        } else {
                            if (slot == UnitUtils.LOC_FRONT_ARMOR) {
                                int tempLocation = location;

                                if (location >= UnitUtils.LOC_CENTER_TORSOR) {
                                    tempLocation -= 7;
                                }

                                if (unit.getEntity().getArmor(tempLocation) ==
                                          unit.getEntity().getOArmor(tempLocation)) {
                                    client.systemMessage(String.format("%s tech work order canceled due to an already repaired Armor.", UnitUtils.techDescription(pos)));
                                    workQueue.remove();
                                    continue;
                                }

                            } else if (slot == UnitUtils.LOC_REAR_ARMOR) {
                                int tempLocation = location;
                                if (location >= UnitUtils.LOC_CENTER_TORSOR) {
                                    tempLocation -= 7;
                                }

                                if (unit.getEntity().getArmor(tempLocation, true) ==
                                          unit.getEntity().getOArmor(tempLocation, true)) {
                                    client.systemMessage(String.format("%s tech work order canceled due to an already repaired Rear Armor.", UnitUtils.techDescription(pos)));
                                    workQueue.remove();
                                    continue;
                                }
                            } else {//Internal!
                                if (unit.getEntity().getInternal(location) == unit.getEntity().getOInternal(location)) {
                                    client.systemMessage(new StringBuilder(UnitUtils.techDescription(pos))
                                          .append(" tech work order canceled due to an already repaired Internal Structure.")
                                          .toString());
                                    workQueue.remove();
                                    continue;
                                }
                            }
                        }
                        //check to see if we are able to process this repair if not continue to the next if so great!
                        if (!UnitUtils.isRepairViabile(unit.getEntity(), location, slot, armor)) {
                            continue;
                        }
                    }

                    int techWorkMod = roll - UnitUtils.getTechRoll(unit.getEntity(), location, slot, pos, armor,
                          this.client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel());

                    if (pos == UnitUtils.TECH_PILOT) {
                        techWorkMod = roll - UnitUtils.getTechRoll(unit.getEntity(),
                              location,
                              slot,
                              unit.getPilot()
                                    .getSkills()
                                    .getPilotSkill(PilotSkill.AsTechSkillID)
                                    .getLevel(),
                              armor,
                              this.client.getData()
                                    .getHouseByName(client.getPlayer().getHouse())
                                    .getTechLevel());
                    }

                    client.sendChat(String.format("/c repairunit#%s#%s#%s#%s#%s#%s#%s#false", unit.getId(), location, slot, armor, pos, retries, techWorkMod));
                    workQueue.remove();
                    availableTechs--;
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
            workOrders.elementAt(tech).removeIf(repair -> repair.startsWith(String.format("%s#", id)));
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

        client.systemMessage(String.format("Removed work orders for for %s techs.", UnitUtils.techDescription(techType)));
    }

    public boolean isQueued(int Location, int slot, int unitID) {
        for (int tech = UnitUtils.TECH_GREEN; tech <= UnitUtils.TECH_PILOT; tech++) {
            for (String repair : workOrders.elementAt(tech)) {
                if (repair.indexOf(Integer.toString(unitID)) == 0) {
                    java.util.StringTokenizer order = new StringTokenizer(repair, "#");
                    order.nextToken();//unit id Already Verified it.

                    int locationid = MathUtility.parseInt(order.nextToken(), -1);
                    int slotID = MathUtility.parseInt(order.nextToken(), -1);

                    if (slotID == UnitUtils.LOC_REAR_ARMOR) {
                        locationid -= 7;
                    }

                    if (locationid == Location && slotID == slot) {
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
                if (repair.startsWith(String.format("%s#", id))) {
                    return true;
                }

            }
        }
        return false;
    }

    public String getRepairQueue(int unitID) {
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
                        data.append(UnitUtils.techDescription(tech))
                              .append(" tech queued for external armor repair ")
                              .append(unit.getEntity().getLocationAbbr(locationID))
                              .append(".");
                    } else if (slotID == UnitUtils.LOC_REAR_ARMOR) {
                        data.append(UnitUtils.techDescription(tech))
                              .append(" tech queued for external armor repair ")
                              .append(unit.getEntity().getLocationAbbr(locationID - 7))
                              .append("(r).");
                    } else if (slotID == UnitUtils.LOC_INTERNAL_ARMOR) {
                        data.append(UnitUtils.techDescription(tech))
                              .append(" tech queued for internal structure repair ")
                              .append(unit.getEntity().getLocationAbbr(locationID))
                              .append(".");
                    } else {
                        CriticalSlot criticalSlot = unit.getEntity().getCritical(locationID, slotID);

                        if (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                            Mounted<?> mounted = criticalSlot.getMount();
                            data.append(UnitUtils.techDescription(tech))
                                  .append(" tech queued for repair of ")
                                  .append(mounted.getName())
                                  .append("(")
                                  .append(unit.getEntity().getLocationAbbr(locationID))
                                  .append(").");
                        } else {
                            if (unit.getEntity() instanceof Mek mek) {
                                data.append(UnitUtils.techDescription(tech))
                                      .append(" tech queued for repair of ")
                                      .append(mek.getSystemName(criticalSlot.getIndex()))
                                      .append("(")
                                      .append(unit.getEntity().getLocationAbbr(locationID))
                                      .append(").");
                            }
                        }//end CS type else

                    }
                    data.append(" <a href=\"REMOVEQUEUEDWORKORDER|")
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
