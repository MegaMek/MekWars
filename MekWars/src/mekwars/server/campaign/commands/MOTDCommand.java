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

/*
 * Created on 11-feb-2004 by Vertigo
 *
 */
package server.campaign.commands;

import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;

/**
 * @author Vertigo
 * (11-feb-2004)
 */
public class MOTDCommand implements Command {

	/*
	 * @see server.Campaign.Commands.Command#process(java.util.StringTokenizer, java.lang.String, server.Campaign.CampaignMain)
	 */
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
		if (p != null && p.getMyHouse().getMotd().trim().length() > 0)
		    CampaignMain.cm.toUser("(Housemail)<h2><b>MOTD:</b></h2><br>" + p.getMyHouse().getMotd(),Username,true);
		if (p != null && p.getMyHouse().getAnnouncement().trim().length() > 0) {
			CampaignMain.cm.toUser("(Housemail)<br /><hr><h3><b>Staff Announcements:</b></h3><br />" + p.getMyHouse().getAnnouncement(), Username, true);
		}
			
		
	}//end process
	
}
