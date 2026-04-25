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

package mekwars.server.campaign.commands.admin;

import common.Unit;

public class AdminPurgeHouseBaysCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#[ALL]unittype#[ALL]unitsize";

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

    public void process(java.util.StringTokenizer command, String Username) {

        // access level check
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        String faction = "";
        String strType = "";
        String strClass = "";
        int unitType = Unit.MEK;
        int unitClass = Unit.LIGHT;

        try {
            faction = command.nextToken();
            strType = command.nextToken();
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser(
                  "Invalid syntax. Try: AdminPurgeHouseBays#faction#[ALL]unittype#[ALL]unitsize",
                  Username,
                  true);
            return;
        }

        server.campaign.SHouse h = server.campaign.CampaignMain.cm.getHouseFromPartialString(faction, Username);

        if (h == null) {
            return;
        }

        try {
            if (strType.equalsIgnoreCase("ALL")) {
                for (java.util.Vector<java.util.Vector<server.campaign.SUnit>> hangers : h.getHangar().values()) {
                    for (int size = Unit.LIGHT; size <= Unit.ASSAULT; size++) {
                        hangers.elementAt(size).clear();
                    }
                }
            }// else select a unit type
            else {
                strClass = command.nextToken();
                unitType = Integer.parseInt(strType);
                java.util.Vector<java.util.Vector<server.campaign.SUnit>> hanger = h.getHangar(unitType);

                if (strClass.equalsIgnoreCase("ALL")) {
                    for (int size = Unit.LIGHT; size <= Unit.ASSAULT; size++) {
                        hanger.elementAt(size).clear();
                    }
                }// else one unit size
                else {
                    unitClass = Integer.parseInt(strClass);
                    hanger.elementAt(unitClass).clear();
                }
            }
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser(
                  "Invalid syntax. Try: AdminPurgeHouseBays#faction#[ALL]unittype#[ALL]unitsize",
                  Username,
                  true);
            return;
        }

        h.updated();
        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " has purged bays for " + h.getName());
    }
}// end AdminPurgeHouseBaysCommand
