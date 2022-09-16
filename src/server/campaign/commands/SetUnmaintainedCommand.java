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

import common.Unit;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;

public class SetUnmaintainedCommand implements Command {
	
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
		int numtoset = 0;//ID# of the mech which is to get set as unmaintained
		
		try {
			numtoset = Integer.parseInt(command.nextToken());
		}//end try
		catch (NumberFormatException ex) {
			CampaignMain.cm.toUser("AM:SetUnmaintained command failed. Check your input. It should be something like this: /c setunmaintained#12",Username,true);
			return;
		}//end catch
		
		SUnit unitToSet = p.getUnit(numtoset);
		if (unitToSet == null) {
			CampaignMain.cm.toUser("AM:Invalid id number. Make sure you're using the right unit number.",Username,true);
			return;
		}
		
		if (unitToSet.getStatus() == Unit.STATUS_UNMAINTAINED) {
			CampaignMain.cm.toUser("AM:This unit is already unmaintained.",Username,true);
			return;
		}
		
		if (unitToSet.getStatus() == Unit.STATUS_FORSALE) {
			CampaignMain.cm.toUser("AM:You may not change the maintenance status of a unit which is being sold.",Username,true);
			return;
		}
		
		if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
			CampaignMain.cm.toUser("AM:You may not unmaintain a unit while active.",Username,true);
			return;
		}
		
		if (p.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
			CampaignMain.cm.toUser("AM:You may not unmaintain a unit while engaged!.",Username,true);
			return;
		}
		
		//passes checks. now actually make the unit unmaintained.
		unitToSet.setUnmaintainedStatus();
		CampaignMain.cm.toUser("PL|SUS|"+unitToSet.getId()+"#"+Unit.STATUS_UNMAINTAINED,Username,false);
		CampaignMain.cm.toUser("PL|SB|"+p.getTotalMekBays(),Username,false);
		CampaignMain.cm.toUser("PL|SF|"+p.getFreeBays(),Username,false);
		CampaignMain.cm.toUser(unitToSet.getPilot().getName() + "'s " + unitToSet.getModelName() + " is no longer being maintained." ,Username,true);
		p.setSave();
		
	}//end process()
}//end SetUnmaintainedCommand class