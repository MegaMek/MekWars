/*
 * MekWars - Copyright (C) 2005
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

import mekwars.server.campaign.CampaignMain;

public class DeclineAttackFromReserveCommand implements Command {

    int accessLevel = 2;
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

        server.campaign.SPlayer ap = null;

        try {
            ap = CampaignMain.campaignMain.getPlayer(command.nextToken());
        } catch (Exception ex) {
            return;
        }

        Long launchTime = ap.getLastAttackFromReserve();

        if (launchTime +
                  (Long.parseLong(CampaignMain.campaignMain.getConfig("AttackFromReserveResponseTime")) * 60000) <
                  System.currentTimeMillis()) {
            CampaignMain.campaignMain.toUser("AM:Sorry but this offer has already expired.", Username, true);
            return;
        }

        //else

        ap.setLastAttackFromReserve(launchTime -
                                          (Long.parseLong(CampaignMain.campaignMain.getConfig(
                                                "AttackFromReserveResponseTime")) * 60000));

        CampaignMain.campaignMain.toUser("AM:" + Username + " has declined your proposal.", ap.getName(), true);
        CampaignMain.campaignMain.toUser("AM:You have declined " + ap.getName() + "'s proposal.", Username, true);

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

}//end AttackFromReserveCommand
