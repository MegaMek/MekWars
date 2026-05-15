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

import java.io.File;
import java.util.Enumeration;
import java.util.StringTokenizer;

import megamek.logging.MMLogger;
import mekwars.common.UnitFactory;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SHouse;
import mekwars.server.campaign.SPlanet;
import mekwars.server.campaign.SPlayer;

public class UnenrollCommand implements Command {
    private final static MMLogger LOGGER = MMLogger.create(UnenrollCommand.class);

    int accessLevel = 0;
    String syntax = "";

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser(STR."AM:Insufficient access level for command. Level: \{userLevel}. Required: \{accessLevel}.",
                      Username,
                      true);
                return;
            }
        }

        if (Username.startsWith("Nobody")) {
            CampaignMain.campaignMain.toUser(
                  "AM:Nobodies can't enroll, hence they can't unenroll. Nice try though.",
                  Username,
                  true);
            return;
        }

        //load the player
        SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
        if (player == null) {
            CampaignMain.campaignMain.toUser(
                  "AM:Couldn't find your player to unenroll. Contact an admin immediately.",
                  Username,
                  true);
            return;
        }

        //check for confirmation
        if (!command.hasMoreTokens()) {
            CampaignMain.campaignMain.toUser(
                  "AM:You didn't confirm the Unenroll command. Enter /c unenroll#confirm if you're absolutely sure you want to quit.",
                  Username,
                  true);
            return;
        }

        String confirmString = command.nextToken();
        if (!confirmString.equalsIgnoreCase("confirm")) {
            CampaignMain.campaignMain.toUser(
                  "AM:You didn't confirm the Unenroll Command. Enter /c unenroll#confirm if you're absolutely sure you want to quit.",
                  Username,
                  true);
            return;
        }

        if (CampaignMain.campaignMain.getOpsManager().getShortOpForPlayer(player) != null
                  || player.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
            CampaignMain.campaignMain.toUser("AM:You cannot unenroll while in a game.", Username, true);
            return;
        }

        if (player.getExperience() == 0) {
            CampaignMain.campaignMain.toUser(
                  "AM:You cannot unenroll with 0 XP. Ask an admin or mod to remove your account.",
                  Username,
                  true);
            return;
        }

        if (CampaignMain.campaignMain.getMarket().hasActiveListings(player)) {
            CampaignMain.campaignMain.toUser(
                  "AM:You cannot unenroll while you have units on the Market. Recall them and try again.",
                  Username,
                  true);
            return;
        }

        if (player.hasRepairingUnits(false)) {
            CampaignMain.campaignMain.toUser(
                  "AM:You cannot unenroll while repairing units. Cancel the repairs and try again.",
                  Username,
                  true);
            return;
        }

        SHouse playerFaction = CampaignMain.campaignMain.getHouseForPlayer(Username);
        if (playerFaction == null) {
            CampaignMain.campaignMain.toUser("AM:Couldn't find faction to unenroll. Contact an admin immediately.",
                  Username,
                  true);
            return;
        }

        //checks passed. do the actual removal.
        playerFaction.removePlayer(player, CampaignMain.campaignMain.getBooleanConfig("DonateUnitsUponUnenrollment"));

        //tell the user
        CampaignMain.campaignMain.toUser("AM:You've been unenrolled.", Username, true);
        removeFaction(playerFaction);

        //delete the player's saved info if a pfile exists
        File fp = new File(STR."./campaign/players/\{player.getName().toLowerCase()}.dat");
        if (fp.exists()) {
            fp.delete();
        }

        //tell the mods and add to iplog.0
        java.net.InetAddress ip = CampaignMain.campaignMain.getServer().getIP(Username);
        //MWLogger.modLog(Username + " unenrolled from the campaign (IP: " + ip + ").");
        LOGGER.info(STR."UNENROLL: \{Username} IP: \{ip}");
        CampaignMain.campaignMain.doSendModMail("NOTE",
              STR."\{Username} unenrolled from the campaign (IP: \{ip}).");
    }//end process

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

    private void removeFaction(SHouse faction) {

        if (!CampaignMain.campaignMain.getBooleanConfig("AllowSinglePlayerFactions")) {
            return;
        }

        Enumeration<SPlanet> planets = faction.getPlanets().elements();
        while (planets.hasMoreElements()) {
            SPlanet planet = planets.nextElement();
            planet.doGainInfluence(CampaignMain.campaignMain.getHouseById(-1), faction, Integer.MAX_VALUE, true);
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
        CampaignMain.campaignMain.getData().removeHouse(faction.getId());
        CampaignMain.campaignMain.updateHousePlanetUpdate();
        CampaignMain.campaignMain.doSendToAllOnlinePlayers("PL|RPF|" + faction.getId(), false);

    }
}//end UnenrollCommand
