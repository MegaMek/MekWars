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

public class AdminDestroyPlanetCommand implements server.campaign.commands.Command {

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

        server.campaign.SPlanet p = server.campaign.CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),
              Username);

        //remove the world from its owner
        server.campaign.SHouse h = p.getOwner();
        if (h != null) {h.removePlanet(p);}

        //remove the world from the data in memory
        server.campaign.CampaignMain.cm.getData().removePlanet(p.getId());

        //finally, remove the world's flat file
        java.io.File fp = new java.io.File("./campaign/planets/" + p.getName().toLowerCase().trim() + ".dat");
        if (fp.exists()) {fp.delete();}

        server.campaign.CampaignMain.cm.updateHousePlanetUpdate();
        //server.MWLogger.modLog(Username + " unleashed the Death Star on " + p.getName() + ". Planet destroyed!");
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " unleashed the Death Star on " + p.getName() + ". Planet destroyed!");

    }
}
