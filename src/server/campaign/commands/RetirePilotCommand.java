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

import java.util.Enumeration;
import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.pilot.SPilot;


public class RetirePilotCommand implements Command {
	
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
		
		if (command.hasMoreElements()) {
			
			//load the player issuing the command
			SPlayer p = CampaignMain.cm.getPlayer(Username);
			
			//try to load the mech whose pilot is being retired
			int mechid = -1;
			try {
				mechid = Integer.parseInt((String)command.nextElement());
			} catch (Exception e) {
				CampaignMain.cm.toUser("AM:Improper format. Try: /c retirepilot#UnitID",Username,true);
				return;
			}
			
			//get the actual unit, and check for a null
			SUnit m = p.getUnit(mechid);
			if (m == null) {
				CampaignMain.cm.toUser("AM:You do not have unit #" + mechid + ".",Username,true);
				return;
			}
			
			//see if its even possible to retire pilots
			if (Boolean.parseBoolean(CampaignMain.cm.getConfig("PilotRetirementAllowed")) == false) {
				CampaignMain.cm.toUser("AM:Pilot retirement is not allowed.",Username,true);
				return;
			}
			
			if ( m.hasVacantPilot() ){
				CampaignMain.cm.toUser("AM:This unit does not currently have pilot in it.", Username);
				return;
			}
			
			//don't let someone retire while active, if unit is in an army
			if (p.getDutyStatus() != SPlayer.STATUS_RESERVE) {
				
				//check the unit's armies.
				Enumeration<SArmy> f = p.getArmies().elements();
				while (f.hasMoreElements()) {
					SArmy currArmy = f.nextElement();
					if (currArmy.getUnit(m.getId()) != null) {
						CampaignMain.cm.toUser("AM:You may not dismiss/retire a pilot while his unit is part of an active army.",Username,true);
						return;
					}//end if(army contains the )
				}//end while(more armies to check)
			}//end if(player is active or fighting)
			
			
			//check to see if the command was confirmed. used later.
			boolean commandConfirmed = false;
			if (command.hasMoreElements()) {
				if(command.nextToken().equals("CONFIRM"))
					commandConfirmed = true;
			}
			
			/*
			 * two possible paths for retirement.
			 * 
			 * 1st - pilot has skills which add to (or are below) the
			 * free retirement threshold. If the command is confirmed,
			 * retire him. If not, send a confirmation link.
			 * 
			 * 2nd - pilot is under the free threshold. check to see if
			 * early retirement is possible, breaking out if it isnt, sending
			 * cost info and a confirmed re-issue if not (and not confirmed).
			 */
			
			int totalSkill = m.getPilot().getGunnery() + m.getPilot().getPiloting();
			int skillForFree = CampaignMain.cm.getIntegerConfig("TotalSkillForFreeRetirement");
			int retirementCost = -1;//used later
			if (totalSkill <= skillForFree) {
				
				//qualify for free.
				if (!commandConfirmed) {//send confirm link
					String toReturn = "AM:<br>Are you sure you want to retire " + m.getPilot().getName() + "?<br><a href=\"MEKWARS/c retirepilot#" + mechid + "#CONFIRM\">Click here to confirm the retirment order.</a>";
					CampaignMain.cm.toUser(toReturn,Username,true);
					return;
				}
				
			} else { //didnt meet free requiremnt.
				
				//determine costs
				int numLevelsNeeded = totalSkill - skillForFree;
				int costPerLevelNeeded = CampaignMain.cm.getIntegerConfig("CostPerLevelToRetireEarly");
				retirementCost = costPerLevelNeeded * numLevelsNeeded;
				
				//check to see if early is allowed. if not, break out.
				boolean earlyAllowed = Boolean.parseBoolean(CampaignMain.cm.getConfig("EarlyRetirementAllowed"));
				if (!earlyAllowed) {
					String toReturn = m.getPilot().getName() + " may not retire yet. He needs to " +
					"level up " + numLevelsNeeded + " more time";
					if (numLevelsNeeded > 1)
						toReturn += "s";
					toReturn += " before he can call it quits.";
					CampaignMain.cm.toUser(toReturn,Username,true);
					return;
				}
				
				if (p.getMoney() < retirementCost) {
					CampaignMain.cm.toUser("AM:You don't have enough money to dismiss " + m.getPilot().getName() + ". Bribing HQ to reassign him would cost " + CampaignMain.cm.moneyOrFluMessage(true,true,retirementCost)+".",Username,true);
					return;
				}
				
				//else (early retirement allowed).
				if (!commandConfirmed) {
					//not confirmed. send a link.
					String toReturn = "AM:You can get " + m.getPilot().getName() + " transfered out of your unit, but you'll have " +
					"to bribe someone at HQ. Are you willing to spend " +CampaignMain.cm.moneyOrFluMessage(true,true,retirementCost)+" to make an \"accident\" happen?<br>" +
					"<a href=\"MEKWARS/c retirepilot#" + mechid + "#CONFIRM\">Click here to pay the " + CampaignMain.cm.moneyOrFluMessage(true,true,retirementCost)+".</a>";
					CampaignMain.cm.toUser(toReturn,Username,true);
					return;
				}
				
			}//end (couldnt be retired for free)
			
			/*
			 * Every break passed and the command was confirmed. We
			 * can go about the actual retirement now.
			 */
			
			// Figure out if the pilot is going to take his unit with him
			Boolean retireMechToo = false;
			String unitName = m.getModelName();
			if (!CampaignMain.cm.getBooleanConfig("AllowPersonalPilotQueues") && (CampaignMain.cm.getIntegerConfig("RetiredPilotTakesMechChance") > 0 )) {
				if (CampaignMain.cm.getRandomNumber(100) <= CampaignMain.cm.getIntegerConfig("RetiredPilotTakesMechChance")) {
					retireMechToo = true;
				}
			}
			
			//set up the message for the player
			String toReturn = "";
			if (retirementCost <= 0) {
				toReturn = m.getPilot().getName() + " retired, went home to his family, and lived happily ever after." + (retireMechToo ? (" Unfortunately, he took his family's " + unitName + " with him.") : "");
				
			} else { 
				toReturn = "AM:You dismissed " + m.getPilot().getName() + " (-" + CampaignMain.cm.moneyOrFluMessage(true,true,retirementCost)+")." + (retireMechToo ? (" Unfortunately, he took his family's " + unitName + " with him.") : "");
			}
			
			//take money away
			if (retirementCost > 0)
				p.addMoney(-retirementCost);
			
			//tell the user about the retirement
			CampaignMain.cm.toUser(toReturn,Username,true);

			//now, handle the pilot. if PPQs are in use leave the unit vacant,
			//otherwise add a new pilot from the faction queue.
			boolean allowPPQs = Boolean.parseBoolean(CampaignMain.cm.getConfig("AllowPersonalPilotQueues"));
			if (allowPPQs && m.isSinglePilotUnit()) {
				SPilot pilot = new SPilot("Vacant",99,99);
			    m.setPilot(pilot);
			}
			
			else {//add a new pilot from the queue
				m.setPilot(p.getMyHouse().getNewPilot(m.getType()));
				m.setExperience(0);
			}
			
			//continue normally. update unit and its armies, etc.
			CampaignMain.cm.toUser("PL|UU|"+m.getId()+"|"+m.toString(true),Username,false);
			
			Enumeration<SArmy> f = p.getArmies().elements();
			while (f.hasMoreElements()) {
				SArmy currArmy = f.nextElement();
				if (currArmy.getUnit(m.getId()) != null) {
					currArmy.setBV(0);//not null so recalc BV of the army
					CampaignMain.cm.toUser("PL|SAD|"+currArmy.toString(true,"%"),Username,false);
					CampaignMain.cm.getOpsManager().checkOperations(currArmy,true);//update legal operations
				}//end if(army contains the )
			}//end while(more armies to check)
			
			if (retireMechToo) {
				p.removeUnit(m.getId(), true);
			}
		}
	}
}