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

public class FactionLeaderFluffCommand implements server.campaign.commands.Command {

    int accessLevel = server.campaign.CampaignMain.cm.getIntegerConfig("factionLeaderLevel");
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

        server.campaign.SPlayer leader = server.campaign.CampaignMain.cm.getPlayer(Username);
        server.campaign.SPlayer p = null;
        String fluff = "";

        try {
            p = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("AM:Improper command. Try: /c factionleaderfluff#PlayerName#text",
                  Username,
                  true);
            return;
        }

        if (command.hasMoreElements()) {fluff = command.nextToken();} else {fluff = null;}

        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Couldn't find a player with that name.", Username, true);
            return;
        }

        if (!leader.getMyHouse().getName().equalsIgnoreCase(p.getMyHouse().getName())) {
            server.campaign.CampaignMain.cm.toUser("AM:You are not in the same faction as " +
                                                         p.getName() +
                                                         " therefore you maynot change their fluff!", Username, true);
            return;
        }

        if (fluff == null) {p.setFluffText("");} else {
            if (fluff.indexOf("~") > 0 ||
                      fluff.indexOf("$") > 0 ||
                      fluff.indexOf("#") > 0 ||
                      fluff.indexOf("|") > 0) {
                server.campaign.CampaignMain.cm.toUser(
                      "AM:Illegal characters in the fluff text try again without '|','$','#', or '~' characters",
                      Username,
                      true);
                return;
            }

            //set the text
            p.setFluffText(fluff);
        }

        server.campaign.CampaignMain.cm.toUser("AM:New fluff text for " + p.getName() + ": " + fluff, Username, true);
        server.campaign.CampaignMain.cm.toUser(Username + " set your fluff to: " + fluff, p.getName(), true);
        //server.MWLogger.modLog(Username + " set " + p.getName() + "'s fluff to '" + fluff + "'.");
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " set " + p.getName() + "'s fluff to '" + fluff + "'.");
        server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers("PI|FT|" + p.getName() + "|" + p.getFluffText(),
              false);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
