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
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.pilot.SPilot;

public class BuyPilotsFromHouseCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		SHouse h = p.getMyHouse();

		if ( !Boolean.parseBoolean(h.getConfig("AllowPersonalPilotQueues")) )
			return;
		
		//access check
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		if (command.hasMoreElements()) {

			int unitType = Integer.parseInt(command.nextToken());
			int weightClass = Integer.parseInt(command.nextToken());
			int numberOfPilots = 1;
			
			if ( command.hasMoreTokens() )
				numberOfPilots = Integer.parseInt(command.nextToken());
			
			if ( p.getPersonalPilotQueue().getPilotQueue(unitType,weightClass).size() > 0 
					&& !h.getBooleanConfig("AllowPlayerToBuyPilotsFromHouseWhenPoolIsFull") ){
				CampaignMain.cm.toUser("AM:You faction will not let you plunder their pilot reserves while you have perfectly able pilots in your barracks!",Username,true);
				return;
			}
			
			//ok the faction will allow them to buy pilots even with them in the queue but how many?
			if (p.getPersonalPilotQueue().getPilotQueue(unitType,weightClass).size() + numberOfPilots > Integer.parseInt(h.getConfig("MaxAllowedPilotsInQueueToBuyFromHouse"))){
				CampaignMain.cm.toUser("AM:Your Faction will only allow you to buy pilots from their reserve when you have "+h.getIntegerConfig("MaxAllowedPilotsInQueueToBuyFromHouse")+", or less, pilots in your barracks.",Username,true);
				return;
			}
			
			//ok the player has enough space lets see if they have the cash.
			int money = 0;
            
            if ( unitType == Unit.MEK )
                money = h.getIntegerConfig("CostToBuyNewPilot");
            else
                money = h.getIntegerConfig("CostToBuyNewProtoPilot");
            
			if ( p.getMoney() < money*numberOfPilots ){
				CampaignMain.cm.toUser("AM:You do not have enough money to procure a new pilot from your faction.("+ CampaignMain.cm.moneyOrFluMessage(true,true,money) + ") needed.",Username,true);
				return;
			}
			
			for ( int pilotCount = 0; pilotCount < numberOfPilots; pilotCount++) {
				p.addMoney(-money);
				
				SPilot pilot = h.getNewPilot(unitType);
	            //he is fresh out of the queue nothing gets set until he gets placed
	            //in a unit!
				p.getPersonalPilotQueue().addPilot(pilot,unitType, weightClass);
				
				String toUser = "";
				String skills = pilot.getSkillString(true,h.getPilotQueues().getBasePilotSkill(unitType)).trim();
				if (unitType == Unit.MEK) {
					toUser = "AM:You have purchased "+pilot.getName()+" ("+pilot.getGunnery()+"/"+pilot.getPiloting();
					if (skills == null || skills.equals(""))
						toUser  += ") from your faction for "+CampaignMain.cm.moneyOrFluMessage(true,true,money)+".";
					else
						toUser  += " " + skills + ") from your faction for "+CampaignMain.cm.moneyOrFluMessage(true,true,money)+".";
				} else {
					toUser = "AM:You have purchased " + pilot.getName() + " ("+pilot.getGunnery();
					if (skills == null || skills.equals(""))
						toUser  +=  ") from your faction for "+CampaignMain.cm.moneyOrFluMessage(true,true,money)+".";
					else
						toUser  += " " + skills + ") from your faction for "+CampaignMain.cm.moneyOrFluMessage(true,true,money)+".";
				}
				CampaignMain.cm.toUser(toUser,Username,true);
				//CampaignMain.cm.toUser("PL|PPQ|"+p.getPersonalPilotQueue().toString(true),Username,false);
	            CampaignMain.cm.toUser("PL|AP2PPQ|"+unitType+"|"+weightClass+"|"+pilot.toFileFormat("#",true),Username,false);
			}
		}//end hasMoreElements
	}//end process
}//end class