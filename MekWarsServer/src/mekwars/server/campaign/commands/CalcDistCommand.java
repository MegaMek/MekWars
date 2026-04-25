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

public class CalcDistCommand implements Command {

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

        if (!command.hasMoreElements()) {
            server.campaign.CampaignMain.cm.toUser("SM|You need to enter 2 Planet Names!", Username, false);
            return;
        }

        server.campaign.SPlanet p1 = server.campaign.CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),
              Username);
        if (!command.hasMoreElements()) {
            server.campaign.CampaignMain.cm.toUser("SM|You need to enter 2 Planet Names!", Username, false);
            return;
        }

        server.campaign.SPlanet p2 = server.campaign.CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),
              Username);
        if (p1 != null && p2 != null) {
            int xdiff = (int) (Math.pow(p1.getPosition().getX() - p2.getPosition().getX(), 2));
            int ydiff = (int) (Math.pow(p1.getPosition().getY() - p2.getPosition().getY(), 2));
            int newdist = (int) Math.sqrt(xdiff + ydiff);
            server.campaign.CampaignMain.cm.toUser("SM|The distance between " +
                                                         p1.getName() +
                                                         " and " +
                                                         p2.getName() +
                                                         " is " +
                                                         newdist +
                                                         " LY", Username, false);
        }

    }//end process()
}
