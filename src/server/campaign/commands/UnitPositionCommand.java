/*
 * MekWars - Copyright (C) 2004 
 * 
 * original author - nmorris (urgru@users.sourceforge.net)
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
import server.campaign.SArmy;
import server.campaign.SPlayer;
import server.campaign.SUnit;


public class UnitPositionCommand implements Command {
	
	//conforming methods
	public int getExecutionLevel(){return 0;}
	public void setExecutionLevel(int i) {}
	String syntax = "";
	public String getSyntax() { return syntax;}

	public void process(StringTokenizer command,String Username) {
		
		//get the player
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		if (p == null) {
			CampaignMain.cm.toUser("Null player. Report this immediately!",Username,true);
			return;
		}
		
		//allowing people to reorder in games could break reports
		if (CampaignMain.cm.getOpsManager().getShortOpForPlayer(p) != null) {
			CampaignMain.cm.toUser("AM:You may not change unit positions while you are in a game!", Username, true);
			return;
		}
		
		if (command.hasMoreElements()) {
			
			int armyid = -1;
			int unitid = -1;
			int newposition = -1;
			try {
				armyid = Integer.parseInt((String)command.nextElement());
				unitid = Integer.parseInt((String)command.nextElement());
				newposition = Integer.parseInt((String)command.nextElement());
			} catch (Exception e) {
				CampaignMain.cm.toUser("AM:Improper format. Try: /c unitposition#army#unit#newposition", Username, true);
				return;
			}
			
			
			//load (and check) the army to modify
			SArmy a = p.getArmy(armyid);
			if (a == null) {
				CampaignMain.cm.toUser("AM:You do not have an Army with ID #" + armyid + ".", Username, true);
				return;
			}
			
			//break out if the lance is locked. NOT duplicative with the earlier game check,
			//since locks can/could be set for other reasons (non-reports, etc.)
			if (a.isLocked()) {
				CampaignMain.cm.toUser("AM:Army #" + armyid + " is locked.", Username, true);
				return;
			}
			
			SUnit u = p.getUnit(unitid);
			if (u == null) {
				CampaignMain.cm.toUser("AM:You do not have a Unit with ID #" + unitid + ".", Username, true);
				return;
			}
			
			if (u.getStatus() == Unit.STATUS_UNMAINTAINED) {
				CampaignMain.cm.toUser("AM:You may not change the position of an unmaintained unit.", Username, true);
				return;
			}
		
			//check validity of new position
			if (newposition < 0) {
				CampaignMain.cm.toUser("AM:You may not place a unit in a negative order/position.", Username, true);
				return;
			} 
			
			if (newposition > a.getAmountOfUnits()) {
				CampaignMain.cm.toUser("AM:Highest position available in Army #" + armyid + " is Pos. #" + a.getAmountOfUnits() + ".",Username,true);
				return;
			}
			
			if (a.isPlayerLocked()) {
				CampaignMain.cm.toUser("AM:You cannot modify a locked army.", Username, true);
				return;
			}
			
			//do the actual shift server side. remove the unit
			//from the army and then immediately re-add it at the
			//new position
			a.removeUnit(unitid);
			if (newposition >= a.getAmountOfUnits())
				a.addUnit(u);//put at end
			else
				a.addUnit(u, newposition);
			
			//now, send an update command to the client
			CampaignMain.cm.toUser("PL|RPU|"+a.getID()+"#"+u.getId()+"#"+newposition,Username,false);
			
			//and send the user some nice chat
			CampaignMain.cm.toUser("AM:Army #"+ a.getID() + "'s order was changed. The " + u.getModelName() + "  is now Unit " + (newposition + 1) + "." ,Username,true);
			
			//this doesnt trigger any of the add/remove save flags, but
			//if would still be nice to record. So set the player's save state.
			p.setSave();

		}
	}
}