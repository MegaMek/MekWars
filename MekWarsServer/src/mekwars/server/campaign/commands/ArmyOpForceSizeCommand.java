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

public class ArmyOpForceSizeCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        //access check
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

            //first, make sure limiters are allowed ...
            boolean useForceSize = Boolean.parseBoolean(server.campaign.CampaignMain.cm.getConfig("UseOperationsRule"));
            if (!useForceSize) {
                server.campaign.CampaignMain.cm.toUser("AM:Force size is disabled.", Username, true);
                return;
            }


            int armyid = Integer.parseInt((String) command.nextElement());
            if (command.hasMoreElements()) {

                float limit = Float.parseFloat(command.nextToken());
                server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
                if (p != null) {
                    if (p.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE) {
                        server.campaign.CampaignMain.cm.toUser("AM:You cannot change op force size while active.",
                              Username,
                              true);
                        return;
                    }
                    server.campaign.SArmy army = p.getArmy(armyid);
                    if (army != null) {

                        if (limit < Army.NO_LIMIT) {//-1 is NO_LIMIT
                            server.campaign.CampaignMain.cm.toUser("AM:You may not set negative op force size.",
                                  Username,
                                  true);
                            return;
                        }

                        //check to make sure that the proposed limit doesnt hit the buffer
                        army.setOpForceSize(limit);

                        if (limit == -1) {
                            server.campaign.CampaignMain.cm.toUser("AM:Army #" + armyid + "'s op force size disabled.",
                                  Username,
                                  true);
                        } else {
                            server.campaign.CampaignMain.cm.toUser("AM:Army #" +
                                                                         armyid +
                                                                         "'s op force size set to " +
                                                                         limit +
                                                                         ".", Username, true);
                        }

                        server.campaign.CampaignMain.cm.toUser("PL|SAOFS|" + army.getID() + "#" + army.getOpForceSize(),
                              Username,
                              false);
                    }
                }
            }
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
