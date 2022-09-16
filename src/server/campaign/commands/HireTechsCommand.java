/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Nathan Morris (urgru)
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

import common.util.StringUtils;
import common.util.UnitUtils;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;

public class HireTechsCommand implements Command {
	
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
		
        if (CampaignMain.cm.isUsingAdvanceRepair() ){
            hireAdvanceTechs(command,Username);
            return;
        }
        
        
		//use /c hiretech#numbertohire
		int numtohire = 1;//default to 1 if no number present
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		
		int techCost = 0;//default cost
		
		//don't let SOL players hire techs
		if (p.getMyHouse().isNewbieHouse()) {
			CampaignMain.cm.toUser("AM:You are in a training faction, and may not hire techs until you join a normal faction.",Username,true);
			return;
		}
		
		try {
			numtohire = Integer.parseInt(command.nextToken());
		} catch (NumberFormatException ex) {
			CampaignMain.cm.toUser("AM:Hire command failed. Check your input. It should be something like this: /c hiretechs#3",Username,true);
			return;
		}//end catch
		
		//get cost per tech, after XP adjustment
		techCost = p.getTechHiringFee();
		
		//get the total cost to hire these techs (cost for each * number to hire)
		techCost = techCost * numtohire;
		
		//send a message is the techs 
		if (techCost > p.getMoney()) {
			CampaignMain.cm.toUser("AM:Hiring " + numtohire + " techs will cost you "+CampaignMain.cm.moneyOrFluMessage(true,false,techCost)+". You only have " + p.getMoney() + " "+CampaignMain.cm.moneyOrFluMessage(true,false,p.getMoney())+".",Username,true);
			return;
		}//end if(player doenst have enough money) 	
		
		
		int maxTechs = Integer.parseInt(p.getMyHouse().getConfig("MaxTechsToHire"));
		if (maxTechs != -1 && (p.getTechnicians() + numtohire > maxTechs)) {
			CampaignMain.cm.toUser("AM:Sorry but the max number of technicians you can hire is " + maxTechs + ".", Username, true);
			return;
		}
		
		
		//passed all of the return scenarios, so add the technicians
		p.addTechnicians(numtohire);
		// had to do it grammer bad! Torren
		if ( numtohire == 1 )
			CampaignMain.cm.toUser("AM:You've hired a technician! (-" +CampaignMain.cm.moneyOrFluMessage(true,false,techCost)+")",Username,true);
		else
			CampaignMain.cm.toUser("AM:You've hired " + numtohire + " technicians! (-" +CampaignMain.cm.moneyOrFluMessage(true,false,techCost)+")",Username,true);
		p.addMoney(-techCost); //Forgot to deduct the cost of Techs Torren.
	}//end process()
    
    private void hireAdvanceTechs(StringTokenizer command, String Username){
        
        SPlayer player = CampaignMain.cm.getPlayer(Username);
        SHouse house = player.getMyHouse();
        
        int numberToHire = Integer.parseInt(command.nextToken());
        int techType = UnitUtils.TECH_GREEN;
        
        int maxLevelTechHire = UnitUtils.TECH_REG;
        
        if ( !Boolean.parseBoolean(house.getConfig("AllowRegTechsToBeHired")))
            maxLevelTechHire = UnitUtils.TECH_GREEN;
        
        if ( command.hasMoreElements() )
            techType = Integer.parseInt(command.nextToken());
        
        if ( techType > maxLevelTechHire){
            CampaignMain.cm.toUser("AM:Sorry there are no techs of that skill level on the market.",Username,true);
            return;
        }
        
        int hireCost = Integer.parseInt(house.getConfig(UnitUtils.techDescription(techType)+"TechHireCost"));
        
        hireCost *= numberToHire;
        
        if ( player.getMoney() < hireCost){
            CampaignMain.cm.toUser("AM:Hiring " + numberToHire + " "+UnitUtils.techDescription(techType)+" techs will cost you "+CampaignMain.cm.moneyOrFluMessage(true,false,hireCost)+". You only have " + player.getMoney() + " "+CampaignMain.cm.moneyOrFluMessage(true,false,player.getMoney())+".",Username,true);
            return;
        }

        //passed all of the return scenarios, so add the technicians
        player.addTotalTechs(techType,numberToHire);
        player.addAvailableTechs(techType,numberToHire);
        player.addMoney(-hireCost); 
        
        if ( numberToHire == 1 )
            CampaignMain.cm.toUser("AM:You've hired "+ StringUtils.aOrAn(UnitUtils.techDescription(techType),true) + " technician! (-" +CampaignMain.cm.moneyOrFluMessage(true,false,hireCost)+")",Username,true);
        else
            CampaignMain.cm.toUser("AM:You've hired " + numberToHire + " "+UnitUtils.techDescription(techType) + " technicians! (-" +CampaignMain.cm.moneyOrFluMessage(true,false,hireCost) + ")",Username,true);
        
        
    }//end hireAdvanceTech
    
}//end HireTechsCommand()