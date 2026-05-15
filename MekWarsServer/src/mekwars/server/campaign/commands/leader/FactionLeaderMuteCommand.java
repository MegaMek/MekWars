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

package mekwars.server.campaign.commands.leader;

import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;

public class FactionLeaderMuteCommand implements server.campaign.commands.Command {

    int accessLevel = CampaignMain.campaignMain.getIntegerConfig("factionLeaderLevel");
    String syntax = "";

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

        server.campaign.SPlayer leader = CampaignMain.campaignMain.getPlayer(Username);
        server.campaign.SPlayer p = null;

        try {
            p = CampaignMain.campaignMain.getPlayer(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper command. Try: /c factionleadermute#PlayerName",
                  Username,
                  true);
            return;
        }

        if (p == null) {
            CampaignMain.campaignMain.toUser("AM:Couldn't find a player with that name.", Username, true);
            return;
        }

        if (!leader.getMyHouse().getName().equalsIgnoreCase(p.getMyHouse().getName())) {
            CampaignMain.campaignMain.toUser("AM:You are not in the same faction as " +
                                                   p.getName() +
                                                   ". You may not mute them!", Username, true);
            return;
        }


        java.util.Vector<String> factionIgnores = CampaignMain.campaignMain.getServer()
                                                        .getFactionLeaderIgnoreList();

        //do the actual mute
        if (factionIgnores.indexOf(p.getName()) == -1) {
            factionIgnores.add(p.getName());
            //server.MWLogger.modLog(Username + " faction muted " + p.getName());
            CampaignMain.campaignMain.doSendModMail("NOTE", Username + " faction muted " + p.getName());
            CampaignMain.campaignMain.getServer()
                  .sendChat(Username + " muted " + p.getName() + " (faction mute).");
        } else { //unmute
            factionIgnores.remove(p.getName());
            MWLogger.modLog(Username + " faction unmuted " + p.getName());
            CampaignMain.campaignMain.doSendModMail("NOTE", Username + " faction unmuted " + p.getName());
            CampaignMain.campaignMain.getServer()
                  .sendChat(Username + " unmuted " + p.getName() + " (faction mute).");
        }

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
