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


public class AdminListHouseBannedAmmoCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name";

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

        String faction = null;
        if (command.hasMoreTokens()) {faction = command.nextToken();} else {
            server.campaign.CampaignMain.cm.toUser("Unkown House. Syntax: /c AdminListHouseBannedAmmo#HouseName",
                  Username,
                  true);
            return;
        }

        server.campaign.SHouse h = server.campaign.CampaignMain.cm.getHouseFromPartialString(faction, Username);

        if (h == null || h.getBannedAmmo().size() <= 0) {
            server.campaign.CampaignMain.cm.toUser("That faction is not currently banning any ammo.", Username, true);
        } else {
            server.campaign.CampaignMain.cm.toUser("Banned ammo for Faction " + h.getName(), Username, true);
            java.util.Enumeration<String> ammoBan = h.getBannedAmmo().keys();
            java.util.Hashtable<Long, String> munitions = server.campaign.CampaignMain.cm.getData()
                                                                .getMunitionsByNumber();
            while (ammoBan.hasMoreElements()) {
                String ammoName = ammoBan.nextElement();
                server.campaign.CampaignMain.cm.toUser(munitions.get(Long.parseLong(ammoName)), Username, true);
            }
        }

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
