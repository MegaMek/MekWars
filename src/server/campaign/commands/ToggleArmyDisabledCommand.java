	/*
	 * MekWars - Copyright (C) 2007 
	 * 
	 * Original author - Bob Eldred (billypinhead@users.sourceforge.net)
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

import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;

public class ToggleArmyDisabledCommand implements Command {
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
			CampaignMain.cm.toUser("AM:Null Player while Disabling/Enabling army. Report This!.",Username,true);
			return;
		}
		int aid = -1;
		try {
			aid = Integer.parseInt((String)command.nextElement());
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper format. Try: /c togglearmydisabled#ID",Username,true);
			return;
		}
		SArmy army = p.getArmy(aid);
		if (army == null) {
			CampaignMain.cm.toUser("AM:Could not find an Army #" + aid + ".",Username,true);
			return;
		}
		
		if (CampaignMain.cm.getOpsManager().getShortOpForPlayer(p) != null) {
			CampaignMain.cm.toUser("AM:You may not modify your armies while in a game.",Username,true);
			return;
		}
		
		if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE){
			CampaignMain.cm.toUser("AM:You may not modify armies while active.",Username,true);
			return;
		}
		
		army.toggleArmyDisabled();
		p.resetWeightedArmyNumber();
		CampaignMain.cm.toUser("AM:Army " + army.getID() + (army.isDisabled() ? " disabled." : " enabled."),Username,true);
	}
}
