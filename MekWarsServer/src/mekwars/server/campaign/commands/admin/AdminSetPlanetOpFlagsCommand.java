/*
 * MekWars - Copyright (C) 2006
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


public class AdminSetPlanetOpFlagsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times.<br>NOTE:This will reset all the flags for the planet to these flags!";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        if (!command.hasMoreTokens()) {
            server.campaign.CampaignMain.cm.toUser(
                  "Syntax AdminSetPlanetOpFlags#Planet#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times.<br>NOTE:This will reset all the flags for the planet to these flags!",
                  Username);
            return;
        }

        server.campaign.SPlanet planet = (server.campaign.SPlanet) server.campaign.CampaignMain.cm.getData()
                                                                         .getPlanetByName(command.nextToken());
        if (planet == null) {
            server.campaign.CampaignMain.cm.toUser("Unknown Planet", Username, true);
            return;
        }

        java.util.TreeMap<String, String> map = new java.util.TreeMap<String, String>();
        try {
            while (command.hasMoreTokens()) {
                String key = command.nextToken();
                if (server.campaign.CampaignMain.cm.getData().getPlanetOpFlags().containsKey(key)) {
                    map.put(key, server.campaign.CampaignMain.cm.getData().getPlanetOpFlags().get(key));
                } else {server.campaign.CampaignMain.cm.toUser(key + " is not a valid plant ops flag!", Username);}
            }
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser(
                  "Syntax AdminSetPlanetOpFlags#Planet#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times.<br>NOTE:This will reset all the flags for the planet to these flags!",
                  Username);
            return;
        }

        planet.setPlanetFlags(map);
        server.campaign.CampaignMain.cm.toUser("Op flags set for " + planet.getName(), Username, true);
        //server.MWLogger.modLog(Username + " set the op flags for "+planet.getName());
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has set the op flags for " + planet.getName());

        planet.updated();
    }
}
