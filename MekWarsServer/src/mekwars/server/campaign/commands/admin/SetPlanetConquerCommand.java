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


public class SetPlanetConquerCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name#[true/false]";

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

        server.campaign.SPlanet p = null;
        boolean conquer = true;

        try {
            p = server.campaign.CampaignMain.cm.getPlanetFromPartialString(command.nextToken(), Username);
            conquer = Boolean.parseBoolean(command.nextToken());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("Improper command. Try: /c setplanetconquer#planet#true/false",
                  Username,
                  true);
            return;
        }

        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("Couldn't find a planet with that name.", Username, true);
            return;
        }

        p.setConquerable(conquer);
        p.updated();

        server.campaign.CampaignMain.cm.toUser("You set " + p.getName() + "'s conquer status to " + conquer,
              Username,
              true);
        //server.MWLogger.modLog(Username + " has changed the infaction conquer for " + p.getName()+" to "+conquer);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has changed the conquer status for " + p.getName() + " to " + conquer);

    }
}
