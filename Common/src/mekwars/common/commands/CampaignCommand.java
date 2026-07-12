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
 * Client-side command that delivers a generic campaign-layer update from the server to the client's
 * {@code Campaign} object, then refreshes the HQ, player, and BM (battle-mek) GUI panels so they reflect the
 * newly applied state. Similar in shape to {@link PS}, but does not additionally refresh the status panel.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class CampaignCommand extends Command {

    /**
     * Creates the command bound to the given client.
     *
     * @param client the client whose campaign state will be updated
     */
    public CampaignCommand(IClient client) {
        super(client);
    }

    /**
     * Passes the full raw {@code input} (unmodified, including this command's own prefix) straight to the client's
     * {@code Campaign} for decoding, then triggers a refresh of the HQ, player, and BM GUI panels.
     *
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        client.getCampaign().decodeCommand(input);
        client.refreshGUI(IClient.REFRESH_HQ_PANEL);
        client.refreshGUI(IClient.REFRESH_PLAYER_PANEL);
        client.refreshGUI(IClient.REFRESH_BM_PANEL);
    }

    /**
     * No reply-argument parsing is needed for this command; intentionally a no-op.
     */
    @Override
    public void parseReplyArgs(String string) {

    }

    /**
     * Overridden to intentionally do nothing: this command instance's client binding is fixed at construction and
     * is never reassigned.
     */
    @Override
    public void setClient(IClient client) {

    }

    /**
     * This command is never sent by a client to the server, so server-side argument parsing is a no-op.
     */
    @Override
    public void parseArguments(String string) {

    }
}
