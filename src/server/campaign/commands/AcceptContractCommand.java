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

import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.mercenaries.ContractInfo;
import server.campaign.mercenaries.MercHouse;

public class AcceptContractCommand implements Command {
	
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
		
		String offeringPlayerName = (String)command.nextElement();
		SPlayer offeringPlayer = CampaignMain.cm.getPlayer(offeringPlayerName);
		SPlayer claimingPlayer = CampaignMain.cm.getPlayer(Username);
		String claimingPlayerName = claimingPlayer.getName();
		ContractInfo info = null;
		boolean contractAccepted = false;
		for (int i = 0; i < CampaignMain.cm.getUnresolvedContracts().size(); i++) {
			info = CampaignMain.cm.getUnresolvedContracts().get(i);
			String receivingPlayerName = info.getPlayerName();
			if (CampaignMain.cm.getPlayer(receivingPlayerName) == claimingPlayer) {//player can attempt to take offer
				//check to see if offer is from player merc wants to accept from
				if (info.getOfferingPlayerName().equalsIgnoreCase(offeringPlayerName)) {
					//save contract
					MercHouse factionToSaveContract = (MercHouse)(claimingPlayer.getMyHouse());
					factionToSaveContract.setContract(info, claimingPlayer);
					contractAccepted = true;
					CampaignMain.cm.getUnresolvedContracts().remove(i);
					CampaignMain.cm.getUnresolvedContracts().trimToSize();
					break;
				}
			}//end if(reciev = user)
		}//end for(search vector)
		
		if(info == null) {
			// No contract found
			return;
		}
		
		if (contractAccepted) {
			CampaignMain.cm.toUser("AM:You have accepted the contract offered by " + offeringPlayerName + " and are now in the employment of " + (offeringPlayer.getMyHouse()).getName(),Username,true);
			CampaignMain.cm.toUser(claimingPlayerName + "has accepted your contract offer.",offeringPlayerName,true);
			//do finances.
			int contractPay = info.getPayment();
			//code to pay player
			int immediatePay = (contractPay/2);
			claimingPlayer.addMoney(immediatePay);
			CampaignMain.cm.toUser("AM:Received " +CampaignMain.cm.moneyOrFluMessage(true,true,immediatePay)+". The remainder of your pay will be delivered upon contract completion.",Username,true);
			//code to decrease money of offering player.
			offeringPlayer.addMoney(0 - contractPay);
			CampaignMain.cm.toUser("AM:You have spent " +CampaignMain.cm.moneyOrFluMessage(true,true,contractPay)+" to hire " + claimingPlayerName,offeringPlayerName,true);
            CampaignMain.cm.toUser("PL|SHFF|"+claimingPlayer.getHouseFightingFor().getName(),claimingPlayer.getName(), false);
            
		}//end if(contractAccepted)
		else {//contract not set
			CampaignMain.cm.toUser("AM:You have no contract to accept from a player of the given name",Username,true);
		}//end else(no contract)
	}//end AcceptContractCommand
	
}