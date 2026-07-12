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
 * Client-side command that forces a full GUI refresh: unconditionally triggers a refresh of every known panel
 * (HQ, player, BM/battle-mek, status, battle table, and user list), regardless of {@code input}. Used by the
 * server as a blunt "tell the client to redraw everything" signal rather than a targeted single-panel update.
 *
 * @author Salient RefreshGUICommand = Refresh GUI just a command that makes the client refresh I'm sure there was some
 *       preexisting way of doing this but I looked, and got tired of looking So...
 */
public class RefreshGUICommand extends Command {

    /**
     * Creates the command bound to the given client.
     *
     * @param client the client whose GUI panels will be refreshed
     */
    public RefreshGUICommand(IClient client) {
        super(client);
    }

    /**
     * Ignores {@code input} entirely and refreshes every GUI panel the client exposes.
     *
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        client.refreshGUI(IClient.REFRESH_HQ_PANEL);
        client.refreshGUI(IClient.REFRESH_PLAYER_PANEL);
        client.refreshGUI(IClient.REFRESH_BM_PANEL);
        client.refreshGUI(IClient.REFRESH_STATUS);
        client.refreshGUI(IClient.REFRESH_BATTLE_TABLE);
        client.refreshGUI(IClient.REFRESH_USERLIST);
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
