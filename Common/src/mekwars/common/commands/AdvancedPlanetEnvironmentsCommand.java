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

package mekwars.common.commands;

import mekwars.common.AdvancedTerrain;
import mekwars.common.campaign.clientutils.protocol.IClient;

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
    public AdvancedPlanetEnvironmentsCommand(IClient client) {
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

    @Override
    public void parseReplyArgs(String s) {
        
    }

    @Override
    public void setClient(IClient mwClient) {

    }

    @Override
    public void parseArguments(String s) {

    }
}
