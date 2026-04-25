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

public class ChangeNameCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Old Name#New Name";

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

        //variables
        String oldName = "";
        String newName = "";

        try {
            oldName = command.nextToken();
            newName = command.nextToken();
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("Improper command. Try: /c changename#oldname#newname",
                  Username,
                  true);
            return;
        }


        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(oldName);
        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("Couldn't find player with name " + oldName, Username, true);
            return;
        }

        if (p.getDutyStatus() != server.campaign.SPlayer.STATUS_RESERVE) {
            server.campaign.CampaignMain.cm.toUser("You may only rename players who are in reserve.", Username, true);
            return;
        }

        //old player exists. nuke him in the faction, then re-add with a new name.
        server.campaign.CampaignMain.cm.doLogoutPlayer(oldName);//logout, for safety ...
        p.getMyHouse().removePlayer(p, false);//delete account. dont dupe the units.

        //delete old pfile
        java.io.File fp = new java.io.File("./campaign/players/" + p.getName().toLowerCase() + ".dat");
        if (fp.exists()) {fp.delete();}

        //change the name
        p.setName(newName);
        server.campaign.CampaignMain.cm.forceSavePlayer(p);

        server.campaign.CampaignMain.cm.toUser("You changed " + oldName + "'s name to '" + newName + "'.",
              Username,
              true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " changed " + oldName + "'s name to '" + newName + "'.");
        server.campaign.CampaignMain.cm.toUser(Username +
                                                     " changed your name from '" +
                                                     oldName +
                                                     "' name to '" +
                                                     newName +
                                                     "'. Quit and re-join " +
                                                     "for the change to take full effect.", oldName, true);

    }//end process()

}
