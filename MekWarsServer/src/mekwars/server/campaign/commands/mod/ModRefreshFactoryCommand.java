/*
 * MekWars - Copyright (C) 2007
 *
 * Original author - Torren (torren@users.sourceforge.net)
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

//modrefreshfactory#planet#factory
public class ModRefreshFactoryCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Planet Name#Factory Name";

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

        String planetName;
        String factoryName;
        try {
            planetName = command.nextToken();
            factoryName = command.nextToken();
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("Improper format. Try: /c modrefreshfactory#planetname#factoryname",
                  Username,
                  true);
            return;
        }

        server.campaign.SPlanet p = (server.campaign.SPlanet) server.campaign.CampaignMain.cm.getData()
                                                                    .getPlanetByName(planetName);
        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("Could not find planet: " + planetName + ".", Username, true);
            return;
        }

        server.campaign.SUnitFactory uf = (server.campaign.SUnitFactory) server.campaign.CampaignMain.cm.getData()
                                                                               .getFactoryByName(p, factoryName);
        if (uf == null) {
            server.campaign.CampaignMain.cm.toUser("Could not find factory: " + factoryName + ".", Username, true);
            return;
        }

        int ticksToRemove = uf.getTicksUntilRefresh();
        String refresh = uf.addRefresh(-ticksToRemove, true);//use get and add instead of set b/c add sends HS update


        //send update to all players
        if (p.getOwner() != null) {
            server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers(p.getOwner(), "HS|" + refresh, false);
        }

        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " has refreshed factory " + uf.getName() + " on planet " + p.getName() + "!");
    }
}
