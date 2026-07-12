/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - Torren (torren@users.sourceforge.net)
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

/**
 *
 * @author Torren (Jason Tighe) 10.13.05
 *
 */

package mekwars.server.campaign.commands;

import mekwars.server.campaign.CampaignMain;

public class DisplayUnitRepairJobsCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        try {
            int unitid = Integer.parseInt(command.nextToken());
            String data = CampaignMain.campaignMain.getRTT().unitRepairTimes(unitid);
            if (data != null) {CampaignMain.campaignMain.toUser("FSM|" + data, Username, false);} else {
                server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
                server.campaign.SUnit unit = player.getUnit(unitid);

                CampaignMain.campaignMain.toUser("FSM|#" +
                                                       unitid +
                                                       " " +
                                                       unit.getEntity().getShortNameRaw() +
                                                       " has the following repair jobs pending:<br><b>None.</b><br>",
                      Username,
                      false);
            }
        } catch (Exception ex) {}
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
