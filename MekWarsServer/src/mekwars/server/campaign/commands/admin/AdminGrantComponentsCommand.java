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

// AdminGrantComponents#Faction#Type#WeightClass#Components
public class AdminGrantComponentsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "faction#type#weight#numcomponents";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        //vars
        server.campaign.SHouse h = null;
        String typestring = "";
        String weightstring = "";
        int comps = -1;
        int unitType = Unit.MEK;
        int unitWeight = Unit.LIGHT;

        try {
            h = (server.campaign.SHouse) server.campaign.CampaignMain.cm.getData().getHouseByName(command.nextToken());
            typestring = command.nextToken();
            weightstring = command.nextToken();
            comps = Integer.parseInt(command.nextToken());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser(
                  "Improper command. Try: /c admingrancomponents#faction#type#weight#numcomponents",
                  Username,
                  true);
            return;
        }

        if (h == null) {
            server.campaign.CampaignMain.cm.toUser("Couldn't find a faction with that name.", Username, true);
            return;
        }

        try {
            unitType = Integer.parseInt(typestring);
        } catch (Exception ex) {
            unitType = Unit.getTypeIDForName(typestring);
        }

        try {
            unitWeight = Integer.parseInt(weightstring);
        } catch (Exception ex) {
            unitWeight = Unit.getWeightIDForName(weightstring.toUpperCase());
        }

        h.addPP(unitWeight, unitType, comps, true);
        server.campaign.CampaignMain.cm.toUser("You granted " + comps + " Comps to " + h.getName(), Username, true);
        //server.MWLogger.modLog(Username + " granted " + comps+ " Comps to " + h.getName());
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " granted " + comps + " Comps to " + h.getName());

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
