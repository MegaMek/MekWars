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
import server.campaign.SArmy;
import server.campaign.SPlayer;
import server.campaign.SUnit;

public class DirectSellUnitCommand implements Command {

    int accessLevel = 2;

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    String syntax = "buyer#seller#unitid#sellPrice";

    public String getSyntax() {
        return syntax;
    }

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
                return;
            }
        }

        // Syntax directsellUnit#buyername#sellername#unitid#sellprice
        SPlayer p = CampaignMain.cm.getPlayer(Username);

        if (p.mayAcquireWelfareUnits()) {
            CampaignMain.cm.toUser("AM:You may not sell any of you units while you are on welfare.", Username, true);
            return;
        }

        // Acquire needed Data
        String buyer = command.nextToken();
        String seller = command.nextToken();

        int unitid = Integer.parseInt((String) command.nextElement());
        int sellPrice = Integer.parseInt(command.nextToken());
        SPlayer pBuyer = CampaignMain.cm.getPlayer(buyer);
        SPlayer pSeller = CampaignMain.cm.getPlayer(seller);
        boolean usesTechs = CampaignMain.cm.getBooleanConfig("UseTechnicians");

        if (!CampaignMain.cm.getBooleanConfig("UseDirectSell")) {
            CampaignMain.cm.toUser("AM:Direct Sell is not allowed on this server!", Username, true);
            return;
        }

        if (pSeller == null) {
            CampaignMain.cm.toUser("AM:Selling player (" + seller + ") could not be found.", Username, true);
            return;
        }

        if (pBuyer == null) {
            CampaignMain.cm.toUser("AM:Buying player (" + buyer + ") could not be found.", Username, true);
            return;
        }

        // Newbie House may not send units!
        if (pSeller.getMyHouse().isNewbieHouse()) {
            CampaignMain.cm.toUser("AM:Players in SOL may not direct sell units.", Username, true);
            return;
        }

        // Acquire Unit
        SUnit m = pSeller.getUnit(unitid);
        if (m == null) {
            CampaignMain.cm.toUser("AM:Could not find a unit with ID#" + unitid + ".", Username, true);
            return;
        }

        if (m.getStatus() == Unit.STATUS_FORSALE) {
            CampaignMain.cm.toUser("AM:You may not directly sell units already on the open market.", Username, true);
            return;
        }

        if (m.isChristmasUnit() && !CampaignMain.cm.getBooleanConfig("Christmas_AllowDirectSell")) {
        	CampaignMain.cm.toUser("AM:You may not sell Christmas units.", Username);
        	return;
        }
        
        // Target has no room?
        if (pBuyer.getFreeBays() < SUnit.getHangarSpaceRequired(m, pBuyer.getMyHouse()) && !usesTechs) {
            // on a tech server, can accept units past limit. theyre just marked
            // unmaintained
            CampaignMain.cm.toUser(pBuyer.getName() + " has no room for that unit.", seller, true);
            return;
            // Target is not logged in?
        } else if (!pBuyer.getMyHouse().isLoggedIntoFaction(buyer)) {
            CampaignMain.cm.toUser(pBuyer.getName() + " is not logged in. You may only transfer to pSellers who are online.", Username, true);
            return;
            // Same IP address?
        } else if (CampaignMain.cm.getBooleanConfig("IPCheck")) {
            if (CampaignMain.cm.getServer().getIP(pSeller.getName()).toString().equals(CampaignMain.cm.getServer().getIP(pBuyer.getName()).toString())) {
                CampaignMain.cm.toUser(pBuyer.getName() + " has the same IP as you do. You can't send him units.", Username, true);
                return;
            }
        } else if (!pBuyer.hasRoomForUnit(m.getType(), m.getWeightclass())) {
            CampaignMain.cm.toUser(pBuyer.getName() + " has no room for another " + Unit.getWeightClassDesc(m.getWeightclass()) + " " + Unit.getTypeClassDesc(m.getType()), Username, true);
            return;
        } else if (m.getStatus() == Unit.STATUS_UNMAINTAINED) {
            CampaignMain.cm.toUser("AM:You may not sell unmaintained mechs.", Username, true);
            return;
        } else if (pSeller.getAmountOfTimesUnitExistsInArmies(m.getId()) > 0 && pSeller.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
            CampaignMain.cm.toUser("AM:You may not sell units which are in active armies.", Username, true);
            return;
        }// end (unit is in armies and player is active)

        for (SArmy currA : pSeller.getArmies()) {
            if (currA.isLocked() && currA.getUnit(m.getId()) != null) {
                CampaignMain.cm.toUser("AM:You may not sell units which are in fighting armies.", pSeller.getName(), true);
                return;
            }
        }

        boolean confirmedSend = false;
        if (command.hasMoreElements()) {
            if (((String) command.nextElement()).equals("CONFIRM")) {
                confirmedSend = true;
            }
        }

        // check transfer charge configuration
        String basePayment = "SellDirect" + Unit.getWeightClassDesc(m.getWeightclass()) + Unit.getTypeClassDesc(m.getType()) + "Price";
        int transferPayment = CampaignMain.cm.getIntegerConfig(basePayment);

        // if the receiver pays, make sure he can afford the transfer without
        // technicians quitting.
        int costToRecipient = transferPayment;
        costToRecipient += sellPrice;

        if (!confirmedSend) {
            if (Username.equals(buyer)) {
                CampaignMain.cm.toUser("AM:Illegal transaction! The staff was alerted!", buyer, true);
                CampaignMain.cm.doSendModMail("NOTE", Username + " tried to illegally buy a unit from another player<br>Username: " + Username + " Buyer: " + buyer + " Seller: " + seller + " Unitid: " + m.getId());
                pSeller.setPlayerSellingto("");
                pBuyer.setPlayerSellingto("");
                return;
            }

            pSeller.setPlayerSellingto(buyer);
            CampaignMain.cm.toUser(seller + " is trying to sell you " + StringUtils.aOrAn(m.getModelName(), true) + " for " + CampaignMain.cm.moneyOrFluMessage(true, true, costToRecipient) + "<br><a href=\"MEKWARS/c directsellunit#" + pBuyer.getName() + "#" + pSeller.getName() + "#" + unitid + "#" + sellPrice + "#CONFIRM\">Click here to buy</a>", buyer, true);
            return;
        }

        if (!pSeller.getPlayerSellingto().equalsIgnoreCase(buyer)) {
            CampaignMain.cm.toUser("AM:Illegal transaction! The staff was alerted!", buyer, true);
            CampaignMain.cm.doSendModMail("NOTE", Username + " tried to illegally buy a unit from another player<br>Username: " + Username + " Buyer: " + buyer + " Seller: " + seller + " Unitid: " + m.getId());
            pSeller.setPlayerSellingto("");
            pBuyer.setPlayerSellingto("");
            return;
        }

        if (pBuyer.getMoney() < costToRecipient + 1) {
            CampaignMain.cm.toUser("AM:You tried to sell " + StringUtils.aOrAn(m.getModelName(), true) + " to " + pBuyer.getName() + ", but they " + "cannot afford the payment. Transaction aborted.", seller, true);
            CampaignMain.cm.toUser(seller + " tried to sell you " + StringUtils.aOrAn(m.getModelName(), true) + "; however, you could not " + "afford the payment (" + CampaignMain.cm.moneyOrFluMessage(true, true, costToRecipient) + ").", pBuyer.getName(), true);
            pSeller.setPlayerSellingto("");
            pBuyer.setPlayerSellingto("");
            return;
        }// end if(receiver cant pay)

        // Nothing prevents it from happening, so send the unit
        pBuyer.addMoney(-costToRecipient);
        pSeller.addMoney(sellPrice);

        String result = "AM:The " + m.getModelName() + " was sold to " + pBuyer.getName() + " for " + CampaignMain.cm.moneyOrFluMessage(true, true, costToRecipient);

        if (costToRecipient - sellPrice > 0) {
            result += ".  However your contacts took " + CampaignMain.cm.moneyOrFluMessage(true, true, (costToRecipient - sellPrice)) + " for themselves.";
        }

        CampaignMain.cm.toUser(result, seller, true);
        CampaignMain.cm.toUser(pSeller.getName() + " has sold you " + StringUtils.aOrAn(m.getModelName(), true) + " for " + CampaignMain.cm.moneyOrFluMessage(true, true, costToRecipient) + ".", pBuyer.getName(), true);

        pSeller.removeUnit(m.getId(), true);
        pBuyer.addUnit(m, true);

        pSeller.setPlayerSellingto("");
        pBuyer.setPlayerSellingto("");

        if (p.mayAcquireWelfareUnits()) {
            CampaignMain.cm.doSendModMail("NOTE", Username + " has used the Direct Sell Command and sent themself into welfare.");
        }

    }
}