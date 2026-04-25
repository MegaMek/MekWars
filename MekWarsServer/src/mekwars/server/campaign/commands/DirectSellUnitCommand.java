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

package mekwars.server.campaign.commands;

import common.Unit;
import common.util.StringUtils;

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

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }

        // Syntax directsellUnit#buyername#sellername#unitid#sellprice
        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);

        if (p.mayAcquireWelfareUnits()) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not sell any of you units while you are on welfare.",
                  Username,
                  true);
            return;
        }

        // Acquire needed Data
        String buyer = command.nextToken();
        String seller = command.nextToken();

        int unitid = Integer.parseInt((String) command.nextElement());
        int sellPrice = Integer.parseInt(command.nextToken());
        server.campaign.SPlayer pBuyer = server.campaign.CampaignMain.cm.getPlayer(buyer);
        server.campaign.SPlayer pSeller = server.campaign.CampaignMain.cm.getPlayer(seller);
        boolean usesTechs = server.campaign.CampaignMain.cm.getBooleanConfig("UseTechnicians");

        if (!server.campaign.CampaignMain.cm.getBooleanConfig("UseDirectSell")) {
            server.campaign.CampaignMain.cm.toUser("AM:Direct Sell is not allowed on this server!", Username, true);
            return;
        }

        if (pSeller == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Selling player (" + seller + ") could not be found.",
                  Username,
                  true);
            return;
        }

        if (pBuyer == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Buying player (" + buyer + ") could not be found.",
                  Username,
                  true);
            return;
        }

        // Newbie House may not send units!
        if (pSeller.getMyHouse().isNewbieHouse()) {
            server.campaign.CampaignMain.cm.toUser("AM:Players in SOL may not direct sell units.", Username, true);
            return;
        }

        // Acquire Unit
        server.campaign.SUnit m = pSeller.getUnit(unitid);
        if (m == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Could not find a unit with ID#" + unitid + ".", Username, true);
            return;
        }

        if (m.getStatus() == Unit.STATUS_FORSALE) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not directly sell units already on the open market.",
                  Username,
                  true);
            return;
        }

        if (m.isChristmasUnit() && !server.campaign.CampaignMain.cm.getBooleanConfig("Christmas_AllowDirectSell")) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not sell Christmas units.", Username);
            return;
        }

        // Target has no room?
        if (pBuyer.getFreeBays() < server.campaign.SUnit.getHangarSpaceRequired(m, pBuyer.getMyHouse()) && !usesTechs) {
            // on a tech server, can accept units past limit. theyre just marked
            // unmaintained
            server.campaign.CampaignMain.cm.toUser(pBuyer.getName() + " has no room for that unit.", seller, true);
            return;
            // Target is not logged in?
        } else if (!pBuyer.getMyHouse().isLoggedIntoFaction(buyer)) {
            server.campaign.CampaignMain.cm.toUser(pBuyer.getName() +
                                                         " is not logged in. You may only transfer to pSellers who are online.",
                  Username,
                  true);
            return;
            // Same IP address?
        } else if (server.campaign.CampaignMain.cm.getBooleanConfig("IPCheck")) {
            if (server.campaign.CampaignMain.cm.getServer().getIP(pSeller.getName()).toString().equals(
                  server.campaign.CampaignMain.cm.getServer().getIP(pBuyer.getName()).toString())) {
                server.campaign.CampaignMain.cm.toUser(pBuyer.getName() +
                                                             " has the same IP as you do. You can't send him units.",
                      Username,
                      true);
                return;
            }
        } else if (!pBuyer.hasRoomForUnit(m.getType(), m.getWeightclass())) {
            server.campaign.CampaignMain.cm.toUser(pBuyer.getName() +
                                                         " has no room for another " +
                                                         Unit.getWeightClassDesc(m.getWeightclass()) +
                                                         " " +
                                                         Unit.getTypeClassDesc(m.getType()), Username, true);
            return;
        } else if (m.getStatus() == Unit.STATUS_UNMAINTAINED) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not sell unmaintained mechs.", Username, true);
            return;
        } else if (pSeller.getAmountOfTimesUnitExistsInArmies(m.getId()) > 0 &&
                         pSeller.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not sell units which are in active armies.",
                  Username,
                  true);
            return;
        }// end (unit is in armies and player is active)

        for (server.campaign.SArmy currA : pSeller.getArmies()) {
            if (currA.isLocked() && currA.getUnit(m.getId()) != null) {
                server.campaign.CampaignMain.cm.toUser("AM:You may not sell units which are in fighting armies.",
                      pSeller.getName(),
                      true);
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
        String basePayment = "SellDirect" +
                                   Unit.getWeightClassDesc(m.getWeightclass()) +
                                   Unit.getTypeClassDesc(m.getType()) +
                                   "Price";
        int transferPayment = server.campaign.CampaignMain.cm.getIntegerConfig(basePayment);

        // if the receiver pays, make sure he can afford the transfer without
        // technicians quitting.
        int costToRecipient = transferPayment;
        costToRecipient += sellPrice;

        if (!confirmedSend) {
            if (Username.equals(buyer)) {
                server.campaign.CampaignMain.cm.toUser("AM:Illegal transaction! The staff was alerted!", buyer, true);
                server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                      Username +
                            " tried to illegally buy a unit from another player<br>Username: " +
                            Username +
                            " Buyer: " +
                            buyer +
                            " Seller: " +
                            seller +
                            " Unitid: " +
                            m.getId());
                pSeller.setPlayerSellingto("");
                pBuyer.setPlayerSellingto("");
                return;
            }

            pSeller.setPlayerSellingto(buyer);
            server.campaign.CampaignMain.cm.toUser(seller +
                                                         " is trying to sell you " +
                                                         StringUtils.aOrAn(m.getModelName(), true) +
                                                         " for " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               true,
                                                               costToRecipient) +
                                                         "<br><a href=\"MEKWARS/c directsellunit#" +
                                                         pBuyer.getName() +
                                                         "#" +
                                                         pSeller.getName() +
                                                         "#" +
                                                         unitid +
                                                         "#" +
                                                         sellPrice +
                                                         "#CONFIRM\">Click here to buy</a>", buyer, true);
            return;
        }

        if (!pSeller.getPlayerSellingto().equalsIgnoreCase(buyer)) {
            server.campaign.CampaignMain.cm.toUser("AM:Illegal transaction! The staff was alerted!", buyer, true);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username +
                        " tried to illegally buy a unit from another player<br>Username: " +
                        Username +
                        " Buyer: " +
                        buyer +
                        " Seller: " +
                        seller +
                        " Unitid: " +
                        m.getId());
            pSeller.setPlayerSellingto("");
            pBuyer.setPlayerSellingto("");
            return;
        }

        if (pBuyer.getMoney() < costToRecipient + 1) {
            server.campaign.CampaignMain.cm.toUser("AM:You tried to sell " +
                                                         StringUtils.aOrAn(m.getModelName(), true) +
                                                         " to " +
                                                         pBuyer.getName() +
                                                         ", but they " +
                                                         "cannot afford the payment. Transaction aborted.",
                  seller,
                  true);
            server.campaign.CampaignMain.cm.toUser(seller +
                                                         " tried to sell you " +
                                                         StringUtils.aOrAn(m.getModelName(), true) +
                                                         "; however, you could not " +
                                                         "afford the payment (" +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               true,
                                                               costToRecipient) +
                                                         ").", pBuyer.getName(), true);
            pSeller.setPlayerSellingto("");
            pBuyer.setPlayerSellingto("");
            return;
        }// end if(receiver cant pay)

        // Nothing prevents it from happening, so send the unit
        pBuyer.addMoney(-costToRecipient);
        pSeller.addMoney(sellPrice);

        String result = "AM:The " +
                              m.getModelName() +
                              " was sold to " +
                              pBuyer.getName() +
                              " for " +
                              server.campaign.CampaignMain.cm.moneyOrFluMessage(true, true, costToRecipient);

        if (costToRecipient - sellPrice > 0) {
            result += ".  However your contacts took " +
                            server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                  true,
                                  (costToRecipient - sellPrice)) +
                            " for themselves.";
        }

        server.campaign.CampaignMain.cm.toUser(result, seller, true);
        server.campaign.CampaignMain.cm.toUser(pSeller.getName() +
                                                     " has sold you " +
                                                     StringUtils.aOrAn(m.getModelName(), true) +
                                                     " for " +
                                                     server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                           true,
                                                           costToRecipient) +
                                                     ".", pBuyer.getName(), true);

        pSeller.removeUnit(m.getId(), true);
        pBuyer.addUnit(m, true);

        pSeller.setPlayerSellingto("");
        pBuyer.setPlayerSellingto("");

        if (p.mayAcquireWelfareUnits()) {
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username + " has used the Direct Sell Command and sent themself into welfare.");
        }

    }
}
