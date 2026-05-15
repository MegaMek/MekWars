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

public class AdminSetServerAmmoBanCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Munition Number";

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

        String ammoName = "";
        try {
            ammoName = command.nextToken();
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("Invalid syntax. Try: adminsetserveradmmoban#munitionnumber",
                  Username,
                  true);
        }

        if (CampaignMain.campaignMain.getServerBannedAmmo().get(ammoName) != null) {
            CampaignMain.campaignMain.getServerBannedAmmo().remove(ammoName);
            CampaignMain.campaignMain.getData()
                  .setServerBannedAmmo(CampaignMain.campaignMain.getServerBannedAmmo());
            ammoName = CampaignMain.campaignMain.getData().getMunitionsByNumber().get(Long.parseLong(ammoName));
            CampaignMain.campaignMain.toUser("Server-wide ban on " + ammoName + " lifted.", Username, true);
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  Username + " lifted the server-wide ban on " + ammoName + ".");
        } else {
            CampaignMain.campaignMain.getServerBannedAmmo().put(ammoName, "banned");
            CampaignMain.campaignMain.getData()
                  .setServerBannedAmmo(CampaignMain.campaignMain.getServerBannedAmmo());
            ammoName = CampaignMain.campaignMain.getData().getMunitionsByNumber().get(Long.parseLong(ammoName));
            CampaignMain.campaignMain.toUser(ammoName + " banned server-wide.", Username, true);
            CampaignMain.campaignMain.doSendModMail("NOTE", Username + " banned " + ammoName + " server-wide.");
        }

        CampaignMain.campaignMain.saveBannedAmmo();
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
