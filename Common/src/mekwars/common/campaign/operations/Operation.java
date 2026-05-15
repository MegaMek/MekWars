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
 * Stores all params and info relevant to this particular
 * Operation (launch params). SmallOperations hold single-game
 * info. LongOperations hold status for multigame affairs.
 *
 * There's actually very little interaction between an Operation
 * and ongoing games/resolutions. Operation should be looked at
 * as a template or guidebook. When fed to a resolver in conjunction
 * with a functional operation (long or short), actual results are
 * generated.
 *
 * Generally speaking, there is only one instance of each type of
 * operation on a server.
 *
 * RELATIONSHIPS OF NOTE:
 * - ShortOperation.java
 * - LongOperation.java
 * - ModifiyingOperations.java [userland: Special Ops]
 */
package mekwars.common.campaign.operations;

//IMPORTS

import java.util.Properties;
import java.util.TreeMap;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.MWXMLWriter;
import mekwars.common.MWXmlSerializable;
import mekwars.common.util.MMNetXStream;

public class Operation implements MWXmlSerializable {
    private static final MMLogger LOGGER = MMLogger.create(Operation.class);

    //IVARS

    /*
     * Static ints, used as quick indicators. In particular:
     * - indicate that op is pure short (no long portion)
     * - indicate that modifiers can be used with op
     * [Expect more overtime ???]
     */
    public static int TYPE_SHORT_ONLY = 0; //default
    public static int TYPE_SHORT_AND_LONG = 1;

    public static int MODS_NOT_ACCEPTED = 0;//default
    public static int MODS_ACCEPTED = 1;
    //TreeMap of modifiers. As modifiers are loaded, those
    //targeting an operation are added to this map.
    TreeMap<String, ModifyingOperation> modifyingOperations;
    //Operation properties (hashtable of configured params)
    Properties opValues;
    //other loads ...
    DefaultOperation opsDefaults;
    String opName;//Name of this op. EG - "Assault"
    //private ints which hold current state
    private int type_indicator;
    private int mods_indicator;

    /**
     * Operation CONSTRUCTOR. Takes a name (used to assemble filenames for param loading) and a set of default vals.
     * <p>
     * Operations are constructed in OperationLoader.java
     */
    public Operation(String opName, DefaultOperation defaults, Properties params) {

        //save name
        this.opName = opName;

        //save the default parameters
        opsDefaults = defaults;

        //set the default indicators
        type_indicator = Operation.TYPE_SHORT_ONLY;
        mods_indicator = Operation.MODS_NOT_ACCEPTED;

        //create mod map
        modifyingOperations = new TreeMap<>();

        //set the value tables
        opValues = params;
    }

    public boolean getBooleanValue(String valToGet) {
        try {
            return Boolean.parseBoolean(getValue(valToGet));
        } catch (Exception ex) {
            return false;
        }
    }

    public String getValue(String valToGet) {
        return getValue(valToGet, true);
    }

    /**
     * Method which attempts to look up the value of a given Paramater in an Operation's local Tree. If the value is
     * unavailable, for any reason (typo, intentionally unset), a default value is checked and returned.
     */
    public String getValue(String valToGet, boolean log) {
        //look at the short list every time
        String toReturn = (String) opValues.get(valToGet);

        //if not present, load a default
        if (toReturn == null) {
            toReturn = opsDefaults.getDefault(valToGet);
        }

        //catastrophic failure. sysexit.
        if (toReturn == null && log) {
            LOGGER.error(STR."Failed getting value \"\{valToGet}\" from \{this.getName()} and DefaultOp. Returning null.");
        }

        return toReturn;
    }

    /**
     * Method which returns name of an operation, as drawn from filename.
     */
    public String getName() {
        return this.opName;
    }

    public int getIntValue(String valToGet) {
        return MathUtility.parseInt(getValue(valToGet), -1);
    }

    public double getDoubleValue(String valToGet) {
        return MathUtility.parseDouble(getValue(valToGet), -1.0);
    }

    public float getFloatValue(String valToGet) {
        return MathUtility.parseFloat(getValue(valToGet), 1.0f);
    }

    /**
     * Method which adds a mod op to this operation's tree of valid mods. Set from OperationManager @ load time, drawn
     * from modops' target params.
     * <p>
     * Toggle mods indicator to show that this op does have potential mods to check for @ startup and during
     * resolution.
     */
    public void addModifyingOperation(ModifyingOperation m) {
        modifyingOperations.put(m.getName(), m);
        mods_indicator = Operation.MODS_ACCEPTED;
    }

    /**
     * Methods which return and set type info via a boolean (short only, long+short, etc.)
     */
    public int getTypeIndicator() {
        return type_indicator;
    }

    public void setTypeIndicator(int i) {
        type_indicator = i;
    }

    /**
     * Methods which set and returns modifier status (accepts or no-mods, etc)
     */
    public int getModsIndicator() {
        return mods_indicator;
    }

    public void setModsIndicator(int i) {
        mods_indicator = i;
    }

    @Override
    public void writeToXmlFile(String folderName, String fileName) {
        MWXMLWriter writer = new MWXMLWriter(folderName, fileName, opValues);
        writer.writeToFile();
    }

    @Override
    public String getXmlString() {
        MMNetXStream xml = new MMNetXStream();
        return xml.toXML(opValues);
    }

}//end OperationsManager class
