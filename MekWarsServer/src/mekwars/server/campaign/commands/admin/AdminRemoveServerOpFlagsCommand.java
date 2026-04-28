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


public class AdminRemoveServerOpFlagsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

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
                  "Syntax AdminRemoveServerOpFlags#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times.",
                  Username);
            return;
        }

        try {
            while (command.hasMoreTokens()) {
                String key = command.nextToken();
                if (server.campaign.CampaignMain.cm.getData().getPlanetOpFlags().remove(key) != null) {
                    server.campaign.CampaignMain.cm.toUser("Op flag " + key + " removed from the server.",
                          Username,
                          true);
                    //server.MWLogger.modLog(Username + " removed op flag "+key+".");
                    server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " removed op flag " + key + ".");
                }
            }
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser(
                  "Syntax AdminRemoveServerOpFlags#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times.",
                  Username);
            return;
        }

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
