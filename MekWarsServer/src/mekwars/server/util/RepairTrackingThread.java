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

package mekwars.server.util;

import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.equipment.Mounted;
import mekwars.common.util.UnitUtils;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SPlayer;

/**
 *
 * @author Torren Oct 8, 2005
 *       <p>
 *       Thread use to track all repairs made by all users on the server.
 */
public class RepairTrackingThread extends Thread {

    private final Vector<Repair> repairList = new Vector<>(1, 1);
    private final long repairTime;


    public RepairTrackingThread(long Time) {
        super("Repair Tracking Thread");
        repairTime = Time;
    }

    public static Repair Repair(SPlayer player, int unitID, boolean armor, int location, int slot, int techType,
          int retries, int techWorkMod, boolean salvage) {

        return new mekwars.server.util.Repair(player,
              unitID,
              armor,
              location,
              slot,
              techType,
              retries,
              techWorkMod,
              salvage);
    }

    public static mekwars.server.util.Repair Repair(server.campaign.SPlayer player, int unitID,
          java.util.Vector<Integer> techs, int repairTime, boolean salvage) {

        return new mekwars.server.util.Repair(player, unitID, techs, repairTime, true, salvage);
    }

    @Override
    public synchronized void run() {
        try {
            while (true) {
                //MWLogger.errLog("Wait time: "+repairTime);
                this.wait(repairTime);
                checkRepairs();
            }
        } catch (Exception ex) {
            MWLogger.errLog("Error while trying to sleep in RepairTrackingThread");
            MWLogger.errLog(ex);
        }

    }

