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

package mekwars.server.campaign.commands;

import common.House;
import mekwars.server.campaign.CampaignMain;

public class LastOnlineCommand implements Command {

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

        String name = command.nextToken().toLowerCase();

        server.campaign.SmallPlayer smallp = null;
        java.util.Iterator<House> i = CampaignMain.campaignMain.getData().getAllHouses().iterator();
        boolean playerFound = false;
        while (i.hasNext() && !playerFound) {
            server.campaign.SHouse h = (server.campaign.SHouse) i.next();
            smallp = h.getSmallPlayers().get(name);
            if (smallp != null) {playerFound = true;}
        }//end while(more elements && haven't found target yet)

        if (smallp == null || smallp.getLastOnline() == 0) {
            CampaignMain.campaignMain.toUser("AM:Target player doesn't exist, or has not been"
                                                   + " online since the last server restart.", Username, true);
            return;
        }

        //smallp exists. send info.
        java.util.Date lastDate = new java.util.Date(smallp.getLastOnline());
        String result = smallp.getName() +
                              " (" +
                              smallp.getMyHouse().getColoredName() +
                              ") was last online: " +
                              lastDate +
                              ". ";
        result += "That's " + ((System.currentTimeMillis() - smallp.getLastOnline()) / 86400000) + " days";
        result += ", " + ((System.currentTimeMillis() - smallp.getLastOnline()) % 86400000) / 3600000 + " hours";
        CampaignMain.campaignMain.toUser(result, Username, true);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
