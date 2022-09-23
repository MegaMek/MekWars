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

//import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;

//import common.campaign.pilot.Pilot;

/**
 * @author Salient for the SolFreeBuild option. This will allow SOL players
 * to remove units they created from their inventory
 */
public class SolDeleteUnitCommand implements Command {

	int accessLevel = 1;
	String syntax = "Unit ID";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}

	public void process(StringTokenizer command,String Username) {

		//access level checks
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		SHouse h = p.getMyHouse();

		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}

		if(!Boolean.parseBoolean(CampaignMain.cm.getConfig("Sol_FreeBuild"))) {
			CampaignMain.cm.toUser("AM:This command is disabled on this server.",Username,true);
			return;
		}

		if( !h.getName().equalsIgnoreCase(CampaignMain.cm.getConfig("NewbieHouseName"))) {
			CampaignMain.cm.toUser("AM: Only players in " + CampaignMain.cm.getConfig("NewbieHouseName") + " can use this command.",Username,true);
			return;
		}


		//non null target, so attempt the scrap
		int unitID = Integer.parseInt((String)command.nextElement());
		SUnit u = p.getUnit(unitID);

		//break out if the player doesn't have a unit with that id
		if (u == null) {
			CampaignMain.cm.toUser("Target player doesn't have a unit with ID# " + unitID + ".", Username, true);
			return;
		}

		//tell the player
		CampaignMain.cm.toUser("AM:"+ Username + "'s " + u.getModelName() + " was removed.", Username, true);

		p.removeUnit(unitID, true);

		//if the limit is on for SOL players, they have the ability to use this command to delete units
		//So, when they do, we need to subtract one from their free mek counter.
		//though.. i think this may be dead code, pretty sure if there is a limit the del option wont show
		if( CampaignMain.cm.getConfig("FreeBuild_LimitPostDefOnly").equalsIgnoreCase("false") &&
			h.getName().equalsIgnoreCase(CampaignMain.cm.getConfig("NewbieHouseName")) &&
			Integer.parseInt((h.getConfig("FreeBuild_Limit"))) > 0)
		{
			p.addMekToken(-1);
		}

	}
}//end SolDeleteUnitCommand
