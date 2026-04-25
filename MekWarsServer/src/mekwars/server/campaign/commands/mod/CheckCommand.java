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

public class CheckCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

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

        server.campaign.SPlayer p = null;
        try {
            p = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("AM:Improper format. Try: /c check#name", Username, true);
            return;
        }

        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Couldn't find a user with that name.", Username, true);
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
        toMod += "He has " + server.campaign.CampaignMain.cm.moneyOrFluMessage(true, true, p.getMoney()) + ", ";
        toMod += p.getExperience() + " EXP, ";
        toMod += server.campaign.CampaignMain.cm.moneyOrFluMessage(false, true, p.getInfluence()) + " and ";
        toMod += p.getReward() + " " + server.campaign.CampaignMain.cm.getConfig("RPShortName") + "s.<br>";
        toMod += " - Client version is " + p.getPlayerClientVersion() + ".<br>";
        toMod += " - IP addess is " + server.campaign.CampaignMain.cm.getServer().getIP(p.getName()) + ".<br>";
        toMod += " - Userlevel is " + server.campaign.CampaignMain.cm.getServer().getUserLevel(p.getName()) + ".";
        toMod += " - Multiplayer group is " + p.getGroupAllowance() + " (0 == no group).";

        //send messages and log use
        server.campaign.CampaignMain.cm.toUser(toMod, Username, true);
        //server.MWLogger.modLog(Username + " checked " + p.getName());
        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " checked " + p.getName());

    }
}
