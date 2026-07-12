/*
 * MekWars - Copyright (C) 2005
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
import common.campaign.operations.Operation;
import mekwars.server.campaign.CampaignMain;
import server.campaign.util.ExclusionList;

/**
 * AttackCommand is used to initiate ShortOperations. Checks the validity of the attacking force, checks to ensure that
 * the defenders available to the attacking force can defend the target op type, and then creates the operation and
 * gives attackers their "Special" options. Syntax attackfromreserve#opname#armyid#planet#defender
 */
public class AttackFromReserveCommand implements Command {

    int accessLevel = 2;
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

        server.campaign.operations.newopmanager.I_OperationManager manager = CampaignMain.campaignMain.getOpsManager();
        server.campaign.SPlayer ap = CampaignMain.campaignMain.getPlayer(Username);
        if (ap == null) {
            CampaignMain.campaignMain.toUser(
                  "AM:Null player. Contact an administrator to report this, immediately!",
                  Username,
                  true);
            return;
        }

        if (!CampaignMain.campaignMain.getBooleanConfig("AllowAttackFromReserve")) {
            CampaignMain.campaignMain.toUser("AM:Sorry but attack from reserve is not allowed in this campaign!",
                  Username,
                  true);
            return;
        }

        // Fix for BUG 1491934: AFR possible when campaign locked
        if (Boolean.parseBoolean(CampaignMain.campaignMain.getConfig("CampaignLock")) == true) {
            CampaignMain.campaignMain.toUser(
                  "AM:The campaign is currently locked. Attacks are disabled until the campaign is unlocked.",
                  Username,
                  true);
            return;
        }

        // check time limits
        if (ap.getLastAttackFromReserve() +
                  (Long.parseLong(CampaignMain.campaignMain.getConfig("AttackFromReserveSleepTime")) * 60000) >
                  System.currentTimeMillis()) {
            CampaignMain.campaignMain.toUser("AM:Sorry but you may only attack from reserve once every " +
                                                   CampaignMain.campaignMain.getConfig(
                                                         "AttackFromReserveSleepTime") +
                                                   " mins.", Username, true);
            return;
        }

        // throw up if the player is not in reserve
        if (ap.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE) {
            CampaignMain.campaignMain.toUser(
                  "AM:You are currently active. You must deactivate in order to attack from reserve.)",
                  Username,
                  true);
            return;
        }

        // can't attack while in a game
        if (ap.getDutyStatus() == server.campaign.SPlayer.STATUS_FIGHTING) {
            CampaignMain.campaignMain.toUser("AM:You are already fighting!", Username, true);
            return;
        }

        // must leave/cancel any prior attacks to initiate new attack
        int altID = CampaignMain.campaignMain.getOpsManager().playerIsAnAttacker(ap);
        if (altID >= 0) {
            CampaignMain.campaignMain.toUser("AM:You're only allowed to attack once, and are already in Attack #" +
                                                   altID +
                                                   ".", Username, true);
            return;
        }

        // can't AFR while a listed defendant elsewhere
        altID = CampaignMain.campaignMain.getOpsManager().playerIsADefender(ap);
        if (altID >= 0) {
            CampaignMain.campaignMain.toUser("AM:You're already defending against Attack #" + altID + ".",
                  Username,
                  true);
            return;
        }

        // Check if the SOs have disabled AFR while in negative bays
        if ((CampaignMain.campaignMain.getIntegerConfig("MaxNegativeBaysForAFR") > -1) &&
                  ((ap.getFreeBays() + CampaignMain.campaignMain.getIntegerConfig("MaxNegativeBaysForAFR")) <
                         0)) {
            CampaignMain.campaignMain.toUser("AM:You cannot attack from reserve with more than " +
                                                   CampaignMain.campaignMain.getIntegerConfig(
                                                         "MaxNegativeBaysForAFR") +
                                                   " negative bays.  How about you share the wealth with your housemates.",
                  Username,
                  true);
            return;
        }

