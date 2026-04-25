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

public class DeclineAttackFromReserveCommand implements Command {

    int accessLevel = 2;

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    String syntax = "";

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

        server.campaign.SPlayer ap = null;

        try {
            ap = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());
        } catch (Exception ex) {
            return;
        }

        Long launchTime = ap.getLastAttackFromReserve();

        if (launchTime +
                  (Long.parseLong(server.campaign.CampaignMain.cm.getConfig("AttackFromReserveResponseTime")) * 60000) <
                  System.currentTimeMillis()) {
            server.campaign.CampaignMain.cm.toUser("AM:Sorry but this offer has already expired.", Username, true);
            return;
        }

        //else

        ap.setLastAttackFromReserve(launchTime -
                                          (Long.parseLong(server.campaign.CampaignMain.cm.getConfig(
                                                "AttackFromReserveResponseTime")) * 60000));

        server.campaign.CampaignMain.cm.toUser("AM:" + Username + " has declined your proposal.", ap.getName(), true);
        server.campaign.CampaignMain.cm.toUser("AM:You have declined " + ap.getName() + "'s proposal.", Username, true);

    }//end process

}//end AttackFromReserveCommand
