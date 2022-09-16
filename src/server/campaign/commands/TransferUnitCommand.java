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
import common.util.StringUtils;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;

public class TransferUnitCommand implements Command {

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

		//Acquire needed Data
		String targetPlayer;
		int unitid;
		try {
			targetPlayer = (String)command.nextElement();
			unitid = Integer.parseInt((String)command.nextElement());
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper format. Try: /c transferunit#TargetPlayer#UnitID",Username,true);
			return;
		}

		SPlayer targetplayer = CampaignMain.cm.getPlayer(targetPlayer);
		SPlayer player = CampaignMain.cm.getPlayer(Username);
		SHouse house = player.getMyHouse();
		boolean usesTechs = CampaignMain.cm.getBooleanConfig("UseTechnicians") && !CampaignMain.cm.isUsingAdvanceRepair();
		
		//Newbie House may not send units!
		if (house.isNewbieHouse()){
			CampaignMain.cm.toUser("AM:Players in SOL may not transfer units.", Username, true);
			return;
		}

		if (player.mayAcquireWelfareUnits() ){
			CampaignMain.cm.toUser("AM:You may not transfer any of your units while you are on welfare.",Username,true);
			return;
		}

		if (targetplayer == null) {
			CampaignMain.cm.toUser("AM:Could not find target player.",Username,true);
			return;
		}
		
		//No unit?
		SUnit m = player.getUnit(unitid);
		if (m == null) {
			CampaignMain.cm.toUser("AM:You do not own Unit #" + unitid + ".", Username, true);
			return;
		}

		//unit is in armies, and player is active/fighting
		if (player.getAmountOfTimesUnitExistsInArmies(unitid) > 0 && player.getDutyStatus() >= SPlayer.STATUS_ACTIVE) {
			CampaignMain.cm.toUser("AM:You may not tranfer units which are in active armies.", Username, true);
			return;
		}

		//for (SArmy currA : player.getArmies()) {
		//	if (currA.getUnit(unitid) != null && currA.isLocked()) {
		//		CampaignMain.cm.toUser("You may not transfer units which are in fighting armies.", Username, true);
		//		return;
		//	}
		//}

		//Not the same faction?
		if (!targetplayer.getMyHouse().equals(player.getMyHouse()) && !targetplayer.getMyHouse().getHouseFightingFor(targetplayer).equals(player.getMyHouse())) {
			CampaignMain.cm.toUser("AM:"+targetplayer.getName() + " is not in your faction. You cannot send him units.", Username, true);
			return;
			//Target has no room?
		} else if (targetplayer.getFreeBays() < SUnit.getHangarSpaceRequired(m,targetplayer.getMyHouse()) && !usesTechs) {
			//on a tech server, can accept units past limit. theyre just marked unmaintained
			CampaignMain.cm.toUser("AM:"+targetplayer.getName() + " has no room for that unit.", Username, true);
			return;
			//Target is not logged in?
		} else if (!targetplayer.getMyHouse().isLoggedIntoFaction(targetplayer.getName())) {
			CampaignMain.cm.toUser("AM:"+targetplayer.getName() + " is not logged in. You may only transfer to players who are online.", Username, true);
			return;
			//Same IP address?
		} else if (Boolean.parseBoolean(house.getConfig("IPCheck"))) {
			if (CampaignMain.cm.getServer().getIP(player.getName()).toString().equals(CampaignMain.cm.getServer().getIP(targetplayer.getName()).toString())) {
				CampaignMain.cm.toUser("AM:"+targetplayer.getName() + " has the same IP as you do. You can't send him units.", Username, true);
				return;
			}
		} 
		
		if ( !targetplayer.hasRoomForUnit(m.getType(), m.getWeightclass()) ) {
			CampaignMain.cm.toUser("AM:Sorry, " + targetplayer.getName() + " already has the maximum number of " + Unit.getWeightClassDesc(m.getWeightclass()) + " " + Unit.getTypeClassDesc(m.getType()) + "s", Username);
			return;
		}

		if ( m.isChristmasUnit() && !CampaignMain.cm.getBooleanConfig("Christmas_AllowTransfer")) {
			CampaignMain.cm.toUser("AM:No re-gifting!", Username);
			return;
		}
		
		//check transfer charge configuration
		float transferPayment = Float.parseFloat(house.getConfig("TransferPayment"));
		int senderCost = Math.round(transferPayment * player.getCurrentTechPayment());
		int receiverCost = Math.round(transferPayment * targetplayer.getCurrentTechPayment());
		
		String modName = m.getModelName();

