/*
 * MekWars - Copyright (C) 2007
 *
 * Original author - Torren (torren@users.sourceforge.net)
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
 * Created on 02.25.2007
 *
 */
package mekwars.server.campaign.commands;

import common.util.MWLogger;
import common.util.TokenReader;
import mekwars.server.campaign.CampaignMain;

/**
 * @author Torren (Jason Tighe) Send a factions config to the player. Optional Faction Name variable for Staff to pull
 *       different factions configs.
 */
public class GetFactionConfigsCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        try {
            if (accessLevel != 0) {
                int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
                if (userLevel < getExecutionLevel()) {
                    CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                           userLevel +
                                                           ". Required: " +
                                                           accessLevel +
                                                           ".", Username, true);
                    CampaignMain.campaignMain.toUser("PL|FC|DONE#DONE", Username, false);
                    return;
                }
            }

            server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
            String factionName = player.getMyHouse().getName();
            server.campaign.SHouse faction = null;
            long timeStamp = -1;

            try {
                timeStamp = TokenReader.readLong(command);
            } catch (Exception ex) {
                CampaignMain.campaignMain.toUser("PL|FC|DONE#DONE", Username, false);
                return;
            }

            if (command.hasMoreElements()) {
                factionName = TokenReader.readString(command);
            }

            faction = CampaignMain.campaignMain.getHouseFromPartialString(factionName);

            if (faction == null || faction.getConfig() == null) {
                CampaignMain.campaignMain.toUser("PL|FC|DONE#DONE", Username, false);
                return;
            }

            /*
             * No reason to update. you're up to date. Calling this command from
             * AdminMenu causes it to fully update each time.
             */
            if (timeStamp >= faction.getLongConfig("TIMESTAMP")) {
                CampaignMain.campaignMain.toUser("PL|FC|DONE#DONE", Username, false);
                return;
            }

            StringBuffer result = new StringBuffer("PL|FC|");
            String delimiter = "#";

            java.util.Enumeration<Object> keys = faction.getConfig().keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String value = faction.getConfig(key);
                result.append(key);
                result.append(delimiter);
                result.append(value);
                result.append(delimiter);
            }// End While

            result.append("DONE#DONE");
            CampaignMain.campaignMain.toUser(result.toString(), Username, false);
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("PL|FC|DONE#DONE", Username, false);
            MWLogger.errLog(ex);
        }
    }

    public int getExecutionLevel() {
        return 0;
    }

    public void setExecutionLevel(int i) {
    }

    public String getSyntax() {
        return syntax;
    }
}// end GetFactionConfigsCommand
