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

/**
 * Represents a single server-configured campaign "Operation" - the template/ruleset a player launches or joins a
 * battle under (e.g. an "Assault" or "Conquer" type attack). To the player, an Operation is the menu entry they pick
 * when starting or answering an attack, and its configured parameters (construction limits, costs, payouts, salvage
 * rules, victory conditions, etc.) determine what forces are legal, what the battle looks like, and what the
 * participants win or lose. To the server, an Operation is a bag of named string parameters ({@link #opValues}) plus
 * a fallback to {@link DefaultOperation} for anything not explicitly overridden - this makes Operations tolerant of
 * missing/typo'd config and lets new parameters be introduced without breaking existing Operation definitions.
 * <p>
 * An Operation is deliberately inert: it holds configuration and does very little on its own. It is read by other
 * classes (validators/resolvers, not present in this file) which combine an Operation's settings with the state of
 * an in-progress game/resolution to produce actual results. In other words, this class is a template or guidebook,
 * not an active participant in a fight.
 * <p>
 * There is normally only one live instance of a given Operation (e.g. one "Assault" object) per server; players
 * launching multiple attacks all reference the same configured Operation.
 * <p>
 * Operations are constructed and wired together by {@code OperationLoader.java}. Related classes referenced by
 * legacy comments in this package include {@code ShortOperation.java} (single-game rules) and
 * {@code LongOperation.java} (status across a series of games spanning multiple games); {@link ModifyingOperation}
 * ("Special Ops" in operator-facing terminology) can additionally attach to an Operation to override/supersede its
 * parameters for specific, more restrictive circumstances - see {@link #addModifyingOperation(ModifyingOperation)}.
 *
 * @see DefaultOperation
 * @see ModifyingOperation
 */
public class Operation implements MWXmlSerializable {
    private static final MMLogger LOGGER = MMLogger.create(Operation.class);

    //IVARS

    /*
     * Static ints, used as quick indicators. In particular:
     * - indicate that op is pure short (no long portion)
     * - indicate that modifiers can be used with op
     * [Expect more overtime ???]
     */
    /** Value for {@link #type_indicator} meaning this Operation is single-game only (no linked long-form portion). This is the default. */
    public static int TYPE_SHORT_ONLY = 0; //default
    /** Value for {@link #type_indicator} meaning this Operation has both a short (single game) and a long (multi-game) portion. */
    public static int TYPE_SHORT_AND_LONG = 1;

    /** Value for {@link #mods_indicator} meaning no {@link ModifyingOperation} currently targets this Operation. This is the default. */
    public static int MODS_NOT_ACCEPTED = 0;//default
    /** Value for {@link #mods_indicator} meaning at least one {@link ModifyingOperation} targets this Operation and should be considered. */
    public static int MODS_ACCEPTED = 1;
    //TreeMap of modifiers. As modifiers are loaded, those
    //targeting an operation are added to this map.
    /**
     * Modifying operations ("Special Ops") that are legal for this Operation, keyed by modifier name. Populated
     * incrementally via {@link #addModifyingOperation(ModifyingOperation)} as the server loads modifier configs and
     * discovers this Operation listed among their legal targets.
     */
    TreeMap<String, ModifyingOperation> modifyingOperations;
    //Operation properties (hashtable of configured params)
    /** This Operation's own explicitly-configured parameter values, keyed by parameter name (e.g. "MaxAttackerBV"). Values are always stored/read as Strings and parsed on demand by the {@code get*Value} accessors. */
    Properties opValues;
    //other loads ...
    /** Fallback parameter source consulted by {@link #getValue(String, boolean)} whenever {@link #opValues} has no entry for a requested key. */
    DefaultOperation opsDefaults;
    /** Name of this Operation (e.g. "Assault"), derived from its config filename at load time. */
    String opName;//Name of this op. EG - "Assault"
    //private ints that hold current state
    /** Current value of the type indicator; one of {@link #TYPE_SHORT_ONLY} or {@link #TYPE_SHORT_AND_LONG}. */
    private int type_indicator;
    /** Current value of the modifier-acceptance indicator; one of {@link #MODS_NOT_ACCEPTED} or {@link #MODS_ACCEPTED}. */
    private int mods_indicator;