		//if the sender pays, make sure he can afford the transfer without technicians quitting.
		if (!Boolean.parseBoolean(house.getConfig("SenderPaysOnTransfer"))) {senderCost = 0;}
		if (senderCost > player.getMoney()) {
			CampaignMain.cm.toUser("AM:You tried to send " + StringUtils.aOrAn(modName,true) + " to " + targetPlayer + ", but you cannot afford the transfer payment (" + CampaignMain.cm.moneyOrFluMessage(true,true,senderCost)+").", Username, true);
			return;
		}

		//if the receiver pays, make sure he can afford the transfer without technicians quitting.
		if (!Boolean.parseBoolean(house.getConfig("ReceiverPaysOnTransfer"))) {receiverCost = 0;}
		if (receiverCost > targetplayer.getMoney()) {
			CampaignMain.cm.toUser("AM:You tried to send " + StringUtils.aOrAn(modName,true) + " to " + targetPlayer + ", but he cannot afford the transfer payment. Transfer aborted.", Username, true);
			CampaignMain.cm.toUser("AM:"+Username + " tried to send you "+StringUtils.aOrAn(modName,true) + "; however, you cannot afford the tech payment the transfer would trigger (" + CampaignMain.cm.moneyOrFluMessage(true,true,receiverCost)+").", targetPlayer, true);
			return;
		}

		//check for confirmation
		boolean confirmedSend = false;
		if (command.hasMoreElements() && command.nextToken().equals("CONFIRM"))
			confirmedSend = true;

		//load the configurable
		int scrapLevel = Integer.parseInt(house.getConfig("TransferScrapLevel"));

		if (m.getMaintainanceLevel() <= scrapLevel && !confirmedSend) {
			CampaignMain.cm.toUser("AM:The unit you are trying to tranfer is not well maintained." +
					" Equipment which is already in a poor state of repair may be" +
					" irreparably damaged in transit.<br>" +
					" <a href=\"MEKWARS/c transferunit#" + targetplayer.getName() + "#" + unitid + 
					"#CONFIRM\">Click here to send the unit anyway</a>", Username, true);
			return;
		} else if (m.getMaintainanceLevel() <= scrapLevel && confirmedSend) {
			int rnd = CampaignMain.cm.getRandomNumber(100) + 1;
			if (rnd > m.getMaintainanceLevel()) {

				//if scrapping costs bills, subtract the appropriate amount.
				int mechscrapprice = 0;
				if (house.getIntegerConfig("ScrapsAllowed") > 0) {
					mechscrapprice = (int)(player.getMyHouse().getPriceForUnit(m.getWeightclass(), m.getType()) * Double.parseDouble(house.getConfig("ScrapCostMultiplier")));
					if (player.getMoney() < mechscrapprice)
						mechscrapprice = player.getMoney();
					player.addMoney(-mechscrapprice);
				}

				//remove all flu, even if scrapping is free
				int flutolose = player.getInfluence();
				player.addInfluence(-flutolose);

				String toSend = "AM:The " + modName + " didn't survive transit intact. HQ is displeased (";
				if (mechscrapprice > 0)
					toSend += CampaignMain.cm.moneyOrFluMessage(true,false, -mechscrapprice, true) + ", ";
				toSend += CampaignMain.cm.moneyOrFluMessage(false, false,-flutolose, true) + ").";
				CampaignMain.cm.toUser(toSend,player.getName(),true);

				CampaignMain.cm.toUser("AM:"+player.getName() + " tried to send you a " + modName + ", but it didn't survive the trip.",targetplayer.getName(),true);
				player.removeUnit(m.getId(), true);
				return;
			}
		}//end if(badly maintained and transfer confirmed)

		//send messages to the players, and take money if need be.
		String toSender = "AM:You transferred the " + modName + " to " + targetplayer.getName() + ".";
		if (senderCost > 0) {
			toSender += " Paid " + CampaignMain.cm.moneyOrFluMessage(true, false, senderCost) + " to your technicians.";
			player.addMoney(-senderCost);
		}
		
		String toReceiver = "AM:"+Username + " sent you " + StringUtils.aOrAn(modName, true) + ".";
		if (receiverCost > 0) {
			toReceiver += "Paid " + CampaignMain.cm.moneyOrFluMessage(true, false, receiverCost) + " to your technicians.";
			targetplayer.addMoney(-receiverCost);
		}
			
		CampaignMain.cm.toUser(toSender, Username, true);
		CampaignMain.cm.toUser(toReceiver, targetPlayer, true);

		//actual unit transfer
		player.removeUnit(m.getId(), true);
		targetplayer.addUnit(m, true);
		
		//check to see if this put the player into welfare
		if (player.mayAcquireWelfareUnits())
			CampaignMain.cm.doSendModMail("NOTE",Username + " has used the Transfer Unit Command to send himself into welfare.");
		
	}
}