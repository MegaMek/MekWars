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

/*
 * Created on 10.01.2004
 *  
 */
package server.campaign.commands;

import java.util.StringTokenizer;

import common.Unit;
import common.util.StringUtils;
import common.util.UnitUtils;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.operations.OpsScrapThread;
import server.campaign.pilot.SPilot;

/**
 * @author Helge Richter
 *  
 */
public class ScrapCommand implements Command {
	
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
		SHouse house = p.getMyHouse();
		
		int scrapsAllowed = Integer.parseInt(house.getConfig("ScrapsAllowed"));
		if (scrapsAllowed <= 0) {
			CampaignMain.cm.toUser("AM:Scrapping is not allowed on this server.", Username, true);
			return;
		}
		
		if (house.isNewbieHouse()) {
			CampaignMain.cm.toUser("AM:SOL players may not Sell, Scrap or Donate their units!", Username, true);
			return;
		}
		
        if ( p.mayAcquireWelfareUnits() ){
            CampaignMain.cm.toUser("AM:You may not scrap any of your units while you are on welfare.",Username,true);
            return;
        }

        int mechid = -1;
		try {
			mechid = Integer.parseInt((String)command.nextElement());
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Formatting error. Try: /c scrap#ID Number", Username, true);
			return;
		}
		
        String strConfirm = "";
        if (command.hasMoreTokens()) {
            strConfirm = command.nextToken();
        }
        
		SUnit m = p.getUnit(mechid);
		if (m == null) {
			CampaignMain.cm.toUser("AM:Could not find a unit with the given ID.", Username, true);
			return;
		}
		
        
        if ( UnitUtils.isRepairing(m.getEntity()) ){
            CampaignMain.cm.toUser("AM:This unit is currently being repaired. You cannot scrap it until the repairs are complete!",Username,true);
            return;
        }
        
		if (m.getStatus() == Unit.STATUS_FORSALE) {
			CampaignMain.cm.toUser("AM:Units that are for sale on the Market may not be scrapped.", Username, true);
			return;
		}
		
		if (m.isChristmasUnit() && !CampaignMain.cm.getBooleanConfig("Christmas_AllowScrap")) {
			CampaignMain.cm.toUser("AM:Scrapping a Christmas gift?  Bad form.", Username);
			return;
		}
		
