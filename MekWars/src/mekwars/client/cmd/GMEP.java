/*
 * MekWars - Copyright (C) 2004 
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

import java.util.StringTokenizer;

import client.MWClient;

/**
 * @@author Torren (Jason Tighe)
 * 
 * Used for Game Map Edge Placement
 * Normally used when you have buildings being placed on the map
 * and want the defender to start near them.
 * 
 */

public class GMEP extends Command {

	/**
	 * @see Command#Command(MMClient)
	 */
	public GMEP(MWClient mwclient) {
		super(mwclient);
	}

	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		StringTokenizer st = decode(input);
        int edge = Integer.parseInt(st.nextToken());
        mwclient.setPlayerStartingEdge(edge);
	}
}
