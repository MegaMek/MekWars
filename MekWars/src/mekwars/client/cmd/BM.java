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

import java.util.StringTokenizer;

import client.MWClient;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class BM extends Command {

	/**
	 * @param client
	 */
	public BM(MWClient mwclient) {
		super(mwclient);
	}

	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		
		StringTokenizer st = decode(input);
		String cmd = st.nextToken();
		
		if (!st.hasMoreTokens())
			return;
		else if (cmd.equals("AD")) //all BM data, BM|AD|
			mwclient.getCampaign().setBMData(st.nextToken());
		else if (cmd.equals("AU")) //add BM unit BM|AU|
			mwclient.getCampaign().addBMUnit(st.nextToken());
		else if (cmd.equals("RU")) //remove BM unit, BM|RU|
			mwclient.getCampaign().removeBMUnit(st.nextToken());
		else if (cmd.equals("CU")) //change BM unit, BM|CU
			mwclient.getCampaign().changeBMUnit(st.nextToken());
		
		mwclient.refreshGUI(MWClient.REFRESH_HQPANEL);
		mwclient.refreshGUI(MWClient.REFRESH_PLAYERPANEL);
		mwclient.refreshGUI(MWClient.REFRESH_BMPANEL);
		
	}
}
