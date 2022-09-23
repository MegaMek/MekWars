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
package server.campaign.commands;

import java.util.StringTokenizer;

import common.campaign.operations.Operation;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.operations.OperationManager;
import server.campaign.operations.newopmanager.I_OperationManager;

/**
 * AttackCommand is used to initiate ShortOperations. Checks the validity of the
 * attacking force, checks to ensure that the defenders available to the
 * attacking force can defend the target op type, and then creates the operation
 * and gives attackers their "Special" options.
 */
public class AttackCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

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

        I_OperationManager manager = CampaignMain.cm.getOpsManager();
        SPlayer ap = CampaignMain.cm.getPlayer(Username);
        if (ap == null) {
            CampaignMain.cm.toUser("AM:Null player. Contact an administrator to report this, immediately!", Username, true);
            return;
        }

        // throw up if the player is not active or fighting
        if (ap.getDutyStatus() < SPlayer.STATUS_ACTIVE) {
            CampaignMain.cm.toUser("AM:You aren't on the front lines! (You are currently in Reserve. Activate in order to attack.)", Username, true);
            return;
        }

        // can't attack while in a game
        if (ap.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
            CampaignMain.cm.toUser("AM:You are already fighting!", Username, true);
            return;
        }

        // can only attack once
        int altID = CampaignMain.cm.getOpsManager().playerIsAnAttacker(ap);
        if (altID >= 0) {
            CampaignMain.cm.toUser("AM:You're only allowed to attack once, and are already in Attack #" + altID + ".", Username, true);
            return;
        }

        // cant only defend once
        altID = CampaignMain.cm.getOpsManager().playerIsADefender(ap);
        if (altID >= 0) {
            CampaignMain.cm.toUser("AM:You're already defending against Attack #" + altID + ".", Username, true);
            return;
        }

        // narc if the player hasn't been active long enough to attack
        boolean minActiveMet = (System.currentTimeMillis() - ap.getActiveSince()) >= (Long.parseLong(CampaignMain.cm.getConfig("MinActiveTime")) * 1000);
        if (!minActiveMet) {
            CampaignMain.cm.toUser("AM:You're still on your way to the frontline. You cannot attack until you arrive.", Username, true);
            return;
        }

        // get the operation type
        String opName = "";
        try {
            opName = command.nextToken();
        } catch (Exception e) {
            CampaignMain.cm.toUser("AM:No operation name given. Try again.", Username, true);
            return;
        }

        Operation o = manager.getOperation(opName);
        if (o == null) {
            CampaignMain.cm.toUser("AM:Operation Type: " + opName + " does not exist.", Username, true);
            return;
        }

        // get the army being used to attack
        int armyID = -1;
        try {
            armyID = Integer.parseInt(command.nextToken());
        } catch (Exception e) {
            CampaignMain.cm.toUser("AM:Non-number given for Army ID. Try again.", Username, true);
            return;
        }

        SArmy aa = ap.getArmy(armyID);
        boolean MULArmy = o.getBooleanValue("MULArmiesOnly");
        if (aa == null) {
            if (MULArmy) {
                aa = new SArmy(-1, Username);
            } else {
                CampaignMain.cm.toUser("AM:An error occured while creating your Army (The Army was null. This usually means " + "the army doesn't exist. Example: you tried to use Army 1, but you only have Armies 0 and 2.)", Username, true);
                return;
            }
        } else if (aa.getBV() == 0 && !MULArmy) {
            CampaignMain.cm.toUser("AM:Army #" + armyID + " has a BV of 0 and may not be used to attack.", Username, true);
            return;
        } else if (aa.isDisabled()) {
            CampaignMain.cm.toUser("AM: Army #" + armyID + " is disabled and may not be used to attack.", Username, true);
            return;
        }

        // get the planet being attacked
        String planetName = "";
        try {
            planetName = command.nextToken();
        } catch (Exception e) {
            CampaignMain.cm.toUser("AM:No planet name given. Try again.", Username, true);
            return;
        }

        SPlanet target = CampaignMain.cm.getPlanetFromPartialString(planetName, Username);
        if (target == null) {
            // getPlanetFromPartialString informs the user itself
            return;
        }

        // save some resources and throw out any attack that
        // should have a long, but does not.
        int longID = -1;
        if (o.getTypeIndicator() == Operation.TYPE_SHORTANDLONG) {

            if (!manager.hasSpecificLongOnPlanet(ap.getHouseFightingFor(), target, o)) {
                CampaignMain.cm.toUser("AM:Your faction has no " + opName + " in progress on " + target.getName() + ".", Username, true);
                return;
            }

            // else, get the ID
            longID = manager.getLongID(ap.getHouseFightingFor(), target);
        }

        /*
         * Breaks passed. lets validate the attack =)
         * 
         * The validator and manager will handle everything from this point on,
         * assuming that no failure reasons are returned.
         */
        String s = manager.validateShortAttack(ap, aa, o, target, longID, false);
        if (s != null && !s.trim().equals("")) {
            CampaignMain.cm.toUser("AM:Attack failed " + s, Username, true);
            return;
        }
        // Let's set teams manually
        int teamNumber = -1;
        if (!o.getBooleanValue("TeamOperation")) {
        	teamNumber = 1;
        	CampaignMain.cm.toUser("PL|STN|" + teamNumber, Username, false);
            ap.setTeamNumber(teamNumber);
        }
        if (o.getBooleanValue("TeamOperation") && o.getIntValue("NumberOfTeams") > 1) {
            teamNumber = 1;

            // Ok we got the attack and its valid. lets check to see if its a
            // team Faction game if so add all active faciton Members
            if (o.getBooleanValue("TeamsMustBeSameFaction")) {
                SHouse attackingFaction = ap.getHouseFightingFor();
                StringBuilder toSend;
                teamNumber = 1;
                boolean sendCommand = false;
                for (String player : attackingFaction.getActivePlayers().keySet()) {
                    if (player.equalsIgnoreCase(ap.getName()))
                        continue;
                    toSend = new StringBuilder(ap.getName() + " is performing a " + o.getName() + " on " + target.getName() + " you may join with the following armies:");
                    sendCommand = false;
                    SPlayer joiningPlayer = attackingFaction.getActivePlayers().get(player);
                    for (SArmy army : joiningPlayer.getArmies()) {
                        String failures = manager.validateShortAttack(joiningPlayer, army, o, target, longID, true);
                        if (failures != null && s != null && !s.trim().equals(""))
                            continue;
                        sendCommand = true;
                        toSend.append("<a href=\"MEKWARS/c joinattack#" + ap.getName() + "#" + army.getID() + "\">Army #" + army.getID() + "</a> (Units: " + army.getUnits().size() + " / BV: " + army.getBV() + ")");

                    }
                    if (sendCommand)
                        CampaignMain.cm.toUser(toSend.toString(), player);
                }
            }

            CampaignMain.cm.toUser("PL|STN|" + teamNumber, Username, false);
            CampaignMain.cm.toUser("AM:You have been assigned to team #" + teamNumber, Username);
            ap.setTeamNumber(teamNumber);
        }
    }// end process

}// end AttackCommand
