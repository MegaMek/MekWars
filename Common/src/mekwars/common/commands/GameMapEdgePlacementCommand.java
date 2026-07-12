/*
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

import megamek.codeUtilities.MathUtility;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Client-side command that tells the client which board edge its forces should start deploying from for the
 * upcoming battle. Normally used when buildings are being placed on the map and the defender should start near
 * them, so the server dictates a specific starting edge instead of letting the client pick its own.
 *
 * @author Torren (Jason Tighe)
 *       <p>
 *       Used for Game Map Edge Placement Normally used when you have buildings being placed on the map and want the
 *       defender to start near them.
 *
 */

public class GameMapEdgePlacementCommand extends Command {

    /**
     * @see Command#Command(IClient)
     */
    public GameMapEdgePlacementCommand(IClient client) {
        super(client);
    }

    /**
     * Decodes {@code input} to obtain the edge token, parses it as an integer (defaulting to {@code 0} if the
     * token is not a valid number), and sets it as the client's player starting edge.
     *
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        int edge = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
        client.setPlayerStartingEdge(edge);
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
