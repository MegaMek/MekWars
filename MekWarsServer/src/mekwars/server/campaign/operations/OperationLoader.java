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
 * A utility which reads operation information.
 *
 * Has methods to read
 *  - Operations
 *  - ModifyingOperations
 *  - SAVED LongOperations
 *
 * NOTE: The loader/writer DO NOT handle the reading and writing
 * of all the Operation/ModifyingOperation params. Modification of
 * these files must be done by hand, or with an external utility
 */
package mekwars.server.campaign.operations;

//IMPORTS

import java.io.FileInputStream;
import java.util.Properties;

import megamek.logging.MMLogger;
import mekwars.common.campaign.operations.DefaultOperation;
import mekwars.common.campaign.operations.ModifyingOperation;
import mekwars.common.campaign.operations.Operation;

public class OperationLoader {
    private final static MMLogger LOGGER = MMLogger.create(OperationLoader.class);

    //IVARS
    DefaultOperation defaults;

    //CONSTRUCTORS
    public OperationLoader() {
        defaults = new DefaultOperation();
    }

    //METHODS

    /**
     * Method which loads an operations values from server flat files. Unspecified values will revert to hardcoded
     * defaults.
     * <p>
     * NOTE: two files are read, but only one Properties/Hashmap is populated.
     * <p>
     * Also note: # can be used w/i op files as a comment. tabs are treated as whitespace. property names are case
     * sensitive.
     * <p>
     * Suggested formatting follows ... ---
     * <p>
     * #COMMENT RE: SETTING #MORE COMMENT DETAIL Param1			= value LongNameParam	= value
     * <p>
     * #COMMENTS ON NEW BLOCK Param2			= value
     * <p>
     * - See Properties javadoc for additional commenting info.
     *
     * @urgru 5/30/05
     */
    public Operation loadOpValues(String opName) {
        Properties opValues = new Properties();

        //attempt to load short vals
        String shortFilename = String.format("./data/operations/short/%s", opName);
        try {
            opValues.load(new FileInputStream(shortFilename));
        } catch (Exception e) {
            LOGGER.error(e, "Problems loading short op: {}", opName);
        }

        //attempt to load long vals
        String longFilename = String.format("./data/operations/long/%s", opName);
        try {
            opValues.load(new FileInputStream(longFilename));
        } catch (Exception e) {
            LOGGER.error(e, "Problems loading long op: {}", opName);
        }

        opName = opName.substring(0, opName.length() - 4);//remove ".txt"
        return new Operation(opName.trim(), defaults, opValues);
    }

    /**
     * Method which loads modifying operations from flat files. Unspecified values revert to those of the underlying
     * operation first, and then to values from DefaultOperation.
     * <p>
     * Unlike standard operations, which read from 2 files, modops only read from one flat file.
     * <p>
     * See this.loadOpValues() and Properties javadoc for commenting info.
     * <p>
     * NOTE: addition of modops to standard Op's mod treemaps is handled in OperationManager.java, shortly after this
     * load method.
     *
     * @urgru 6/10/05
     */
    public ModifyingOperation loadModOpValues(String opName) {

        java.util.Properties modValues = new java.util.Properties();

        //attempt load
        String modFilename = String.format("./data/operations/modifiers/%s", opName);
        try {
            modValues.load(new java.io.FileInputStream(modFilename));
        } catch (Exception e) {
            LOGGER.error(e, String.format("Problems loading mod op: %s", opName));
        }

        opName = opName.substring(0, opName.length() - 5);//remove ".txt"
        return new ModifyingOperation(opName.trim(), modValues);
    }

}//end OperationsManager class
