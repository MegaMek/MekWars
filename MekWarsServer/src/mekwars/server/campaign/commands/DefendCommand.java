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

import common.campaign.operations.Operation;
import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;

/**
 * DefendCommand is analagous to the Task system's "join" command - it allows a player to register himself as the
 * defender for an attack.
 */
public class DefendCommand implements Command {

    int accessLevel = 0;
    String syntax = "attack number#army number#team number";

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

        // get the Op ID and the Army ID
        int opID = -1;
        int armyID = -1;
        int teamNumber = -1;
        boolean isAttacker = false;

        try {
            opID = Integer.parseInt(command.nextToken());
            armyID = Integer.parseInt(command.nextToken());
            teamNumber = Integer.parseInt(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper format. Try: /c defend#" + syntax, Username, true);
            return;
        }

        // get the player
        server.campaign.SPlayer dp = CampaignMain.campaignMain.getPlayer(Username);
        if (dp == null) {
            CampaignMain.campaignMain.toUser("AM:Null player. Report this immediately!", Username, true);
            return;
        }

        // check the attack
        server.campaign.operations.ShortOperation so = CampaignMain.campaignMain.getOpsManager()
                                                             .getRunningOps()
                                                             .get(opID);

        if (so == null) {
            CampaignMain.campaignMain.toUser("AM:Defend failed. Attack #" + opID + " does not exist.",
                  Username,
                  true);
            return;
        }

        Operation o = CampaignMain.campaignMain.getOpsManager().getOperation(so.getName());

        // check the army
        server.campaign.SArmy da = dp.getArmy(armyID);
        if (da == null) {
            if (o.getBooleanValue("MULArmiesOnly")) {
                da = new server.campaign.SArmy(-1, Username);
            } else {
                CampaignMain.campaignMain.toUser("AM:Defend failed. Army #" + armyID + " does not exist.",
                      Username,
                      true);
                return;
            }
        }

        // don't let players defend multiple games
        if (dp.getDutyStatus() == server.campaign.SPlayer.STATUS_FIGHTING) {
            CampaignMain.campaignMain.toUser("AM:You are already fighting!", Username, true);
            return;
        }

        // Don't defend with a disabled army
        if (da != null && da.isDisabled()) {
            CampaignMain.campaignMain.toUser("AM: Defend failed.  Army #" +
                                                   armyID +
                                                   " is disabled and cannot be used to defend.", Username, true);
            return;
        }

        // check the player's activity
        if (dp.getDutyStatus() != server.campaign.SPlayer.STATUS_ACTIVE) {
            CampaignMain.campaignMain.toUser("AM:Defend failed. You must be active to defend against an attack.",
                  Username,
                  true);
            return;
        }

        // make sure the operation is accepting defenders
        if (so.getStatus() != server.campaign.operations.ShortOperation.STATUS_WAITING) {

            if (so.getStatus() == server.campaign.operations.ShortOperation.STATUS_FINISHED) {
                CampaignMain.campaignMain.toUser("AM:Defend failed. Attack #" + opID + " is finished.",
                      Username,
                      true);
                return;
            }

            // else, neither waiting nor finished. assume running.
            CampaignMain.campaignMain.toUser("AM:Defend failed. Attack #" + opID + " is already defended.",
                  Username,
                  true);
            return;

        }

        /*
         * Make sure the defender can actually defend this attack. If the player
         * is in this chickenTree, with this army, we can assume that he/it have
         * already passed validation. If not, he's probably trying to defend
         * illegally. Run him through the validator and give him failure
         * reasons. If he doesn't fail he probably activated after the attack,
         * and we should let him defend ...
         */
        server.campaign.operations.OpsChickenThread pThread = so.getChickenThreads().get(dp.getName().toLowerCase());

        if (pThread == null || !pThread.getArmies().contains(da)) {

            // check to see if the attacking army is on the da's oplist
            server.campaign.SPlayer attacker = CampaignMain.campaignMain.getPlayer(so.getAttackers().firstKey());
            server.campaign.SArmy aa = attacker.getArmy(so.getAttackers().get(so.getAttackers().firstKey()));

            boolean isAnOpponent = false;
            for (server.campaign.SArmy currA : da.getOpponents()) {
                if (currA.equals(aa)) {
                    isAnOpponent = true;
                    break;
                }
            }

            if (!isAnOpponent) {
                CampaignMain.campaignMain.toUser("AM:Defend failed. Army #" +
                                                       da.getID() +
                                                       " is not an opponent " +
                                                       "for the army in Attack #" +
                                                       so.getShortID() +
                                                       ". BV's do not match.", Username, true);
                return;
            }

            // is an opponent (BV wise), so check for an op match
            String s = CampaignMain.campaignMain.getOpsManager().validateShortDefense(dp, da, o, null);
            if (s != null && !s.trim().equals("")) {
                CampaignMain.campaignMain.toUser("AM:Defend failed " + s, Username, true);
                return;
            }
        }
        // Let's try setting teams manually
        if (!o.getBooleanValue("TeamOperation")) {
            teamNumber = 2;
            CampaignMain.campaignMain.toUser("PL|STN|" + teamNumber, Username, false);
            dp.setTeamNumber(teamNumber);
        }
        if (o.getBooleanValue("TeamOperation")) {

            if (teamNumber < 1 || teamNumber > 8) {
                CampaignMain.campaignMain.toUser("Invalid Team Number! Try again!", Username);
                CampaignMain.campaignMain.toUser(so.getChickenThreads()
                                                       .get(Username.toLowerCase())
                                                       .generateAttackDialogCall(), Username, false);
                return;
            }
            int bv = 0;
            String message = "";
            if (o.getBooleanValue("TeamsMustBeSameFaction")) {
                teamNumber = so.getFactionTeam(dp.getHouseFightingFor().getName());
                bv = da.getBV();
                message = so.checkTeam(teamNumber, bv, false);
                if (message.trim().length() > 0) {
                    CampaignMain.campaignMain.toUser(message, Username);
                    CampaignMain.campaignMain.toUser(so.getChickenThreads()
                                                           .get(Username.toLowerCase())
                                                           .generateAttackDialogCall(), Username, false);
                    return;
                }

            } else if (o.getBooleanValue("RandomTeamDetermination")) {
                int numberOfTeams = Math.max(2, Math.min(8, o.getIntValue("NumberOfTeams")));

                teamNumber = numberOfTeams + 1;
                for (int team = 1; team <= numberOfTeams; team++) {
                    if (so.checkTeam(team).trim().length() < 1) {
                        teamNumber = team;
                        break;
                    }
                }

                if (teamNumber > numberOfTeams) {
                    CampaignMain.campaignMain.toUser("Sorry but a team could not be found for you.", Username);
                    return;
                }
            }

            dp.setTeamNumber(teamNumber);
            CampaignMain.campaignMain.toUser("PL|STN|" + teamNumber, Username, false);
            CampaignMain.campaignMain.toUser("AM:You've been assigned to team #" + teamNumber + ".", Username);

        }

        // If you join the attackers team then you will be added to the
        // attackers array.

        if (teamNumber > 0 && so.getAttackersTeam() == teamNumber) {
            so.addAttacker(dp, da, "");
            /*
             * At this point, we can assume that we have a valid army - Add the
             * player to the ShortOperation as an attacker.
             * This stops any running chicken threads and may cancel other attacks.
             */
            CampaignMain.campaignMain.getOpsManager().removePlayerFromAllDefenderLists(dp, so, true);
            isAttacker = true;
        } else {
            so.addDefender(dp, da, "");// add defender
            /*
             * At this point, we can assume that we have a valid army - Add the
             * defender to the ShortOperation. - Remove the defender from any other
             * ShortOperations he is involved in. This stops any running chicken
             * threads and may cancel other attacks.
             */
            CampaignMain.campaignMain.getOpsManager().removePlayerFromAllAttackerLists(dp, so, true);
            CampaignMain.campaignMain.getOpsManager().removePlayerFromAllDefenderLists(dp, so, true);
        }

        /*
         * We can assume that the player has enough money/etc to defend the
         * game, or he would not have passed the checks. This is a VERY VERY
         * dangerous step. It is possible for people to have enough when they go
         * active and then drop under (ie - buy a unit). This is a massive hole,
         * as the returns when an op is terminated are flat. This means a player
         * can defend, transfer and then cancel to get huge stacks of cash ...
         * This is why defender costs for non-modifying operations are so
         * strongly discouraged in the DefaultOperation comments.
         */
        int money = o.getIntValue("DefenderCostMoney");
        int flu = o.getIntValue("DefenderCostInfluence");
        int rp = o.getIntValue("DefenderCostReward");

        String toSend = "AM:You are now ";

        if (isAttacker) {
            toSend += "joining ";
        } else {
            toSend += "defending ";
        }

        toSend += "Attack #" + opID;
        boolean hasCost = false;

        if (money > 0) {
            dp.addMoney(-money);
            toSend += "(" + CampaignMain.campaignMain.moneyOrFluMessage(true, true, money);
            hasCost = true;
        }
        if (flu > 0) {
            dp.addInfluence(-flu);
            if (hasCost) {toSend += ", ";} else {toSend += "(";}
            toSend += CampaignMain.campaignMain.moneyOrFluMessage(false, true, flu);
            hasCost = true;
        }
        if (rp > 0) {
            dp.addReward(-rp);
            if (hasCost) {toSend += ", ";} else {toSend += "(";}
            toSend += "-" + rp + " " + CampaignMain.campaignMain.getConfig("RPShortName");
            hasCost = true;
        }

        if (hasCost) {toSend += ").";} else {toSend += ".";}

        // tell the defender that he has successfully joined the attack.
        MWLogger.gameLog("Defend: " + so.getShortID() + "/" + dp.getName() + " w. Army #" + da.getID());
        CampaignMain.campaignMain.toUser(toSend, Username, true);

        if (o.getBooleanValue("FreeForAllOperation")) {
            toSend = "AM:" + dp.getName() + " has joined the operation, as ";
            if (isAttacker) {
                toSend += "an attacker.";
            } else {
                toSend += "a defender.";
            }

            toSend += " <a href=\"MEKWARS/c commenceoperation#" + opID + "#CONFIRM\">Click here to commence</a>";
            CampaignMain.campaignMain.toUser(toSend, so.getInitiator().getName(), true);
        }

        // Defender had an outstanding attack and that attack needs to be
        // terminated.
        int altID = CampaignMain.campaignMain.getOpsManager().playerIsAnAttacker(dp);
        if (altID > 0) {
            server.campaign.operations.ShortOperation attackingOp = CampaignMain.campaignMain.getOpsManager()
                                                                          .getRunningOps()
                                                                          .get(altID);
            CampaignMain.campaignMain.getOpsManager()
                  .terminateOperation(attackingOp, server.campaign.operations.OperationManager.TERM_NOATTACKERS, null);
        }

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

}// end DefendCommand
