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

import mekwars.server.campaign.CampaignMain;

public class AdminDestroyPlanetCommand implements server.campaign.commands.Command {

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

        server.campaign.SPlanet p = CampaignMain.campaignMain.getPlanetFromPartialString(command.nextToken(),
              Username);

        //remove the world from its owner
        server.campaign.SHouse h = p.getOwner();
        if (h != null) {h.removePlanet(p);}

        //remove the world from the data in memory
        CampaignMain.campaignMain.getData().removePlanet(p.getId());

        //finally, remove the world's flat file
        java.io.File fp = new java.io.File("./campaign/planets/" + p.getName().toLowerCase().trim() + ".dat");
        if (fp.exists()) {fp.delete();}

        CampaignMain.campaignMain.updateHousePlanetUpdate();
        //server.MWLogger.modLog(Username + " unleashed the Death Star on " + p.getName() + ". Planet destroyed!");
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " unleashed the Death Star on " + p.getName() + ". Planet destroyed!");

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
