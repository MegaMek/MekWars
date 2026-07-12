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
import mekwars.server.campaign.CampaignMain;
import server.campaign.pilot.SPilot;

public class AdminHousePilotsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "faction";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }


        //vars
        server.campaign.SHouse h = null;

        try {
            h = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), Username);
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("Improper command. Try: /c adminhousepilots#faction",
                  Username,
                  true);
            return;
        }

        if (h == null) {
            CampaignMain.campaignMain.toUser("Couldn't find a faction with that name.", Username, true);
            return;
        }

        //input looks good. fetch queue from house
        server.campaign.PilotQueues currQ = h.getPilotQueues();
        StringBuilder toReturn = new StringBuilder();

        //loop through queue and put results in a StringBuilder
        for (int type = 0; type < Unit.MAXBUILD; type++) {
            toReturn.append("Faction Base Pilot: " +
                                  currQ.getBaseGunnery(type) +
                                  "/" +
                                  currQ.getBasePiloting(type) +
                                  "<br>");
            toReturn.append("Faction Base Pilot Skills: " + currQ.getBasePilotSkill(type) + "<br>");
            toReturn.append("<b>Queue for " + Unit.getTypeClassDesc(type) + ":</b><OL>");
            java.util.LinkedList<SPilot> l = currQ.getPilotQueue(type);
            for (SPilot currP : l) {
                toReturn.append("<LI>" +
                                      currP.getName() +
                                      "(" +
                                      currP.getGunnery() +
                                      "/" +
                                      currP.getPiloting() +
                                      ") [" +
                                      currP.getSkillString(true) +
                                      "]</LI>");
            }
            toReturn.append("</OL>");
        }

        h.updated();
        //send to caller and notify mod channel
        CampaignMain.campaignMain.toUser(toReturn.toString(), Username, true);
        //server.MWLogger.modLog(Username + " checked House " + h.getName() + " Pilots");
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " checked House " + h.getName() + " Pilots");


    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
