/*
 * MekWars - Copyright (C) 2010 
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

import java.util.StringTokenizer;
import java.util.Vector;

import client.MWClient;

/**
 * @author Spork (billypinhead@users.sourceforge.net)
 */
public class SBT extends Command {

	/**
	 * @see Command#Command(MMClient)
	 */
	public SBT(MWClient mwclient) {
		super(mwclient);
	}

	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		StringTokenizer st = decode(input);
		mwclient.getData().getBannedTargetingSystems().clear();
		Vector<Integer> bans = new Vector<Integer>(1,1);
		while(st.hasMoreTokens()) {
			int ban = Integer.parseInt(st.nextToken());
			if (ban != 0 ) {
				// Don't ban standard TS
				bans.add(ban);
			}
		}
		mwclient.getData().setBannedTargetingSystems(bans);
	}
}
