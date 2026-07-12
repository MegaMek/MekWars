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

public class ChangeHouseColorCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#htmlhexcolor";

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

        server.campaign.SHouse h = null;
        String newColor = "";
        server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);

        try {
            h = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken(), null);
            newColor = command.nextToken();
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper command. Try: /c changehousecolor#faction#htmlhexcolor",
                  Username,
                  true);
            return;
        }

        if (h == null) {
            CampaignMain.campaignMain.toUser("AM:Couldn't find a faction with that name.", Username, true);
            return;
        }

        if (userLevel < server.MWChatServer.auth.IAuthenticator.MODERATOR) {h = player.getMyHouse();}

        //make the change & update timestampe
        h.setHouseColor(newColor);
        h.updated();

        CampaignMain.campaignMain.toUser(h.getName() + " color changed!", Username, true);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
