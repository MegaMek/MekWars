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

public class AdminSetHouseAmmoBanCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#Munition Number";

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

        String faction = "";
        String ammoName = "";
        try {
            faction = command.nextToken();
            ammoName = command.nextToken();
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("Invalid syntax. Try: adminsethouseammoban#faction#munitionnumber",
                  Username,
                  true);
        }

        server.campaign.SHouse h = CampaignMain.campaignMain.getHouseFromPartialString(faction, Username);

        if (h == null) {return;}

        //Hashtable munitions = CampaignMain.cm.getData().getMunitionsByNumber();

        if (h.getBannedAmmo().get(ammoName) != null) {
            h.getBannedAmmo().remove(ammoName);
            ammoName = CampaignMain.campaignMain.getData().getMunitionsByNumber().get(Long.parseLong(ammoName));
            CampaignMain.campaignMain.toUser("Ban on " + ammoName + " lifted for " + h.getName() + ".",
                  Username,
                  true);
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  Username + " lifted the ban on " + ammoName + " for " + h.getName() + ".");
        } else {
            h.getBannedAmmo().put(ammoName, "banned");
            ammoName = CampaignMain.campaignMain.getData().getMunitionsByNumber().get(Long.parseLong(ammoName));
            CampaignMain.campaignMain.toUser("Banned " + ammoName + " for " + h.getName() + ".", Username, true);
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  Username + " banned " + ammoName + " for " + h.getName() + ".");
        }

        h.updated();
        CampaignMain.campaignMain.saveBannedAmmo();
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
