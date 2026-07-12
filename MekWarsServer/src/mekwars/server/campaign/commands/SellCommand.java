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
package mekwars.server.campaign.commands;

import common.Unit;
import common.util.UnitUtils;
import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;

/**
 * @author Helge Richter
 *
 */
public class SellCommand implements Command {
    private static final MMLogger LOGGER = MMLogger.create(SellCommand.class);

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        //load the player.
        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
        server.campaign.SHouse house = p.getMyHouse();

        /*
         * Check player/faction access before reading the command ...
         */
        //players in training houses may not sell units
        if (p.getMyHouse().isNewbieHouse()) {
            CampaignMain.campaignMain.toUser(
                  "AM:Players in training factions may not sell, scrap or donate their units.",
                  Username,
                  true);
            return;
        }

        //players whose factions don't have market selling access cannot sell units
        if (!p.getMyHouse().maySellOnBM()) {
            CampaignMain.campaignMain.toUser(
                  "AM:You are not allowed to sell units on the market. Your faction forbids it!",
                  Username,
                  true);
            return;
        }

        //players need XP to sell.
        int minBMEXP = Integer.parseInt(house.getConfig("MinEXPforBMSelling"));
        if (p.getExperience() < minBMEXP) {
            CampaignMain.campaignMain.toUser(
                  "AM:You are not allowed to sell units on the Market. Required Experience: " + minBMEXP + ".",
                  Username,
                  true);
            return;
        }

