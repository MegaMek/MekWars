/*
 * MekWars - Copyright (C) 200
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

/**
 * @author Salient
 */

package mekwars.server.campaign.commands;


public class TransferInfluenceCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }

        server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);
        server.campaign.SHouse house = player.getMyHouse();

        if (player.getMyHouse().isNewbieHouse()) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not transfer " +
                                                         server.campaign.CampaignMain.cm.getConfig("FluLongName") +
                                                         " while in a training faction.", Username, true);
            return;
        }

        //Acquire needed Data
        String targetPlayer;
        int amount;

        try {
            targetPlayer = (String) command.nextElement();
            amount = Integer.parseInt((String) command.nextElement());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Improper format. Try: /c transferinfluencepoints#TargetPlayer#amount",
                  Username,
                  true);
            return;
        }

        server.campaign.SPlayer targetplayer = server.campaign.CampaignMain.cm.getPlayer(targetPlayer);

        if (targetplayer == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Could not find a player named " + targetPlayer + ".",
                  Username,
                  true);
            return;
        }

        //no negative amounts
        if (amount < 1) {
            server.campaign.CampaignMain.cm.toUser("AM:You must transfer at least 1 " +
                                                         server.campaign.CampaignMain.cm.getConfig("FluLongName") +
                                                         ".", Username, true);
            return;
        }

        // check for same-ip interaction
        boolean ipcheck = Boolean.parseBoolean(house.getConfig("IPCheck"));
        if (ipcheck && server.campaign.CampaignMain.cm.getServer().getIP(player.getName()).toString().equals(
              server.campaign.CampaignMain.cm.getServer().getIP(targetplayer.getName()).toString())) {
            server.campaign.CampaignMain.cm.toUser("AM:" +
                                                         targetplayer.getName() +
                                                         " has the same IP as you do. You can't send them " +
                                                         server.campaign.CampaignMain.cm.getConfig("FluLongName") +
                                                         ".", Username, true);
            return;
        }

        // if the player is neither in the faction of the target, nor fighting for that faction
        if (!targetplayer.getHouseFightingFor().equals(player.getMyHouse()) &&
                  !targetplayer.getMyHouse().equals(player.getMyHouse())) {
            server.campaign.CampaignMain.cm.toUser("AM:" +
                                                         targetplayer.getName() +
                                                         " is not from your faction! You can't send them " +
                                                         server.campaign.CampaignMain.cm.getConfig("FluLongName") +
                                                         ".", Username, true);
            return;
        }

        if (!Boolean.parseBoolean(server.campaign.CampaignMain.cm.getConfig("AllowFluTransfer"))) {
            server.campaign.CampaignMain.cm.toUser("AM:This feature has been disabled by the server operators.",
                  Username,
                  true);
            return;
        }

        //do the transfer
        player.addInfluence(-amount);
        targetplayer.addInfluence(amount);
        server.campaign.CampaignMain.cm.toUser("AM:You've transferred " +
                                                     amount +
                                                     " " +
                                                     server.campaign.CampaignMain.cm.getConfig("FluLongName") +
                                                     " to " +
                                                     targetplayer.getName(), Username, true);
        server.campaign.CampaignMain.cm.toUser("AM:" +
                                                     player.getName() +
                                                     " sends you " +
                                                     amount +
                                                     " " +
                                                     server.campaign.CampaignMain.cm.getConfig("FluLongName") +
                                                     ".", targetPlayer, true);

    }
}
