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

public class ChangeNameCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Old Name#New Name";

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

        //variables
        String oldName = "";
        String newName = "";

        try {
            oldName = command.nextToken();
            newName = command.nextToken();
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("Improper command. Try: /c changename#oldname#newname",
                  Username,
                  true);
            return;
        }


        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(oldName);
        if (p == null) {
            CampaignMain.campaignMain.toUser("Couldn't find player with name " + oldName, Username, true);
            return;
        }

        if (p.getDutyStatus() != server.campaign.SPlayer.STATUS_RESERVE) {
            CampaignMain.campaignMain.toUser("You may only rename players who are in reserve.", Username, true);
            return;
        }

        //old player exists. nuke him in the faction, then re-add with a new name.
        CampaignMain.campaignMain.doLogoutPlayer(oldName);//logout, for safety ...
        p.getMyHouse().removePlayer(p, false);//delete account. dont dupe the units.

        //delete old pfile
        java.io.File fp = new java.io.File("./campaign/players/" + p.getName().toLowerCase() + ".dat");
        if (fp.exists()) {fp.delete();}

        //change the name
        p.setName(newName);
        CampaignMain.campaignMain.forceSavePlayer(p);

        CampaignMain.campaignMain.toUser("You changed " + oldName + "'s name to '" + newName + "'.",
              Username,
              true);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " changed " + oldName + "'s name to '" + newName + "'.");
        CampaignMain.campaignMain.toUser(Username +
                                               " changed your name from '" +
                                               oldName +
                                               "' name to '" +
                                               newName +
                                               "'. Quit and re-join " +
                                               "for the change to take full effect.", oldName, true);

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}
