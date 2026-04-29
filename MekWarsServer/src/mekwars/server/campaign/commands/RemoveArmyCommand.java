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


import mekwars.server.campaign.SPlayer;

public class RemoveArmyCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

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

        //get the player
        SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);

        if (p == null) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Null player while removing army. Report this to an admin. Remove fails.",
                  Username,
                  true);
            return;
        }

        //get ID to remove
        int id = -1;
        try {
            id = Integer.parseInt(command.nextToken());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("AM:Improper usage. Try: /c removearmy#ID", Username, true);
            return;
        }

        //try to load the army, and check for a null
        server.campaign.SArmy toRemove = p.getArmy(id);
        if (toRemove == null) {
            server.campaign.CampaignMain.cm.toUser("AM:No army with that ID. Remove failed.", Username, true);
            return;
        }

        if (server.campaign.CampaignMain.cm.getOpsManager().getShortOpForPlayer(p) != null) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not modify your armies while in a game.",
                  Username,
                  true);
            return;
        }

        if (p.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE && toRemove.getAmountOfUnits() != 0) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not modify armies while active.", Username, true);
            return;
        }

        //break outs passed, so remove the army.
        p.removeArmy(id);
        p.resetWeightedArmyNumber();
        server.campaign.CampaignMain.cm.toUser("AM:Army #" + id + " was removed.", Username, true);


    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