    public void checkRepairs() {

        try {
            java.util.concurrent.ConcurrentLinkedQueue<Repair> tempVector = new java.util.concurrent.ConcurrentLinkedQueue<Repair>(
                  repairList);
            synchronized (tempVector) {
                for (mekwars.server.util.Repair repairOrder : tempVector) {
                    //MWLogger.errLog("Start Time: "+ new Date(repairOrder.getStartTime()).toString()+" End Time: "+new Date(repairOrder.getEndTime()).toString());
                    //double minutes = (repairOrder.getEndTime()-System.currentTimeMillis())/60000;
                    //MWLogger.errLog("ETA: "+Double.toString(minutes));
                    if (repairOrder == null) {
                        repairList.removeElement(repairOrder);
                        return;
                    }
                    if (repairOrder.getEndTime() <= System.currentTimeMillis()) {
                        try {
                            if ((CampaignMain.campaignMain.getPlayer(repairOrder.getUsername()) == null)
                                      || repairOrder.finishRepair()) {
                                repairList.removeElement(repairOrder);
                                server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(repairOrder.getUsername());
                                player.checkAndUpdateArmies(player.getUnit(repairOrder.getUnitID()));
                            }
                        } catch (Exception ex) {
                            MWLogger.errLog("Unable to finish repair for " +
                                                  repairOrder.getUsername() +
                                                  " for unit #" +
                                                  repairOrder.getUnitID() +
                                                  " " +
                                                  repairOrder.getUnit().getShortNameRaw());
                            MWLogger.errLog(ex);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            MWLogger.errLog("Error while checking repair. Containing and continuing.");
            MWLogger.errLog(ex);
        }
    }

    public String unitRepairTimes(int unitID) {

        String results = "";
        String unitName = "";
        int SEC = 1000;
        int MIN = SEC * 60;
        int HOUR = MIN * 60;
        int DAY = HOUR * 24;

        synchronized (repairList) {
            for (mekwars.server.util.Repair repairOrder : repairList) {
                if (repairOrder.getUnitID() == unitID) {
                    if (unitName.length() < 1) {
                        unitName = repairOrder.getUnit().getShortNameRaw();
                    }

                    long mills = (repairOrder.getEndTime() - System.currentTimeMillis());///60000;

                    if (mills < 0) {
                        mills += CampaignMain.campaignMain.getDoubleConfig("TimeForEachRepairPoint") * 1000;
                    }
                    java.util.Calendar time = java.util.Calendar.getInstance();
                    time.setTimeInMillis(mills);


                    String output = "";

                    if (mills >= DAY) {
                        output += (mills / DAY) + "d ";
                        mills %= DAY;
                    }

                    if (mills >= HOUR) {
                        output += (mills / HOUR) + "h ";
                        mills %= HOUR;
                    }

                    if (mills >= MIN) {
                        output += (mills / MIN) + "m ";
                        mills %= MIN;
                    }

                    if (mills >= SEC) {
                        output += (mills / SEC) + "s ";
                        mills %= SEC;
                    }

                    /*   if ( mills > 0 )
	                	output += mills +"ms ";*/

                    output = output.trim();

                    if (repairOrder.isSimpleRepair()) {
                        results = "#" +
                                        unitID +
                                        " " +
                                        unitName +
                                        " is undergoing a complete repair cycle. ETA: " +
                                        output +
                                        ".";
                        return results;
                    }

                    if (repairOrder.getArmor()) {
                        boolean rear = false;
                        int armorLocation = repairOrder.getLocation();
                        //External armor
                        if (repairOrder.getSlot() < UnitUtils.LOC_INTERNAL_ARMOR) {

                            switch (armorLocation) {
                                case UnitUtils.LOC_CTR:
                                    armorLocation = UnitUtils.LOC_CT;
                                    rear = true;
                                    break;
                                case UnitUtils.LOC_LTR:
                                    armorLocation = UnitUtils.LOC_LT;
                                    rear = true;
                                    break;
                                case UnitUtils.LOC_RTR:
                                    armorLocation = UnitUtils.LOC_RT;
                                    rear = true;
                                    break;
                                default:
                                    rear = false;
                                    break;
                            }

                            if (rear) {
                                results += "Repairing external armor " +
                                                 repairOrder.getUnit().getLocationAbbr(armorLocation) +
                                                 "(r) ETA: " +
                                                 output +
                                                 ".";
                            } else {
                                results += "Repairing external armor " +
                                                 repairOrder.getUnit().getLocationAbbr(armorLocation) +
                                                 " ETA: " +
                                                 output +
                                                 ".";
                            }
                        }//Internal armor
                        else {
                            results += "Repairing internal structure " +
                                             repairOrder.getUnit().getLocationAbbr(armorLocation) +
                                             " ETA: " +
                                             output +
                                             ".";
                        }
                    } else {
                        CriticalSlot cs = repairOrder.getUnit()
                                                .getCritical(repairOrder.getLocation(), repairOrder.getSlot());

                        if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                            Mounted mounted = cs.getMount();
                            results += "Repairing " +
                                             mounted.getName() +
                                             "(" +
                                             repairOrder.getUnit().getLocationAbbr(repairOrder.getLocation()) +
                                             ") ETA: " +
                                             output +
                                             ".";
                        }// end CS type if
                        else {
                            if (repairOrder.getUnit() instanceof Mech) {
                                results += "Repairing " +
                                                 ((Mech) repairOrder.getUnit()).getSystemName(cs.getIndex()) +
                                                 "(" +
                                                 repairOrder.getUnit().getLocationAbbr(repairOrder.getLocation()) +
                                                 ") ETA: " +
                                                 output +
                                                 ".";
                            }
                        }//end CS type else
                    }
                    results += " <a href=\"MEKWARS/c stoprepairjob#" +
                                     unitID +
                                     "#" +
                                     repairOrder.getLocation() +
                                     "#" +
                                     repairOrder.getSlot() +
                                     "#" +
                                     repairOrder.getArmor() +
                                     "\">Click here to stop</a><br>";
                }//end if
            }//end for

        }

        if (results.length() > 1) {
            results = "#" + unitID + " " + unitName + " has the following repair jobs pending:<br>" + results;
            return results;
        }

        return null;
    }

    public boolean isBeingRepaired(int unitID, int location, int slot, boolean armor) {

        java.util.Vector<Repair> tempRepairList = new java.util.Vector<Repair>(getRepairList());
        synchronized (tempRepairList) {
            for (mekwars.server.util.Repair repairOrder : tempRepairList) {
                if (repairOrder.matches(unitID, location, slot, armor)) {
                    return true;
                }
            }
        }
        return false;
    }

    public java.util.Vector<Repair> getRepairList() {
        return repairList;
    }

    public void stopRepair(int unitID, int location, int slot, boolean armor) {

        java.util.concurrent.ConcurrentLinkedQueue<Repair> tempRepairList = new java.util.concurrent.ConcurrentLinkedQueue<Repair>(
              getRepairList());

        synchronized (tempRepairList) {
            for (mekwars.server.util.Repair repairOrder : tempRepairList) {
                if (repairOrder.matches(unitID, location, slot, armor)) {
                    server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(repairOrder.getUsername());
                    server.campaign.SUnit unit = player.getUnit(unitID);
                    CampaignMain.campaignMain.toUser("FSM|Repair order cancelled.",
                          repairOrder.getUsername(),
                          false);
                    if (repairOrder.getTechType() == UnitUtils.TECH_PILOT) {
                        unit.setPilotIsRepairing(false);
                    } else {
                        player.addAvailableTechs(repairOrder.getTechType(), 1);
                    }
                    repairOrder.stopRepair();
                    getRepairList().removeElement(repairOrder);
                    unit.setEntity(repairOrder.getUnit());
                    CampaignMain.campaignMain.toUser("PL|UU|" + unitID + "|" + unit.toString(true),
                          player.getName(),
                          false);
                    return;
                }
            }
        }
    }

    public void stopAllRepairJobs(int unitID, server.campaign.SPlayer player) {

        java.util.concurrent.ConcurrentLinkedQueue<Repair> tempRepairList = new java.util.concurrent.ConcurrentLinkedQueue<Repair>(
              getRepairList());

        synchronized (tempRepairList) {

            for (mekwars.server.util.Repair repairOrder : tempRepairList) {
                if (repairOrder.getUnitID() == unitID) {
                    try {
                        int techType = repairOrder.getTechType();
                        if (techType != UnitUtils.TECH_PILOT) {
                            player.addAvailableTechs(techType, 1);
                        }
                    } catch (Exception ex) {}
                    repairOrder.stopRepair();
                    getRepairList().removeElement(repairOrder);
                }
            }
        }
        try {
            server.campaign.SUnit unit = player.getUnit(unitID);
            CampaignMain.campaignMain.toUser("PL|UU|" + unitID + "|" + unit.toString(true),
                  player.getName(),
                  false);
        } catch (Exception ex) {}

    }
}

