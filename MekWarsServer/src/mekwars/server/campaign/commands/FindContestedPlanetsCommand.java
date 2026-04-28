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

import common.Planet;
import server.campaign.util.PlanetNameComparator;

//import server.campaign.operations.ShortOperation;

//BarukKahzad 20151129 with much copy and paste from HouseCommand.java
public class FindContestedPlanetsCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        String sret = "";
        String Name1 = "";
        String Name2 = "";
        server.campaign.SHouse h1 = null;
        server.campaign.SHouse h2 = null;
        int PercentAmount = -1;

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

        try {
            sret = "AM:Could not find attacking faction.<br>...Try /FindCP#AttackingHouse#DefendingHouse#%Owned";
            Name1 = (String) command.nextToken();
            h1 = (server.campaign.SHouse) server.campaign.CampaignMain.cm.getData().getHouseByName(Name1);
            if (h1 == null || h1.getId() < 0) {
                server.campaign.CampaignMain.cm.toUser(sret, Username, true);
                return;
            }
            sret = "AM:Could not find defending faction.<br>...Try /FindCP#AttackingHouse#DefendingHouse#%Owned";
            Name2 = (String) command.nextToken();
            h2 = (server.campaign.SHouse) server.campaign.CampaignMain.cm.getData().getHouseByName(Name2);
            if (h2 == null || h2.getId() < 0) {
                server.campaign.CampaignMain.cm.toUser(sret, Username, true);
                return;
            }
            sret = "AM:Could not resolve %Owned to a postive integer.<br>...Try /FindCP#AttackingHouse#DefendingHouse#%Owned";
            PercentAmount = Integer.parseInt(command.nextToken());
            if (PercentAmount <= 0) {
                server.campaign.CampaignMain.cm.toUser(sret, Username, true);
                return;
            }

            //cleared breaks. start assembling a status return.
            String s = "<br><b><u>Contested Planet list with minimum " +
                             PercentAmount +
                             "% owned for " +
                             h1.getColoredName() +
                             " attacking " +
                             h2.getColoredName() +
                             ":</u></b><br>";

            //sort out planets owned/fighting on, etc.
            java.util.Vector<server.campaign.SPlanet> contestedWorlds = new java.util.Vector<server.campaign.SPlanet>(1,
                  1);

            java.util.Iterator<Planet> it = server.campaign.CampaignMain.cm.getData().getAllPlanets().iterator();
            while (it.hasNext()) {
                server.campaign.SPlanet p = (server.campaign.SPlanet) it.next();
                if (p.getInfluence().getInfluence(h1.getId()) >= 0.01 * PercentAmount * p.getConquestPoints() &&
                          p.getInfluence().getInfluence(h2.getId()) > 0) {
                    //update lists
                    contestedWorlds.add(p);
                }
            }
            //sort planets by alpha, instead of ID
            java.util.Collections.sort(contestedWorlds, new PlanetNameComparator());
            java.util.Iterator<server.campaign.SPlanet> i = contestedWorlds.iterator();
            if (!i.hasNext()) {s += " none<br>";}

            while (i.hasNext()) {
                server.campaign.SPlanet currPlanet = (server.campaign.SPlanet) i.next();
                s += currPlanet.getNameAsColoredLink();
                if (currPlanet.getFactoryCount() > 0) {s += "*";}
                s += "(";
                //show Max cp
                s += currPlanet.getConquestPoints() + ", ";
                //show Attacker and Defender % owned
                s += h1.getColoredAbbreviation(false) +
                           " " +
                           currPlanet.getInfluence().getInfluence(h1.getId()) +
                           "cp, " +
                           h2.getColoredAbbreviation(false) +
                           " " +
                           currPlanet.getInfluence().getInfluence(h2.getId()) +
                           "cp)";
                s += "<br>";
            }
            server.campaign.CampaignMain.cm.toUser("SM|" + s, Username, false);
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser(sret, Username, true);
            return;
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
