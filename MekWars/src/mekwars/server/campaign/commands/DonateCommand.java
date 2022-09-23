/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Helge Richter
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
 * Created on 10.01.2004
 *
 */
package server.campaign.commands;

import java.util.StringTokenizer;

import common.Unit;
import common.util.MWLogger;
import common.util.StringUtils;
import common.util.UnitUtils;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.pilot.SPilot;

/**
 * @author Helge Richter
 */
public class DonateCommand implements Command {
	
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
		
		SPlayer p =  CampaignMain.cm.getPlayer(Username);
		SHouse house = p.getMyHouse();
		
		int donationsAllowed = Integer.parseInt(house.getConfig("DonationsAllowed"));
		if (donationsAllowed <= 0) {
			CampaignMain.cm.toUser("AM:Donations are not allowed on this server.",Username,true);
			return;
		}
		
		if (p.getMyHouse().isNewbieHouse()) {
			CampaignMain.cm.toUser("AM:SOL Players are not allowed to donate units, sorry!",Username,true);
			return;
		}
		
        if (p.mayAcquireWelfareUnits()) {
            CampaignMain.cm.toUser("AM:You may not donate any of your units while you are on welfare.",Username,true);
            return;
        }

        int unitid = -1;
        try {
        	unitid = Integer.parseInt((String)command.nextElement());
        } catch (Exception e) {
        	CampaignMain.cm.toUser("AM:Improper format. Try: /c donate#unitid",Username,true);
            return;
        }
        
		SUnit m = p.getUnit(unitid);
		if (m == null) {
			CampaignMain.cm.toUser("AM:You do not have a unit with ID#" + unitid + ".",Username,true);
            return;
		}
		
		if (m.isChristmasUnit() && !CampaignMain.cm.getBooleanConfig("Christmas_AllowDonate")) {
			CampaignMain.cm.toUser("AM:Sorry, you cannot donate Christmas units.  You should play with your new toys.", Username);
			return;
		}
		
        if (m.getModelName().startsWith("Error") || m.getModelName().startsWith("OMG")){
            CampaignMain.cm.toUser("AM:You tried to donate an Error unit. The unit was auto-scrapped and the staff was alerted.",Username,true);
            CampaignMain.cm.doSendModMail("NOTE",Username + " tried to donate an OMG. Unit auto-scrapped. Data: " + m.getProducer());
            MWLogger.errLog(Username + " tried to donate an OMG. Unit auto-scrapped. Data: " + m.getProducer());
            p.removeUnit(unitid, true);
            return;
        }

		if (m.getStatus() == Unit.STATUS_FORSALE){
			CampaignMain.cm.toUser("AM:Units that are for sale on the Market may not be donated.", Username, true);
			return;
		}

		if (p.getAmountOfTimesUnitExistsInArmies(unitid) > 0 && p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
			CampaignMain.cm.toUser("AM:You may not donate units which are in active armies.", Username, true);
			return;	
		}
		
		for (SArmy currA : p.getArmies()) {
			if (currA.isLocked() && currA.getUnit(unitid) != null) {
				CampaignMain.cm.toUser("AM:You may not donate units which are in fighting armies.", Username, true);
				return;
			}
		}
		
		if (p.getDonationsThisTick() >= donationsAllowed) {
			CampaignMain.cm.toUser("AM:You may only donate " + donationsAllowed + " unit(s) each tick.", Username, true);
			return;
		}

        if (!Boolean.parseBoolean(house.getConfig("AllowDonatingOfDamagedUnits")) && (UnitUtils.hasArmorDamage(m.getEntity()) || UnitUtils.hasCriticalDamage(m.getEntity()))) {
            CampaignMain.cm.toUser("AM:You may not donate damaged units.",Username,true);
            return;
        }
        
		//Determine donation cost multiplier
		float costMulti = Float.parseFloat(house.getConfig("DonationCostMultiplier"));
	
		//Now that we have the multimpliers, determine how much the scarp costs (or gives)
		int moneyToDonate = Math.round(p.getMyHouse().getPriceForUnit(m.getWeightclass(), m.getType()) * costMulti);
		int infToDonate = (int)(p.getMyHouse().getInfluenceForUnit(m.getWeightclass(), m.getType())* costMulti);
		
		//Allow negative monetary costs (give money back), but don't allow donations to grant flu.
		if (infToDonate < 0)
			infToDonate = 0;

		//Check to ensure player can afford the scrap
		if (p.getMoney() < moneyToDonate || p.getInfluence() < infToDonate) {
			CampaignMain.cm.toUser("AM:You cannot afford to donate this unit. You need " + CampaignMain.cm.moneyOrFluMessage(true,true,moneyToDonate)+" and " +CampaignMain.cm.moneyOrFluMessage(false,true,infToDonate)+".",Username,true);
			return;
		}
		
		//add/deduct the standard donation cost
		if (m.getScrappableFor() >= 0) {
			p.addMoney(m.getScrappableFor()/2);
			p.addInfluence(infToDonate/2);
			CampaignMain.cm.toUser("AM:You donated the " + m.getModelName() + " (" + CampaignMain.cm.moneyOrFluMessage(true,true,m.getScrappableFor()/2,true)+ ", " + CampaignMain.cm.moneyOrFluMessage(false,true,infToDonate/2,true) + ").", Username, true);
		} else {
			p.addMoney(-moneyToDonate);
			p.addInfluence(-infToDonate);
			p.addDonationThisTick();
			CampaignMain.cm.toUser("AM:You donated the " + m.getModelName() + " (" + CampaignMain.cm.moneyOrFluMessage(true,true,-moneyToDonate,true)+ ", " + CampaignMain.cm.moneyOrFluMessage(false,true,-infToDonate,true) + ").", Username, true);
		}

		//notify house and, if needed, send warning to mod channel
		CampaignMain.cm.doSendHouseMail(p.getMyHouse(), "NOTE", p.getName() + " donated " + StringUtils.aOrAn(m.getVerboseModelName(),true) + " to the faction.");
        if (p.mayAcquireWelfareUnits())
            CampaignMain.cm.doSendModMail("NOTE",Username + " donated a unit and sent himself into welfare.");

		//do the actual remove last, so the checkops show under the donation string. replace pilot before adding to house.
		p.removeUnit(unitid, true);
				
		//Save the pilot
		SPilot oldPilot = (SPilot)m.getPilot();
		if (Boolean.parseBoolean(house.getConfig("AllowPersonalPilotQueues")) && !m.hasVacantPilot() && m.isSinglePilotUnit()) {
			p.getPersonalPilotQueue().addPilot(m.getPilot(), m.getWeightclass());
			CampaignMain.cm.toUser("PL|AP2PPQ|"+m.getType() + "|" + m.getWeightclass() + "|" + oldPilot.toFileFormat("#",true),Username,false);
            CampaignMain.cm.toUser(oldPilot.getName() + " was moved to your barracks.",Username,true);
    	    p.getPersonalPilotQueue().checkQueueAndWarn(p.getName(), m.getType(), m.getWeightclass());
            //Stick a vacant pilot into the unit before sending to the house
            m.setPilot(new SPilot("Vacant",99,99));
		} else if ( !CampaignMain.cm.getBooleanConfig("CrewsStayWithUnits")){
			p.getMyHouse().addDispossessedPilot(m, false);
            m.setPilot(new SPilot("Vacant",99,99));
		}
		
		p.getMyHouse().addUnit(m,true);
	
	}
}

