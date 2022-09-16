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

package client.gui.commands;

import java.awt.event.ActionEvent;
import java.util.StringTokenizer;

import client.MWClient;

/**
 * Ping command
 */
public class PingGCmd extends CGUICommand
{
	/**
     * 
     */
    private static final long serialVersionUID = -1052902282489028283L;

    public PingGCmd(MWClient mwclient)
	{
		super(mwclient);
		name = "ping";
		command = "ping";
	}

	@Override
	public boolean execute(String input) {
		StringTokenizer ST = new StringTokenizer(input, " ");
		if (check(ST.nextToken())) {
			input = decompose(input);
			send(input + delimiter + String.valueOf(System.currentTimeMillis()));
			return true;
		}
		//else {
		return false;
	}

	public void actionPerformed(ActionEvent e) {}
}