/*
 * MekWars - Copyright (C) 2007
 *
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
 * @author jtighe This command is used to set the tech level of a faction.
 */
package mekwars.server.campaign.commands.admin;

import megamek.common.TechConstants;
import mekwars.server.campaign.CampaignMain;


// comand /c AdminSetHouseTechLevel#House#TechLevel
public class AdminSetHouseTechLevelCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#TechLevel";

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

        server.campaign.SHouse house = null;
        int techLevel;

        try {
            house = CampaignMain.campaignMain.getHouseFromPartialString(command.nextToken());
            techLevel = Integer.parseInt(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("Improper command. Try: /c adminsethousetechlevel#faction#techlevel",
                  Username,
                  true);
            return;
        }

        house.setTechLevel(techLevel);
        house.updated();

        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username +
                    " has set " +
                    house.getName() +
                    "'s tech level to " +
                    TechConstants.getLevelDisplayableName(techLevel) +
                    ".");

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
