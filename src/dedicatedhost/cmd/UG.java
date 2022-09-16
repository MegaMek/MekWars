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
public class UG extends Command {

	/**
	 * @param client
	 */
	public UG(MWDedHost mwclient) {
		super(mwclient);
	}

	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		StringTokenizer st = decode(input);
        //UG = User Gone (UG|<MMClientInfo.toString>|[Gone]) Gone is used when the client didn't just change his name
        //Create a new MMClienrInfo-Object from the String
        CUser mmci = new CUser((String) st.nextElement());
        if (mwclient.isDedicated()){
        	mwclient.getUsers().remove(mwclient.getUser(mmci.getName()));
            return;
        }
	}        
}
