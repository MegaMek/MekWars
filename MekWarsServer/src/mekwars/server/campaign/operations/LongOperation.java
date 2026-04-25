/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - nmorris (urgru@users.sourceforge.net)
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
 * LongOperations are holders for info necessary to resolve
 * multiple game Operations (win ratios, etc).
 *
 * Creation of a LongOperation on will fail if a player lacks
 * necassary privledges (faction status, user level, whatever
 * is being used to regulate creation on a given server).
 *
 * NOTE: LongOperations need not be used. For simple servers,
 * or servers without formalized structures, they're wholly
 * inappropriate.
 */

package mekwars.server.campaign.operations;

// TODO: Remove "unused" once longs are running
@SuppressWarnings("unused")
public class LongOperation implements Comparable<LongOperation> {

    // IVARS
    public static int STATUS_IDLE = 0;
    public static int STATUS_OPINPROGRESS = 1;
    public static int STATUS_FINISHED = 2;

    // holding vars for pertinant info
    private server.campaign.SPlanet targetWorld;
    private java.util.Vector<Integer> shortIDs;// running short ops
    private server.campaign.SHouse attackingHouse;
    private server.campaign.SHouse defendingHouse;
    private int longID;

    // counters
    private int gamesPlayed;
    private int gamesWon;
    private int duration;

    // status ints
    private int currentStatus;
    private int showsToClear;

    // short ops being players
    private java.util.Vector<ShortOperation> activeShorts;

    /*
     * finally, the underpinning operation type use name instead of reference.
     * lookup latest version of the op when resolving, in case of param changes.
     */
    private String opName;

    // CONSTRUCTOR
    public LongOperation(String opName, server.campaign.SPlanet target, int id, server.campaign.SHouse attacker,
          server.campaign.SHouse defender, int duration) {

        // save params
        this.opName = opName;
        this.targetWorld = target;
        this.longID = id;
        this.attackingHouse = attacker;
        this.defendingHouse = defender;
        this.duration = duration;

        // set games played/won
        gamesPlayed = 0;
        gamesWon = 0;

        // set current status;
        currentStatus = mekwars.server.campaign.operations.LongOperation.STATUS_IDLE;
    }

    // METHODS

    /**
     * Method which returns current op status.
     */
    public int getStatus() {
        return currentStatus;
    }

    /**
     * method which sets current status.
     */
    public void setStatus(int i) {
        currentStatus = i;
    }

    /**
     * Method which returns world targetted by this op.
     */
    public server.campaign.SPlanet getTargetWorld() {
        return targetWorld;
    }

    /**
     * Method which returns the ID # of this op. NOTE: this is by definition a long ID ...
     */
    public int getID() {
        return longID;
    }

    /**
     * Method which returns the name (type) of this op. Used to pull matching Operation, which contains paramaters, from
     * Manager and to match with shorts.
     */
    public String getName() {
        return opName;
    }

    /**
     * Method which returns a set of IDs for in-progress short ops under this particular longop instance.
     */
    public java.util.Vector<ShortOperation> getActiveShortops() {
        if (this.getStatus() == STATUS_OPINPROGRESS) {return activeShorts;}
        // else
        return null;
    }

    /**
     * Method to add a new ShortOperation to the activeShortOps vector. After addition, the vector is re-sorted by ID.
     */
    public void addShortOp(ShortOperation o) {
        activeShorts.add(o);
        java.util.Collections.sort(activeShorts);
    }

    /**
     * Method which returns info for an operation which is waiting for players to join.
     */
    public String getIdleInfo() {

        // strings
        String resultString = "";

        // start the return
        resultString += "#" + getID() + " ";

        // add attackers to result
        resultString += attackingHouse.getColoredNameAsLink() + " is ";

        // add planet
        resultString += " has begun a " + opName + " on " + targetWorld.getNameAsColoredLink() + ". ";
        resultString += defendingHouse.getNameAsLink() + " forces are mobilizing in defense.";

        // make return
        return resultString;
    }

    /**
     * Method which returns the attacking faction.
     */
    public server.campaign.SHouse getAttackingHouse() {
        return this.attackingHouse;
    }

    /**
     * Method which returns the defending faction.
     */
    public server.campaign.SHouse getDefendingHouse() {
        return this.defendingHouse;
    }

    /**
     * Method which returns info for an operation which is in-progress.
     */
    public String getInProgressInfo(boolean complete) {
        return "";
    }

    /**
     * Method which returns info for an operation which is in-progress.
     */
    public String getFinishedInfo(boolean complete) {
        return "";
    }

    /**
     * compareTo required for compliance with Comparable interface.
     */
    public int compareTo(mekwars.server.campaign.operations.LongOperation o) {

        try {
            mekwars.server.campaign.operations.LongOperation compOp = o;
            if (compOp.getID() > this.getID()) {return 1;} else if (compOp.getID() == this.getID()) {return 0;} else {
                return -1;
            }
        } catch (Exception ex) {
            return 0;
        }
    }

}// end OperationsManager class
