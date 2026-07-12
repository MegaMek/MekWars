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
 * Client-side command that sets the display/chat text color for a named user, as pushed by the server (e.g. after
 * another user changes their color preference or a moderator assigns one). Looks up the local {@link CUser} for
 * the given username and, if known to this client, applies the given HTML color string.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class UserColorCommand extends Command {

    /**
     * Creates the command bound to the given client.
     *
     * @param client the client whose user list will be updated
     */
    public UserColorCommand(IClient client) {
        super(client);
    }

    /**
     * Decodes {@code input} to obtain the username and the new HTML color, then applies the color to the matching
     * {@link CUser} if that user is known to this client. Silently does nothing if the user is not found (e.g. not
     * yet in the client's user list).
     *
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        CUser user = (CUser) client.getUser(stringTokenizer.nextToken());

        if (user != null) {
            user.setHTMLColor(stringTokenizer.nextToken());
        }
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
