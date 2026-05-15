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

public class TransferMoneyCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(Username);
        server.campaign.SHouse house = player.getMyHouse();

        if (player.getMyHouse().isNewbieHouse()) {
            CampaignMain.campaignMain.toUser("AM:You may not transfer " +
                                                   house.getConfig("MoneyLongName") +
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
            CampaignMain.campaignMain.toUser("AM:Improper format. Try: /c transferunit#TargetPlayer#UnitID",
                  Username,
                  true);
            return;
        }

        server.campaign.SPlayer targetplayer = CampaignMain.campaignMain.getPlayer(targetPlayer);
        if (targetplayer == null) {
            CampaignMain.campaignMain.toUser("AM:Could not find a player named " + targetPlayer + ".",
                  Username,
                  true);
            return;
        }

        //no negative amounts
        if (amount < 1) {
            CampaignMain.campaignMain.toUser("AM:You must transfer at least 1 " +
                                                   house.getConfig("MoneyLongName") +
                                                   ".", Username, true);
            return;
        }

        // check for same-ip interaction
        boolean ipcheck = Boolean.parseBoolean(house.getConfig("IPCheck"));
        if (ipcheck && CampaignMain.campaignMain.getServer().getIP(player.getName()).toString().equals(
              CampaignMain.campaignMain.getServer().getIP(targetplayer.getName()).toString())) {
            CampaignMain.campaignMain.toUser("AM:" +
                                                   targetplayer.getName() +
                                                   " has the same IP as you do. You can't send them " +
                                                   house.getConfig("MoneyLongName"), Username, true);
            return;
        }

        // if the player is neither in the faction of the target, nor fighting for that faction
        if (!targetplayer.getHouseFightingFor().equals(player.getMyHouse()) &&
                  !targetplayer.getMyHouse().equals(player.getMyHouse())) {
            CampaignMain.campaignMain.toUser("AM:" +
                                                   targetplayer.getName() +
                                                   " is not from your faction! You can't send them " +
                                                   house.getConfig("MoneyLongName"), Username, true);
            return;
        }

        // welfare check
        if (player.getMoney() - Integer.parseInt(house.getConfig("WelfareCeiling")) <= amount) {
            CampaignMain.campaignMain.toUser("AM:You may not send that much money!", Username, true);
            return;
        }

        //do the transfer
        player.addMoney(-amount);
        targetplayer.addMoney(amount);
        CampaignMain.campaignMain.toUser("AM:You've transferred " +
                                               CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                     true,
                                                     amount) +
                                               " to " +
                                               targetplayer.getName(), Username, true);
        CampaignMain.campaignMain.toUser("AM:" +
                                               player.getName() +
                                               " sends you " +
                                               CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                     true,
                                                     amount) +
                                               ".", targetPlayer, true);

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
