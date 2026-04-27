/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package mekwars.client.commands;

import mekwars.client.MWClient;
import mekwars.common.AdvancedTerrain;

/**
 * @@author Torren (Jason Tighe)
 *       <p>
 *       Used for Advanced Planet Environments.
 *
 */

public class AdvancedPlanetEnvironmentsCommand extends Command {

    /**
     * @see Command#Command(MWClient)
     */
    public AdvancedPlanetEnvironmentsCommand(MWClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        AdvancedTerrain aTerrain = new AdvancedTerrain(st.nextToken());

        client.setAdvancedTerrain(aTerrain);
    }
}
