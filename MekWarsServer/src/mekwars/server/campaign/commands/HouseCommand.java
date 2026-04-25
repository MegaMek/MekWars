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
import megamek.common.TechConstants;
import server.campaign.util.PlanetNameComparator;

public class HouseCommand implements Command {

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

        if (!command.hasMoreElements()) {
            server.campaign.CampaignMain.cm.toUser("AM:Improper format. Try: /c faction#name", Username, false);
            return;
        }

        String Name = (String) command.nextElement();
        server.campaign.SHouse h = (server.campaign.SHouse) server.campaign.CampaignMain.cm.getData()
                                                                  .getHouseByName(Name);
        if (h == null || h.getId() < 0) {
            server.campaign.CampaignMain.cm.toUser("AM:Could not find faction. Command fails.", Username, false);
            return;
        }

        //cleared breaks. start assembling a status return.
        String s = "<br><b><u>Status for: " + h.getColoredName() + "</u></b><br>";

        //add up number of various player numbers
        int totalOnline = h.getAllOnlinePlayers().size();
        int sinceLastRestart = h.getSmallPlayers().size();

        s += "Players: " + totalOnline + " online, " + sinceLastRestart + " logins since last restart.<br>";

        //sort out planets owned/fighting on, etc.
        //use this loop to generate a ranking as well.
        java.util.Vector<server.campaign.SPlanet> ownedWorlds = new java.util.Vector<server.campaign.SPlanet>(1, 1);
        java.util.Vector<server.campaign.SPlanet> contestedWorlds = new java.util.Vector<server.campaign.SPlanet>(1, 1);
        int totalOwnership = 0;

        java.util.Iterator<Planet> it = server.campaign.CampaignMain.cm.getData().getAllPlanets().iterator();
        while (it.hasNext()) {
            server.campaign.SPlanet p = (server.campaign.SPlanet) it.next();

            //update total
            int ownership = p.getInfluence().getInfluence(h.getId());
            totalOwnership = totalOwnership + ownership;

            //update lists
            if (p.getOwner() != null && p.getOwner().equals(h)) {ownedWorlds.add(p);} else if (ownership > 0) {
                contestedWorlds.add(p);
            }
        }


        //Current Ranking
        String rankString = "";
        int diff = totalOwnership - h.getInitialHouseRanking();
        if (diff > 0) {rankString += "+" + diff;} else {rankString += diff;}
        rankString += "/" + h.getInitialHouseRanking();

        s += "Ranking: " + rankString + "<br>";

        //assume at least one player
        if (sinceLastRestart < 1) {sinceLastRestart = 1;}

        int ownedWorldsSize = ownedWorlds.size();

        if (ownedWorldsSize < 1) {ownedWorldsSize = 1;}

        //economy stats
        s += "Total Economic Value: " + h.getComponentProduction() + "<br>" +
                   "  - Avg. Planet: " + h.getComponentProduction() / ownedWorldsSize + "<br>" +
                   "  - Per Capita: " + h.getComponentProduction() / sinceLastRestart + "<br><br>";

        s += "<br><b>Tech Level: </b>" + TechConstants.getLevelDisplayableName(h.getTechLevel()) + ".";

        if (server.campaign.CampaignMain.cm.getPlayer(Username).getMyHouse().equals(h) ||
                  server.campaign.CampaignMain.cm.getServer().isModerator(Username)) {
            s += " Current Research: " +
                       h.getTechResearchPoints() +
                       " of " +
                       server.campaign.CampaignMain.cm.getConfig("TechPointsNeedToLevel");
        }

        s += "<br>";

        //sort planets by alpha, instead of ID
        java.util.Collections.sort(ownedWorlds, new PlanetNameComparator());
        java.util.Collections.sort(contestedWorlds, new PlanetNameComparator());

        java.util.Iterator<server.campaign.SPlanet> i = ownedWorlds.iterator();
        s += "<b>Planets (Owned):</b>";

        if (!i.hasNext()) {s += " none<br>";} else {s += "<br>";}

        while (i.hasNext()) {
            server.campaign.SPlanet currPlanet = (server.campaign.SPlanet) i.next();
            s += currPlanet.getNameAsColoredLink();
            if (currPlanet.getFactoryCount() > 0) {s += "*";}

            //show % owned, if < 100
            int amtOwned = currPlanet.getInfluence().getInfluence(h.getId());
            if (amtOwned < currPlanet.getConquestPoints()) {s += " (" + amtOwned + "cp)";}

            if (i.hasNext()) {s += ", ";} else {s += "<br>";}
        }

        s += "<br><b>Planets (Contested):</b>";
        i = contestedWorlds.iterator();

        if (!i.hasNext()) {s += " none<br>";} else {s += "<br>";}

        while (i.hasNext()) {
            server.campaign.SPlanet currPlanet = (server.campaign.SPlanet) i.next();
            s += currPlanet.getNameAsColoredLink();
            if (currPlanet.getFactoryCount() > 0) {s += "*";}

            //show % owned
            server.campaign.SHouse owner = currPlanet.getOwner();
            if (owner != null) {
                s += " (" +
                           currPlanet.getInfluence().getInfluence(h.getId()) +
                           "cp, " +
                           owner.getColoredAbbreviation(false) +
                           " " +
                           currPlanet.getInfluence().getInfluence(owner.getId()) +
                           "cp)";
            } else {s += "(" + currPlanet.getInfluence().getInfluence(h.getId()) + "cp, No Owner)";}

            if (i.hasNext()) {s += ", ";} else {s += "<br>";}
        }

        //show the games the factions' players are involved in
        s += "<br><b>Current Games: </b>";
        String gameStrings = "";

        java.util.Iterator<server.campaign.operations.ShortOperation> games = server.campaign.CampaignMain.cm.getOpsManager()
                                                                                    .getRunningOps()
                                                                                    .values()
                                                                                    .iterator();
        while (games.hasNext()) {
            server.campaign.operations.ShortOperation so = (server.campaign.operations.ShortOperation) games.next();
            if (so.hasPlayerWhoseHouseBeginsWith(h.getName())) {gameStrings += "<br>" + so.getInfo(false, false);}
        }//end while(more elements)

        if (gameStrings.trim().equals("")) {s += "none.<br>";} else {s += gameStrings + "<br>";}

        server.campaign.CampaignMain.cm.toUser("SM|" + s, Username, false);

    }
}
