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
import common.util.MWLogger;

/**
 * DefendCommand is analagous to the Task system's "join" command - it allows a player to register himself as the
 * defender for an attack.
 */
public class AcceptAttackFromReserveCommand implements Command {

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

        //Issuer of command will be defending player
        server.campaign.SPlayer dp = server.campaign.CampaignMain.cm.getPlayer(Username);
        if (dp == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Null player in AcceptAttackFromReserve. Report this immediately!",
                  Username,
                  true);
            return;
        }

        //Get the attacker's name, the op id, army id, and so on
        server.campaign.operations.newopmanager.I_OperationManager manager = server.campaign.CampaignMain.cm.getOpsManager();
        int armyID = -1;
        int attackingArmyID = -1;
        server.campaign.SPlayer ap = null;
        String opName = "";
        server.campaign.SPlanet target = null;

        try {
            ap = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());
            attackingArmyID = Integer.parseInt(command.nextToken());
            armyID = Integer.parseInt(command.nextToken());
            opName = command.nextToken();
            target = server.campaign.CampaignMain.cm.getPlanetFromPartialString(command.nextToken(), null);
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Improper format. Should be /c acceptattackfromreserve#attacker name#attack army#your army#opname#world",
                  Username,
                  true);
            return;
        }

        //Load the actual armies
        server.campaign.SArmy aa = null;
        server.campaign.SArmy da = null;

        aa = ap.getArmy(attackingArmyID);
        if (aa == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Defend failed. Attacker does not have an army with ID #" +
                                                         armyID +
                                                         ".",
                  Username,
                  true);
            return;
        }

        da = dp.getArmy(armyID);
        if (da == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Defend failed. Army #" + armyID + " does not exist.",
                  Username,
                  true);
            return;
        }

        if (da.isDisabled()) {
            server.campaign.CampaignMain.cm.toUser("AM:Defend failed. Army #" +
                                                         armyID +
                                                         " is disabled and cannot be used to defend.",
                  Username,
                  true);
            return;
        }

        if ((server.campaign.CampaignMain.cm.getIntegerConfig("MaxNegativeBaysForAFR") > -1) &&
                  ((dp.getFreeBays() + server.campaign.CampaignMain.cm.getIntegerConfig("MaxNegativeBaysForAFR")) <
                         0)) {
            server.campaign.CampaignMain.cm.toUser("AM:Defend failed. " + dp.getName() + " has too many negative bays.",
                  ap.getName(),
                  true);
            server.campaign.CampaignMain.cm.toUser("AM:Defend failed.  You have too many negative bays.",
                  Username,
                  true);
            return;
        }

        //Ensure offer is still valid
        Long launchTime = ap.getLastAttackFromReserve();
        if (launchTime +
                  (Long.parseLong(server.campaign.CampaignMain.cm.getConfig("AttackFromReserveResponseTime")) * 60000) <
                  System.currentTimeMillis()) {
            server.campaign.CampaignMain.cm.toUser("AM:Sorry - this offer has expired.", Username, true);
            return;
        }

        //Don't let players defend multiple games
        if (dp.getDutyStatus() == server.campaign.SPlayer.STATUS_FIGHTING) {
            server.campaign.CampaignMain.cm.toUser("AM:You are already fighting!", Username, true);
            return;
        }

        // Check if the SOs have disabled AFR while over unit limits
        if (server.campaign.CampaignMain.cm.getBooleanConfig("DisableAFRIfOverHangarLimits") &&
                  dp.isOverAnyUnitLimits()) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM: Defend failed. You have exceeded one or more hangar limits.  AFR is disabled until you get under those limits.",
                  Username,
                  true);
            return;
        }

        /*
         * Check the defending army to make sure it is valid. If not, return. We validate
         * the army every time, even though AttackFromReserve already looked for a valid
         * defender, because players in reserve can change their armies and could create
         * an illegal army _after_ the request issues.
         */
        Operation o = manager.getOperation(opName);
        java.util.ArrayList<server.campaign.SArmy> fullMatches = new java.util.ArrayList<server.campaign.SArmy>();
        java.util.ArrayList<Integer> defensiveFailures = manager.getShortValidator()
                                                               .validateShortDefender(dp, da, o, target);
        if (defensiveFailures.size() == 0) {
            fullMatches.add(da);
        } else {
            server.campaign.CampaignMain.cm.toUser("AM:Army #" +
                                                         da.getID() +
                                                         "could not defend " +
                                                         manager.getShortValidator()
                                                               .failuresToString(defensiveFailures),
                  Username,
                  true);
            return;
        }

        /*
         * Check for units with Vacant pilots. This is important because the
         * ShortValidator checks legality assuming that someone has activated
         * - and Vacants are checked at activation.
         */
        for (Unit currU : da.getUnits()) {
            if (currU.hasVacantPilot()) {
                server.campaign.CampaignMain.cm.toUser("AM:You may not defend using an army with pilotless units.",
                      Username,
                      true);
                return;
            }
        }

        /*
         * Create a new short op and add it to the manager. As it's new, we don't need to check
         * the status (running, waiting, etc) before attaching players to the game.
         */
        int opID = manager.getFreeShortID();
        server.campaign.operations.ShortOperation so = new server.campaign.operations.ShortOperation(o.getName(),
              target,
              ap,
              aa,
              fullMatches,
              opID,
              -1,
              true);
        manager.addShortOperation(so, ap, o);

        //make sure they are now active and removed from Reserve queue.
        ap.setActive(true);
        dp.setActive(true);

        /*
         * At this point, we can assume that we have a valid army
         *
         * - Add the defender to the ShortOperation.
         * - Remove the defender from any other ShortOperations
         *   he is involved in. This stops any running chicken
         *   threads and may cancel cancel other attacks.
         */
        server.campaign.CampaignMain.cm.getOpsManager().removePlayerFromAllAttackerLists(dp, so, true);
        server.campaign.CampaignMain.cm.getOpsManager().removePlayerFromAllDefenderLists(dp, so, true);
        so.addDefender(dp, da, "");//add defender

        /*
         * We can assume that the player has enough money/etc to defend
         * the game, or he would not have passed the checks. This is a
         * VERY VERY dangerous step. It is possible for people to have
         * enough when they go active and then drop under (ie - buy a
         * unit). This is a massive hole, as the returns when an op is
         * terminated are flat. This means a player can defend, transfer
         * and then cancel to get huge stacks of cash ...
         *
         * This is why defender costs for non-modifying operations are
         * so strongly discouraged in the DefaultOperation comments.
         */
        //Operation o = CampaignMain.cm.getOpsManager().getOperation(so.getName());
        int money = o.getIntValue("DefenderCostMoney");
        int flu = o.getIntValue("DefenderCostInfluence");
        int rp = o.getIntValue("DefenderCostReward");

        String toSend = "AM:You are now defending Attack #" + opID;

        boolean hasCost = false;

        if (money > 0) {
            dp.addMoney(-money);
            toSend += "(" + server.campaign.CampaignMain.cm.moneyOrFluMessage(true, true, money);
            hasCost = true;
        }
        if (flu > 0) {
            dp.addInfluence(-flu);
            if (hasCost) {toSend += ", ";} else {toSend += "(";}
            toSend += server.campaign.CampaignMain.cm.moneyOrFluMessage(false, true, flu);
            hasCost = true;
        }
        if (rp > 0) {
            dp.addReward(-rp);
            if (hasCost) {toSend += ", ";} else {toSend += "(";}
            toSend += "-" + rp + " " + server.campaign.CampaignMain.cm.getConfig("RPShortName");
            hasCost = true;
        }

        if (hasCost) {toSend += ").";} else {toSend += ".";}

        //tell the defender that he has succesfully joined the attack.
        MWLogger.gameLog("AcceptAttackFromReserve: " +
                               so.getShortID() +
                               "/" +
                               dp.getName() +
                               " w. Army #" +
                               da.getID());
        server.campaign.CampaignMain.cm.toUser(toSend, Username, true);

    }//end process

}//end DefendCommand
