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

package mekwars.server.campaign.commands.admin;
import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;

public class AdminRemoveAllFactoriesCommand implements server.campaign.commands.Command {
    private static final MMLogger LOGGER = MMLogger.create(AdminRemoveAllFactoriesCommand.class);

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name";

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

        try {
            server.campaign.SPlanet p = CampaignMain.campaignMain.getPlanetFromPartialString(command.nextToken(),
                  Username);

            if (p == null) {return;}

            p.getUnitFactories().clear();
            p.updated();

            //server.MWLogger.modLog(Username + "  removed " + factoryname + " from " + p.getName() + ".");
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  Username + "  removed all factories  from " + p.getName() + ".");
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }//end catch

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
