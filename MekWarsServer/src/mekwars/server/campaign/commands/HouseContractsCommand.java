/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Nathan Morris (urgru@verizon.net)
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

public class HouseContractsCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

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

        StringBuilder toSend = new StringBuilder();
        int count = 0;

        server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);

        //if a merc, show everyone's employers
        if (player.getMyHouse().isMercHouse()) {

            //store the merchouse
            server.campaign.SHouse ourH = player.getMyHouse();

            //employed players
            toSend.append("Employed: <br>");
            for (server.campaign.SPlayer currP : ourH.getAllOnlinePlayers().values()) {
                if (!ourH.getHouseFightingFor(currP).equals(ourH)) {
                    toSend.append(currP.getName() + ": " + ourH.getHouseFightingFor(currP).getColoredName() + "<br>");
                    count++;
                }
            }

            //if none employed, say so.
            if (count == 0) {toSend.append("- NONE<br><br>");} else {toSend.append("<br>");}

            //unemployed players
            count = 0;//reset count
            toSend.append("Unemployed: <br>");
            for (server.campaign.SPlayer currP : ourH.getAllOnlinePlayers().values()) {
                if (ourH.getHouseFightingFor(currP).equals(ourH)) {
                    toSend.append(currP.getName() + ": " + ourH.getHouseFightingFor(currP).getColoredName() + "<br>");
                    count++;
                }
            }

            //if none unemployed, say so.
            if (count == 0) {toSend.append("- NONE<br>");}

            //send the string
            server.campaign.CampaignMain.cm.toUser(toSend.toString(), Username, true);
            return;

        }//end if(merc)

        //not a merc, therefor must be is a normal faction member
        toSend.append("Mercenaries employed by your faction: <br>");

        //get merc factions, loop through
        java.util.Vector<server.campaign.mercenaries.MercHouse> mh = server.campaign.CampaignMain.cm.getMercHouses();
        for (server.campaign.mercenaries.MercHouse searchHouse : mh) {
            for (server.campaign.SPlayer currP : searchHouse.getAllOnlinePlayers().values()) {
                if (searchHouse.getHouseFightingFor(currP).equals(player.getMyHouse())) {
                    toSend.append(" - " + currP.getName() + "<br>");
                    count++;
                }
            }
        }

        //if none unemployed, say so.
        if (count == 0) {toSend.append("- NONE<br>");}

        server.campaign.CampaignMain.cm.toUser(toSend.toString(), Username, true);

    }//end process()
}
