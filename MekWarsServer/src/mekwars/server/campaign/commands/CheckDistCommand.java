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

public class CheckDistCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }

        if (command.hasMoreElements()) {
            server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);
            server.campaign.SPlanet p = (server.campaign.SPlanet) server.campaign.CampaignMain.cm.getData()
                                                                        .getPlanetByName(command.nextToken());
            if (p != null) {
                server.campaign.CampaignMain.cm.toUser("SM|Distance to " +
                                                             p.getName() +
                                                             " is " +
                                                             player.getMyHouse()
                                                                   .getDistanceTo(p,
                                                                         server.campaign.CampaignMain.cm.getPlayer(
                                                                               Username)), Username, false);
            }
        }
    }//end process()

}
