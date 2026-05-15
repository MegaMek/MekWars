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

package mekwars.server.campaign.commands.mod;

import megamek.common.Entity;
import mekwars.server.campaign.CampaignMain;

/**
 * @author Helge Richter
 */
public class ModFullRepairCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Player Name#Unit ID";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        //SPlayer admin =  CampaignMain.cm.getPlayer(Username);
        String targetName = (String) command.nextElement();
        server.campaign.SPlayer target = CampaignMain.campaignMain.getPlayer(targetName);

        if (target == null) {
            CampaignMain.campaignMain.toUser("AM:Target player could not be found. Try again.", Username, true);
            return;
        }

        //non null target, so attempt the scrap
        int unitID = Integer.parseInt((String) command.nextElement());
        server.campaign.SUnit m = target.getUnit(unitID);

        //break out if the player doesn't have a unit with that id
        if (m == null) {
            CampaignMain.campaignMain.toUser("AM:Target player doesn't have a unit with ID# " + unitID + ".",
                  Username,
                  true);
            return;
        }

        Entity unitEntity = server.campaign.SUnit.loadMech(m.getUnitFilename());
        m.setEntity(unitEntity);

        //tell the player you're going to scrap the unit ...
        CampaignMain.campaignMain.toUser("AM:" +
                                               targetName +
                                               "'s " +
                                               m.getModelName() +
                                               " is now fully repaired.", Username, true);
        CampaignMain.campaignMain.toUser("AM:" +
                                               Username +
                                               " mod-repaired your " +
                                               m.getModelName() +
                                               " (ID#" +
                                               m.getId() +
                                               ")", targetName, true);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " mod-repaired a " + m.getModelName() + " belonging to " + targetName);
        target.setSave();
        target.checkAndUpdateArmies(m);
        CampaignMain.campaignMain.toUser("PL|UU|" + m.getId() + "|" + m.toString(true), targetName, false);
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}//end ModFullRepairCommand
