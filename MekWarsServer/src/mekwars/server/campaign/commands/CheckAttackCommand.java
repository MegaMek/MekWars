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

import common.campaign.operations.Operation;

public class CheckAttackCommand implements Command {


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

        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);

        //break if player isnt active
        boolean canProbeFromReserve = server.campaign.CampaignMain.cm.getBooleanConfig("ProbeInReserve");
        if (!canProbeFromReserve && p.getDutyStatus() < server.campaign.SPlayer.STATUS_ACTIVE) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You are not on the frontline. You can't probe enemy forces from reserve!",
                  Username,
                  true);
            return;
        }

        //don't allow fighting players to /c ca spot for their comrades
        if (p.getDutyStatus() == server.campaign.SPlayer.STATUS_FIGHTING) {
            server.campaign.CampaignMain.cm.toUser("AM:You should focus on playing your game!", Username, true);
            return;
        }

        //if not fighting, check to make sure minactivetime is met
        boolean minActiveMet = System.currentTimeMillis() - p.getActiveSince() >=
                                     Long.parseLong(server.campaign.CampaignMain.cm.getConfig("MinActiveTime")) * 1000;
        if (!canProbeFromReserve && !minActiveMet) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You're still on your way to the frontline. Contact an intelligence officer once you arrive at your post.",
                  Username,
                  true);
            return;
        }

        //don't allow uncontracted mercs to /c ca spot for their friends
        if (p.getMyHouse().isMercHouse() && p.getHouseFightingFor() == p.getMyHouse()) {
            server.campaign.CampaignMain.cm.toUser("AM:You are not under contract!", Username, true);
            return;
        }

        //All the tosses are passed, so check to see if the return should use normal or Operations BVs.
        boolean usingOpRules = server.campaign.CampaignMain.cm.getBooleanConfig("UseOperationsRule");

        String Desc = "<br>";//output

        //if command has more elements, spit out checkattack for specific armies. no real reason for
        //a user to do this through the CLI, but it is called by right clicking armies in HQ
        if (command.hasMoreElements()) {
            Desc = "<br><table><tr><td>Army ";

            int armyID = -1;
            try {
                armyID = Integer.parseInt(command.nextToken());
            } catch (Exception e) {
                server.campaign.CampaignMain.cm.toUser(
                      "AM:Improper format. Try: /c checkattack or /c checkattack#armyid",
                      Username,
                      true);
                return;
            }

            server.campaign.SArmy arm = p.getArmy(armyID);
            if (arm == null) {
                server.campaign.CampaignMain.cm.toUser("AM:Army #" + armyID + " doesn't exist.", Username, true);
                return;
            }

            if (arm.isDisabled()) {
                server.campaign.CampaignMain.cm.toUser("AM: Army #" + armyID + " is disabled.", Username, true);
                return;
            }

            Desc += arm.getID() + " (" + arm.getBV() + " BV) ";
            Desc += " may attack: </td><td>&nbsp;</td><td>&nbsp;</td></tr>";

            java.util.Enumeration<server.campaign.SArmy> targets = arm.getOpponents().elements();
            while (targets.hasMoreElements()) {
                server.campaign.SArmy currTarget = targets.nextElement();
                server.campaign.SPlayer currTargetP = server.campaign.CampaignMain.cm.getPlayer(currTarget.getPlayerName());
                String coloredHouseName = currTargetP.getMyHouse().getHouseFightingFor(currTargetP).getColoredName();
                String defendableOps = listDefendableOperations(arm, currTargetP, currTarget, p.getHouseFightingFor());

                if (defendableOps.equals("[]")) {continue;}

                Desc += "<tr><td>&nbsp;</td><td>";

                //adjust return for infantry settings
                if (server.campaign.CampaignMain.cm.getBooleanConfig("ShowInfInCheckAttack")) {
                    Desc += coloredHouseName + "(" + currTarget.getAmountOfUnits() + ")";
                } else {Desc += coloredHouseName + "(" + currTarget.getAmountOfUnitsWithoutInfantry() + ")";}

                if (usingOpRules && arm.getAmountOfUnits() > currTarget.getAmountOfUnits()) {
                    Desc += "(BV Against: " + arm.getOperationsBV(currTarget) + ")";
                }

                Desc += "</td><td>" + defendableOps;

                Desc += "</td></tr>";
            }//end while(more targets)
            Desc += "</table>";
        }//end (if for a specific army)

        //otherwise, loop out *all* the armies
        else {
            Desc = "Intelligence reports the following attack options:<br>";
            java.util.Enumeration<server.campaign.SArmy> e = p.getArmies().elements();
            while (e.hasMoreElements()) {
                server.campaign.SArmy arm = e.nextElement();
                Desc += "<table><tr><td>";
                if (arm != null && !arm.isDisabled()) {
                    Desc += "Army " + arm.getID();
                    if (usingOpRules) {Desc += " (" + arm.getBV() + " BV)";}
                    Desc += ": </td>";

                    java.util.Enumeration<server.campaign.SArmy> targets = arm.getOpponents().elements();
                    while (targets.hasMoreElements()) {
                        server.campaign.SArmy currTarget = targets.nextElement();
                        server.campaign.SPlayer currTargetP = server.campaign.CampaignMain.cm.getPlayer(currTarget.getPlayerName());
                        if (currTargetP == null) {continue;}
                        String defendableOps = listDefendableOperations(arm,
                              currTargetP,
                              currTarget,
                              p.getHouseFightingFor());
                        if (defendableOps.equals("[]")) {continue;}
                        String coloredHouseName = currTargetP.getMyHouse()
                                                        .getHouseFightingFor(currTargetP)
                                                        .getColoredName();
                        Desc += "<td>";
                        //adjust return for infantry settings
                        if (server.campaign.CampaignMain.cm.getBooleanConfig("ShowInfInCheckAttack")) {
                            Desc += coloredHouseName + "(" + currTarget.getAmountOfUnits() + ")";
                        } else {Desc += coloredHouseName + "(" + currTarget.getAmountOfUnitsWithoutInfantry() + ")";}

                        if (usingOpRules && arm.getAmountOfUnits() > currTarget.getAmountOfUnits()) {
                            Desc += "(BV Against: " + arm.getOperationsBV(currTarget) + ")";
                        }

                        Desc += "</td><td>" + defendableOps;

                        Desc += "</td></tr>";
                        if (targets.hasMoreElements()) {Desc += "<tr><td>&nbsp;</td>";}
                    }//end while(more targets)
                    Desc += "</table>";
                }
            }
        }

        server.campaign.CampaignMain.cm.toUser(Desc + "<br>", Username, true);

    }

    private String listDefendableOperations(server.campaign.SArmy aa, server.campaign.SPlayer dp,
          server.campaign.SArmy da, server.campaign.SHouse ah) {
        StringBuffer report = new StringBuffer(" [");
        server.campaign.operations.newopmanager.I_OperationManager manager = server.campaign.CampaignMain.cm.getOpsManager();
        for (String attack : aa.getLegalOperations().keySet()) {
            Operation o = manager.getOperation(attack);
            // Don't show AFR-only attacks
            if (o.getBooleanValue("OnlyAllowedFromReserve")) {
                continue;
            }

            if (!aa.matches(da, o)) {continue;}

            if (dp.getHouseFightingFor().equals(ah) && !o.getBooleanValue("AllowInFaction")) {continue;}

            if (manager.validateShortDefense(dp, da, o, null) == null) {
                report.append(attack);
                report.append(", ");
            }

        }
        report.trimToSize();
        if (report.length() <= 2) {return "[]";}
        report.delete(report.length() - 2, report.length());
        report.append("]");
        return report.toString();
    }
}//end CheckAttackCommand
