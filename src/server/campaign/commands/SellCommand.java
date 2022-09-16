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
 * To change the template for this generated file go to Window - Preferences - Java - Code Generation - Code and Comments
 */
package server.campaign.commands;

import java.util.StringTokenizer;

import common.CampaignData;
import common.Unit;
import common.util.MWLogger;
import common.util.UnitUtils;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;

/**
 * @author Helge Richter
 *  
 */
public class SellCommand implements Command {
	
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
		
		//load the player.
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		SHouse house = p.getMyHouse();
		
		/*
		 * Check player/faction access before reading the command ...
		 */
		//players in training houses may not sell units
		if (p.getMyHouse().isNewbieHouse()) {
			CampaignMain.cm.toUser("AM:Players in training factions may not sell, scrap or donate their units.", Username, true);
			return;
		}
		
		//players whose factions don't have market selling access cannot sell units
		if (!p.getMyHouse().maySellOnBM()) {
			CampaignMain.cm.toUser("AM:You are not allowed to sell units on the market. Your faction forbids it!", Username, true);
			return;
		}
		
		//players need XP to sell.
		int minBMEXP = Integer.parseInt(house.getConfig("MinEXPforBMSelling"));
		if (p.getExperience() < minBMEXP) {
			CampaignMain.cm.toUser("AM:You are not allowed to sell units on the Market. Required Experience: " + minBMEXP + ".", Username, true);
			return;
		}
		
		//welfare recipients may not auction their units
		if (p.mayAcquireWelfareUnits()) {
			CampaignMain.cm.toUser("AM:You may not auction any of your units while you are on welfare.",Username,true);
			return;
		}
		
		/*
		 * Faction may use the BM. Make sure the command is properly
		 * formatted and that the player has a unit with the given ID.
		 */
		int unitID = -1;
		int salesTicks = -1;
		int minBid = -1;
    
		try {
			unitID = Integer.parseInt(command.nextToken());
			salesTicks = Integer.parseInt((String)command.nextElement());
			minBid= Integer.parseInt((String)command.nextElement());
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper format. Try: /c sell#unitid#ticks#minbid",Username,true);
			return;
		}
		SUnit unitToSell = p.getUnit(unitID);
		if (unitToSell == null) {
			CampaignMain.cm.toUser("AM:You do not have a unit with ID#" + unitToSell + ".",Username,true);
			return;
		}
		
		//unmaintained units may not be sold.
		if(unitToSell.getStatus() == Unit.STATUS_UNMAINTAINED) {
			CampaignMain.cm.toUser("AM:You may not sell unmaintained units on the Market.", Username, true);
			return;
		}
		
		//make sure the unit isn't already being sold ...
		if(unitToSell.getStatus() == Unit.STATUS_FORSALE) {
			CampaignMain.cm.toUser("AM:The " + unitToSell.getModelName() + " is already for sale.", Username, true);
			return;
		}
		
		//some servers don't allow players to sell clan-tech units
		if(unitToSell.getEntity().isClan() && Boolean.parseBoolean(house.getConfig("BMNoClan"))) {
			CampaignMain.cm.toUser("AM:Clan units may not be sold on the Market.", Username, true);
			return;
		}
		
		//some types/weights of units may not be sold. ask the unit if it's eligible.
		if(!SUnit.mayBeSoldOnMarket(unitToSell)) {
			CampaignMain.cm.toUser("AM:The " + unitToSell.getModelName() + " may not be sold on the Market.", Username, true);
			return;
		}
		
		if(unitToSell.isChristmasUnit() && !CampaignMain.cm.getBooleanConfig("Christmas_AllowBM")) {
			CampaignMain.cm.toUser("AM:You are not allowed to sell Christmas units.", Username);
			return;
		}
		