		if (p.getAmountOfTimesUnitExistsInArmies(mechid) > 0 && p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {	
			CampaignMain.cm.toUser("AM:You may not scrap units which are in active armies.", Username, true);
			return;
		}//end (unit is in armies and player is active)
		
		if (p.isUnitInLockedArmy(m.getId())) {
			CampaignMain.cm.toUser("AM:You may not scrap units which are in fighting armies.", Username, true);
			return;
		}
		
		//If he has not scrapped this tick and Scrapping is allowed, OR if the entity was salvaged recently
		if (p.getScrapsThisTick() >= scrapsAllowed && m.getScrappableFor() <= 0) {
			CampaignMain.cm.toUser("AM:You may only scrap " + scrapsAllowed + " unit(s) per tick.", Username, true);
			return;
		}
		
		//Determine scrap cost multiplier
		float costMulti = 0;
        if (!CampaignMain.cm.isUsingAdvanceRepair() || m.getType() != Unit.MEK)
        	costMulti = Float.parseFloat(house.getConfig("ScrapCostMultiplier"));
        else if (UnitUtils.getNumberOfDamagedEngineCrits(m.getEntity())  >= 3)
        	costMulti = Float.parseFloat(house.getConfig("CostToScrapEngined"));
        else if (UnitUtils.hasCriticalDamage(m.getEntity()))
        	costMulti = Float.parseFloat(house.getConfig("CostToScrapCriticallyDamaged"));
        else if (UnitUtils.hasArmorDamage(m.getEntity()))
        	costMulti = Float.parseFloat(house.getConfig("CostToScrapOnlArmorDamage"));
        else
        	costMulti = Float.parseFloat(house.getConfig("ScrapCostMultiplier"));
	
		//Now that we have the multimpliers, determine how much the scarp costs (or gives)
		int moneyToScrap = Math.round(p.getMyHouse().getPriceForUnit(m.getWeightclass(), m.getType()) * costMulti);
		int infToScrap = (int)(p.getMyHouse().getInfluenceForUnit(m.getWeightclass(), m.getType())* costMulti);
		
		//Allow negative monetary costs (give money back), but don't allow scrapping to grant flu.
		if (infToScrap < 0)
			infToScrap = 0;

		//Check to ensure player can afford the scrap
		if (p.getMoney() < moneyToScrap || p.getInfluence() < infToScrap && m.getScrappableFor() < 0) {
			CampaignMain.cm.toUser("AM:You cannot afford to scrap this unit. You need " + CampaignMain.cm.moneyOrFluMessage(true,true,moneyToScrap)+" and " +CampaignMain.cm.moneyOrFluMessage(false,true,infToScrap)+".",Username,true);
			return;
		}
		
		//Give the player the amount the unit can be scrapped for (post-game), or add/deduct the standard cost
		if (m.getScrappableFor() >= 0) {
			p.addMoney(m.getScrappableFor());
			CampaignMain.cm.toUser("AM:You scrapped the " + m.getModelName() + " (" + CampaignMain.cm.moneyOrFluMessage(true,true,m.getScrappableFor(),true)+ ").", Username, true);
		} else {
			// it was intentional to allow the post-game scrap to process without a confirm... you don't want the guy getting a message "scrap for +1 cbill" then getting "scrapped for -1800cb" because of the time lag 
            if (!strConfirm.equals("CONFIRM")) {
                String result = "AM:Quartermaster command will charge you " + CampaignMain.cm.moneyOrFluMessage(true, false, moneyToScrap) + " to scrap #" + m.getId() + " " + m.getModelName() + ".";
                result += "<br><a href=\"MEKWARS/c scrap#" + m.getId() + "#CONFIRM";
                result += "\">Click here to scrap the unit.</a>";
                CampaignMain.cm.toUser(result, Username, true);
                return;
            } else {
			p.addMoney(-moneyToScrap);
			p.addInfluence(-infToScrap);
			p.addScrapThisTick();
			CampaignMain.cm.toUser("AM:You scrapped the " + m.getModelName() + " (" + CampaignMain.cm.moneyOrFluMessage(true,true,-moneyToScrap,true) +  ", " + CampaignMain.cm.moneyOrFluMessage(false,true,-infToScrap,true) + ").", Username, true);
            }
		}

		//notify house and, if needed, send warning to mod channel
		CampaignMain.cm.doSendHouseMail(p.getMyHouse(), "NOTE", p.getName() + " scrapped " + StringUtils.aOrAn(m.getVerboseModelName(),true) + ".");
        if (p.mayAcquireWelfareUnits())
            CampaignMain.cm.doSendModMail("NOTE",Username + " scrapped a unit and sent himself into welfare.");

		//do the actual remove last, so the checkops show under the scrap string
		p.removeUnit(mechid, true);
		
		//if the player has an ops scrap thread running, try to remove this unit
		OpsScrapThread scrapT = CampaignMain.cm.getOpsManager().getScrapThreads().get(p.getName().toLowerCase());
		if (scrapT != null)
			scrapT.scrapUnit(mechid);

		//if PPQ's are on the pilot goes to the players barraks - no free getting rid of pilots.
        if ( Boolean.parseBoolean(house.getConfig("AllowPersonalPilotQueues")) 
        		&& !m.hasVacantPilot() 
        		&& m.isSinglePilotUnit() ){
            SPilot pilot = (SPilot)m.getPilot();
            p.getPersonalPilotQueue().addPilot(m.getPilot(), m.getWeightclass());
            CampaignMain.cm.toUser("PL|AP2PPQ|"+m.getType()+"|"+m.getWeightclass()+"|"+pilot.toFileFormat("#",true),Username,false);
            CampaignMain.cm.toUser(pilot.getName() + " was moved to your barracks.",Username,true);
            p.getPersonalPilotQueue().checkQueueAndWarn(p.getName(), m.getType(), m.getWeightclass());

        }
        else
            p.getMyHouse().addDispossessedPilot(m, false);

		CampaignMain.cm.addMechStat(m.getUnitFilename(), m.getWeightclass(), 0, 0, 1);

		//add PP to the faction for the scrapped unit. 1/4th of original components.
		int initialPP = p.getMyHouse().getPPCost(m.getWeightclass(), m.getType());
		p.getMyHouse().addPP(m.getWeightclass(), m.getType(), initialPP/4, true);
	}//end process()

}//end ScrapCommand