        // Check if the SOs have disabled activation while over unit limits
        if (CampaignMain.campaignMain.getBooleanConfig("DisableAFRIfOverHangarLimits") &&
                  ap.isOverAnyUnitLimits()) {
            CampaignMain.campaignMain.toUser(
                  "AM: You have exceeded one or more hangar limits.  Activation is disabled until you get under those limits.",
                  Username,
                  true);
            return;
        }

        // get the operation type
        String opName = command.nextToken();
        Operation o = manager.getOperation(opName);
        if (o == null) {
            CampaignMain.campaignMain.toUser("AM:Operation Type: " + opName + " does not exist.", Username, true);
            return;
        }

        // get the army being used to attack
        int armyID = -1;
        try {
            armyID = Integer.parseInt(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Non-number given for Army ID. Try again.", Username, true);
            return;
        }

        server.campaign.SArmy aa = ap.getArmy(armyID);
        boolean mulArmy = o.getBooleanValue("MULArmiesOnly");
        if (aa == null) {
            if (mulArmy) {
                aa = new server.campaign.SArmy(-1, Username);
            } else {
                CampaignMain.campaignMain.toUser("AM:You do not have an army with ID #" + armyID + ".",
                      Username,
                      true);
                return;
            }
        }
        if (aa.getBV() == 0 && !mulArmy) {
            CampaignMain.campaignMain.toUser("AM:Army #" +
                                                   armyID +
                                                   " has a BV of 0 and may not be used to attack.",
                  Username,
                  true);
            return;
        }

        if (aa.isDisabled()) {
            CampaignMain.campaignMain.toUser("AM:Army #" + armyID + " is disabled and may not be used to attack.",
                  Username,
                  true);
            return;
        }

        // return if any unpiloted units in attacking army.
        for (Unit currU : aa.getUnits()) {
            if (currU.hasVacantPilot()) {
                CampaignMain.campaignMain.toUser("AM:You may not attack using an army with pilotless units.",
                      Username,
                      true);
                return;
            }
        }

        // get the planet being attacked
        String planetName = command.nextToken();
        server.campaign.SPlanet target = CampaignMain.campaignMain.getPlanetFromPartialString(planetName,
              Username);
        if (target == null) {
            // getPlanetFromPartialString informs the user itself
            return;
        }

        // check to see if the attacker has enough Flu/RP/Money
        java.util.ArrayList<Integer> failureReasons = new java.util.ArrayList<Integer>();
        CampaignMain.campaignMain.getOpsManager()
              .getShortValidator()
              .checkAttackerRange(failureReasons, ap, o, target);
        CampaignMain.campaignMain.getOpsManager()
              .getShortValidator()
              .checkAttackerMilestones(failureReasons, ap, o);
        CampaignMain.campaignMain.getOpsManager().getShortValidator().checkAttackerCosts(failureReasons, ap, o);
        CampaignMain.campaignMain.getOpsManager()
              .getShortValidator()
              .checkAttackerConstruction(failureReasons, aa, o);

        if (failureReasons.size() > 0) {
            CampaignMain.campaignMain.toUser(CampaignMain.campaignMain.getOpsManager()
                                                   .getShortValidator()
                                                   .failuresToString(failureReasons), Username);
            return;
        }

        // Find the defending player and make sure they can defend.
        String toFind = command.nextToken();
        server.campaign.SPlayer dp = CampaignMain.campaignMain.getPlayer(toFind);

        if (dp == null) {
            CampaignMain.campaignMain.toUser("AM:Could not find a player named " + toFind + ". Try again?",
                  Username,
                  true);
            return;
        }

        if (ap.equals(dp)) {
            CampaignMain.campaignMain.toUser("AM:You cannot attack yourself. Nice try though.", Username, true);
            return;
        }

        // check for modnoplays
        if (ap.getExclusionList().checkExclude(dp.getName()) == ExclusionList.ADMIN_EXCLUDED ||
                  dp.getExclusionList().checkExclude(ap.getName()) == ExclusionList.ADMIN_EXCLUDED) {
            CampaignMain.campaignMain.toUser("AM:A moderator-added no play stops you from playing with " +
                                                   dp.getName() +
                                                   ".", Username, true);
            return;
        }

        // Check that the opponent is not on the same IP
        if (CampaignMain.campaignMain.getBooleanConfig("IPCheck")) {
            String apip = CampaignMain.campaignMain.getServer().getIP(ap.getName()).toString();
            String dpip = CampaignMain.campaignMain.getServer().getIP(dp.getName()).toString();
            if (apip.equalsIgnoreCase(dpip)) {
                CampaignMain.campaignMain.toUser("AM: You cannot attack a player on the same IP as you.",
                      Username,
                      true);
                return;
            }
        }
        // Make Sure the defenders faction owns part of the target
        if (target.getInfluence().getInfluence(dp.getHouseFightingFor().getId()) < 1) {
            CampaignMain.campaignMain.toUser(dp.getName() + " cannot defend " + target.getName(), Username, true);
            return;
        }

        // build list of all armies target player has that may defend
        java.util.ArrayList<server.campaign.SArmy> defendingArmies = new java.util.ArrayList<server.campaign.SArmy>();
        for (server.campaign.SArmy currArmy : dp.getArmies()) {
            java.util.ArrayList<Integer> defenderFails = manager.getShortValidator()
                                                               .validateShortDefender(dp, currArmy, o, target);
            if (defenderFails.size() == 0 && aa.matches(currArmy, o))// if army can defend, add
            {defendingArmies.add(currArmy);}
        }

        // if target player can't defend, return
        if (defendingArmies.size() == 0) {
            CampaignMain.campaignMain.toUser("AM:" +
                                                   dp.getName() +
                                                   " cannot defend your attack with his current force(s).",
                  Username,
                  true);
            return;
        }

        // set the exclusion time
        ap.setLastAttackFromReserve(System.currentTimeMillis());

        // send messages informing the involved players
        CampaignMain.campaignMain.toUser("AM:Your attack proposal was sent to " + dp.getName(), Username, true);
        StringBuilder toSend = new StringBuilder("AM:" +
                                                       ap.getName() +
                                                       " proposes you a game of " +
                                                       o.getName() +
                                                       " on planet " +
                                                       target.getNameAsColoredLink() +
                                                       " with " +
                                                       aa.getAmountOfUnits() +
                                                       " units totalling " +
                                                       aa.getBV() +
                                                       " BV. You may accept with:  <br>");

        // give clickables to potential defender
        for (server.campaign.SArmy currArmy : defendingArmies) {

            int aID = currArmy.getID();
            int aBV = currArmy.getOperationsBV(null);
            int aUnits = currArmy.getAmountOfUnits();

            toSend.append("<a href=\"MEKWARS/c acceptattackfromreserve#" +
                                ap.getName() +
                                "#" +
                                aa.getID() +
                                "#" +
                                aID +
                                "#" +
                                opName +
                                "#" +
                                target.getName() +
                                "\">Army #" +
                                aID +
                                " </a> (Units: " +
                                aUnits +
                                " / BV: " +
                                aBV +
                                ")<br>");
        }

        toSend.delete(toSend.lastIndexOf("<br>"), toSend.length());
        toSend.append("<br>Or <a href=\"MEKWARS/c declineattackfromreserve#" + ap.getName() + "\">decline</a>.");

        toSend.append("<br>You have " +
                            CampaignMain.campaignMain.getConfig("AttackFromReserveResponseTime") +
                            " mins to accept, or the attack will be automatically declined.");
        CampaignMain.campaignMain.toUser(toSend.toString(), dp.getName(), true);

    }// end process

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

}// end AttackFromReserveCommand
