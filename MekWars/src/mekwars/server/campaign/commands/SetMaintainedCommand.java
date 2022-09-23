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

public class SetMaintainedCommand implements Command {
	
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
		
        if ( CampaignMain.cm.isUsingAdvanceRepair() )
            return;

		SPlayer p = CampaignMain.cm.getPlayer(Username);
		int numtoset = 0;//ID# of the mech which is to get set as maintained
		
		try {
			numtoset = Integer.parseInt(command.nextToken());
		}//end try
		catch (NumberFormatException ex) {
			CampaignMain.cm.toUser("AM:SetMaintained command failed. Check your input. It should be something like this: /c setmaintained#12",Username,true);
			return;
		}//end catch
		
		SUnit unitToSet = p.getUnit(numtoset);
		if (unitToSet == null) {
			CampaignMain.cm.toUser("AM:Invalid id number. Make sure you're using the right unit number.",Username,true);
			return;
		}
		
		if (unitToSet.getStatus() == Unit.STATUS_OK) {
			CampaignMain.cm.toUser("AM:This unit is already maintained.",Username,true);
			return;
		}
		
		if (unitToSet.getStatus() == Unit.STATUS_FORSALE) {
			CampaignMain.cm.toUser("AM:You may not change the maintenance status of a unit which is being sold.",Username,true);
			return;
		}
		
		int unitSpace = SUnit.getHangarSpaceRequired(unitToSet,p.getMyHouse());
		if (p.getFreeBays() < unitSpace) {
			
			/*
			 * Player doesnt have enough techs. determine how many he needs and how much they will cost.
			 * If the player doesn't have enough money to maintain the unit in question, tell him. Else,
			 * present him with a link which will hire more techs and retry the maintain command.
			 */
			int techCost = p.getTechHiringFee();
			int numTechs = unitSpace - p.getFreeBays();
			techCost = techCost * numTechs;
			
			if (techCost > p.getMoney()) {
				
				String plural = "";
				if(numTechs == 1)
					plural = "s";
				
				CampaignMain.cm.toUser("AM:You need to hire " + numTechs + " more technician" + plural + " in order to maintain this unit. Doing so would " +
						" cost " + CampaignMain.cm.moneyOrFluMessage(true,false,techCost)+", and you only have " +CampaignMain.cm.moneyOrFluMessage(true,true,p.getMoney()) + ".",Username,true);
				return;	
			}
			
			String toReturn = "AM:You must hire " + numTechs + " more technicians in order to maintain this unit. Doing so will cost " +CampaignMain.cm.moneyOrFluMessage(true,true,techCost)+".<br>";
			toReturn += "AM:<a href=\"MEKWARS/c hireandmaintain#" + numTechs + "#" + numtoset + "\">Click here to hire the technicians and maintain the unit.</a>";    	
			CampaignMain.cm.toUser(toReturn,Username,true);
			return;
		}//end if(not enough techs to maintain)
		
		//passes checks. now actually make the unit maintained.
		unitToSet.setStatus(Unit.STATUS_OK);
		CampaignMain.cm.toUser("PL|SB|"+p.getTotalMekBays(),Username,false);
		CampaignMain.cm.toUser("PL|SF|"+p.getFreeBays(),Username,false);
		CampaignMain.cm.toUser("PL|SUS|"+unitToSet.getId()+"#"+Unit.STATUS_OK,Username,false);
		CampaignMain.cm.toUser(unitToSet.getPilot().getName() + "'s " + unitToSet.getModelName() + " is now being maintained." ,Username,true);
		p.setSave();
		
	}//end process() 
}//end SetMaintainedCommand class

