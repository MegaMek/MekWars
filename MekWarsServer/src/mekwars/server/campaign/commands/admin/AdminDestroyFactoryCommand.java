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

import common.UnitFactory;
import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;

public class AdminDestroyFactoryCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN, factoryID;
    String syntax = "Planet Name#Factory Name";

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
            String factoryname = command.nextToken();
            if (p == null) {
                CampaignMain.campaignMain.toUser("Planet not found:", Username, true);
                return;
            }

            if (p.getUnitFactories().size() < 1) {
                CampaignMain.campaignMain.toUser("This planet does not have any factories!", Username, true);
                return;
            }

            UnitFactory foundFactory = null;
            for (UnitFactory UF : p.getUnitFactories()) {
                if (UF.getName().equalsIgnoreCase(factoryname)) {
                    foundFactory = UF;
                    break;
                }
            }

            if (foundFactory == null) {
                CampaignMain.campaignMain.toUser("Factory " + factoryname + " not found", Username, true);
                return;
            }

            p.getUnitFactories().removeElement(foundFactory);
            p.getUnitFactories().trimToSize();


            p.updated();
            //server.MWLogger.modLog(Username + "  removed " + factoryname + " from " + p.getName() + ".");
            CampaignMain.campaignMain.toUser(factoryname + " removed from " + p.getName() + ".", Username, true);
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  Username + "  removed " + factoryname + " from " + p.getName() + ".");
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }//end catch

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
