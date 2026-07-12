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

/**
 * Client-side background thread that periodically processes a queue of pending unit "salvage" work
 * orders (one queue per tech skill level, see {@link UnitUtils#TECH_GREEN} through
 * {@link UnitUtils#TECH_PILOT}), simulating techs/pilots stripping parts/armor/structure off a unit
 * at a fixed cadence and sending completed salvage attempts to the server via
 * {@link IClient#sendChat(String)} (as a {@code /c salvageunit#...} campaign command).
 * <p>
 * This class is the salvage counterpart of {@link RepairManagmentThread} and follows the exact same
 * queue/threading pattern: work orders are queued via {@link #addWorkOrder(int, String)} (each a
 * "#"-delimited string of unit id, location, and slot), and this thread wakes up roughly every
 * {@link #averageRepairTime} milliseconds to pop and validate as many orders as there are available
 * techs of that type, canceling orders that are no longer valid (e.g. the unit is gone, the section
 * targeted is already salvaged/undamaged, or there are no techs of that type on the payroll).
 * <p>
 * As with {@link RepairManagmentThread}, this class never talks to a socket directly — all server
 * communication goes through the supplied {@link IClient}.
 */
public class SalvageManagmentThread extends Thread {
    private final static MMLogger LOGGER = MMLogger.create(SalvageManagmentThread.class);
    /**
     * One {@link ConcurrentLinkedQueue} per tech type, indexed by tech level constant (e.g.
     * {@link UnitUtils#TECH_GREEN}..{@link UnitUtils#TECH_PILOT}); each queue holds pending
     * "#"-delimited salvage work order strings for that tech type.
     */
    private final Vector<ConcurrentLinkedQueue<String>> workOrders = new Vector<>(5, 1);
    private final IClient client;
    /** Milliseconds between processing passes; defaults to 1000ms unless overridden by the constructor. */
    private long averageRepairTime = 1000;

    /**
     * Sets the salvage-processing cadence (only if it's greater than 1000ms; otherwise the default
     * of 1000ms is kept) and initializes one empty work order queue for every tech type from
     * {@link UnitUtils#TECH_GREEN} through {@link UnitUtils#TECH_PILOT}.
     *
     * @param repairTime desired delay in milliseconds between processing passes (ignored, keeping
     *                   the 1000ms default, if not greater than 1000)
     * @param client     client used to look up the player's units/techs and to send salvage
     *                   commands to the server
     */
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

    /**
     * Infinite loop (no stop/shutdown mechanism — runs for the lifetime of the JVM) that waits
     * {@link #averageRepairTime} milliseconds (via {@link Object#wait(long)} on this thread's own
     * monitor, since the method is {@code synchronized}) and then processes all queued work orders
     * via {@link #processWorkOrders()}. Nothing ever calls {@code notify()} on this object, so the
     * wait always times out naturally. Any exception during a processing pass is caught, reported
     * to the player via {@link IClient#systemMessage(String)}, and logged — the loop itself is
     * never aborted by an error.
     */
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

