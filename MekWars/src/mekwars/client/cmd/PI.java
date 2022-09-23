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

import client.CUser;
import client.MWClient;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class PI extends Command {

	/**
	 * @param client
	 */
	public PI(MWClient mwclient) {
		super(mwclient);
	}

	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		
		StringTokenizer st = decode(input);
        String task = st.nextToken();
        CUser user = null;
        if (task.equals("PL")) {
            while (st.hasMoreTokens()) {
                user = mwclient.getUser(st.nextToken());
                if (user != null) {
                	user.setCampaignData(mwclient, st.nextToken());
                }
            }
        } else if (task.equals("DA")) {
            user = mwclient.getUser(st.nextToken());
            if (user != null) {
            	user.setCampaignData(mwclient, st.nextToken());
            	if ( user.getName().equalsIgnoreCase(mwclient.getPlayer().getName())){
            	    mwclient.getMainFrame().enableMenu();
            	}
            }
        } else if (task.equals("CS")) {
        	user = mwclient.getUser(st.nextToken());
            if (user != null) {
            	user.setStatus(Integer.parseInt(st.nextToken()));
            }
        } else if (task.equals("FT")) {
            user = mwclient.getUser(st.nextToken());
            if (user != null && st.hasMoreTokens()) {
                user.setFluff(st.nextToken());
            }
        } else if (task.equals("SSN")) {
            user = mwclient.getUser(st.nextToken());
            if (user != null && st.hasMoreTokens()) {
                user.setSubFactionName(st.nextToken());
            }
        } else if (task.equals("EX")) {
        	user = mwclient.getUser(st.nextToken());
            if (user != null && st.hasMoreTokens()) {
                user.setExp(Integer.parseInt(st.nextToken()));
            }
        } else if (task.equals("RA")) {
        	user = mwclient.getUser(st.nextToken());
            if (user != null && st.hasMoreTokens()) {
                user.setRating(Float.parseFloat(st.nextToken()));
            }
        } 
        
        mwclient.refreshGUI(MWClient.REFRESH_USERLIST);
	}
}
