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


import mekwars.server.campaign.CampaignMain;

public class AdminListHouseBannedAmmoCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name";

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

        String faction = null;
        if (command.hasMoreTokens()) {faction = command.nextToken();} else {
            CampaignMain.campaignMain.toUser("Unkown House. Syntax: /c AdminListHouseBannedAmmo#HouseName",
                  Username,
                  true);
            return;
        }

        server.campaign.SHouse h = CampaignMain.campaignMain.getHouseFromPartialString(faction, Username);

        if (h == null || h.getBannedAmmo().size() <= 0) {
            CampaignMain.campaignMain.toUser("That faction is not currently banning any ammo.", Username, true);
        } else {
            CampaignMain.campaignMain.toUser("Banned ammo for Faction " + h.getName(), Username, true);
            java.util.Enumeration<String> ammoBan = h.getBannedAmmo().keys();
            java.util.Hashtable<Long, String> munitions = CampaignMain.campaignMain.getData()
                                                                .getMunitionsByNumber();
            while (ammoBan.hasMoreElements()) {
                String ammoName = ammoBan.nextElement();
                CampaignMain.campaignMain.toUser(munitions.get(Long.parseLong(ammoName)), Username, true);
            }
        }

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
