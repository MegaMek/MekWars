/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Copyright (C) 2004 Helge Richter (McWizard)
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

import java.util.StringTokenizer;

import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Client-side handler for the {@code UsersCommand} protocol message, which the server sends to push a full
 * snapshot of the currently connected user list (e.g. right after login). Executing it replaces the client's
 * entire local user list with the users named in the message and refreshes the user-list GUI panel.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class UsersCommand extends Command {

    /**
     * Creates the command bound to the given client, as required by {@link Command#Command(IClient)}.
     *
     * @param client the client instance that will receive the parsed user list
     */
    public UsersCommand(IClient client) {
        super(client);
    }

    /**
     * Parses the {@code UsersCommand} payload, where every remaining token after the command prefix is a single
     * username, and rebuilds the client's user list from scratch.
     * <p>
     * Note: the entire list is cleared before repopulating, so this command always represents a full replace, never
     * an incremental update. For dedicated (headless) clients, the GUI refresh is skipped since there is no user
     * list panel to update.
     *
     * @param input the raw, delimited protocol line for this command; see {@link Command#decode(String)}
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        client.getUsers().clear();

        //add all users to the list
        while (stringTokenizer.hasMoreElements()) {
            client.getUsers().add(new CUser(stringTokenizer.nextToken()));
        }

        if (client.isDedicated()) {
            return;
        }

        client.refreshGUI(IClient.REFRESH_USERLIST);
    }

    /**
     * Unused on the client side; this command has no reply-argument parsing behavior.
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * Unused on the client side; this command is never parsed as server-bound arguments.
     */
    @Override
    public void parseArguments(String s) {

    }
}
