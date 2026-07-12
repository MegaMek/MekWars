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

import mekwars.server.campaign.CampaignMain;

//refreshfactory#planet#factory#useflu(true/false)
public class RefreshFactoryCommand implements Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.GUEST;
    String syntax = "";

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

        server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
        String planetName;
        String factoryName;
        Boolean useFlu = false; //@salient

        try {
            planetName = command.nextToken();
            factoryName = command.nextToken();
            if (command.hasMoreTokens()) {useFlu = Boolean.parseBoolean(command.nextToken());}
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser(
                  "AM:Improper format. Try: /c refreshfactory#planetname#factoryname#useflu(true/false)",
                  Username,
                  true);
            return;
        }

        if (!CampaignMain.campaignMain.getBooleanConfig("AllowFactoryRefreshForRewards") && !useFlu) {
            CampaignMain.campaignMain.toUser("AM:You may not use " +
                                                   CampaignMain.campaignMain.getConfig("RPShortName") +
                                                   " to refresh a factory on this server.", Username, true);
            return;
        }

        if (CampaignMain.campaignMain.getIntegerConfig("FluToRefreshFactory") == 0 && useFlu) {
            CampaignMain.campaignMain.toUser("AM:You may not use " +
                                                   CampaignMain.campaignMain.getConfig("FluShortName") +
                                                   " to refresh a factory on this server.", Username, true);
            return;
        }

        server.campaign.SPlanet p = (server.campaign.SPlanet) CampaignMain.campaignMain.getData()
                                                                    .getPlanetByName(planetName);
        if (p == null) {
            CampaignMain.campaignMain.toUser("AM:Could not find planet: " + planetName + ".", Username, true);
            return;
        }

        server.campaign.SUnitFactory uf = (server.campaign.SUnitFactory) CampaignMain.campaignMain.getData()
                                                                               .getFactoryByName(p, factoryName);
        if (uf == null) {
            CampaignMain.campaignMain.toUser("AM:Could not find factory: " + factoryName + ".", Username, true);
            return;
        }

        int rpCost = CampaignMain.campaignMain.getIntegerConfig("RewardPointToRefreshFactory");
        int fluCost = CampaignMain.campaignMain.getIntegerConfig("FluToRefreshFactory");
        int playerRP = player.getReward();
        int playerFlu = player.getInfluence();

        if (playerRP < rpCost && !useFlu) {
            CampaignMain.campaignMain.toUser(rpCost +
                                                   " " +
                                                   CampaignMain.campaignMain.getConfig("RPLongName") +
                                                   " required to refresh " +
                                                   uf.getName() +
                                                   ". You only have " +
                                                   playerRP +
                                                   ".", Username, true);
            return;
        }

        if (playerFlu < fluCost && useFlu) {
            CampaignMain.campaignMain.toUser(fluCost +
                                                   " " +
                                                   CampaignMain.campaignMain.getConfig("FluLongName") +
                                                   " required to refresh " +
                                                   uf.getName() +
                                                   ". You only have " +
                                                   playerFlu +
                                                   ".", Username, true);
            return;
        }

        if (!useFlu) {player.addReward(-rpCost);} else {player.addInfluence(-fluCost);}

        int ticksToRemove = uf.getTicksUntilRefresh();
        String refresh = uf.addRefresh(-ticksToRemove, true);//use get and add instead of set b/c add sends HS update

        CampaignMain.campaignMain.doSendToAllOnlinePlayers(player.getMyHouse(), "HS|" + refresh, false);

        CampaignMain.campaignMain.toUser("AM:You refreshed " + uf.getName() + " on planet " + p.getName(),
              Username,
              true);
        CampaignMain.campaignMain.doSendHouseMail(player.getMyHouse(),
              "NOTE",
              player.getName() + " refreshed " + uf.getName() + " on planet " + p.getName());
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
