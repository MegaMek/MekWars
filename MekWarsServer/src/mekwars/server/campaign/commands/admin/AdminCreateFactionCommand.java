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

/*
 * Created on 14.04.2004
 *
 */
package mekwars.server.campaign.commands.admin;


import mekwars.server.campaign.CampaignMain;

/**
 * @author Helge Richter
 */
public class AdminCreateFactionCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "name#color(hex)#basegunner#basePilot#Abbreviation";

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

        try {
            String name = command.nextToken();
            String color = command.nextToken();
            int baseGunner = Integer.parseInt(command.nextToken());
            int basePilot = Integer.parseInt(command.nextToken());
            String Abb = command.nextToken();

            server.campaign.SHouse newfaction = new server.campaign.SHouse(CampaignMain.campaignMain.getData()
                                                                                 .getUnusedHouseID(),
                  name,
                  "#" + color,
                  baseGunner,
                  basePilot,
                  Abb);
            newfaction.updated();

            CampaignMain.campaignMain.addHouse(newfaction);
            CampaignMain.campaignMain.doSendToAllOnlinePlayers("PL|ANH|" + newfaction.addNewHouse(), false);
            CampaignMain.campaignMain.toUser("Faction created!", Username, true);
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  Username + " has created faction " + newfaction.getName());
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser(
                  "Invalid Syntax: /AdminCreateFaction Name#Color(hex)#BaseGunner#BasePilot#Abberviation",
                  Username,
                  true);
            return;
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
