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


/*
 * ModifyingOperaions are much more closely related to Operations
 * proper than the functional ops (Long and Small). Modifiers are
 * like the basic Op in several ways:
 *
 *  - generally only a single instance created per server
 *  - store campaign related entry requirements (XP, etc)
 *  - read configs from disk instead of another class
 *
 * What exactly is a modifying operation? In practice, it should
 * have more restrictive entry requirements than an operation, and
 * modify an Operation's variables.
 *
 * Example: There is a "Planetary Assault" operation which is generally
 * available. The ModifyingOperation "Fast Mover" has Planetary Assault
 * set as a legal target. A player who meets the Fast Mover requirements
 * can choose to use is modifiers.
 *
 * Modifiers should be used to:
 * 1) create high risk games. require pay-ins or disincentives (salvage
 *    reductions, CBill costs) for higher payouts, or
 * 2) Encourage diversity of play. Use modifier requirements and advantages
 *    to get players to use unusual or otherwise undesirable forces, offer
 *    incentives for games w/ no assault units, etc.
 *
 * ModOp params SUPERCEDE those set in an Operation. Some things are 0-checked;
 * however, many paramaters will accept potentially damaging negative settings or
 * params which strongly conflict with the underlying Operation. This is an
 * Operator request (maximum flexibility), but will require strenuous testing
 * of ModOp settings.
 */

package mekwars.common.campaign.operations;

import java.util.Properties;

//IMPORTS

public class ModifyingOperation {

    //IVARS
    private final String opName;
    private final Properties modValues;

    /**
     * ModifyingOperation CONSTRUCTOR. Takes a name (same as used to assemble filenames for param loading) and a set of
     * param values.
     * <p>
     * ModifyingOperations are constructed in OperationLoader.java
     */
    public ModifyingOperation(String opName, Properties modValues) {
        this.opName = opName;
        this.modValues = modValues;
    }

    //METHODS

    /**
     * Method which returns values, pre-cast to string.
     */
    public String getValueAsString(String valToGet) {
        return (String) getModValue(valToGet);
    }

    /**
     * Method which attempts to look up the value of a given Paramater in an ModOperation's local Tree. If the value is
     * unavailable, a null is returned.
     */
    public Object getModValue(String valToGet) {
        return modValues.get(valToGet);
    }

    /**
     * Method which returns name of ModOp, as derived from filename @ loadtime.
     */
    public String getName() {
        return this.opName;
    }

}//end OperationsManager class
