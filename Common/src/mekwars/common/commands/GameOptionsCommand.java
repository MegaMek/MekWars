/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Copyright (C) 2026 Helge Richter (McWizard)
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


package mekwars.common.commands;

import java.io.File;
import java.util.HashMap;
import java.util.StringTokenizer;

import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.Option;
import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class GameOptionsCommand extends Command {
    private final static MMLogger LOGGER = MMLogger.create(GameOptionsCommand.class);

    public GameOptionsCommand(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        client.getGameOptions().clear();
        HashMap<String, IBasicOption> optionsHash = new HashMap<>();
        IBasicOption gameOption;

        File mmconf = new File("./mmconf");

        if (!mmconf.exists()) {
            mmconf.mkdir();
        }

        while (stringTokenizer.hasMoreElements()) {
            String option = stringTokenizer.nextToken();
            String value = stringTokenizer.nextToken();

            try {
                gameOption = new Option(new GameOptions(), option, Integer.parseInt(value));
                optionsHash.put(gameOption.getName(), gameOption);
            } catch (Exception ex) {
                try {
                    gameOption = new Option(new GameOptions(), option, Float.parseFloat(value));
                    optionsHash.put(gameOption.getName(), gameOption);
                } catch (Exception ex1) {
                    try {
                        // This was always detecting as boolean - strings were returning false
                        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                            gameOption = new Option(new GameOptions(), option, Boolean.parseBoolean(value));
                            optionsHash.put(gameOption.getName(), gameOption);
                        } else {
                            gameOption = new Option(new GameOptions(), option, value);
                            optionsHash.put(gameOption.getName(), gameOption);
                        }
                    } catch (Exception ex2) {
                        LOGGER.info(STR."Unknown format: \{option} :: \{value}");
                    }
                }
            }
        }//end while

        client.getGameOptions().addAll(optionsHash.values());

        GameOptions.saveOptions(client.getGameOptions());

        client.setWaiting(false);
    }//end execute

    /**
     *
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     *
     */
    @Override
    public void parseArguments(String s) {

    }
}//end GameOptionsCommand.java