		/*
		 * Determine the amount of influence the player needs to sell
		 * this unit (base cost + weight mod), then check amount.
		 */
		int sellFluCost = Integer.parseInt(house.getConfig("BMSellFlu"));
		sellFluCost = sellFluCost + (unitToSell.getWeightclass()) * house.getIntegerConfig("BMFluSizeCost");
		if(p.getInfluence() < sellFluCost) {
			CampaignMain.cm.toUser("AM:You need " + CampaignMain.cm.moneyOrFluMessage(false,true,sellFluCost)
					+ " to sell the " + unitToSell.getModelName() + ".", Username, true);
			return;
		}
		
		/*
		 * Ensure that the auction meets minimum bid and minimum time reqs.
		 */
		int minticks = Integer.parseInt(house.getConfig("MinBMSalesTicks"));
		int minprice = Integer.parseInt(house.getConfig("MinBMSalesPrice"));
		if (salesTicks < minticks) {
			CampaignMain.cm.toUser("AM:Units must be offered for at least " + minticks + " ticks.", Username, true);
			return;
		}
		if (minBid < minprice) {
			CampaignMain.cm.toUser("AM:Units must have a minimum asking price of at least "
					+ CampaignMain.cm.moneyOrFluMessage(true,false,minprice) + ".", Username, true);
			return;
		}
		
		/*
		 * Check the max requirements as well.
		 */
		int maxticks = Integer.parseInt(house.getConfig("MaxBMSalesTicks"));
		int maxprice = Integer.parseInt(house.getConfig("MaxBMSalesPrice"));
		if (salesTicks > maxticks && maxticks > 0) {
			CampaignMain.cm.toUser("AM:Units may not be offered for more than " + maxticks + " ticks.", Username, true);
			return;
		}
		if (minBid > maxprice && maxprice > 0) {
			CampaignMain.cm.toUser("AM:Units may not have an asking price of more than  "
					+ CampaignMain.cm.moneyOrFluMessage(true,false,maxprice) + ".", Username, true);
			return;
		}
		
		
		/*
		 * Don't let anyone sell a unit which is in an army. Ever. The old market actually
		 * removed a unit from a player, which would update armies. The new market cannot
		 * adjust the armies (one of the few ways the old was better), so we need to ensure
		 * the unit isn't being used.
		 */
		if (p.getAmountOfTimesUnitExistsInArmies(unitID) > 0) {
			CampaignMain.cm.toUser("AM:The " + unitToSell.getModelName() + " must be removed from all armies before being added to the Market.", Username, true);
			return;
		}
		
		//check to see if partially repaired units may be sold.
		if (!Boolean.parseBoolean(house.getConfig("AllowSellingOfDamagedUnits")) 
				&& (UnitUtils.hasArmorDamage(unitToSell.getEntity()) || UnitUtils.hasCriticalDamage(unitToSell.getEntity()))){
			CampaignMain.cm.toUser("AM:You may not sell damaged units on the black market!",Username,true);
			return;
		}
		
		/*
		 * Decrease the players influence and add the sale.
		 */
		CampaignMain.cm.getMarket().addListing(Username, unitToSell, minBid, salesTicks);
		p.addInfluence(-sellFluCost);//this sets the player save, as well.
		
		/*
		 * Inform the player and his faction.
		 */
		CampaignMain.cm.toUser("AM:The " + unitToSell.getModelName() + " is now on the Market "
				+ "(" + CampaignMain.cm.moneyOrFluMessage(false,false,-sellFluCost,true) + ").", Username, true);
		if (! CampaignMain.cm.getBooleanConfig("HiddenBMUnits")) {
			CampaignMain.cm.doSendHouseMail(p.getMyHouse(), "NOTE", p.getName() + " added a unit to the market [" + unitToSell.getModelName() + "].");
		}
		MWLogger.bmLog(p.getName() + " added a " + unitToSell.getModelName() + ". Asking: " + minBid + ". Length: " + salesTicks);
				
	}//end process()
}//end SellCommand.java
