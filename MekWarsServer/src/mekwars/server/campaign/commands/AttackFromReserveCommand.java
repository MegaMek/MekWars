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

        server.campaign.operations.newopmanager.I_OperationManager manager = server.campaign.CampaignMain.cm.getOpsManager();
        server.campaign.SPlayer ap = server.campaign.CampaignMain.cm.getPlayer(Username);
        if (ap == null) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Null player. Contact an administrator to report this, immediately!",
                  Username,
                  true);
            return;
        }

        if (!server.campaign.CampaignMain.cm.getBooleanConfig("AllowAttackFromReserve")) {
            server.campaign.CampaignMain.cm.toUser("AM:Sorry but attack from reserve is not allowed in this campaign!",
                  Username,
                  true);
            return;
        }

        // Fix for BUG 1491934: AFR possible when campaign locked
        if (Boolean.parseBoolean(server.campaign.CampaignMain.cm.getConfig("CampaignLock")) == true) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:The campaign is currently locked. Attacks are disabled until the campaign is unlocked.",
                  Username,
                  true);
            return;
        }

        // check time limits
        if (ap.getLastAttackFromReserve() +
                  (Long.parseLong(server.campaign.CampaignMain.cm.getConfig("AttackFromReserveSleepTime")) * 60000) >
                  System.currentTimeMillis()) {
            server.campaign.CampaignMain.cm.toUser("AM:Sorry but you may only attack from reserve once every " +
                                                         server.campaign.CampaignMain.cm.getConfig(
                                                               "AttackFromReserveSleepTime") +
                                                         " mins.", Username, true);
            return;
        }

        // throw up if the player is not in reserve
        if (ap.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You are currently active. You must deactivate in order to attack from reserve.)",
                  Username,
                  true);
            return;
        }

        // can't attack while in a game
        if (ap.getDutyStatus() == server.campaign.SPlayer.STATUS_FIGHTING) {
            server.campaign.CampaignMain.cm.toUser("AM:You are already fighting!", Username, true);
            return;
        }

        // must leave/cancel any prior attacks to initiate new attack
        int altID = server.campaign.CampaignMain.cm.getOpsManager().playerIsAnAttacker(ap);
        if (altID >= 0) {
            server.campaign.CampaignMain.cm.toUser("AM:You're only allowed to attack once, and are already in Attack #" +
                                                         altID +
                                                         ".", Username, true);
            return;
        }

        // can't AFR while a listed defendant elsewhere
        altID = server.campaign.CampaignMain.cm.getOpsManager().playerIsADefender(ap);
        if (altID >= 0) {
            server.campaign.CampaignMain.cm.toUser("AM:You're already defending against Attack #" + altID + ".",
                  Username,
                  true);
            return;
        }

        // Check if the SOs have disabled AFR while in negative bays
        if ((server.campaign.CampaignMain.cm.getIntegerConfig("MaxNegativeBaysForAFR") > -1) &&
                  ((ap.getFreeBays() + server.campaign.CampaignMain.cm.getIntegerConfig("MaxNegativeBaysForAFR")) <
                         0)) {
            server.campaign.CampaignMain.cm.toUser("AM:You cannot attack from reserve with more than " +
                                                         server.campaign.CampaignMain.cm.getIntegerConfig(
                                                               "MaxNegativeBaysForAFR") +
                                                         " negative bays.  How about you share the wealth with your housemates.",
                  Username,
                  true);
            return;
        }

        // Check if the SOs have disabled activation while over unit limits
        if (server.campaign.CampaignMain.cm.getBooleanConfig("DisableAFRIfOverHangarLimits") &&
                  ap.isOverAnyUnitLimits()) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM: You have exceeded one or more hangar limits.  Activation is disabled until you get under those limits.",
                  Username,
                  true);
            return;
        }

        // get the operation type
        String opName = command.nextToken();
        Operation o = manager.getOperation(opName);
        if (o == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Operation Type: " + opName + " does not exist.", Username, true);
            return;
        }

        // get the army being used to attack
        int armyID = -1;
        try {
            armyID = Integer.parseInt(command.nextToken());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("AM:Non-number given for Army ID. Try again.", Username, true);
            return;
        }

        server.campaign.SArmy aa = ap.getArmy(armyID);
        boolean mulArmy = o.getBooleanValue("MULArmiesOnly");
        if (aa == null) {
            if (mulArmy) {
                aa = new server.campaign.SArmy(-1, Username);
            } else {
                server.campaign.CampaignMain.cm.toUser("AM:You do not have an army with ID #" + armyID + ".",
                      Username,
                      true);
                return;
            }
        }
        if (aa.getBV() == 0 && !mulArmy) {
            server.campaign.CampaignMain.cm.toUser("AM:Army #" +
                                                         armyID +
                                                         " has a BV of 0 and may not be used to attack.",
                  Username,
                  true);
            return;
        }

        if (aa.isDisabled()) {
            server.campaign.CampaignMain.cm.toUser("AM:Army #" + armyID + " is disabled and may not be used to attack.",
                  Username,
                  true);
            return;
        }

        // return if any unpiloted units in attacking army.
        for (Unit currU : aa.getUnits()) {
            if (currU.hasVacantPilot()) {
                server.campaign.CampaignMain.cm.toUser("AM:You may not attack using an army with pilotless units.",
                      Username,
                      true);
                return;
            }
        }

        // get the planet being attacked
        String planetName = command.nextToken();
        server.campaign.SPlanet target = server.campaign.CampaignMain.cm.getPlanetFromPartialString(planetName,
              Username);
        if (target == null) {
            // getPlanetFromPartialString informs the user itself
            return;
        }

        // check to see if the attacker has enough Flu/RP/Money
        java.util.ArrayList<Integer> failureReasons = new java.util.ArrayList<Integer>();
        server.campaign.CampaignMain.cm.getOpsManager()
              .getShortValidator()
              .checkAttackerRange(failureReasons, ap, o, target);
        server.campaign.CampaignMain.cm.getOpsManager()
              .getShortValidator()
              .checkAttackerMilestones(failureReasons, ap, o);
        server.campaign.CampaignMain.cm.getOpsManager().getShortValidator().checkAttackerCosts(failureReasons, ap, o);
        server.campaign.CampaignMain.cm.getOpsManager()
              .getShortValidator()
              .checkAttackerConstruction(failureReasons, aa, o);

        if (failureReasons.size() > 0) {
            server.campaign.CampaignMain.cm.toUser(server.campaign.CampaignMain.cm.getOpsManager()
                                                         .getShortValidator()
                                                         .failuresToString(failureReasons), Username);
            return;
        }

        // Find the defending player and make sure they can defend.
        String toFind = command.nextToken();
        server.campaign.SPlayer dp = server.campaign.CampaignMain.cm.getPlayer(toFind);

        if (dp == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Could not find a player named " + toFind + ". Try again?",
                  Username,
                  true);
            return;
        }

        if (ap.equals(dp)) {
            server.campaign.CampaignMain.cm.toUser("AM:You cannot attack yourself. Nice try though.", Username, true);
            return;
        }

        // check for modnoplays
        if (ap.getExclusionList().checkExclude(dp.getName()) == ExclusionList.ADMIN_EXCLUDED ||
                  dp.getExclusionList().checkExclude(ap.getName()) == ExclusionList.ADMIN_EXCLUDED) {
            server.campaign.CampaignMain.cm.toUser("AM:A moderator-added no play stops you from playing with " +
                                                         dp.getName() +
                                                         ".", Username, true);
            return;
        }

        // Check that the opponent is not on the same IP
        if (server.campaign.CampaignMain.cm.getBooleanConfig("IPCheck")) {
            String apip = server.campaign.CampaignMain.cm.getServer().getIP(ap.getName()).toString();
            String dpip = server.campaign.CampaignMain.cm.getServer().getIP(dp.getName()).toString();
            if (apip.equalsIgnoreCase(dpip)) {
                server.campaign.CampaignMain.cm.toUser("AM: You cannot attack a player on the same IP as you.",
                      Username,
                      true);
                return;
            }
        }
        // Make Sure the defenders faction owns part of the target
        if (target.getInfluence().getInfluence(dp.getHouseFightingFor().getId()) < 1) {
            server.campaign.CampaignMain.cm.toUser(dp.getName() + " cannot defend " + target.getName(), Username, true);
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
            server.campaign.CampaignMain.cm.toUser("AM:" +
                                                         dp.getName() +
                                                         " cannot defend your attack with his current force(s).",
                  Username,
                  true);
            return;
        }

        // set the exclusion time
        ap.setLastAttackFromReserve(System.currentTimeMillis());

        // send messages informing the involved players
        server.campaign.CampaignMain.cm.toUser("AM:Your attack proposal was sent to " + dp.getName(), Username, true);
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
                            server.campaign.CampaignMain.cm.getConfig("AttackFromReserveResponseTime") +
                            " mins to accept, or the attack will be automatically declined.");
        server.campaign.CampaignMain.cm.toUser(toSend.toString(), dp.getName(), true);

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
