/*
 * Copyright (C) 2007 - Bob Eldred (billypinhead@users.sourceforge.net)
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.StringTokenizer;

import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.panels.CCommPanel;

/**
 * @author Spork
 *       <p>
 *       Handles Build Table up/downloading for admins/mods
 *
 */
public class BuildTableCommand extends Command {
    private final static MMLogger LOGGER = MMLogger.create(BuildTableCommand.class);

    /**
     *
     */
    public BuildTableCommand(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);

        String cmd = stringTokenizer.nextToken();
        boolean viewer = false;

        if (cmd.equalsIgnoreCase("LS")) {
            StringTokenizer folderTokenizer = new StringTokenizer(stringTokenizer.nextToken(), "?");

            if (stringTokenizer.hasMoreTokens()) {
                viewer = Boolean.parseBoolean(stringTokenizer.nextToken());
            }

            while (folderTokenizer.hasMoreTokens()) {
                //Token 1 is the folder
                String dName = folderTokenizer.nextToken();
                LOGGER.info(dName);
                // Token 2 is the names of the lists
                if (folderTokenizer.hasMoreTokens()) {
                    StringTokenizer listTokenizer = new StringTokenizer(folderTokenizer.nextToken(), "*");
                    while (listTokenizer.hasMoreTokens()) {
                        String fileName = listTokenizer.nextToken();
                        long time = 0;
                        File file = new File(STR."./data/buildtables/\{dName}/\{fileName}");

                        if (file.exists()) {
                            time = file.lastModified();
                        }

                        client.sendChat(
                              STR."\{IClient.CAMPAIGN_PREFIX}AdminRequestBuildTable get#\{dName}#\{fileName}#\{time}");
                    }
                }

            }

            if (viewer) {
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}AdminRequestBuildTable view");
            }

        }
        if (cmd.equalsIgnoreCase("PLS")) {
            StringTokenizer folderTokenizer = new StringTokenizer(stringTokenizer.nextToken(), "?");

            if (stringTokenizer.hasMoreTokens()) {
                viewer = Boolean.parseBoolean(stringTokenizer.nextToken());
            }

            while (folderTokenizer.hasMoreTokens()) {
                //Token 1 is the folder
                String dName = folderTokenizer.nextToken();
                LOGGER.info(dName);
                // Token 2 is the names of the lists
                if (folderTokenizer.hasMoreTokens()) {
                    StringTokenizer listTokenizer = new StringTokenizer(folderTokenizer.nextToken(), "*");
                    while (listTokenizer.hasMoreTokens()) {
                        String fileName = listTokenizer.nextToken();
                        long time = 0;
                        File file = new File(STR."./data/buildtables/\{dName}/\{fileName}");

                        if (file.exists()) {
                            time = file.lastModified();
                        }
                        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}RequestBuildTable get#\{dName}#\{fileName}#\{time}");
                    }
                }

            }
            if (viewer) {
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}RequestBuildTable view");
            }
        } else if (cmd.equalsIgnoreCase("BuildTableCommand")) {
            String folder = stringTokenizer.nextToken();
            String table = stringTokenizer.nextToken();
            boolean isMod = client.isMod();
            File file = new File("./data/buildtables");

            if (!file.exists()) {
                file.mkdir();
            }

            file = new File(STR."./data/buildtables/\{folder}");

            if (!file.exists()) {
                file.mkdir();
            }

            file = new File(STR."./data/buildtables/\{folder}/\{table}");

            try {
                file.createNewFile();
            } catch (IOException e1) {
                LOGGER.error(e1, "Unable to create build table file");
            }

            FileOutputStream out;
            try {
                out = new FileOutputStream(file);
                PrintStream printStream = new PrintStream(out);
                while (stringTokenizer.hasMoreTokens()) {
                    printStream.println(stringTokenizer.nextToken());
                }

                if (isMod) {
                    client.addToChat(STR."Received build table \{folder}/\{table}", CCommPanel.CHANNEL_MISC);
                }

                printStream.close();

                try {
                    out.close();
                } catch (IOException e) {
                    LOGGER.error(e, "Unable to close build table file");
                }
            } catch (FileNotFoundException e) {
                LOGGER.error(e, "Unable to find build table file");
            }
        } else if (cmd.equalsIgnoreCase("VS")) {
            client.setWaiting(false);
        }
    }

    /**
     *
     */
    @Override
    public void parseReplyArgs(String string) {

    }

    /**
     *
     */
    @Override
    public void setClient(IClient client) {

    }

    /**
     *
     */
    @Override
    public void parseArguments(String string) {

    }
}
