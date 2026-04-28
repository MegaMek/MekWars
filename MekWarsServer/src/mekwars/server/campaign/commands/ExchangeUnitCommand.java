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

public class ExchangeUnitCommand implements Command {

    int accessLevel = 0;
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

        //get the player
        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Null player. Report this immediately!", Username, true);
            return;
        }

        //allowing people to reorder in game could break reports
        if (server.campaign.CampaignMain.cm.getOpsManager().getShortOpForPlayer(p) != null) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You may not change an army's composition while you are in a game.",
                  Username,
                  true);
            return;
        }


        //Do parsing. Struct is like: /c EXM#1,1#12
        if (command.hasMoreElements()) {
            String parse = (String) command.nextElement();
            java.util.StringTokenizer S = new java.util.StringTokenizer(parse, ",");
            int position = -1;
            int armyid = Integer.parseInt((String) S.nextElement());
            if (!S.hasMoreElements()) {return;}
            int mechid = Integer.parseInt((String) S.nextElement());
            int changeid = -1;
            if (command.hasMoreElements()) {changeid = Integer.parseInt((String) command.nextElement());}

            server.campaign.SArmy a = p.getArmy(armyid);
            if (a == null) {
                server.campaign.CampaignMain.cm.toUser("AM:You do not have an  Army #" + armyid, Username, true);
                return;
            }

            //Is the Lance in a fight atm?
            if (a.isLocked()) {
                server.campaign.CampaignMain.cm.toUser("AM:This Army is currently in a game.", Username, true);
                return;
            }

            if (a.isPlayerLocked()) {
                server.campaign.CampaignMain.cm.toUser("AM:You cannot modify a locked army.", Username, true);
                return;
            }

            server.campaign.SUnit oldMech = (server.campaign.SUnit) a.getUnit(mechid);
            server.campaign.SUnit changeMech = p.getUnit(changeid);

            if (changeMech != null && changeMech.getModelName().startsWith("Error")) {
                server.campaign.CampaignMain.cm.toUser("AM:Error units may not be added to armies.", Username, true);
                return;
            }

            if (changeMech != null && a.isUnitInArmy(changeMech)) {
                server.campaign.CampaignMain.cm.toUser("AM:That unit already exits in Army #" + armyid + ".",
                      Username,
                      true);
                return;
            }

            if (p.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE) {
                server.campaign.CampaignMain.cm.toUser("AM:You may not change your armies while on active duty.",
                      Username,
                      true);
                return;
            }

            if (changeMech != null && changeMech.getStatus() == Unit.STATUS_UNMAINTAINED) {
                server.campaign.CampaignMain.cm.toUser("AM:You may not assign unmaintained units to combat formations!",
                      Username,
                      true);
                return;
            }

            if (changeMech != null) {
                int maxAmount = Integer.valueOf(server.campaign.CampaignMain.cm.getConfig("UnitsInMultipleArmiesAmount"))
                                      .intValue();
                if (p.getAmountOfTimesUnitExistsInArmies(changeMech.getId()) >= maxAmount) {
                    if (maxAmount == 1) {
                        server.campaign.CampaignMain.cm.toUser("AM:A unit may only be in one army at a time.",
                              Username,
                              true);
                    } else {
                        server.campaign.CampaignMain.cm.toUser("AM:A unit may be in a maximum of " +
                                                                     maxAmount +
                                                                     " armies.", Username, true);
                    }
                    return;
                }

                if (changeMech.getStatus() == Unit.STATUS_FORSALE) {
                    server.campaign.CampaignMain.cm.toUser(
                          "AM:This unit is being sold on the Market. It may not be added to an army.",
                          Username,
                          true);
                    return;
                }

                int oldID;
                if (oldMech != null) {
                    oldID = oldMech.getId();
                    position = a.getUnitPosition(oldID);
                    a.removeUnit(oldID);
                    server.campaign.CampaignMain.cm.toUser("PL|RAU|" + a.getID() + "#" + oldID + "#" + a.getBV(),
                          Username,
                          false);
                    server.campaign.CampaignMain.cm.toUser("PL|UU|" + oldMech.getId() + "|" + oldMech.toString(true),
                          Username,
                          false);
                    a.checkLegalRatio(Username);
                } else {oldID = mechid;}
                //Exchange the old and the new mech
                //changeMech.setID(oldID);
                if (position > -1) {
                    a.addUnit(changeMech, position);
                    server.campaign.CampaignMain.cm.toUser("PL|AAU|" +
                                                                 a.getID() +
                                                                 "#" +
                                                                 changeMech.getId() +
                                                                 "#" +
                                                                 a.getBV() +
                                                                 "#" +
                                                                 position, Username, false);
                } else {
                    a.addUnit(changeMech);
                    server.campaign.CampaignMain.cm.toUser("PL|AAU|" +
                                                                 a.getID() +
                                                                 "#" +
                                                                 changeMech.getId() +
                                                                 "#" +
                                                                 a.getBV(), Username, false);
                }

                p.resetWeightedArmyNumber();//change made. clear the cached weightedArmyNumber.
                a.checkLegalRatio(Username);
            } else if (oldMech != null) {//changemech is known to be null from previous if statement
                a.removeUnit(oldMech.getId());
                server.campaign.CampaignMain.cm.toUser("PL|RAU|" + a.getID() + "#" + oldMech.getId() + "#" + a.getBV(),
                      Username,
                      false);
                a.checkLegalRatio(Username);
                server.campaign.CampaignMain.cm.toUser("PL|UU|" + oldMech.getId() + "|" + oldMech.toString(true),
                      Username,
                      false);
            }

            //tell the player that his army was changed and inform him of any legal ops changes
            server.campaign.CampaignMain.cm.toUser("AM:Army #" + a.getID() + " was changed. New BV: " + a.getBV(),
                  Username,
                  true);
            server.campaign.CampaignMain.cm.getOpsManager().checkOperations(a, true);

        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
