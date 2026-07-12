/*
 * Copyright (C) 2005 - nmorris (urgru@users.sourceforge.net)
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

package mekwars.common.campaign.operations;

import java.util.Properties;

/**
 * ModifyingOperations are much more closely related to Operations properer than the functional ops (Long and Small).
 * Modifiers are like the basic Op in several ways:
 * <p>
 * - generally only a single instance created per server - store campaign-related entry requirements (XP, etc.) - read
 * configs from disk instead of another class
 * <p>
 * What exactly is a modifying operation? In practice, it should have more restrictive entry requirements than an
 * operation and modify an Operation's variables.
 * <p>
 * Example: There is a "Planetary Assault" operation which is generally available. The ModifyingOperation "Fast Mover"
 * has Planetary Assault set as a legal target. A player who meets the Fast Mover requirements can choose to use is
 * modifiers.
 * <p>
 * Modifiers should be used to: 1) create high-risk games. Require pay-ins or disincentives (salvage reductions, CBill
 * costs) for higher payouts, or 2) Encourage diversity of play. Use modifier requirements and advantages to get players
 * to use unusual or otherwise undesirable forces, offer incentives for games w/ no assault units, etc.
 * <p>
 * ModOp params SUPERSEDE those set in an Operation. Some things are 0-checked; however, many parameters will accept
 * potentially damaging negative settings or params which strongly conflict with the underlying Operation. This is an
 * Operator request (maximum flexibility) but will require strenuous testing of ModOp settings.
 * <p>
 * Unlike {@link Operation}, ModifyingOperation has no fallback to {@link DefaultOperation} - it is a much thinner
 * wrapper around a raw parameter set with no defaulting logic, no type/long-vs-short indicators, and no notion of
 * further modifiers stacking on top of it. A {@link Operation} keeps track of the ModifyingOperations legal for it
 * via {@link Operation#addModifyingOperation(ModifyingOperation)}; this class itself does not know which Operations
 * it targets (that is presumably read from its own params, e.g. a "LinkedOperations"-style key, and applied by the
 * loader/resolver rather than by this class).
 *
 * @see Operation
 * @see DefaultOperation
 */

public class ModifyingOperation {

    //IVARS
    /** Name of this ModifyingOperation (e.g. "Fast Mover"), derived from its config filename at load time. */
    private final String opName;
    /** This ModifyingOperation's configured parameter values, keyed by parameter name. These values supersede the corresponding parameters of any Operation this modifier is applied to; there is no fallback/default lookup as there is for {@link Operation}. */
    private final Properties modValues;

    /**
     * ModifyingOperation CONSTRUCTOR. Takes a name (same as used to assemble filenames for param loading) and a set of
     * param values.
     * <p>
     * ModifyingOperations are constructed in OperationLoader.java
     *
     * @param opName    name of this ModifyingOperation, used both as display name and to assemble filenames for
     *                  param loading
     * @param modValues this ModifyingOperation's configured parameter values, already parsed from its config file
     */
    public ModifyingOperation(String opName, Properties modValues) {
        this.opName = opName;
        this.modValues = modValues;
    }

    //METHODS

    /**
     * Method that returns values, pre-cast to string.
     *
     * @param valToGet name of the parameter to look up
     * @return the value cast to {@link String}, or {@code null} if not set; throws {@link ClassCastException} if
     *         the stored value is somehow not a String (not expected in normal use, since {@link Properties} only
     *         stores String values via its typical API)
     */
    public String getValueAsString(String valToGet) {
        return (String) getModValue(valToGet);
    }

    /**
     * Method that attempts to look up the value of a given Parameter in a ModOperation's local Tree. If the value is
     * unavailable, a null is returned.
     * <p>
     * Note there is no fallback to {@link DefaultOperation} here (contrast with
     * {@link Operation#getValue(String, boolean)}); an unset parameter simply yields {@code null}.
     *
     * @param valToGet name of the parameter to look up
     * @return the raw stored value, or {@code null} if not set
     */
    public Object getModValue(String valToGet) {
        return modValues.get(valToGet);
    }

    /**
     * Method that returns the name of ModOp, as derived from the filename @ load time.
     *
     * @return this ModifyingOperation's name
     */
    public String getName() {
        return this.opName;
    }

}//end OperationsManager class
