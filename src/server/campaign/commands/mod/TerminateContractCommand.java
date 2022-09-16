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

package server.campaign.commands.mod;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.commands.Command;
import server.campaign.mercenaries.ContractInfo;
import server.campaign.mercenaries.MercHouse;


public class TerminateContractCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Player Name";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		//forcibly cancel a mercenary contract, returning escrow funds to hiring faction.
		SPlayer p = CampaignMain.cm.getPlayer(command.nextToken());
		SHouse faction = p.getMyHouse();
		
		if (!faction.isMercHouse()) {
			CampaignMain.cm.toUser("Only mercenary players have contracts. Nice try, though.", Username, true);
			return;
		}
		
		//cast the house and load the contract
		MercHouse mercFaction = (MercHouse)faction;
		ContractInfo contract = mercFaction.getContractInfo(p);
		
		if (contract == null) {
			CampaignMain.cm.toUser(p.getName() + " has no contract to cancel", Username, true);
			return;
		}
		
		//contract exists. terminate and return monies.
		int payment = contract.getPayment();
		SHouse employer = contract.getEmployingHouse();
		int refund = (int) (payment * .5);
		
		SPlayer contractingPlayer = contract.getOfferingPlayer();
		if (contractingPlayer != null) {
			contractingPlayer.addMoney(refund);
			CampaignMain.cm.toUser(Username + " abrogated your contract with"
					+ contract.getEmployingHouse().getName() + ". Funds returned from escrow (" 
					+ CampaignMain.cm.moneyOrFluMessage(true,true,refund,true) + ").", p.getName(), true);
		} else
			contract.getEmployingHouse().setMoney(employer.getMoney() + refund);
		
		mercFaction.endContract(p);
		CampaignMain.cm.toUser(Username + " abrogated your contract with" + contract.getEmployingHouse().getName() + ".", p.getName(), true);
		CampaignMain.cm.toUser("You revoked " + p.getName() + "'s contract with" + contract.getEmployingHouse().getName() + ".", Username, true);
		
	}//end process()
}//end TerminateContract