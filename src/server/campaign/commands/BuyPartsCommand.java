/*
 * MekWars - Copyright (C) 2007 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - JTighe (Torren@users.sourceforge.net)
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

import common.BMEquipment;
import common.util.UnitUtils;
import megamek.common.EquipmentType;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;

public class BuyPartsCommand implements Command {
	
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
		
        if ( !CampaignMain.cm.isUsingAdvanceRepair() ){
            return;
        }
        
		//use /c buyparts#part#numbertobuy
		int numtobuy = 1;//default to 1 if no number present
		String partName = "";
		SPlayer player = CampaignMain.cm.getPlayer(Username);
		try {
			partName = command.nextToken();
			numtobuy = Integer.parseInt(command.nextToken());
		}catch ( Exception ex) {
			CampaignMain.cm.toUser("AM:Invalid Syntax: /BuyParts Name#Amount", Username);
			return;
		}
		boolean allowTechCrossOver = CampaignMain.cm.getBooleanConfig("AllowCrossOverTech");
		BMEquipment bme = CampaignMain.cm.getPartsMarket().getEquipmentList().get(partName);
		
		if ( bme == null || (!allowTechCrossOver && !UnitUtils.isSameTech(bme.getTechLevel(), player.getMyHouse().getTechLevel()))) {
			CampaignMain.cm.toUser("AM:"+partName+" not found on the black market", Username);
			return;
		}
		
		if ( numtobuy < 1 ){
			CampaignMain.cm.toUser("AM:You cannot buy negative parts!", Username);
			return;
		}
		
		int cost = (int)Math.ceil(numtobuy * bme.getCost()); 
		if ( numtobuy > bme.getAmount() ) {
			CampaignMain.cm.toUser("AM:There are only "+bme.getAmount()+" crits left select a smaller amount", Username);
			return;
		}
		
		if (  cost > player.getMoney() ) {
			CampaignMain.cm.toUser("AM:It will cost you "+CampaignMain.cm.moneyOrFluMessage(true, true, cost)+" you only have "+CampaignMain.cm.moneyOrFluMessage(true, true, player.getMoney())+".", Username);
			return;
		}
		
		EquipmentType eq = EquipmentType.get(bme.getEquipmentInternalName());
		
		bme.setAmount(bme.getAmount() - numtobuy);
		player.addMoney(-cost);
		
		if ( eq == null ) {
			CampaignMain.cm.toUser("AM:You have bought "+numtobuy+" "+bme.getEquipmentInternalName()+" crits for "+CampaignMain.cm.moneyOrFluMessage(true, true, cost)+".", Username);
			player.updatePartsCache(bme.getEquipmentInternalName(), numtobuy);
		}
		else {
			CampaignMain.cm.toUser("AM:You have bought "+numtobuy+" "+eq.getName()+" crits for "+CampaignMain.cm.moneyOrFluMessage(true, true, cost)+".", Username);
			player.updatePartsCache(eq.getInternalName(), numtobuy);
		}
		CampaignMain.cm.getPartsMarket().updatePartsBlackMarketAllPlayers();
	}//end process()

}//end BuyPartsCommand()