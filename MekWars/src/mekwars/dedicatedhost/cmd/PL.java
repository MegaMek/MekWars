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

import dedicatedhost.MWDedHost;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class PL extends Command {
	
	/**
	 * @param client
	 */
	public PL(MWDedHost mwclient) {
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

		if(cmd.equals("GBB"))//Go bye bye
			mwclient.getConnector().closeConnection();
        else if(cmd.equals("RSOD"))//Retrieve Short Op Data
            mwclient.retrieveOpData("short",st.nextToken());
        else if(cmd.equals("UCP"))//Update/Set a clients param
            mwclient.updateParam(st);
        else if ( cmd.equals("RMF") ) {
        	mwclient.retrieveMul(st.nextToken());
        } else if ( cmd.equals("UAR") ) {
            while ( st.hasMoreTokens() ){
                mwclient.getConfig().setParam(st.nextToken(), st.nextToken());
            }
            mwclient.getConfig().saveConfig();
        }
		else
			return;
	}
}
