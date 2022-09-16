/*
 * MekWars - Copyright (C) 2006 
 * 
 * Original author nmorris (urgru@users.sourceforge.net)
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
import common.MMGame;

/**
 * Server list commands. All commands relating to servers
 * in the battles tab are processed though SL subcommands.
 */
public class SL extends Command {

	//VARIABLES
	// - none
	
	//CONSTRUCTOR
	public SL(MWClient mwclient) {
		super(mwclient);
	}

	//METHODS
	//@see client.cmd.Command#execute(java.lang.String)
	@Override
	public void execute(String input) {
		
		StringTokenizer st = decode(input);
		String cmd = st.nextToken();//SL
		
		if (!st.hasMoreTokens())
			return;
		
		else if (cmd.equals("NG")) { // new server opened, SL|NG|<MMGame.toString>
			MMGame newGame = new MMGame(st.nextToken());
			mwclient.getServers().put(newGame.getHostName(),newGame);
		}
		
		else if (cmd.equals("CG")) { // server closed, SL|CG|Hostname
			mwclient.getServers().remove(st.nextToken());
		}
		
		else if (cmd.equals("SHS")) { // set host status, SL|SHS|Hostname
			MMGame toUpdate = mwclient.getServers().get(st.nextToken());
			if (toUpdate != null)
				toUpdate.setStatus(st.nextToken());
		}
		
		else if (cmd.equals("JG")) { //player joined a game, GL|JG|Hostname|Playername
			MMGame toUpdate = mwclient.getServers().get(st.nextToken());
			toUpdate.getCurrentPlayers().add(st.nextToken());
		}
		
		else if (cmd.equals("LG")) { //player left a game, GL|LG|Hostname|Playername
			MMGame toUpdate = mwclient.getServers().get(st.nextToken());
			toUpdate.getCurrentPlayers().remove(st.nextToken());
		}
		
		//refresh affected portion of the GUI
        mwclient.refreshGUI(MWClient.REFRESH_BATTLETABLE);
	}
}
