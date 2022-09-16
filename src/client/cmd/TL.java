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

package client.cmd;

import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import client.MWClient;
import common.util.MWLogger;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class TL extends Command {

	/**
	 * @param client
	 */
	public TL(MWClient mwclient) {
		super(mwclient);
	}

	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		StringTokenizer st = decode(input);
        if(!st.hasMoreElements()) { return; } // sanity check
        try {
            FileWriter out = new FileWriter ("./logs/gamedata.log", true); // opened in APPEND mode; will be controlled by config setting
            out.write(st.nextToken()); // dump actual task data as sent by the server
            out.write("\n");
            out.close();
        } catch (IOException e) {
            MWLogger.errLog(e);
        }
	}
}
