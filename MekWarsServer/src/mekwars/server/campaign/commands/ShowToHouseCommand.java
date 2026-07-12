/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - nmorris (urgru@users.sourceforge.net)
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

package mekwars.server.campaign.commands;


import mekwars.server.campaign.CampaignMain;

/*
 * A command which is used to pase lane or army information
 * into a factionchat stream. Convenience command used by links
 * in MyStatusCommand and in portions of the client GUI.
 */
public class ShowToHouseCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        //get the player
        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);

        if (p == null) {
            CampaignMain.campaignMain.toUser("AM:Null player. Report this to an admin. Show fails.",
                  Username,
                  true);
            return;
        }

        String showType = "";
        try {
            showType = command.nextToken();
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper usage. Try: /c showtofaction#type#id", Username, true);
            return;
        }

        //get ID to remove
        int id = -1;
        try {
            id = Integer.parseInt(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper usage. Try: /c showtofaction#type#id", Username, true);
            return;
        }

        //determine type and generate a return
        try {
            if (showType.toLowerCase().startsWith("a")) {
                CampaignMain.campaignMain.doSendHouseMail(p.getMyHouse(),
                      Username,
                      "My army: " + p.getArmy(id).getDescription(true, false, false));
            }
            if (showType.toLowerCase().startsWith("u")) {
                CampaignMain.campaignMain.doSendHouseMail(p.getMyHouse(),
                      Username,
                      "My unit: " + p.getUnit(id).getDescription(false));
            }
            return;
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Error while attempting to Show. Incorrect ID?", Username, true);
            return;
        }

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
