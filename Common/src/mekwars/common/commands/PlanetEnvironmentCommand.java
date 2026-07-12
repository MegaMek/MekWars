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

import java.awt.Dimension;
import java.util.StringTokenizer;

import mekwars.common.PlanetEnvironment;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.TokenReader;

/**
 * Client-side handler for the {@code PlanetEnvironmentCommand} protocol message, sent by the server to tell the
 * client which planetary environment (terrain/map type) and battle-map dimensions apply to an upcoming or current
 * game. Executing it updates the client's environment state used to generate or select the battle map.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class PlanetEnvironmentCommand extends Command {

    /**
     * @see Command#Command(IClient)
     */
    public PlanetEnvironmentCommand(IClient client) {
        super(client);
    }

    /**
     * Parses the environment name plus the desired map's x-size, y-size, and "map medium" (a numeric code
     * describing the terrain/medium, e.g. ground vs space) from the command payload, then pushes the resulting
     * environment onto the client.
     * <p>
     * If the named {@link PlanetEnvironment} reports itself as a static (fixed-size) map, the sizes parsed from the
     * wire are discarded in favor of the environment's own built-in dimensions; otherwise the parsed x/y sizes are
     * used as-is. The map medium value is always taken from the wire regardless of static/non-static.
     *
     * @param input the raw, delimited protocol line for this command; see {@link Command#decode(String)}
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        PlanetEnvironment planetEnvironment = new PlanetEnvironment(stringTokenizer.nextToken());
        int xSize = TokenReader.readInt(stringTokenizer);
        int ySize = TokenReader.readInt(stringTokenizer);
        int mapMedium = TokenReader.readInt(stringTokenizer);

        if (planetEnvironment.isStaticMap()) {
            client.setEnvironment(planetEnvironment,
                  new Dimension(planetEnvironment.getXSize(), planetEnvironment.getYSize()),
                  mapMedium);
        } else {
            client.setEnvironment(planetEnvironment, new Dimension(xSize, ySize), mapMedium);
        }
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