    /**
     * Walks each tech-type queue in {@link #workOrders} (from {@link UnitUtils#TECH_GREEN} to
     * {@link UnitUtils#TECH_PILOT}) and, for each one that has pending orders and at least one
     * available tech of that type, pops orders off the queue and either cancels them (removing them
     * outright) if they're no longer valid — e.g. the target unit can't be found, the pilot lacks
     * the AsTech skill, the pilot is already busy repairing, or the targeted armor/internal/critical
     * has already been salvaged or is damaged — or leaves them queued if not yet actionable.
     * Valid, actionable orders are completed by sending a {@code /c salvageunit#...} chat command to
     * the server and removing the order, decrementing the count of available techs for that pass so
     * each tech only performs one salvage action per invocation.
     * <p>
     * The whole method holds a lock on {@link #workOrders} and additionally synchronizes per-unit on
     * the {@link CUnit} being salvaged while validating/consuming its order.
     */
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
                        if (pos == UnitUtils.TECH_PILOT && !unit.getPilot().getSkills().has(PilotSkill.AsTechSkillID)) {
                            client.systemMessage(new StringBuilder("Work order found for the pilot of ")
                                  .append(unit.getModelName())
                                  .append(" however the pilot cannot salvage this unit.<br>The work order has been terminated.")
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

                            if (criticalSlot.isDamaged()) {
                                client.systemMessage(String.format("%s tech work order canceled because the critical was damaged.", UnitUtils.techDescription(pos)));
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
                                    client.systemMessage(new StringBuilder(UnitUtils.techDescription(pos))
                                          .append(" tech work order canceled, that sections armor has already been salvaged.")
                                          .toString());
                                    workQueue.remove();
                                    continue;
                                }

                            } else if (slot == UnitUtils.LOC_REAR_ARMOR) {
                                if (location >= UnitUtils.LOC_CENTER_TORSO) {
                                    location -= 7;
                                }

                                if (unit.getEntity().getArmor(location, true) <= 0) {
                                    client.systemMessage(new StringBuilder(UnitUtils.techDescription(pos))
                                          .append(" tech work order canceled, that sections armor has already been salvaged.")
                                          .toString());
                                    workQueue.remove();
                                    continue;
                                }
                            } else {//Internal!
                                //Check that everything is gone before you try and salvage internals.
                                if (!UnitUtils.getSalvageMessage(unit.getEntity(), location, slot, armor).isEmpty()) {
                                    continue;
                                }

                                if (unit.getEntity().getInternal(location) <= 0) {
                                    client.systemMessage(new StringBuilder(UnitUtils.techDescription(pos))
                                          .append(" tech work order canceled, that sections Internal Structure has already been salvaged.")
                                          .toString());
                                    workQueue.remove();
                                    continue;
                                }
                            }
                        }

                        client.sendChat(String.format("/c salvageunit#%s#%s#%s#%s#%s#false", unit.getId(), location, slot, armor, pos));
                        workQueue.remove();
                        availableTechs--;
                    }
                }

            }
        }
    }

    /**
     * Enqueues a new salvage work order (a "#"-delimited string of unit id, location, and slot)
     * onto the queue for the given tech type, to be picked up on a future
     * {@link #processWorkOrders()} pass.
     *
     * @param techType  tech level queue to add to
     * @param workOrder the encoded work order string
     */
    public void addWorkOrder(int techType, String workOrder) {
        workOrders.elementAt(techType).add(workOrder);
    }

    /**
     * Removes every queued salvage work order (across all tech types) belonging to the given unit,
     * matched by its id prefix at the start of the encoded work order string.
     *
     * @param unitID id of the unit whose pending work orders should all be discarded
     */
    public void removeAllWorkOrders(int unitID) {
        String id = Integer.toString(unitID);

        for (int tech = UnitUtils.TECH_GREEN; tech <= UnitUtils.TECH_PILOT; tech++) {
            workOrders.elementAt(tech).removeIf(repair -> repair.startsWith(String.format("%s#", id)));
        }
    }

    /**
     * Removes a single specific work order (matched by exact string equality) from the given tech
     * type's queue, and notifies the player via {@link IClient#systemMessage(String)} regardless of
     * whether a match was actually found.
     *
     * @param techType tech level queue to remove from
     * @param data     the exact encoded work order string to remove
     */
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

    /**
     * Checks whether a salvage work order already exists for the given unit at the given location
     * and slot, across all tech-type queues. Rear-armor slots are normalized (-7) before comparing
     * against the stored location.
     *
     * @param Location target internal/armor location
     * @param slot     target critical slot, or one of the armor/internal slot constants
     * @param unitId   unit id to check
     * @return true if a matching pending work order exists
     */
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

    /**
     * @param unitID unit id to check
     * @return true if the given unit has at least one pending salvage work order in any tech queue
     */
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

    /**
     * Builds a human-readable (HTML-formatted) summary of every pending salvage work order for the
     * given unit, one line per order, each with a "click here to remove" link encoding the tech
     * type and raw work order string for later removal (see {@link #removeWorkOrder(int, String)}).
     *
     * @param unitID unit id to summarize
     * @return {@code "None."} if there are no pending orders for this unit, otherwise an HTML
     *         fragment describing each pending order
     */
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
                        data.append(String.format("%s tech queued for external armor salavage %s.", UnitUtils.techDescription(tech), unit.getEntity()
                                                                                                                            .getLocationAbbr(
                                                                                                                                  locationID)));
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
