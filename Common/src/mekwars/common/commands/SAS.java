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

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * "Send As Self"-style client-side command: takes a server-provided line of text and immediately re-sends it back
 * to the server wrapped as a {@code CH} (chat) command. This is effectively a server-triggered echo/relay, letting
 * the server instruct a client to speak a given line of chat as if the client had typed it.
 *
 * @author jtighe
 */
public class SAS extends Command {

    /**
     * Creates the command bound to the given client.
     *
     * @param client the client that will relay the chat line
     */
    public SAS(IClient client) {
        super(client);
    }

    /**
     * Decodes {@code input} (stripping the command prefix token) to obtain the line of text to relay, then sends it
     * back to the server as a {@code CH|<line>} chat command.
     *
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        String line = decode(input).nextToken();
        client.serverSend(String.format("CH|%s", line));
    }

    /**
     * No reply-argument parsing is needed for this command; intentionally a no-op.
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * This command is never sent by a client to the server, so server-side argument parsing is a no-op.
     */
    @Override
    public void parseArguments(String s) {

    }
}
