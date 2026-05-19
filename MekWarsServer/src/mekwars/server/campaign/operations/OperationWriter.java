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
package mekwars.server.campaign.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.campaign.operations.Operation;
import mekwars.server.campaign.CampaignMain;


/**
 * A utility that writes operation information.
 * <p>
 * Has methods to write - LongOperation SAVES
 * <p>
 * NOTE: The loader/writer DO NOT handle the reading and writing of all the Operation/ModifyingOperation params.
 * Modification of these files must be done by hand or with an external utility
 * <p>
 * The OperationWriter is called after loading and writes out an OpList.txt which contains name, range, short/long, and
 * color information for all Operations.
 * <p>
 * This text file is sent to the clients (MD5) when they connect and used to build menus and maps.
 */
public class OperationWriter {
    private final static MMLogger LOGGER = MMLogger.create(OperationWriter.class);
    //IVARS

    //CONSTRUCTORS
    public OperationWriter() {
        //contents
    }

    //METHODS
    public void writeOpList(TreeMap<String, Operation> ops) {

        //path to our file
        String path = "./data/operations/OpList.txt";

        //if there is an old file, delete it before rewriting
        File oldFile = new File(path);
        if (oldFile.exists() && oldFile.delete()) {
            LOGGER.info("Deleted old file..");
        }

        //construct a new file
        try {
            PrintStream printStream = new PrintStream(new FileOutputStream(path));
            printStream.println(STR."#Timestamp=\{System.currentTimeMillis()}");

            for (Operation currO : ops.values()) {
                String name = STR."\{currO.getName()}*";
                String range = STR."\{currO.getValue("OperationRange")}*";
                String color = STR."\{currO.getValue("OperationColor")}*";
                String hasLong = STR."\{currO.getTypeIndicator()}*";//actually an INT. 0 == short. 1 == long.
                String launchOn = STR."\{currO.getValue("PercentageToAttackOnWorld")}*";
                String launchFrom = STR."\{currO.getValue("PercentageToAttackOffWorld")}*";
                String minOwn = STR."\{currO.getValue("MinPlanetOwnership")}*";
                String minOwnIBD = STR."\{currO.getValue("MinPlanetOwnershipIgnoredByDefender")}*"; //Baruk Khazad! - 20151003
                String maxOwn = STR."\{currO.getValue("MaxPlanetOwnership")}*";
                String reserveOnly = STR."\{currO.getValue("OnlyAllowedFromReserve")}*";
                String activeOnly = STR."\{currO.getValue("OnlyAllowedFromActive")}*";
                String minSubFactionLevel = STR."\{currO.getValue("MinSubFactionAccessLevel")}*";

                String facInfo = "any*";

                if (Boolean.parseBoolean(currO.getValue("OnlyAgainstFactoryWorlds"))) {
                    facInfo = "only*";
                } else if (Boolean.parseBoolean(currO.getValue("OnlyAgainstNonFactoryWorlds"))) {
                    facInfo = "none*";
                }

                String homeworldInfo = "any*";
                if (Boolean.parseBoolean(currO.getValue("OnlyAgainstHomeWorlds"))) {
                    homeworldInfo = "only*";
                } else if (Boolean.parseBoolean(currO.getValue("OnlyAgainstNonHomeWorlds"))) {
                    homeworldInfo = "none*";
                }

                StringBuilder legalDefenders = new StringBuilder();
                if (!currO.getValue("LegalDefendFactions").trim().isEmpty()) {
                    legalDefenders.append(currO.getValue("LegalDefendFactions").trim()).append("*");
                } else if (!currO.getValue("IllegalDefendFactions").trim().isEmpty()) {
                    //determine which factions *can't* use the type
                    StringTokenizer illegalTokenizer = new StringTokenizer(currO.getValue("IllegalDefendFactions")
                                                                                 .trim(), "$");
                    TreeSet<String> illegals = new TreeSet<>();
                    while (illegalTokenizer.hasMoreTokens()) {
                        illegals.add(illegalTokenizer.nextToken());
                    }

                    //compare all houses to the TreeSet and look for matches
                    for (House currH : CampaignMain.campaignMain.getData().getAllHouses()) {
                        if (!illegals.contains(currH.getName())) {legalDefenders.append(currH.getName()).append("$");}
                    }

                    legalDefenders.append("*");

                } else {
                    legalDefenders.append("allFactions*");
                }

                StringBuilder allowPlanetFlags = new StringBuilder(currO.getValue("AllowPlanetFlags"));
                if (allowPlanetFlags.isEmpty()) {
                    allowPlanetFlags.append("^ ^*");
                } else {
                    allowPlanetFlags.append("*");
                }

                StringBuilder disallowPlanetFlags = new StringBuilder(currO.getValue("DisallowPlanetFlags"));
                if (disallowPlanetFlags.isEmpty()) {
                    disallowPlanetFlags.append("^ ^*");
                } else {
                    disallowPlanetFlags.append("*");
                }

                printStream.println(name
                                          + range
                                          + color
                                          + hasLong
                                          + facInfo
                                          + homeworldInfo
                                          + launchOn
                                          + launchFrom
                                          + minOwn
                                          + maxOwn
                                          + reserveOnly
                                          + activeOnly
                                          + legalDefenders.toString().trim()
                                          + allowPlanetFlags.toString().trim()
                                          + disallowPlanetFlags.toString().trim()
                                          + minSubFactionLevel
                                          + minOwnIBD);      //Baruk Khazad! - 20151003

            }
            printStream.close();
        } catch (FileNotFoundException fe) {
            LOGGER.error(fe, "Error: could not find {}", path);
        } catch (Exception ex) {
            LOGGER.error(ex, "Unknown exception: {}", ex.getLocalizedMessage());
        }
    }//end writeOpList
}//end OperationsManager class
