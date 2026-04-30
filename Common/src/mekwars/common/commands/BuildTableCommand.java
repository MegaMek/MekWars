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
import java.util.StringTokenizer;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.panels.CCommPanel;
import mekwars.common.util.MWLogger;

/**
 * @author Spork
 *       <p>
 *       Handles Build Table up/downloading for admins/mods
 *
 */
public class BuildTableCommand extends Command {

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
        StringTokenizer st = decode(input);

        String cmd = st.nextToken();
        boolean viewer = false;
        if (cmd.equalsIgnoreCase("LS")) {
            StringTokenizer folderT = new StringTokenizer(st.nextToken(), "?");

            if (st.hasMoreTokens()) {
                viewer = Boolean.parseBoolean(st.nextToken());
            }

            while (folderT.hasMoreTokens()) {
                //Token 1 is the folder
                String dName = folderT.nextToken();
                MWLogger.infoLog(dName);
                // Token 2 is the names of the lists
                if (folderT.hasMoreTokens()) {
                    StringTokenizer listT = new StringTokenizer(folderT.nextToken(), "*");
                    while (listT.hasMoreTokens()) {
                        String fileName = listT.nextToken();
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
            StringTokenizer folderT = new StringTokenizer(st.nextToken(), "?");

            if (st.hasMoreTokens()) {
                viewer = Boolean.parseBoolean(st.nextToken());
            }

            while (folderT.hasMoreTokens()) {
                //Token 1 is the folder
                String dName = folderT.nextToken();
                MWLogger.infoLog(dName);
                // Token 2 is the names of the lists
                if (folderT.hasMoreTokens()) {
                    StringTokenizer listT = new StringTokenizer(folderT.nextToken(), "*");
                    while (listT.hasMoreTokens()) {
                        String fileName = listT.nextToken();
                        long time = 0;
                        File file = new File(STR."./data/buildtables/\{dName}/\{fileName}");

                        if (file.exists()) {
                            time = file.lastModified();
                        }
                        client.sendChat(
                              STR."\{IClient.CAMPAIGN_PREFIX}RequestBuildTable get#\{dName}#\{fileName}#\{time}");
                    }
                }

            }
            if (viewer) {
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}RequestBuildTable view");
            }
        } else if (cmd.equalsIgnoreCase("BuildTableCommand")) {
            String folder = st.nextToken();
            String table = st.nextToken();
            boolean isMod = client.isMod();
            java.io.File file = new java.io.File("./data/buildtables");

            if (!file.exists()) {
                file.mkdir();
            }

            file = new java.io.File(STR."./data/buildtables/\{folder}");

            if (!file.exists()) {
                file.mkdir();
            }

            file = new java.io.File(STR."./data/buildtables/\{folder}/\{table}");

            try {
                file.createNewFile();
            } catch (java.io.IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            java.io.FileOutputStream out;
            try {
                out = new java.io.FileOutputStream(file);
                java.io.PrintStream p = new java.io.PrintStream(out);
                while (st.hasMoreTokens()) {p.println(st.nextToken());}
                if (isMod) {
                    client.addToChat(STR."Received build table \{folder}/\{table}", CCommPanel.CHANNEL_MISC);
                }
                p.close();
                try {
                    out.close();
                } catch (java.io.IOException e) {
                    // TODO Auto-generated catch block
                    MWLogger.errLog(e);
                }
            } catch (java.io.FileNotFoundException e) {
                // TODO Auto-generated catch block
                MWLogger.errLog(e);
            }
        } else if (cmd.equalsIgnoreCase("VS")) {
            client.setWaiting(false);
        }
    }

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
    public void setClient(IClient client) {

    }

    /**
     *
     */
    @Override
    public void parseArguments(String s) {

    }
}
