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

import common.util.MWLogger;

public class AdminRemoveAllFactoriesCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Planet Name";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

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

        try {
            server.campaign.SPlanet p = server.campaign.CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),
                  Username);

            if (p == null) {return;}

            p.getUnitFactories().clear();
            p.updated();

            //server.MWLogger.modLog(Username + "  removed " + factoryname + " from " + p.getName() + ".");
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username + "  removed all factories  from " + p.getName() + ".");
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }//end catch

    }
}
