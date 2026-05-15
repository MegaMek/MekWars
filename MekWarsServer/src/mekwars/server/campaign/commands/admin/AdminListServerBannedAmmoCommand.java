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

public class AdminListServerBannedAmmoCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
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

        if (CampaignMain.campaignMain.getServerBannedAmmo().size() <= 0) {
            CampaignMain.campaignMain.toUser("The server is not currently banning any ammo.", Username, true);
        } else {
            java.util.TreeSet<String> ammoBan = new java.util.TreeSet<String>(
                  CampaignMain.campaignMain.getServerBannedAmmo().keySet());
            java.util.Hashtable<Long, String> munitions = CampaignMain.campaignMain.getData()
                                                                .getMunitionsByNumber();
            for (String ammoName : ammoBan) {
                // MWLogger.errLog("Munition: "+ammoName);
                CampaignMain.campaignMain.toUser(munitions.get(Long.parseLong(ammoName)), Username, true);
            }
        }

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
