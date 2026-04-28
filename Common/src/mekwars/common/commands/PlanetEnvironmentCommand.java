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

import java.awt.Dimension;

import mekwars.common.PlanetEnvironment;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.TokenReader;

/**
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
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        PlanetEnvironment planetEnvironment = new PlanetEnvironment(st.nextToken());
        int xsize = TokenReader.readInt(st);
        int ysize = TokenReader.readInt(st);
        int mapMedium = TokenReader.readInt(st);
        if (planetEnvironment.isStaticMap()) {
            client.setEnvironment(planetEnvironment,
                  new Dimension(planetEnvironment.getXSize(), planetEnvironment.getYSize()),
                  mapMedium);
        } else {client.setEnvironment(planetEnvironment, new Dimension(xsize, ysize), mapMedium);}
    }

    /**
     * @param s
     */
    @Override
    public void parseReplyArgs(String s) {
        
    }

    /**
     * @param s
     */
    @Override
    public void parseArguments(String s) {

    }
}
