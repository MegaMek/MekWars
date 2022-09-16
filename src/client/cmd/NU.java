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
public class NU extends Command {
	
	/**
	 * @param client
	 */
	public NU(MWClient mwclient) {
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
		
        //Check the Users and remove the User
        CUser user = mwclient.getUser(newUser.getName());
        //delete every instance of that user from the list
        while ( mwclient.getUsers().remove(user) ){
            user = mwclient.getUser(newUser.getName());
        }

		mwclient.getUsers().add(newUser);
		
		if (mwclient.isDedicated())
			return;

        if (newUser.isInvis() && newUser.getUserlevel() > mwclient.getUser(mwclient.getPlayer().getName()).getUserlevel()) {
            mwclient.refreshGUI(MWClient.REFRESH_USERLIST);
            return;
        }

        if ( newUser.getName().startsWith("[Dedicated]") )
            return;
        
		//Print an entry message if the information is followed by NEW (NU and UG are used for name changing, too)
		if (st.hasMoreTokens()) {
		
			String name = newUser.getName();
			
			if (!newUser.getCountry().equals("unknown"))
				name += " (" + newUser.getCountry() + ")";
			
			String toSend = "<font color=\"maroon\">>> Enter " +name + "</font>";
			
			if (mwclient.getConfig().isParam("TIMESTAMP")) 
				toSend = mwclient.getShortTime() + toSend;
			
			if (mwclient.getConfig().isParam("SHOWENTERANDEXIT"))
				mwclient.addToChat(toSend);
			
			//play join sound, if one is configured
			mwclient.doPlaySound(mwclient.getConfigParam("SOUNDONJOIN"));
		}
		mwclient.refreshGUI(MWClient.REFRESH_USERLIST);
	}
}
