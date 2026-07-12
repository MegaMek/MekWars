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
 * Created on 18.04.2004
 *
 */
package mekwars.common;

/**
 * Immutable-in-practice wrapper pairing the internal MegaMek name of a "Level 3" pilot option (a Special Pilot
 * Ability / quirk toggle from MegaMek's pilot options list) with a boolean on/off value.
 * <p>
 * Used as a simple carrier when MekWars needs to communicate/store a single pilot option's name and state (e.g.
 * when transmitting a pilot's selected SPAs between server and client), rather than depending directly on
 * MegaMek's own pilot-option data structures.
 *
 * @author Helge Richter
 */
public class MegaMekPilotOption {
    /** The internal (MegaMek) name of the pilot option/SPA, as used by MegaMek's option lookup. */
    private String megaMekName;
    /** Whether the option is enabled ({@code true}) or disabled ({@code false}) for the pilot. */
    private boolean value;

    /**
     * No-arg constructor leaving {@link #megaMekName} {@code null} and {@link #value} {@code false}. Fields must be
     * populated by other means (e.g. reflection/deserialization) since there are no setters.
     */
    public MegaMekPilotOption() {

    }

    /**
     * @param name  the internal MegaMek name of the pilot option.
     * @param value whether the option is currently enabled for the pilot.
     */
    public MegaMekPilotOption(String name, boolean value) {
        megaMekName = name;
        this.value = value;
    }

    /**
     * @return Returns the megaMekName.
     */
    public String getMegaMekName() {
        return megaMekName;
    }

    /**
     * @return Returns the value.
     */
    public boolean isValue() {
        return value;
    }
}
