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
import common.util.MWLogger;

/**
 * Updates the faction status screen
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class HS extends Command {

	/**
	 * @see Command#Command(MMClient)
	 */
	public HS(MWClient mwclient) {
		super(mwclient);
	}

	/**
	 * @see Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {

		StringTokenizer st = decode(input);
		String cmdName;
		String cmdData;
		
		while (st.hasMoreTokens()) {
			cmdName = st.nextToken();
			cmdData = st.nextToken();
			try{
				this.issueSubCommand(cmdName, cmdData);
			}catch(Exception ex){
				MWLogger.errLog(ex);
			}
			
			if(cmdName.equals("CA"))
				return;//return without updating view
		}
        
		//only update after all commands processed
        mwclient.getMainFrame().getMainPanel().getHSPanel().updateDisplay();
	}
	
	/**
	 * HS| Commands can be issued in bulk. This allows short ops, etc
	 * to send ALL changes they make ot impacted house players at once
	 * instead of sending 5-10 separate updates.
	 */
	private void issueSubCommand(String cmdName, String cmdData) {
		
		//MWLogger.errLog("HS Subcommand: " + cmdName + " Data: " + cmdData);
		
		if (cmdName.equals("FN")) //set faction name, HS|FN 
			mwclient.getMainFrame().getMainPanel().getHSPanel().setFactionName(cmdData);
		else if (cmdName.equals("AU")) //add HS unit, HS|AU|
			mwclient.getMainFrame().getMainPanel().getHSPanel().addFactionUnit(cmdData);
		else if (cmdName.equals("RU")) //remove HS unit, HS|RU|
			mwclient.getMainFrame().getMainPanel().getHSPanel().removeFactionUnit(cmdData);
		else if (cmdName.equals("CC")) //change components for a class/type, HS|CC
			mwclient.getMainFrame().getMainPanel().getHSPanel().changeFactionComponents(cmdData);
		
		else if (cmdName.equals("AF")) //add a factory, HS|AF
			mwclient.getMainFrame().getMainPanel().getHSPanel().addFactionFactory(cmdData);
		else if (cmdName.equals("RF")) //remove a factory, HS|RF
			mwclient.getMainFrame().getMainPanel().getHSPanel().removeFactionFactory(cmdData);
		else if (cmdName.equals("CF")) //change a factory, HS|CF
			mwclient.getMainFrame().getMainPanel().getHSPanel().changeFactionFactory(cmdData);
		
		else if (cmdName.equals("CA"))//clear all HS data. usually used by defect.
			mwclient.getMainFrame().getMainPanel().getHSPanel().clearHouseStatusData();
	}
}