        //welfare recipients may not auction their units
        if (p.mayAcquireWelfareUnits()) {
            CampaignMain.campaignMain.toUser("AM:You may not auction any of your units while you are on welfare.",
                  Username,
                  true);
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
            salesTicks = Integer.parseInt((String) command.nextElement());
            minBid = Integer.parseInt((String) command.nextElement());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper format. Try: /c sell#unitid#ticks#minbid",
                  Username,
                  true);
            return;
        }
        server.campaign.SUnit unitToSell = p.getUnit(unitID);
        if (unitToSell == null) {
            CampaignMain.campaignMain.toUser("AM:You do not have a unit with ID#" + unitToSell + ".",
                  Username,
                  true);
            return;
        }

        //unmaintained units may not be sold.
        if (unitToSell.getStatus() == Unit.STATUS_UNMAINTAINED) {
            CampaignMain.campaignMain.toUser("AM:You may not sell unmaintained units on the Market.",
                  Username,
                  true);
            return;
        }

        //make sure the unit isn't already being sold ...
        if (unitToSell.getStatus() == Unit.STATUS_FORSALE) {
            CampaignMain.campaignMain.toUser("AM:The " + unitToSell.getModelName() + " is already for sale.",
                  Username,
                  true);
            return;
        }

        //some servers don't allow players to sell clan-tech units
        if (unitToSell.getEntity().isClan() && Boolean.parseBoolean(house.getConfig("BMNoClan"))) {
            CampaignMain.campaignMain.toUser("AM:Clan units may not be sold on the Market.", Username, true);
            return;
        }

        //some types/weights of units may not be sold. ask the unit if it's eligible.
        if (!server.campaign.SUnit.mayBeSoldOnMarket(unitToSell)) {
            CampaignMain.campaignMain.toUser("AM:The " +
                                                   unitToSell.getModelName() +
                                                   " may not be sold on the Market.", Username, true);
            return;
        }

        if (unitToSell.isChristmasUnit() && !CampaignMain.campaignMain.getBooleanConfig("Christmas_AllowBM")) {
            CampaignMain.campaignMain.toUser("AM:You are not allowed to sell Christmas units.", Username);
            return;
        }

        /*
         * Determine the amount of influence the player needs to sell
         * this unit (base cost + weight mod), then check amount.
         */
        int sellFluCost = Integer.parseInt(house.getConfig("BMSellFlu"));
        sellFluCost = sellFluCost + (unitToSell.getWeightclass()) * house.getIntegerConfig("BMFluSizeCost");
        if (p.getInfluence() < sellFluCost) {
            CampaignMain.campaignMain.toUser("AM:You need " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(false,
                                                         true,
                                                         sellFluCost)
                                                   +
                                                   " to sell the " +
                                                   unitToSell.getModelName() +
                                                   ".", Username, true);
            return;
        }

        /*
         * Ensure that the auction meets minimum bid and minimum time reqs.
         */
        int minticks = Integer.parseInt(house.getConfig("MinBMSalesTicks"));
        int minprice = Integer.parseInt(house.getConfig("MinBMSalesPrice"));
        if (salesTicks < minticks) {
            CampaignMain.campaignMain.toUser("AM:Units must be offered for at least " + minticks + " ticks.",
                  Username,
                  true);
            return;
        }
        if (minBid < minprice) {
            CampaignMain.campaignMain.toUser("AM:Units must have a minimum asking price of at least "
                                                   +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         false,
                                                         minprice) +
                                                   ".", Username, true);
            return;
        }

        /*
         * Check the max requirements as well.
         */
        int maxticks = Integer.parseInt(house.getConfig("MaxBMSalesTicks"));
        int maxprice = Integer.parseInt(house.getConfig("MaxBMSalesPrice"));
        if (salesTicks > maxticks && maxticks > 0) {
            CampaignMain.campaignMain.toUser("AM:Units may not be offered for more than " + maxticks + " ticks.",
                  Username,
                  true);
            return;
        }
        if (minBid > maxprice && maxprice > 0) {
            CampaignMain.campaignMain.toUser("AM:Units may not have an asking price of more than  "
                                                   +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         false,
                                                         maxprice) +
                                                   ".", Username, true);
            return;
        }


        /*
         * Don't let anyone sell a unit which is in an army. Ever. The old market actually
         * removed a unit from a player, which would update armies. The new market cannot
         * adjust the armies (one of the few ways the old was better), so we need to ensure
         * the unit isn't being used.
         */
        if (p.getAmountOfTimesUnitExistsInArmies(unitID) > 0) {
            CampaignMain.campaignMain.toUser("AM:The " +
                                                   unitToSell.getModelName() +
                                                   " must be removed from all armies before being added to the Market.",
                  Username,
                  true);
            return;
        }

        //check to see if partially repaired units may be sold.
        if (!Boolean.parseBoolean(house.getConfig("AllowSellingOfDamagedUnits"))
                  &&
                  (UnitUtils.hasArmorDamage(unitToSell.getEntity()) ||
                         UnitUtils.hasCriticalDamage(unitToSell.getEntity()))) {
            CampaignMain.campaignMain.toUser("AM:You may not sell damaged units on the black market!",
                  Username,
                  true);
            return;
        }

        /*
         * Decrease the players influence and add the sale.
         */
        CampaignMain.campaignMain.getMarket().addListing(Username, unitToSell, minBid, salesTicks);
        p.addInfluence(-sellFluCost);//this sets the player save, as well.

        /*
         * Inform the player and his faction.
         */
        CampaignMain.campaignMain.toUser("AM:The " +
                                               unitToSell.getModelName() +
                                               " is now on the Market "
                                               +
                                               "(" +
                                               CampaignMain.campaignMain.moneyOrFluMessage(false,
                                                     false,
                                                     -sellFluCost,
                                                     true) +
                                               ").", Username, true);
        if (!CampaignMain.campaignMain.getBooleanConfig("HiddenBMUnits")) {
            CampaignMain.campaignMain.doSendHouseMail(p.getMyHouse(),
                  "NOTE",
                  p.getName() + " added a unit to the market [" + unitToSell.getModelName() + "].");
        }
        LOGGER.info(p.getName() +
                             " added a " +
                             unitToSell.getModelName() +
                             ". Asking: " +
                             minBid +
                             ". Length: " +
                             salesTicks);

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}//end SellCommand.java
