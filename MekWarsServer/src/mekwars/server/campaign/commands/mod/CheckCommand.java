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

package mekwars.server.campaign.commands.mod;

import mekwars.server.campaign.CampaignMain;

public class CheckCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

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

        server.campaign.SPlayer p = null;
        try {
            p = CampaignMain.campaignMain.getPlayer(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper format. Try: /c check#name", Username, true);
            return;
        }

        if (p == null) {
            CampaignMain.campaignMain.toUser("AM:Couldn't find a user with that name.", Username, true);
            return;
        }

        //String to return
        String toMod = p.getColoredName() + " is currently ";

        //player's status
        int status = p.getDutyStatus();
        if (status == server.campaign.SPlayer.STATUS_FIGHTING) {toMod += "fighting. ";} else if (status ==
                                                                                                       server.campaign.SPlayer.STATUS_ACTIVE) {
            toMod += "active. ";
        } else {toMod += "inactive. ";}

        //player's resources and levels
        toMod += "He has " + CampaignMain.campaignMain.moneyOrFluMessage(true, true, p.getMoney()) + ", ";
        toMod += p.getExperience() + " EXP, ";
        toMod += CampaignMain.campaignMain.moneyOrFluMessage(false, true, p.getInfluence()) + " and ";
        toMod += p.getReward() + " " + CampaignMain.campaignMain.getConfig("RPShortName") + "s.<br>";
        toMod += " - client version is " + p.getPlayerClientVersion() + ".<br>";
        toMod += " - IP addess is " + CampaignMain.campaignMain.getServer().getIP(p.getName()) + ".<br>";
        toMod += " - Userlevel is " + CampaignMain.campaignMain.getServer().getUserLevel(p.getName()) + ".";
        toMod += " - Multiplayer group is " + p.getGroupAllowance() + " (0 == no group).";

        //send messages and log use
        CampaignMain.campaignMain.toUser(toMod, Username, true);
        //server.MWLogger.modLog(Username + " checked " + p.getName());
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " checked " + p.getName());

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
