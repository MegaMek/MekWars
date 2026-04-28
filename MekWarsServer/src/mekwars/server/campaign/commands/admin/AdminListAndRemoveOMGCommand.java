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

import common.House;
import common.Unit;

public class AdminListAndRemoveOMGCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
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

        /*
         * We know that OMG's are always light meks, so we can loop
         * through every faction's light mek queue to remove them.
         */
        for (House house : server.campaign.CampaignMain.cm.getData().getAllHouses()) {
            server.campaign.SHouse faction = (server.campaign.SHouse) house;
            java.util.concurrent.ConcurrentLinkedQueue<server.campaign.SUnit> units = new java.util.concurrent.ConcurrentLinkedQueue<server.campaign.SUnit>(
                  faction.getHangar(Unit.MEK).elementAt(Unit.LIGHT));
            for (server.campaign.SUnit currU : units) {
                if (currU.getModelName().equals("OMG-UR-FD")) {
                    server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                          Username +
                                " removed an OMG from the " +
                                faction.getName() +
                                " bays. Should have been a " +
                                currU.getUnitFilename() +
                                ".");
                    server.campaign.CampaignMain.cm.toUser("Removed an OMG from the " +
                                                                 faction.getName() +
                                                                 " bays. Should have been a " +
                                                                 currU.getUnitFilename() +
                                                                 ".", Username, true);
                    faction.getHangar(Unit.MEK).elementAt(Unit.LIGHT).removeElement(currU);
                    server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers(faction,
                          "HS|" + faction.getHSUnitRemovalString(currU),
                          false);
                }
            }//end while(units remain in light mek hangar)
        }//end while(factions remain)

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}//end AdminListAndRemoveOMGCommand
