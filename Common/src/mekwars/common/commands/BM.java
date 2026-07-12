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

/**
 * Client-side handler for the {@code BM} ("Black Market", based on the campaign methods it delegates to) protocol
 * message. The server uses it to push updates about black-market data/units, and executing it dispatches a
 * sub-command that mutates the client's campaign black-market state, then refreshes the HQ, player, and black
 * market GUI panels.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class BM extends Command {

    public BM(IClient client) {
        super(client);
    }

    /**
     * Reads a sub-command code followed by its single string argument and dispatches to the matching
     * {@code Campaign} black-market mutator:
     * <ul>
     *     <li>{@code AD} - replace the black market data ({@link mekwars.common.campaign clientutils.protocol
     *     IClient#getCampaign() campaign}{@code .setBMData})</li>
     *     <li>{@code AU} - add a unit to the black market</li>
     *     <li>{@code RU} - remove a unit from the black market</li>
     *     <li>{@code CU} - change/update an existing black market unit entry</li>
     * </ul>
     * If the sub-command has no following argument token, the method returns immediately without dispatching or
     * refreshing the GUI. Any sub-command code other than the four above is silently ignored, but the GUI panels
     * are still refreshed afterward as long as an argument token was present.
     *
     * @param input the raw, delimited protocol line for this command; see {@link Command#decode(String)}
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        String command = stringTokenizer.nextToken();

        if (!stringTokenizer.hasMoreTokens()) {
            return;
        } else if (command.equals("AD")) {
            client.getCampaign().setBMData(stringTokenizer.nextToken());
        } else if (command.equals("AU")) {
            client.getCampaign().addBMUnit(stringTokenizer.nextToken());
        } else if (command.equals("RU")) {
            client.getCampaign().removeBMUnit(stringTokenizer.nextToken());
        } else if (command.equals("CU")) {
            client.getCampaign().changeBMUnit(stringTokenizer.nextToken());
        }

        client.refreshGUI(IClient.REFRESH_HQ_PANEL);
        client.refreshGUI(IClient.REFRESH_PLAYER_PANEL);
        client.refreshGUI(IClient.REFRESH_BM_PANEL);

    }

    /**
     * Unused on the client side; this command has no reply-argument parsing behavior.
     */
    @Override
    public void parseReplyArgs(String string) {

    }

    /**
     * No-op override; this command does not support having its bound client changed after construction.
     */
    @Override
    public void setClient(IClient client) {

    }

    /**
     * Unused on the client side; this command is never parsed as server-bound arguments.
     */
    @Override
    public void parseArguments(String string) {

    }
}
