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


public class RangeCommand implements Command {

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

        if (command.hasMoreElements()) {
            int range = Integer.parseInt(command.nextToken());

            server.campaign.SHouse targetfaction = null;
            if (command.hasMoreElements()) {
                String target = command.nextToken();
                targetfaction = server.campaign.CampaignMain.cm.getHouseFromPartialString(target, Username);

                //RFE 1121663 - Range Calculator and Couldn't find Name @urgru 6.5.05
                if (targetfaction == null) {
                    server.campaign.CampaignMain.cm.toUser("AM:Could not find faction " + target + ". Try again?",
                          Username,
                          true);
                    return;
                }
            }

            //RFE 1076505: Range - Factories, @urgru 6.5.05
            boolean facWorldsOnly = false;
            if (command.hasMoreElements()) {

                try {
                    facWorldsOnly = Boolean.parseBoolean(command.nextToken());
                } catch (Exception e) {
                    server.campaign.CampaignMain.cm.toUser(
                          "AM:Improper format. Try: /c range#Distance#Faction#true to return only production worlds.",
                          Username,
                          true);
                    return;
                }
            }

            server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);
            server.campaign.SHouse h = player.getMyHouse();
            String result = "These planets are within " + range + " light-years ";
            if (targetfaction != null) {
                if (facWorldsOnly) {
                    result += "and " + targetfaction.getName() + " production facilities are present ";
                } else {result += "and " + targetfaction.getName() + " military assets are present ";}
            }

            result += ":<br>";
            java.util.Iterator<Planet> e = server.campaign.CampaignMain.cm.getData().getAllPlanets().iterator();
            while (e.hasNext()) {
                server.campaign.SPlanet p = (server.campaign.SPlanet) e.next();
                if (h.getDistanceTo(p, server.campaign.CampaignMain.cm.getPlayer(Username)) <= range) {
                    if (targetfaction == null) {

                        server.campaign.SHouse owner = p.getOwner();
                        if (facWorldsOnly && owner.equals(targetfaction) && p.getFactoryCount() > 0) {
                            result += "<font color=\"" +
                                            p.getOwner().getHouseColor() +
                                            "\">" +
                                            p.getShortDescription(true) +
                                            "</font>" +
                                            "<br>";
                        } else {//!facworldsonly, do normal faction-specific check
                            if (owner != null) {
                                result += "<font color=\"" +
                                                p.getOwner().getHouseColor() +
                                                "\">" +
                                                p.getShortDescription(true) +
                                                "</font>" +
                                                "<br>";
                            } else {result += p.getSmallStatus(true) + "<br>";}
                        }

                    } else {
                        if (p.getInfluence().getInfluence(targetfaction.getId()) > 0) {
                            server.campaign.SHouse owner = p.getOwner();
                            if (owner != null) {
                                result += "<font color=\"" +
                                                p.getOwner().getHouseColor() +
                                                "\">" +
                                                p.getShortDescription(true) +
                                                "</font>" +
                                                "<br>";
                            } else {result += p.getSmallStatus(true) + "<br>";}
                        }
                    }
                }
            }
            result = result.substring(0, result.length() - 2);
            server.campaign.CampaignMain.cm.toUser("SM|" + result, Username, false);
        }
    }
}
