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

package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;

// AdminAllowHouseDefection#House#true/false
public class AdminAllowHouseDefectionCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "factionname#to/from#true/false";

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

        try {
            server.campaign.SHouse faction = (server.campaign.SHouse) CampaignMain.campaignMain.getData()
                                                                            .getHouseByName(command.nextToken());
            if (faction == null) {
                CampaignMain.campaignMain.toUser("Unknown faction!", Username, true);
                return;
            }
            String toFrom = command.nextToken();

            boolean lock;
            if (command.hasMoreElements()) {lock = Boolean.parseBoolean(command.nextToken());} else {
                if (toFrom.equalsIgnoreCase("to")) {
                    if (faction.getHouseDefectionTo()) {lock = false;} else {lock = true;}
                } else {
                    if (faction.getHouseDefectionFrom()) {lock = false;} else {lock = true;}
                }
            }

            if (toFrom.equalsIgnoreCase("to")) {faction.setHouseDefectionTo(lock);} else {
                faction.setHouseDefectionFrom(lock);
            }

            if (!lock) {
                CampaignMain.campaignMain.toUser("You've blocked defection " +
                                                       toFrom.toLowerCase() +
                                                       " " +
                                                       faction.getName(), Username, true);
                //server.MWLogger.modLog(Username + " has blocked defection "+toFrom.toLowerCase()+" "+ faction.getName());
                CampaignMain.campaignMain.doSendModMail("NOTE",
                      Username + " has blocked defection " + toFrom.toLowerCase() + " " + faction.getName());
            } else {
                CampaignMain.campaignMain.toUser("You've allowed defection " +
                                                       toFrom.toLowerCase() +
                                                       " " +
                                                       faction.getName(), Username, true);
                //server.MWLogger.modLog(Username + " has allowed defection "+toFrom.toLowerCase()+" "+ faction.getName());
                CampaignMain.campaignMain.doSendModMail("NOTE",
                      Username + " has allowed defections " + toFrom.toLowerCase() + " " + faction.getName());
            }

            faction.updated();
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser(
                  "Command failed. Make sure format was: /c adminallowhousedefection#factionname#to/from#true/false",
                  Username,
                  true);
        }

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
