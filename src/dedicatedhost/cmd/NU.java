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

package dedicatedhost.cmd;

import java.util.StringTokenizer;

import dedicatedhost.CUser;
import dedicatedhost.MWDedHost;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class NU extends Command {
	
	/**
	 * @param client
	 */
	public NU(MWDedHost mwclient) {
		super(mwclient);
	}
	
	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		
		StringTokenizer st = decode(input);
		
		//NU = New User (NU|<MMClientInfo.toString>|[NEW]) NEW is used the same way as GONE in UG
		//Read the MMClientInfo from the Stream (It's been put on the Stream with MMClientInfo.toString())
		
		CUser newUser = new CUser(st.nextToken());
		
		if ( !mwclient.getUsers().contains(newUser) )
			mwclient.getUsers().add(newUser);
		
		if (mwclient.isDedicated())
			return;

	}
}
