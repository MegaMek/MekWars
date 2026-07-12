/*
 * MekWars - Copyright (C) 2008
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

/**
 *
 * @author Torren (Jason Tighe) 3.9.08
 *
 */

package mekwars.common.util;

import mekwars.common.Unit;

/**
 * Configuration/value holder describing a rule for converting a named salvage/repair "component" into critical slots
 * ("crits") on a unit. An instance captures which component ({@link #critName}) applies to which kind of unit
 * ({@link #componentUsedType}, {@link #componentUsedWeight}) and the minimum critical-slot level it is valid for
 * ({@link #minCritLevel}). Used by the campaign salvage/repair system when deciding how components map to critical
 * hits, and serialized/deserialized via {@link #toString(String)} / a corresponding parser elsewhere in that system.
 */
public class ComponentToCritsConverter {

    /** Minimum critical-slot level this rule applies to. */
    private int minCritLevel = 10;
    /** Unit category (e.g. {@link Unit#MEK}) this component rule applies to. */
    private int componentUsedType = Unit.MEK;
    /** Unit weight class (e.g. {@link Unit#LIGHT}) this component rule applies to. */
    private int componentUsedWeight = Unit.LIGHT;
    /** Name of the component/critical this rule governs; "All" means it is not restricted to a specific component. */
    private String critName = "All";

    /**
     * @return the minimum critical-slot level this rule applies to
     */
    public int getMinCritLevel() {
        return this.minCritLevel;
    }

    /**
     * @param level the minimum critical-slot level this rule should apply to
     */
    public void setMinCritLevel(int level) {
        this.minCritLevel = level;
    }

    /**
     * @return the unit type (e.g. {@link Unit#MEK}) this component rule applies to
     */
    public int getComponentUsedType() {
        return this.componentUsedType;
    }

    /**
     * @param type the unit type (e.g. {@link Unit#MEK}) this component rule should apply to
     */
    public void setComponentUsedType(int type) {
        this.componentUsedType = type;
    }

    /**
     * @return the unit weight class (e.g. {@link Unit#LIGHT}) this component rule applies to
     */
    public int getComponentUsedWeight() {
        return this.componentUsedWeight;
    }

    /**
     * @param weight the unit weight class (e.g. {@link Unit#LIGHT}) this component rule should apply to
     */
    public void setComponentUsedWeight(int weight) {
        this.componentUsedWeight = weight;
    }

    /**
     * @return the name of the component/critical this rule governs
     */
    public String getCritName() {
        return this.critName;
    }

    /**
     * @param crit the name of the component/critical this rule should govern
     */
    public void setCritName(String crit) {
        this.critName = crit;
    }

    /**
     * @return this rule serialized using {@code "|"} as the field separator; see {@link #toString(String)}.
     */
    public String toString() {
        return this.toString("|");
    }

    /**
     * Serializes this rule's fields into a single delimited string in the order: crit name, min crit level,
     * component used type, component used weight, each separated (and trailed) by {@code token}. Note the crit
     * name is followed by a literal space before the first token, so the format is
     * {@code "<critName> <token><minCritLevel><token><componentUsedType><token><componentUsedWeight><token>"}.
     *
     * @param token the delimiter string to place between (and after) each field
     * @return the delimited string representation described above
     */
    public String toString(String token) {

        return critName +
                     " " +
                     token +
                     minCritLevel +
                     token +
                     componentUsedType +
                     token +
                     componentUsedWeight +
                     token;
    }

}
