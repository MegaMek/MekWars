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

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.MWLogger;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class TL extends Command {

    /**
     * @param client
     */
    public TL(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        if (!st.hasMoreElements()) {return;} // sanity check
        try {
            java.io.FileWriter out = new java.io.FileWriter("./logs/gamedata.log",
                  true); // opened in APPEND mode; will be controlled by config setting
            out.write(st.nextToken()); // dump actual task data as sent by the server
            out.write("\n");
            out.close();
        } catch (java.io.IOException e) {
            MWLogger.errLog(e);
        }
    }
}