    /**
     * Constructs an Operation from its name and already-loaded configuration.
     * <p>
     * Operations are constructed in OperationLoader.java, which reads the on-disk parameter file for {@code opName}
     * into {@code params} and supplies the shared {@link DefaultOperation} instance used as a fallback for any
     * parameter this Operation does not explicitly set.
     *
     * @param opName   name of this Operation, used both as display name and to assemble filenames for param loading
     * @param defaults shared default-value lookup consulted when a parameter is missing from {@code params}
     * @param params   this Operation's explicitly-configured parameter values (already parsed from its config file)
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

    /**
     * Looks up a parameter and parses it as a boolean via {@link Boolean#parseBoolean(String)}.
     *
     * @param valToGet name of the parameter to look up
     * @return the parsed boolean value, or {@code false} if the parameter is missing/unparsable or lookup throws
     */
    public boolean getBooleanValue(String valToGet) {
        try {
            return Boolean.parseBoolean(getValue(valToGet));
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Looks up a parameter's raw string value, logging an error if it cannot be resolved at all (see
     * {@link #getValue(String, boolean)}).
     *
     * @param valToGet name of the parameter to look up
     * @return the resolved value, or {@code null} if neither this Operation nor its {@link DefaultOperation} has it
     */
    public String getValue(String valToGet) {
        return getValue(valToGet, true);
    }

    /**
     * Method that attempts to look up the value of a given Parameter in an Operation's local Tree. If the value is
     * unavailable, for any reason (typo, intentionally unset), a default value is checked and returned.
     * <p>
     * NOTE: despite the inline comment below referring to this branch as "catastrophic failure. sys exit.", this
     * method does not actually terminate the server - it only logs an error and returns {@code null} to the caller,
     * which must handle a null result itself.
     *
     * @param valToGet name of the parameter to look up
     * @param log      if {@code true}, logs an error when the value is unresolvable from both this Operation and
     *                 its {@link DefaultOperation}; pass {@code false} to suppress that logging (e.g. for
     *                 expected-optional lookups)
     * @return the resolved value, or {@code null} if it could not be found anywhere
     */
    public String getValue(String valToGet, boolean log) {
        //look at the short list every time
        String toReturn = (String) opValues.get(valToGet);

        //if not present, load a default
        if (toReturn == null) {
            toReturn = opsDefaults.getDefault(valToGet);
        }

        //catastrophic failure. sys exit.
        if (toReturn == null && log) {
            LOGGER.error(String.format("Failed getting value \"%s\" from %s and DefaultOp. Returning null.", valToGet, this.getName()));
        }

        return toReturn;
    }

    /**
     * Method that returns the name of an operation, as drawn from the filename.
     */
    public String getName() {
        return this.opName;
    }

    /**
     * Looks up a parameter and parses it as an int.
     * <p>
     * NOTE: unlike {@link #getFloatValue(String)} (which falls back to {@code 1.0f}), this falls back to
     * {@code -1} on missing/unparsable values, so a "not found" case is distinguishable here but not there.
     *
     * @param valToGet name of the parameter to look up
     * @return the parsed int value, or {@code -1} if missing/unparsable
     */
    public int getIntValue(String valToGet) {
        return MathUtility.parseInt(getValue(valToGet), -1);
    }

    /**
     * Looks up a parameter and parses it as a double.
     *
     * @param valToGet name of the parameter to look up
     * @return the parsed double value, or {@code -1.0} if missing/unparsable
     */
    public double getDoubleValue(String valToGet) {
        return MathUtility.parseDouble(getValue(valToGet), -1.0);
    }

    /**
     * Looks up a parameter and parses it as a float.
     * <p>
     * NOTE: the fallback here is {@code 1.0f}, inconsistent with {@link #getIntValue(String)} and
     * {@link #getDoubleValue(String)} which fall back to {@code -1}/{@code -1.0}. A missing float parameter is
     * therefore indistinguishable from an explicitly-configured value of {@code 1.0}.
     *
     * @param valToGet name of the parameter to look up
     * @return the parsed float value, or {@code 1.0f} if missing/unparsable
     */
    public float getFloatValue(String valToGet) {
        return MathUtility.parseFloat(getValue(valToGet), 1.0f);
    }

    /**
     * Method that adds mod op to this operation's tree of valid mods. Set from OperationManager @ load time, drawn from
     * mod ops' target params.
     * <p>
     * Toggle the mod indicator to show that this op does have potential mods to check for @ startup and during
     * resolution.
     *
     * @param m the {@link ModifyingOperation} that lists this Operation as one of its legal targets; stored keyed
     *          by its name, overwriting any previously-registered modifier of the same name
     */
    public void addModifyingOperation(ModifyingOperation m) {
        modifyingOperations.put(m.getName(), m);
        mods_indicator = Operation.MODS_ACCEPTED;
    }

    /**
     * Methods that return and set type info via a boolean (short only, long+short, etc.)
     *
     * @return current value of {@link #type_indicator}: {@link #TYPE_SHORT_ONLY} or {@link #TYPE_SHORT_AND_LONG}
     */
    public int getTypeIndicator() {
        return type_indicator;
    }

    /**
     * Sets the type indicator (see {@link #TYPE_SHORT_ONLY}, {@link #TYPE_SHORT_AND_LONG}). No validation is
     * performed on {@code i} - callers are expected to pass one of the two defined constants.
     *
     * @param i new type indicator value
     */
    public void setTypeIndicator(int i) {
        type_indicator = i;
    }

    /**
     * Methods that set and returns modifier status (accepts or no-mods, etc.)
     *
     * @return current value of {@link #mods_indicator}: {@link #MODS_NOT_ACCEPTED} or {@link #MODS_ACCEPTED}
     */
    public int getModsIndicator() {
        return mods_indicator;
    }

    /**
     * Sets the modifier-acceptance indicator (see {@link #MODS_NOT_ACCEPTED}, {@link #MODS_ACCEPTED}). No
     * validation is performed on {@code i}.
     *
     * @param i new mods indicator value
     */
    public void setModsIndicator(int i) {
        mods_indicator = i;
    }

    /**
     * Serializes this Operation's configured parameter values ({@link #opValues}, not the resolved/defaulted view)
     * to an XML file on disk.
     *
     * @param folderName destination folder
     * @param fileName   destination file name
     */
    @Override
    public void writeToXmlFile(String folderName, String fileName) {
        MWXMLWriter writer = new MWXMLWriter(folderName, fileName, opValues);
        writer.writeToFile();
    }

    /**
     * Serializes this Operation's configured parameter values ({@link #opValues}) to an XML string, e.g. for
     * network transmission to clients.
     *
     * @return XML representation of {@link #opValues}
     */
    @Override
    public String getXmlString() {
        MMNetXStream xml = new MMNetXStream();
        return xml.toXML(opValues);
    }

}//end OperationsManager class
