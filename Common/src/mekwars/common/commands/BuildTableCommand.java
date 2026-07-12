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
 * Handles the {@code "BuildTableCommand"}-prefixed protocol messages used to distribute admin/moderator "build
 * table" files (unit-availability/build lists stored under {@code ./data/buildtables/}) from server to client.
 * {@link #execute(String)} multiplexes on a sub-command token immediately following the prefix:
 * <ul>
 * <li>{@code "LS"} — for each {@code folder}/list-of-file-names pair encoded in the payload, look up each named
 * file's last-modified timestamp on disk (0 if it doesn't exist) and ask the server for it via an
 * {@code AdminRequestBuildTable get#...} chat command; if the {@code viewer} flag token is {@code true}, also
 * request that the build table viewer be opened server-side.</li>
 * <li>{@code "PLS"} — the same listing/timestamp logic as {@code "LS"}, but requesting via
 * {@code RequestBuildTable get#...} (the non-admin/player-facing variant) instead.</li>
 * <li>{@code "BuildTableCommand"} — receives the actual file content: creates
 * {@code ./data/buildtables/<folder>/<table>} (creating parent directories as needed) and writes each remaining
 * token as one line of the file; notifies moderators in chat once written.</li>
 * <li>{@code "VS"} — clears the client's "waiting" flag ({@code client.setWaiting(false)}).</li>
 * </ul>
 * Note the {@code if}/{@code else if} structure: the {@code "LS"} branch is a standalone {@code if}, so if
 * {@code cmd} is {@code "LS"} the subsequent {@code "PLS"}/{@code "BuildTableCommand"}/{@code "VS"} branches are
 * still evaluated in sequence as a separate {@code if}/{@code else if} chain (though none of their conditions can
 * also match {@code "LS"}, so this has no practical effect).
 * This class is client-inbound only: {@link #parseReplyArgs(String)}, {@link #setClient(IClient)} and
 * {@link #parseArguments(String)} are all overridden with empty bodies.
 *
 * @author Spork
 *       <p>
 *       Handles Build Table up/downloading for admins/mods
 *
 */
public class BuildTableCommand extends Command {
    private final static MMLogger LOGGER = MMLogger.create(BuildTableCommand.class);

    /**
     * Constructs a client-side instance bound to {@code client}, as required by the {@link Command} contract.
     */
    public BuildTableCommand(IClient client) {
        super(client);
    }

    /**
     * Dispatches on the sub-command token ({@code "LS"}, {@code "PLS"}, {@code "BuildTableCommand"}, or
     * {@code "VS"}) following the prefix; see the class-level docs for what each sub-command does.
     *
     * @param input the full raw protocol line, prefix included
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
                        File file = new File(String.format("./data/buildtables/%s/%s", dName, fileName));

                        if (file.exists()) {
                            time = file.lastModified();
                        }

                        client.sendChat(
                              String.format("%sAdminRequestBuildTable get#%s#%s#%s", IClient.CAMPAIGN_PREFIX, dName, fileName, time));
                    }
                }

            }

            if (viewer) {
                client.sendChat(String.format("%sAdminRequestBuildTable view", IClient.CAMPAIGN_PREFIX));
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
                        File file = new File(String.format("./data/buildtables/%s/%s", dName, fileName));

                        if (file.exists()) {
                            time = file.lastModified();
                        }
                        client.sendChat(String.format("%sRequestBuildTable get#%s#%s#%s", IClient.CAMPAIGN_PREFIX, dName, fileName, time));
                    }
                }

            }
            if (viewer) {
                client.sendChat(String.format("%sRequestBuildTable view", IClient.CAMPAIGN_PREFIX));
            }
        } else if (cmd.equalsIgnoreCase("BuildTableCommand")) {
            String folder = stringTokenizer.nextToken();
            String table = stringTokenizer.nextToken();
            boolean isMod = client.isMod();
            File file = new File("./data/buildtables");

            if (!file.exists()) {
                file.mkdir();
            }

            file = new File(String.format("./data/buildtables/%s", folder));

            if (!file.exists()) {
                file.mkdir();
            }

            file = new File(String.format("./data/buildtables/%s/%s", folder, table));

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
                    client.addToChat(String.format("Received build table %s/%s", folder, table), CCommPanel.CHANNEL_MISC);
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
     * No-op. This command is client-inbound only; it is never sent as a request awaiting a coded reply.
     */
    @Override
    public void parseReplyArgs(String string) {

    }

    /**
     * No-op. Overrides {@link Command#setClient(IClient)} but does not update {@link #client} — calling this on an
     * existing instance silently has no effect, unlike the inherited base-class behavior other {@code Command}
     * subclasses rely on.
     */
    @Override
    public void setClient(IClient client) {

    }

    /**
     * No-op. This command is never dispatched server-side through the {@link ServerCommand} path (see
     * {@link Command} class-level docs), so there are no server-bound arguments to parse.
     */
    @Override
    public void parseArguments(String string) {

    }
}
