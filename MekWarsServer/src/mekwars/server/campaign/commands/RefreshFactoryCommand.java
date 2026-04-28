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

//refreshfactory#planet#factory#useflu(true/false)
public class RefreshFactoryCommand implements Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.GUEST;
    String syntax = "";

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

        server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);
        String planetName;
        String factoryName;
        Boolean useFlu = false; //@salient

        try {
            planetName = command.nextToken();
            factoryName = command.nextToken();
            if (command.hasMoreTokens()) {useFlu = Boolean.parseBoolean(command.nextToken());}
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Improper format. Try: /c refreshfactory#planetname#factoryname#useflu(true/false)",
                  Username,
                  true);
            return;
        }

        if (!server.campaign.CampaignMain.cm.getBooleanConfig("AllowFactoryRefreshForRewards") && !useFlu) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not use " +
                                                         server.campaign.CampaignMain.cm.getConfig("RPShortName") +
                                                         " to refresh a factory on this server.", Username, true);
            return;
        }

        if (server.campaign.CampaignMain.cm.getIntegerConfig("FluToRefreshFactory") == 0 && useFlu) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not use " +
                                                         server.campaign.CampaignMain.cm.getConfig("FluShortName") +
                                                         " to refresh a factory on this server.", Username, true);
            return;
        }

        server.campaign.SPlanet p = (server.campaign.SPlanet) server.campaign.CampaignMain.cm.getData()
                                                                    .getPlanetByName(planetName);
        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Could not find planet: " + planetName + ".", Username, true);
            return;
        }

        server.campaign.SUnitFactory uf = (server.campaign.SUnitFactory) server.campaign.CampaignMain.cm.getData()
                                                                               .getFactoryByName(p, factoryName);
        if (uf == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Could not find factory: " + factoryName + ".", Username, true);
            return;
        }

        int rpCost = server.campaign.CampaignMain.cm.getIntegerConfig("RewardPointToRefreshFactory");
        int fluCost = server.campaign.CampaignMain.cm.getIntegerConfig("FluToRefreshFactory");
        int playerRP = player.getReward();
        int playerFlu = player.getInfluence();

        if (playerRP < rpCost && !useFlu) {
            server.campaign.CampaignMain.cm.toUser(rpCost +
                                                         " " +
                                                         server.campaign.CampaignMain.cm.getConfig("RPLongName") +
                                                         " required to refresh " +
                                                         uf.getName() +
                                                         ". You only have " +
                                                         playerRP +
                                                         ".", Username, true);
            return;
        }

        if (playerFlu < fluCost && useFlu) {
            server.campaign.CampaignMain.cm.toUser(fluCost +
                                                         " " +
                                                         server.campaign.CampaignMain.cm.getConfig("FluLongName") +
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

        server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers(player.getMyHouse(), "HS|" + refresh, false);

        server.campaign.CampaignMain.cm.toUser("AM:You refreshed " + uf.getName() + " on planet " + p.getName(),
              Username,
              true);
        server.campaign.CampaignMain.cm.doSendHouseMail(player.getMyHouse(),
              "NOTE",
              player.getName() + " refreshed " + uf.getName() + " on planet " + p.getName());
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
