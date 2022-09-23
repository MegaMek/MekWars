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

package server.campaign.commands.admin;

import java.util.HashMap;
import java.util.StringTokenizer;

import common.Influences;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.NewbieHouse;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class AdminCreateSolarisCommand implements Command {

    int accessLevel = IAuthenticator.ADMIN;
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

        // access level check
        int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
            return;
        }

        // Add the Newbie-SHouse
        SHouse solaris = new NewbieHouse(CampaignMain.cm.getData().getUnusedHouseID(), CampaignMain.cm.getConfig("NewbieHouseName"), "#33CCCC", 4, 5, "SOL");

        CampaignMain.cm.addHouse(solaris);
        HashMap<Integer, Integer> solFlu = new HashMap<Integer, Integer>();
        solFlu.put(CampaignMain.cm.getHouseFromPartialString(CampaignMain.cm.getConfig("NewbieHouseName"), null).getId(), 100);
        SPlanet newbieP = new SPlanet(0, "Solaris VII", new Influences(solFlu), 0, 0, -3, -2);
        CampaignMain.cm.addPlanet(newbieP);

        solaris.addPlanet(newbieP);
        CampaignMain.cm.toUser(CampaignMain.cm.getConfig("NewbieHouseName"), Username, true);
        CampaignMain.cm.doSendModMail("NOTE", Username + " has created " + CampaignMain.cm.getConfig("NewbieHouseName"));

    }
}