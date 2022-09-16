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

package server.campaign.commands.admin;

import java.util.StringTokenizer;

import common.House;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class AdminLockCampaignCommand implements Command {

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

        if (Boolean.parseBoolean(CampaignMain.cm.getConfig("CampaignLock")) == true) {
            CampaignMain.cm.toUser("Campaign is already locked.", Username, true);
            return;
        }

        // deactivate all active players, and tell them why.
        for (House house : CampaignMain.cm.getData().getAllHouses()) {
            SHouse h = (SHouse) house;
            for (SPlayer p : h.getActivePlayers().values()) {
                p.setActive(false);
                CampaignMain.cm.toUser("AM:" + Username + " locked the campaign. You were deactivated.", p.getName(), true);
                CampaignMain.cm.sendPlayerStatusUpdate(p, !Boolean.parseBoolean(CampaignMain.cm.getConfig("HideActiveStatus")));
            }// end while (act members remain)

        }// end while(factions remain)

        // set the lock property, so no new players can activate
        CampaignMain.cm.getConfig().setProperty("CampaignLock", "true");

        // tell the admin he has locked the campaign
        CampaignMain.cm.doSendToAllOnlinePlayers("AM:" + Username + " locked the campaign!", true);
        CampaignMain.cm.toUser("AM:You locked the campaign. Players can no longer activate, and all active players were deactivated. Use 'adminunlockcampaign' to release the activity lock.", Username, true);
        CampaignMain.cm.doSendModMail("NOTE", Username + " locked the campaign.");

    }// end Process()

}