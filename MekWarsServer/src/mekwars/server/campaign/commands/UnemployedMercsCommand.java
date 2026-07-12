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


import mekwars.server.campaign.CampaignMain;

public class UnemployedMercsCommand implements Command {

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

        String s = "Unemployed Mercenaries: ";
        java.util.Vector<server.campaign.mercenaries.MercHouse> mh = CampaignMain.campaignMain.getMercHouses();
        for (int i = 0; i < mh.size(); i++) {
            server.campaign.mercenaries.MercHouse searchHouse = mh.get(i);
            java.util.Enumeration<server.campaign.SPlayer> e = searchHouse.getAllOnlinePlayers().elements();

            boolean foundMerc = false;
            while (e.hasMoreElements()) {
                server.campaign.SPlayer mp = e.nextElement();
                if (mp.getMyHouse().getHouseFightingFor(mp).isMercHouse()) {
                    if (!foundMerc) {
                        s += mp.getName();
                        foundMerc = true;
                    } else {s += ", " + mp.getName();}
                }
            }//end while
        }//end for(all merc factions)

        CampaignMain.campaignMain.toUser(s, Username, true);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
