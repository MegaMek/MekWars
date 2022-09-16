/*
 * MekWars - Copyright (C) 2008 
 *
 * Original author - jtighe (torren@users.sourceforge.net)
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

package server.campaign.commands.mod;

import java.io.File;
import java.util.StringTokenizer;

import megamek.common.MechSummaryCache;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

/**
 * Remove a part from a player.
 */
public class UpdateServerUnitsCacheCommand implements Command {

    int accessLevel = IAuthenticator.MODERATOR;
    String syntax = "Player Name";

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

        if (accessLevel != 0) {
            int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
                return;
            }
        }

        /*
         * clear any cache'd unit files. these will be rebuilt later in the
         * start process. clearing @ each start ensures that updates take hold
         * properly.
         */
        File cache = new File("./data/mechfiles/units.cache");
        if (cache.exists())
            cache.delete();

        MechSummaryCache.getInstance();
        
        CampaignMain.cm.doSendModMail("NOTE", Username + " has updated the servers unit cache.");

    }
}