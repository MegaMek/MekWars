/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

package server.campaign.commands;

import java.util.StringTokenizer;

import common.util.StringUtils;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;


public class NameArmyCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		if (p == null) {
			CampaignMain.cm.toUser("AM:Null Player while renaming army. Report This!.",Username,true);
			return;
		}
		
		int aid = -1;
		String name = null;
		try {
			aid = Integer.parseInt((String)command.nextElement());
			name = (String)command.nextElement();
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper format. Try: /c namearmy#ID#Name",Username,true);
			return;
		}
		
		SArmy army = p.getArmy(aid);
		if (army == null) {
			CampaignMain.cm.toUser("AM:Could not find an Army #" + aid + ".",Username,true);
			return;
		}
		
		if (name.length() > 50)
			name = name.substring(0,50);
		
		if ( StringUtils.hasBadChars(name).trim().length() > 0 ){
		    CampaignMain.cm.toUser(StringUtils.hasBadChars(name), Username);
		    return;
		}
		
		//check for a clear
		if (name.equals("clear"))
			name = " ";
		
		//set the name
		army.setName(name);
		
		//check to see if this is a silent rename (new army). if not, tell player.
		if (command.hasMoreElements()) {
			String silent = (String)command.nextElement();
			if (!silent.equals("SILENT"))
				CampaignMain.cm.toUser("AM:Army " + army.getID() + " renamed.",Username,true);
		}
		
	}//end process()
}//end NameArmyCommand.java