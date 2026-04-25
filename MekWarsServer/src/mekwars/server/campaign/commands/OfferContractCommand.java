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

public class OfferContractCommand implements Command {
    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

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

        //read in contract data
        String receivingPlayerName;
        int contractPayment;
        int contractDuration;
        String strContractType;

        try {
            receivingPlayerName = command.nextToken();
            contractPayment = Integer.parseInt(command.nextToken());
            contractDuration = Integer.parseInt(command.nextToken());
            strContractType = command.nextToken();
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Invalid Syntax: /offercontract Merc Name#ContractPayment#ContarctDuration#Contract Type[land,units,components,delay,exp]",
                  Username);
            return;
        }

        int contractType = server.campaign.mercenaries.ContractInfo.getContractType(strContractType);
        server.campaign.SPlayer offeringPlayer = server.campaign.CampaignMain.cm.getPlayer(Username);
        server.campaign.SPlayer receivingPlayer = server.campaign.CampaignMain.cm.getPlayer(receivingPlayerName);
        if (receivingPlayer == null) {
            server.campaign.CampaignMain.cm.toUser("AM:There's no such player", Username, true);
            return;
        }
        if ((receivingPlayer.getMyHouse()).isMercHouse()) {
            //make sure player is only offering one contract
            boolean noOtherOffers = true;
            int minContract = server.campaign.CampaignMain.cm.getIntegerConfig("MinContractEXP");

            if (contractType == server.campaign.mercenaries.ContractInfo.CONTRACT_DELAY
                      || contractType == server.campaign.mercenaries.ContractInfo.CONTRACT_LAND
                      || contractType == server.campaign.mercenaries.ContractInfo.CONTRACT_UNITS) {minContract /= 10;}

            for (int i = 0; i < server.campaign.CampaignMain.cm.getUnresolvedContracts().size(); i++) {
                server.campaign.mercenaries.ContractInfo info = server.campaign.CampaignMain.cm.getUnresolvedContracts()
                                                                      .get(i);
                if (info.getOfferingPlayer() == offeringPlayer) {
                    noOtherOffers = false;
                }//end if(offering player has contract outstanding)
            }//end for(all offered contracts)
            if (contractPayment < 2) {
                server.campaign.CampaignMain.cm.toUser("AM:You must pay a mercenary at least " +
                                                             server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                                   true,
                                                                   2) +
                                                             ".", Username, true);
            } else if (contractDuration < minContract) {
                server.campaign.CampaignMain.cm.toUser(
                      "AM:Mercenaries must do work to get paid. Set a contract term of at least " +
                            minContract +
                            " " +
                            strContractType,
                      Username,
                      true);
            } else if (noOtherOffers == false) {
                server.campaign.CampaignMain.cm.toUser("AM:You may offer only one contract at a time.", Username, true);
            }//end if(another offer exists)
            //make sure player can pay contract
            else if (offeringPlayer.getMoney() < contractPayment) {
                server.campaign.CampaignMain.cm.toUser(
                      "AM:Your offer exceeds your monetary resources. Get loans or lower your offer.",
                      Username,
                      true);
            }//end elseif(funds too low)
            else if (offeringPlayer.getMyHouse().isNewbieHouse()) {
                server.campaign.CampaignMain.cm.toUser("AM:No Mercenary would ever fight for SOL!", Username, true);
            }//end elseif(no Sol may offer)
            else if ((offeringPlayer.getMyHouse()).isMercHouse()) {
                server.campaign.CampaignMain.cm.toUser("AM:Mercenaries may not employ other mercenaries",
                      Username,
                      true);
            }//end elseif(offering playeris merc)
            //make sure that the merc is online. only loggedin players get contract offers.
            else if ((receivingPlayer.getMyHouse()).isLoggedIntoFaction(receivingPlayerName) == false) {
                server.campaign.CampaignMain.cm.toUser("AM:You may only offer contracts to players currently online",
                      Username,
                      true);
            }//end elseif -> player offline
            else if (((server.campaign.mercenaries.MercHouse) (receivingPlayer.getMyHouse())).getContractInfo(
                  receivingPlayer) != null) {//if a contract already exists
                server.campaign.CampaignMain.cm.toUser("AM:You may not offer contracts to mercenaries with employers",
                      Username,
                      true);
            }//end elseif->player has contract
            else {//player is a merc, non-trader, online, employable, etc so give him the offer.
                server.campaign.CampaignMain.cm.toUser(offeringPlayer.getName() +
                                                             " of " +
                                                             (offeringPlayer.getMyHouse()).getName() +
                                                             " has offered you " +
                                                             server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                                   false,
                                                                   contractPayment) +
                                                             " for " +
                                                             contractDuration +
                                                             " " +
                                                             strContractType +
                                                             " of service.", receivingPlayerName, true);
                server.campaign.CampaignMain.cm.toUser("AM:You have offered " +
                                                             receivingPlayerName +
                                                             " " +
                                                             server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                                   false,
                                                                   contractPayment) +
                                                             " for " +
                                                             contractDuration +
                                                             " " +
                                                             strContractType +
                                                             " of service.", Username, true);
                //make a new contract
                server.campaign.mercenaries.ContractInfo newContract = new server.campaign.mercenaries.ContractInfo(
                      contractDuration,
                      contractPayment,
                      offeringPlayer.getMyHouse(),
                      receivingPlayerName,
                      contractType);
                newContract.setOfferingPlayer(offeringPlayer);//set the offering player
                server.campaign.CampaignMain.cm.getUnresolvedContracts()
                      .add(newContract);//add newContract to vector of contracts.
                server.campaign.CampaignMain.cm.getUnresolvedContracts().trimToSize();
                server.campaign.CampaignMain.cm.toUser("AM:Accept this contract by <a href=\"MEKWARS/c acceptcontract#" +
                                                             offeringPlayer.getName() +
                                                             "\">clicking here</a>", receivingPlayerName, true);
                server.campaign.CampaignMain.cm.toUser("AM:Decline this contract by <a href=\"MEKWARS/c refusecontract#" +
                                                             offeringPlayer.getName() +
                                                             "\">clicking here</a>", receivingPlayerName, true);
                server.campaign.CampaignMain.cm.toUser(
                      "AM:Cancel this offer by <a href=\"MEKWARS/c canceloffer\">clicking here</a>",
                      Username,
                      true);
            }//end if offeringplayer is ok, and receivingplayer is non-trader merc.
        }//end ifismerc
        else {
            server.campaign.CampaignMain.cm.toUser("AM:You may not offer contracts to non-mercenary players.",
                  Username,
                  true);
        }//end else -> not merc.
    }//end elseif OFFERCONTRACT
}
