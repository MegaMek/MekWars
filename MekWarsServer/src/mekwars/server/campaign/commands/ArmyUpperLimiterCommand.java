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

import common.Army;
import mekwars.server.campaign.CampaignMain;

public class ArmyUpperLimiterCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        //access check
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

        if (command.hasMoreElements()) {

            //first, make sure limiters are allowed ...
            boolean limitsAllowed = Boolean.parseBoolean(CampaignMain.campaignMain.getConfig("AllowLimiters"));
            if (!limitsAllowed) {
                CampaignMain.campaignMain.toUser("AM:Limits are disabled.", Username, true);
                return;
            }


            int armyid = Integer.parseInt((String) command.nextElement());
            if (command.hasMoreElements()) {

                int limit = Integer.parseInt(command.nextToken());
                server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
                if (p != null) {
                    if (p.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE) {
                        CampaignMain.campaignMain.toUser("AM:You cannot change limits while active.",
                              Username,
                              true);
                        return;
                    }
                    server.campaign.SArmy army = p.getArmy(armyid);
                    if (army != null) {

                        if (limit < Army.NO_LIMIT) {//-1 is NO_LIMIT
                            CampaignMain.campaignMain.toUser("AM:You may not set negative limits.",
                                  Username,
                                  true);
                            return;
                        }

                        //check to make sure that the proposed limit doesnt hit the buffer
                        int bufferAmt = CampaignMain.campaignMain.getIntegerConfig("UpperLimitBuffer");
                        if (limit < bufferAmt && limit != Army.NO_LIMIT) {
                            CampaignMain.campaignMain.toUser("AM:You must set an upper limit of " +
                                                                   bufferAmt +
                                                                   "or more.", Username, true);
                            return;
                        }

                        army.setUpperLimiter(limit);

                        if (limit == -1) {
                            CampaignMain.campaignMain.toUser("AM:Army #" + armyid + "'s upper limit disabled.",
                                  Username,
                                  true);
                        } else {
                            CampaignMain.campaignMain.toUser("AM:Army #" +
                                                                   armyid +
                                                                   "'s upper limit set to " +
                                                                   limit +
                                                                   ".", Username, true);
                        }

                        CampaignMain.campaignMain.toUser("PL|SAB|" +
                                                               army.getID() +
                                                               "#" +
                                                               army.getLowerLimiter() +
                                                               "#" +
                                                               army.getUpperLimiter(), Username, false);
                    }
                }
            }
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
