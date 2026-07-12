/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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
 * Created on 8/25/2004
 *
 */

package mekwars.common;

import mekwars.common.campaign.clientutils.IPlayer;
import mekwars.common.flags.PlayerFlags;

/**
 * Base class holding the campaign-player state and behavior shared between MekWars' client and server tiers:
 * per-player flags, free-build "mek tokens", technician count/pay tracking, invisibility, team assignment and
 * auto-reorder preference.
 * <p>
 * This class is never used directly at runtime — {@code mekwars.common.campaign.CPlayer} extends it as the
 * client-side player representation, and {@code mekwars.server.campaign.SPlayer} extends it as the (much larger)
 * server-side player representation that additionally implements buying/selling and comparison behavior. Common
 * bookkeeping that both sides need identically lives here to avoid duplication.
 *
 * @author Helge Richter
 */
public class Player implements IPlayer {

    /** Per-player feature/preference flags (server-settable), keyed by flag name. */
    protected PlayerFlags flags = new PlayerFlags();
    /** Default flag values applied to staff; only populated/used for staff accounts. */
    protected PlayerFlags defaultPlayerFlags = new PlayerFlags(); // This is only going to be set for staff
    /** A counter for how many meks a player is allowed to create in free build. */
    protected int mekToken = 0; // A counter for how many meks a player is allowed to create in free build
    /** Used to track hangar BV in mini campaigns. */
    protected int bvTracker = 0; // used to track hangar BV in mini campaigns
    /** Number of technicians the player currently employs. */
    private int technicians = 0;//@urgru 7/17/04
    /** Cached C-bill amount owed to technicians after the last game/task; -1 means "not yet computed". */
    private int currentTechPayment = -1;//num Cbills owed to techs after games
    /** Evil command for Big brother err admins: hides the player from other players below the required access level. */
    private boolean isInvisible = false;//Evil command for Big brother err admins.
    /** The player's team number for the current operation; -1 means unassigned. */
    private int teamNumber = -1;
    /** Whether the player wants hangar parts automatically reordered when depleted. */
    private boolean autoReorderParts = false;

    /**
     * @return bvTracker value
     */
    public int getBVTracker() {
        return bvTracker;
    }

    /**
     * @param tracker the bvTracker value
     */
    public void setBVTracker(int tracker) {
        bvTracker = tracker;
    }

    /**
     * @return the mekToken
     */
    public int getMekToken() {
        return mekToken;
    }

    /**
     * @param mekToken the mekToken to set
     */
    public void setMekToken(int mekToken) {
        this.mekToken = mekToken;
    }

    /**
     * @return current post-task payment to technicians, in Cbills
     */
    public int getCurrentTechPayment() {
        return currentTechPayment;
    }

    /**
     * @param i post-task payment to set, in Cbills
     */
    public void setCurrentTechPayment(int i) {
        currentTechPayment = i;
    }

    /**
     * @return the number of technicians the player has
     */
    public int getTechnicians() {
        return technicians;
    }//end getTechnicians()

    /**
     * @param technicians to set technicians to.
     */
    public void setTechnicians(int technicians) {

        if (technicians < 0) {
            technicians = 0;
        }

        this.technicians = technicians;

        //clear the tech payment any time a new number of techs is set
        currentTechPayment = -1;
    }//end setTechnicians()

    /**
     * @param technician number of technicians to add (subtract) from the player's total
     *                   <p>
     *                   NOTE: subzero cases are checked in setTechs(). no check here.
     */
    public void addTechnicians(int technician) {
        this.setTechnicians(technicians + technician);
    }

    /**
     * does the player have the invisible flag.
     *
     * @return true/false.
     */
    public boolean isInvisible() {
        return isInvisible;
    }

    /**
     * Sets that a player now has the invisible flag. Of course, players with access levels >= this player will still
     * beable to see them.
     *
     * @param invisible the invisible flag to set.
     */
    public void setInvisible(boolean invisible) {
        isInvisible = invisible;
    }

    /**
     * Returns players team number
     *
     * @return the team number for the current operation, or -1 if unassigned.
     */
    public int getTeamNumber() {
        return teamNumber;
    }

    /**
     * Set Players team number for the current op.
     *
     * @param team the team number to assign.
     */
    public void setTeamNumber(int team) {
        this.teamNumber = team;
    }

    /**
     * Returns if the player has auto reorder parts turned on.
     *
     * @return {@code true} if hangar parts should be automatically reordered when depleted.
     */
    public boolean getAutoReorder() {
        return this.autoReorderParts;
    }

    /**
     * Sets if the player wants to reorder parts.
     *
     * @param reorder the auto-reorder preference to set.
     */
    public void setAutoReorder(boolean reorder) {
        this.autoReorderParts = reorder;
    }

    /**
     * Sets a player flag
     *
     * @param name  - the name of the flag to set
     * @param value - true or false (String value)
     */
    public void setFlagStatus(String name, String value) {
        setFlagStatus(name, Boolean.parseBoolean(value));
    }

    /**
     * Sets a player flag
     *
     * @param name  - the name of the flag to set
     * @param value - true or false (boolean value)
     */
    public void setFlagStatus(String name, boolean value) {
        flags.setFlag(name, value);
    }

    /**
     * Returns the value of a flag
     *
     * @param name - the name of the flag to get
     *
     * @return true or false (boolean value)
     */
    public boolean getFlagStatus(String name) {
        return flags.getFlagStatus(name);
    }

    /**
     * Loads the set of server-defined players flags from a string. This should probably only be called during player
     * logon, and then each flag can be set individually
     *
     */
    public void loadFlags(String data) {
        flags.loadDefaults(data);
    }

    /**
     * Exports the string of flags read by loadFlags
     *
     * @return flag data
     */
    public String exportFlags() {
        return flags.export();
    }

    /**
     * @return the defaultPlayerFlags
     */
    public PlayerFlags getDefaultPlayerFlags() {
        return defaultPlayerFlags;
    }

    /**
     * @return the playerFlags
     */
    public PlayerFlags getFlags() {
        return flags;
    }

    /**
     * Base implementation always returns an empty string; subclasses ({@code CPlayer}, {@code SPlayer}) override
     * this to return the actual player name/login. Not intended to be relied upon directly on a bare
     * {@code Player} instance.
     */
    @Override
    public String getName() {
        return "";
    }
}//End Class Player
