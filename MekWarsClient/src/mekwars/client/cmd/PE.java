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

package mekwars.client.cmd;

import common.util.TokenReader;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class PE extends Command {

    /**
     * @see Command#Command(MMClient)
     */
    public PE(client.MWClient mwclient) {
        super(mwclient);
    }

    /**
     * @see client.cmd.Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        common.PlanetEnvironment pe = new common.PlanetEnvironment(st.nextToken());
        int xsize = TokenReader.readInt(st);
        int ysize = TokenReader.readInt(st);
        int mapMedium = TokenReader.readInt(st);
        if (pe.isStaticMap()) {
            mwclient.setEnvironment(pe, new java.awt.Dimension(pe.getXSize(), pe.getYSize()), mapMedium);
        } else {mwclient.setEnvironment(pe, new java.awt.Dimension(xsize, ysize), mapMedium);}
    }
}
