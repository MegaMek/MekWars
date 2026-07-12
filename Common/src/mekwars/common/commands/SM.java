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

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.panels.CCommPanel;

/**
 * Client-side command ("Server Message" / broadcast chat) that delivers a chat line from the server to this
 * client's chat display. If the {@code MAIN_CHANNEL_MISC} client config option is set, the message is added to the
 * client's default/main chat destination; otherwise it is explicitly routed to the misc chat channel.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class SM extends Command {

    /**
     * @see Command#Command(IClient)
     */
    public SM(IClient client) {
        super(client);
    }

    /**
     * Decodes {@code input} and, if a message token is present, adds it to chat — using the single-argument
     * {@code addToChat} overload (implied default channel) when {@code MAIN_CHANNEL_MISC} is set, or explicitly
     * targeting {@link CCommPanel#CHANNEL_MISC} otherwise.
     *
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        if (stringTokenizer.hasMoreElements()) {
            if (client.getConfig().isParam("MAIN_CHANNEL_MISC")) {
                client.addToChat(stringTokenizer.nextToken());
            } else {
                client.addToChat(stringTokenizer.nextToken(), CCommPanel.CHANNEL_MISC);
            }
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
