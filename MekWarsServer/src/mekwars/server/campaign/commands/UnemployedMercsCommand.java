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


public class UnemployedMercsCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }

        String s = "Unemployed Mercenaries: ";
        java.util.Vector<server.campaign.mercenaries.MercHouse> mh = server.campaign.CampaignMain.cm.getMercHouses();
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

        server.campaign.CampaignMain.cm.toUser(s, Username, true);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
