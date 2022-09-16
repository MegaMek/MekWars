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
import server.campaign.SArmy;
import server.campaign.SPlayer;
import server.campaign.SUnit;

public class ExchangeUnitCommand implements Command {
	
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
		
		//get the player
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		if (p == null) {
			CampaignMain.cm.toUser("AM:Null player. Report this immediately!",Username,true);
			return;
		}
		
		//allowing people to reorder in game could break reports
		if (CampaignMain.cm.getOpsManager().getShortOpForPlayer(p) != null) {
			CampaignMain.cm.toUser("AM:You may not change an army's composition while you are in a game.", Username, true);
			return;
		}
		
		
		//Do parsing. Struct is like: /c EXM#1,1#12
		if (command.hasMoreElements()) {
			String parse = (String) command.nextElement();
			StringTokenizer S = new StringTokenizer(parse, ",");
			int position = -1;
			int armyid = Integer.parseInt((String) S.nextElement());
			if (!S.hasMoreElements())
				return;
			int mechid = Integer.parseInt((String) S.nextElement());
			int changeid = -1;
			if (command.hasMoreElements())
				changeid = Integer.parseInt((String) command.nextElement());
			
			SArmy a = p.getArmy(armyid);
			if (a == null) {
				CampaignMain.cm.toUser("AM:You do not have an  Army #" + armyid, Username, true);
				return;
			}
			
			//Is the Lance in a fight atm?
			if (a.isLocked()) {
				CampaignMain.cm.toUser("AM:This Army is currently in a game.", Username, true);
				return;
			}
			
			if (a.isPlayerLocked()) {
				CampaignMain.cm.toUser("AM:You cannot modify a locked army.", Username, true);
				return;
				}
			
			SUnit oldMech = (SUnit)a.getUnit(mechid);
			SUnit changeMech = p.getUnit(changeid);
			
			if (changeMech != null && changeMech.getModelName().startsWith("Error")) {
				CampaignMain.cm.toUser("AM:Error units may not be added to armies.", Username, true);
				return;
			}

			if (changeMech != null && a.isUnitInArmy(changeMech)) {
				CampaignMain.cm.toUser("AM:That unit already exits in Army #" + armyid + ".", Username, true);
				return;
			}
		
			if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
				CampaignMain.cm.toUser("AM:You may not change your armies while on active duty.", Username, true);
				return;
			}
			
			if (changeMech != null && changeMech.getStatus() == Unit.STATUS_UNMAINTAINED) {
				CampaignMain.cm.toUser("AM:You may not assign unmaintained units to combat formations!", Username, true);
				return;
			}
			
			if (changeMech != null) {
				int maxAmount = Integer.valueOf(CampaignMain.cm.getConfig("UnitsInMultipleArmiesAmount")).intValue();
				if (p.getAmountOfTimesUnitExistsInArmies(changeMech.getId()) >= maxAmount ) {
					if (maxAmount == 1 )
						CampaignMain.cm.toUser("AM:A unit may only be in one army at a time.", Username, true);
					else
						CampaignMain.cm.toUser("AM:A unit may be in a maximum of " + maxAmount + " armies.", Username, true);
					return;
				}
				
				if (changeMech.getStatus() == Unit.STATUS_FORSALE) {
					CampaignMain.cm.toUser("AM:This unit is being sold on the Market. It may not be added to an army.", Username, true);
					return;
				}
				
				int oldID;
				if (oldMech != null) {
					oldID = oldMech.getId();
					position = a.getUnitPosition(oldID);
					a.removeUnit(oldID);
					CampaignMain.cm.toUser("PL|RAU|"+a.getID()+"#"+oldID+"#"+a.getBV(),Username,false);
					CampaignMain.cm.toUser("PL|UU|"+oldMech.getId()+"|"+oldMech.toString(true),Username,false);
					a.checkLegalRatio(Username);
				}
				else
					oldID = mechid;
				//Exchange the old and the new mech
				//changeMech.setID(oldID);
				if ( position > -1){
				    a.addUnit(changeMech,position);
				    CampaignMain.cm.toUser("PL|AAU|"+a.getID()+"#"+changeMech.getId()+"#"+a.getBV()+"#"+position,Username,false);
				}
				else{
				    a.addUnit(changeMech);
				    CampaignMain.cm.toUser("PL|AAU|"+a.getID()+"#"+changeMech.getId()+"#"+a.getBV(),Username,false);
				}
				
				p.resetWeightedArmyNumber();//change made. clear the cached weightedArmyNumber.
				a.checkLegalRatio(Username);
			}
			else if (oldMech != null) {//changemech is known to be null from previous if statement
				a.removeUnit(oldMech.getId());
				CampaignMain.cm.toUser("PL|RAU|"+a.getID()+"#"+oldMech.getId()+"#"+a.getBV(),Username,false);
				a.checkLegalRatio(Username);
				CampaignMain.cm.toUser("PL|UU|"+oldMech.getId()+"|"+oldMech.toString(true),Username,false);
			}
			
			//tell the player that his army was changed and inform him of any legal ops changes
			CampaignMain.cm.toUser("AM:Army #"+ a.getID() + " was changed. New BV: " + a.getBV(),Username,true);
			CampaignMain.cm.getOpsManager().checkOperations(a,true);
			
		}
	}
}