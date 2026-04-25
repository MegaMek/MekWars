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

package mekwars.server.campaign.operations;

import common.util.MWLogger;
import server.util.StringUtil;

public class OpsScrapThread extends Thread {

    // VARIABLES
    private String playerName;
    private int maxTotalPayment;
    private int paymentsToDate;
    private long waitTime;
    private boolean isFinished;
    private java.util.TreeMap<Integer, Integer> salvagedUnits;

    // CONSTRUCTORS
    public OpsScrapThread(String playerName) {
        super(playerName + " Scrap Thread");
        this.playerName = playerName;
        waitTime = Long.parseLong(server.campaign.CampaignMain.cm.getConfig("TimeToSelectSalvage"));

        maxTotalPayment = 0;
        paymentsToDate = 0;
        salvagedUnits = new java.util.TreeMap<Integer, Integer>();

        isFinished = false;
    }

    // METHODS
    public void addScrappableUnit(int unitID, int scrapValue) {
        salvagedUnits.put(unitID, scrapValue);
    }

    public void setMaxPayment(int maxPayment) {

        maxTotalPayment = maxPayment;

        /*
         * When max is set, loop through salvaged units and cap their scrap
         * values such that none pays more than the maximum.
         */
        // First loop finds overages
        java.util.TreeMap<Integer, Integer> toReplace = new java.util.TreeMap<Integer, Integer>();
        for (Integer currID : salvagedUnits.keySet()) {
            int currScrapValue = salvagedUnits.get(currID).intValue();
            // MWLogger.errLog("currID: "+currID+" maxTotalPayment: "+maxTotalPayment+" currScrapValue: "+currScrapValue);
            if (currScrapValue > maxTotalPayment) {
                toReplace.put(currID, maxTotalPayment);
            }
        }

        // Second changes values.
        for (int currID : toReplace.keySet()) {
            salvagedUnits.put(currID, toReplace.get(currID));
        }
    }

    public void stopScrap() {

        server.campaign.SPlayer currPlayer = server.campaign.CampaignMain.cm.getPlayer(playerName);
        if (currPlayer == null) {
            return;
        }

        // reset SUnit scrappable values
        for (int currID : salvagedUnits.keySet()) {
            server.campaign.SUnit currU = currPlayer.getUnit(currID);

            // player may have transferred the unit out
            if (currU == null) {
                continue;
            }

            currU.setScrappableFor(-1);
            server.campaign.CampaignMain.cm.toUser("PL|UU|" + currU.getId() + "|" + currU.toString(true),
                  playerName,
                  false);
        }

        if (currPlayer.getDutyStatus() >= server.campaign.SPlayer.STATUS_RESERVE) {
            server.campaign.CampaignMain.cm.toUser("You may only scrap units salvaged in your most recent game.",
                  playerName);
        }

        salvagedUnits.clear();
        isFinished = true;
    }

    public void scrapUnit(int unitID) {

        // if the unit isnt in the tree, break out
        Integer scrapValue = salvagedUnits.remove(unitID);
        if (scrapValue == null) {
            return;
        }

        // get the player. he should never be null, but check and return just in
        // case.
        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(playerName);
        if (p == null) {
            return;
        }

        /*
         * Now that we have a real value, add it to the payments to date and
         * check to ensure that other units in the tree are not going to cross
         * the pay ceiling if they are scrapped. Reduce the scrap value of any
         * unit which would breach the cap, and send a PL|UU to ensure that the
         * owner's GUI shows the same.
         */
        paymentsToDate += scrapValue;
        int maxScrapValue = maxTotalPayment - scrapValue;
        java.util.TreeMap<Integer, Integer> toReplace = new java.util.TreeMap<Integer, Integer>();

        // determine overages
        for (Integer currID : salvagedUnits.keySet()) {
            int currScrapValue = salvagedUnits.get(currID).intValue();
            if (currScrapValue > maxScrapValue) {
                toReplace.put(currID, maxScrapValue);
            }
        }

        /*
         * Check to see that the player still owns the replaced units (hasn't
         * donated or transferred or sold). If so, change their values and send
         * a PL|UU.
         */
        for (int currID : toReplace.keySet()) {

            server.campaign.SUnit currU = p.getUnit(currID);
            if (currU == null) {
                salvagedUnits.remove(currID);
                continue;
            }
            currU.setScrappableFor(maxScrapValue);
            salvagedUnits.put(currID, toReplace.get(currID));
            server.campaign.CampaignMain.cm.toUser("PL|UU|" + currU.getId() + "|" + currU.toString(true),
                  playerName,
                  false);
        }

    }// end scrapUnit()

    @Override
    public synchronized void run() {

        /*
         * Fetch the player. If he's null or logged out, he's presumably quit or
         * disconnected. He doesn't need any PL|UU's or the opportunity to scrap
         * for cash.
         */
        server.campaign.SPlayer currPlayer = server.campaign.CampaignMain.cm.getPlayer(playerName);
        if (currPlayer == null) {
            return;
        } else if (currPlayer.getDutyStatus() < server.campaign.SPlayer.STATUS_RESERVE) {
            return;
        }
        if (!server.campaign.CampaignMain.cm.getBooleanConfig("SelectableSalvage")) {
            return;
        }

        // make the wait-seconds miliseconds
        waitTime *= 1000;

        // Player is real. Send the requisite unit updates.
        for (int currID : salvagedUnits.keySet()) {
            server.campaign.SUnit currU = currPlayer.getUnit(currID);
            if (currU == null) {
                continue;
            }

            currU.setScrappableFor(salvagedUnits.get(currID));
            server.campaign.CampaignMain.cm.toUser("PL|UU|" + currU.getId() + "|" + currU.toString(true),
                  playerName,
                  false);
        }

        // inform the potential scrapper that he can get some cash.
        server.campaign.CampaignMain.cm.toUser("You have " +
                                                     StringUtil.readableTimeWithSeconds(waitTime) +
                                                     " to scrap salvaged units and recover repair costs.",
              playerName,
              true);

        // do the wait
        try {
            this.wait(waitTime);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

        /*
         * Wait is over. Refresh the player (in case he logged out and then
         * returned) and check the number of units left in the scrapmap. If he
         * still has some of the units, 0 their scrap values and send updates.
         */
        if (isFinished) {
            return;
        }

        // only report if player is still missing
        currPlayer = server.campaign.CampaignMain.cm.getPlayer(playerName);
        if (currPlayer == null) {
            return;
        }

        boolean shouldGetText = (currPlayer.getDutyStatus() >= server.campaign.SPlayer.STATUS_RESERVE);

        // Player is real. Send the requisite unit updates.
        boolean scrappablesRemaining = false;
        for (int currID : salvagedUnits.keySet()) {
            server.campaign.SUnit currU = currPlayer.getUnit(currID);

            // player may have transferred or scrapped the unit.
            if (currU == null) {
                continue;
            }

            currU.setScrappableFor(-1);

            if (shouldGetText) {
                server.campaign.CampaignMain.cm.toUser("PL|UU|" + currU.getId() + "|" + currU.toString(true),
                      playerName,
                      false);
                scrappablesRemaining = true;
            }
        }

        if (scrappablesRemaining && shouldGetText) {
            server.campaign.CampaignMain.cm.toUser("[!] Time to scrap salvaged has expired.", playerName, true);
        }

        // clean up.
        salvagedUnits.clear();
        isFinished = true;

    }// end run()

    public boolean isFinished() {
        return isFinished;
    }

}// end OpsChickenThread class
