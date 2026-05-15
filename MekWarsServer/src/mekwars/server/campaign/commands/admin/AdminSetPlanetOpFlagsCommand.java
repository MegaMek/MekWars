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


import mekwars.server.campaign.CampaignMain;

public class AdminSetPlanetOpFlagsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times.<br>NOTE:This will reset all the flags for the planet to these flags!";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        if (!command.hasMoreTokens()) {
            CampaignMain.campaignMain.toUser(
                  "Syntax AdminSetPlanetOpFlags#Planet#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times.<br>NOTE:This will reset all the flags for the planet to these flags!",
                  Username);
            return;
        }

        server.campaign.SPlanet planet = (server.campaign.SPlanet) CampaignMain.campaignMain.getData()
                                                                         .getPlanetByName(command.nextToken());
        if (planet == null) {
            CampaignMain.campaignMain.toUser("Unknown Planet", Username, true);
            return;
        }

        java.util.TreeMap<String, String> map = new java.util.TreeMap<String, String>();
        try {
            while (command.hasMoreTokens()) {
                String key = command.nextToken();
                if (CampaignMain.campaignMain.getData().getPlanetOpFlags().containsKey(key)) {
                    map.put(key, CampaignMain.campaignMain.getData().getPlanetOpFlags().get(key));
                } else {CampaignMain.campaignMain.toUser(key + " is not a valid plant ops flag!", Username);}
            }
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser(
                  "Syntax AdminSetPlanetOpFlags#Planet#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times.<br>NOTE:This will reset all the flags for the planet to these flags!",
                  Username);
            return;
        }

        planet.setPlanetFlags(map);
        CampaignMain.campaignMain.toUser("Op flags set for " + planet.getName(), Username, true);
        //server.MWLogger.modLog(Username + " set the op flags for "+planet.getName());
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " has set the op flags for " + planet.getName());

        planet.updated();
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
