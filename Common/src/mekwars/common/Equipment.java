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

package mekwars.common;

/**
 * Represents a single piece of unit equipment (weapon/component) tracked for factory build-table purposes: its
 * display and internal (MegaMek) names, and the min/max cost and production-count range configured for it.
 * <p>
 * Unlike {@link BMEquipment} (which represents a black-market listing derived from an actual unit's loadout at a
 * point in time), this class models a build-table/catalog entry with a configurable cost and production range,
 * and tracks whether it has been edited via the {@link #isUpdated()} dirty flag so callers (e.g. an editor UI or
 * save routine) know whether the entry needs to be persisted.
 */
public class Equipment {

    /** Display name shown to users. */
    private String equipmentName = "";
    /** Internal (MegaMek) equipment name used to look up the actual {@code EquipmentType}. */
    private String equipmentInternalName = "";
    /** Minimum cost this equipment can be produced/sold for. */
    private double minCost = 0;
    /** Maximum cost this equipment can be produced/sold for. */
    private double maxCost = 0;
    /** Minimum quantity produced per production run. */
    private int minProduction = 0;
    /** Maximum quantity produced per production run. */
    private int maxProduction = 0;
    /** Dirty flag: set whenever one of the cost/production setters actually changes a value. */
    private boolean updated = false;

    /**
     * @return the display name of this equipment.
     */
    public String getEquipmentName() {
        return this.equipmentName;
    }

    /**
     * Sets the display name. Note this does <em>not</em> set {@link #isUpdated()} (unlike the cost/production
     * setters below).
     */
    public void setEquipmentName(String name) {
        this.equipmentName = name;
    }

    /**
     * @return the internal (MegaMek) equipment name.
     */
    public String getEquipmentInternalName() {
        return this.equipmentInternalName;
    }

    /**
     * Sets the internal equipment name. Like {@link #setEquipmentName(String)}, this does not mark the entry as
     * updated.
     */
    public void setEquipmentInternalName(String name) {
        this.equipmentInternalName = name;
    }

    /**
     * @return the minimum production cost.
     */
    public double getMinCost() {
        return this.minCost;
    }

    /**
     * Sets the minimum cost, marking this entry {@link #isUpdated() updated} only if the value actually changes.
     */
    public void setMinCost(double cost) {

        if (this.minCost != cost) {
            this.minCost = cost;
            this.updated = true;
        }
    }

    /**
     * @return the maximum production cost.
     */
    public double getMaxCost() {
        return this.maxCost;
    }

    /**
     * Sets the maximum cost, marking this entry {@link #isUpdated() updated} only if the value actually changes.
     */
    public void setMaxCost(double cost) {

        if (this.maxCost != cost) {
            this.maxCost = cost;
            this.updated = true;
        }
    }

    /**
     * @return the minimum quantity produced per run.
     */
    public int getMinProduction() {
        return this.minProduction;
    }

    /**
     * Sets the minimum production quantity, marking this entry {@link #isUpdated() updated} only if the value
     * actually changes.
     */
    public void setMinProduction(int production) {

        if (this.minProduction != production) {
            this.minProduction = production;
            this.updated = true;
        }
    }

    /**
     * @return the maximum quantity produced per run.
     */
    public int getMaxProduction() {
        return this.maxProduction;
    }

    /**
     * Sets the maximum production quantity, marking this entry {@link #isUpdated() updated} only if the value
     * actually changes.
     */
    public void setMaxProduction(int production) {

        if (this.maxProduction != production) {
            this.maxProduction = production;
            this.updated = true;
        }
    }

    /**
     * @return {@code true} if any cost/production field has been changed via its setter since the flag was last
     *       cleared with {@link #setUpdated(boolean)}.
     */
    public boolean isUpdated() {
        return this.updated;
    }

    /**
     * Explicitly sets/clears the dirty flag (e.g. to reset it to {@code false} after persisting changes).
     */
    public void setUpdated(boolean update) {
        this.updated = update;
    }
}
