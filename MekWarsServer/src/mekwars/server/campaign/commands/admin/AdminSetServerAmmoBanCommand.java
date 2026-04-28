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


public class AdminSetServerAmmoBanCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Munition Number";

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

        String ammoName = "";
        try {
            ammoName = command.nextToken();
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("Invalid syntax. Try: adminsetserveradmmoban#munitionnumber",
                  Username,
                  true);
        }

        if (server.campaign.CampaignMain.cm.getServerBannedAmmo().get(ammoName) != null) {
            server.campaign.CampaignMain.cm.getServerBannedAmmo().remove(ammoName);
            server.campaign.CampaignMain.cm.getData()
                  .setServerBannedAmmo(server.campaign.CampaignMain.cm.getServerBannedAmmo());
            ammoName = server.campaign.CampaignMain.cm.getData().getMunitionsByNumber().get(Long.parseLong(ammoName));
            server.campaign.CampaignMain.cm.toUser("Server-wide ban on " + ammoName + " lifted.", Username, true);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username + " lifted the server-wide ban on " + ammoName + ".");
        } else {
            server.campaign.CampaignMain.cm.getServerBannedAmmo().put(ammoName, "banned");
            server.campaign.CampaignMain.cm.getData()
                  .setServerBannedAmmo(server.campaign.CampaignMain.cm.getServerBannedAmmo());
            ammoName = server.campaign.CampaignMain.cm.getData().getMunitionsByNumber().get(Long.parseLong(ammoName));
            server.campaign.CampaignMain.cm.toUser(ammoName + " banned server-wide.", Username, true);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " banned " + ammoName + " server-wide.");
        }

        server.campaign.CampaignMain.cm.saveBannedAmmo();
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
