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

package mekwars.server.campaign.commands.mod;

import common.House;
import mekwars.server.campaign.CampaignMain;


public class ListMultiPlayerGroupsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "";

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

        //WARNING: CODE EFFECIENCY VERY BAD. COULD CAUSE HIGH SERVER LOAD IF USED OFTEN
        String toSend = "AM:List of Multiplayergroups:";

        /*
         * INCREDIBLY EVIL!
         */
        java.util.Hashtable<String, server.campaign.SPlayer> allPlayers = new java.util.Hashtable<String, server.campaign.SPlayer>();
        for (House vh : CampaignMain.campaignMain.getData().getAllHouses()) {
            server.campaign.SHouse h = (server.campaign.SHouse) vh;
            allPlayers.putAll(h.getAllOnlinePlayers());
        }
        /*
         * End PHENOMENAL EVIL.
         */

        java.util.Hashtable<Integer, java.util.Vector<server.campaign.SPlayer>> result = new java.util.Hashtable<Integer, java.util.Vector<server.campaign.SPlayer>>();
        java.util.Enumeration<server.campaign.SPlayer> e = allPlayers.elements();
        while (e.hasMoreElements()) {
            //Check all players for equal Groupentries..
            server.campaign.SPlayer p = e.nextElement();
            if (p.getGroupAllowance() != 0) {
                java.util.Vector<server.campaign.SPlayer> v;
                if (result.get(p.getGroupAllowance()) == null) {
                    v = new java.util.Vector<server.campaign.SPlayer>(1, 1);
                } else {v = result.get(p.getGroupAllowance());}
                v.add(p);
                result.put(p.getGroupAllowance(), v);
            }
        }

        java.util.Enumeration<Integer> groups = result.keys();
        while (groups.hasMoreElements()) {
            Integer GroupID = groups.nextElement();
            java.util.Vector<server.campaign.SPlayer> members = result.get(GroupID);
            toSend += "<br>Group #" + GroupID + ":";
            for (int i = 0; i < members.size(); i++) {
                server.campaign.SPlayer p = (server.campaign.SPlayer) members.elementAt(i);
                toSend += p.getName() + " + ";
            }
            toSend = toSend.substring(0, toSend.lastIndexOf("+") - 1);

        }
        CampaignMain.campaignMain.toUser(toSend, Username, true);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
