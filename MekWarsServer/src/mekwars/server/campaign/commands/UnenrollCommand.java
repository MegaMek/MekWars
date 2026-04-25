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

import common.UnitFactory;
import common.util.MWLogger;


public class UnenrollCommand implements Command {

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

        if (Username.startsWith("Nobody")) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Nobodies can't enroll, hence they can't unenroll. Nice try though.",
                  Username,
                  true);
            return;
        }

        //load the player
        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
        if (p == null) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Couldn't find your player to unenroll. Contact an admin immediately.",
                  Username,
                  true);
            return;
        }

        //check for confirmation
        if (!command.hasMoreTokens()) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You didn't confirm the Unenroll command. Enter /c unenroll#confirm if you're absolutely sure you want to quit.",
                  Username,
                  true);
            return;
        }

        String confirmString = command.nextToken();
        if (!confirmString.equalsIgnoreCase("confirm")) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You didn't confirm the Unenroll Command. Enter /c unenroll#confirm if you're absolutely sure you want to quit.",
                  Username,
                  true);
            return;
        }

        if (server.campaign.CampaignMain.cm.getOpsManager().getShortOpForPlayer(p) != null
                  || p.getDutyStatus() == server.campaign.SPlayer.STATUS_FIGHTING) {
            server.campaign.CampaignMain.cm.toUser("AM:You cannot unenroll while in a game.", Username, true);
            return;
        }

        if (p.getExperience() == 0) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You cannot unenroll with 0 XP. Ask an admin or mod to remove your account.",
                  Username,
                  true);
            return;
        }

        if (server.campaign.CampaignMain.cm.getMarket().hasActiveListings(p)) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You cannot unenroll while you have units on the Market. Recall them and try again.",
                  Username,
                  true);
            return;
        }

        if (p.hasRepairingUnits(false)) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You cannot unenroll while repairing units. Cancel the repairs and try again.",
                  Username,
                  true);
            return;
        }

        server.campaign.SHouse hisfaction = server.campaign.CampaignMain.cm.getHouseForPlayer(Username);
        if (hisfaction == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Couldn't find faction to unenroll. Contact an admin immediately.",
                  Username,
                  true);
            return;
        }

        //checks passed. do the actual removal.
        hisfaction.removePlayer(p, server.campaign.CampaignMain.cm.getBooleanConfig("DonateUnitsUponUnenrollment"));


        //tell the user
        server.campaign.CampaignMain.cm.toUser("AM:You've been unenrolled.", Username, true);
        removeFaction(hisfaction);

        //delete the player's saved info, if a pfile exists
        java.io.File fp = new java.io.File("./campaign/players/" + p.getName().toLowerCase() + ".dat");
        if (fp.exists()) {fp.delete();}

        //tell the mods and add to iplog.0
        java.net.InetAddress ip = server.campaign.CampaignMain.cm.getServer().getIP(Username);
        //MWLogger.modLog(Username + " unenrolled from the campaign (IP: " + ip + ").");
        MWLogger.ipLog("UNENROLL: " + Username + " IP: " + ip);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " unenrolled from the campaign (IP: " + ip + ").");
    }//end process

    private void removeFaction(server.campaign.SHouse faction) {

        if (!server.campaign.CampaignMain.cm.getBooleanConfig("AllowSinglePlayerFactions")) {return;}

        java.util.Enumeration<server.campaign.SPlanet> planets = faction.getPlanets().elements();
        while (planets.hasMoreElements()) {
            server.campaign.SPlanet planet = planets.nextElement();
            planet.doGainInfluence(server.campaign.CampaignMain.cm.getHouseById(-1), faction, Integer.MAX_VALUE, true);
            java.util.Enumeration<UnitFactory> factories = planet.getUnitFactories().elements();
            while (factories.hasMoreElements()) {
                UnitFactory factory = factories.nextElement();
                if (factory.getFounder().equalsIgnoreCase(faction.getName())) {
                    planet.getUnitFactories().removeElement(factory);
                }
            }
            planet.setBaysProvided(0);
            planet.updated();
            planet.updateInfluences();
        }
        server.campaign.CampaignMain.cm.getData().removeHouse(faction.getId());
        server.campaign.CampaignMain.cm.updateHousePlanetUpdate();
        server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers("PL|RPF|" + faction.getId(), false);

    }
}//end UnenrollCommand
