/*
 * MekWars - Copyright (C) 2004
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

import common.House;
import mekwars.server.campaign.CampaignMain;

public class AdminLockCampaignCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

    public String getSyntax() {
        return syntax;
    }

    public void process(java.util.StringTokenizer command, String Username) {

        // access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        if (Boolean.parseBoolean(CampaignMain.campaignMain.getConfig("CampaignLock")) == true) {
            CampaignMain.campaignMain.toUser("Campaign is already locked.", Username, true);
            return;
        }

        // deactivate all active players, and tell them why.
        for (House house : CampaignMain.campaignMain.getData().getAllHouses()) {
            server.campaign.SHouse h = (server.campaign.SHouse) house;
            for (server.campaign.SPlayer p : h.getActivePlayers().values()) {
                p.setActive(false);
                CampaignMain.campaignMain.toUser("AM:" + Username + " locked the campaign. You were deactivated.",
                      p.getName(),
                      true);
                CampaignMain.campaignMain.sendPlayerStatusUpdate(p, !Boolean.parseBoolean(
                      CampaignMain.campaignMain.getConfig("HideActiveStatus")));
            }// end while (act members remain)

        }// end while(factions remain)

        // set the lock property, so no new players can activate
        CampaignMain.campaignMain.getConfig().setProperty("CampaignLock", "true");

        // tell the admin he has locked the campaign
        CampaignMain.campaignMain.doSendToAllOnlinePlayers("AM:" + Username + " locked the campaign!", true);
        CampaignMain.campaignMain.toUser(
              "AM:You locked the campaign. Players can no longer activate, and all active players were deactivated. Use 'adminunlockcampaign' to release the activity lock.",
              Username,
              true);
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " locked the campaign.");

    }// end Process()

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

}
