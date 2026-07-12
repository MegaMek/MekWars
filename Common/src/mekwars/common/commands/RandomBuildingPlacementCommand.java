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

import mekwars.common.campaign.Buildings;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Client-side command that delivers a building placement template used for randomly-generated maps (RMGs —
 * Random Map Generator). Builds a {@link Buildings} instance from the encoded token stream and installs it as the
 * client's current building template, so subsequently placed/rendered buildings on the map match what the server
 * decided.
 *
 * @author Torren (Jason Tighe)
 *       <p>
 *       Used for Randomn Building Placement on RMG's
 *
 */

public class RandomBuildingPlacementCommand extends Command {

    /**
     * @see Command#Command(IClient)
     */
    public RandomBuildingPlacementCommand(IClient client) {
        super(client);
    }

    /**
     * Decodes {@code input}, constructs a new {@link Buildings} and populates it from the remaining tokens via
     * {@link Buildings#fromString(StringTokenizer)}, then sets it as the client's building template.
     *
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        Buildings building = new Buildings();

        building.fromString(stringTokenizer);

        client.setBuildingTemplate(building);
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
