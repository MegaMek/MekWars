/*
 * MekWars - Copyright (C) 2005
 *
 * Original Author - Nathan Morris (urgru@users.sourceforge.net)
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

public class AdminDonateCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

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


        String targetName = "name#unitid";
        server.campaign.SPlayer target = null;
        int unitID = -1;
        server.campaign.SUnit m = null;

        try {
            targetName = (String) command.nextElement();
            target = server.campaign.CampaignMain.cm.getPlayer(targetName);
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("Improper format. Try: /c admindonate#name#unitid", Username, true);
            return;
        }

        if (target == null) {
            server.campaign.CampaignMain.cm.toUser("Target player could not be found. Try again.", Username, true);
            return;
        }

        //non null target, so attempt the scrap
        try {
            unitID = Integer.parseInt((String) command.nextElement());
            m = target.getUnit(unitID);
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("Improper format. Try: /c admindonate#name#unitid", Username, true);
            return;
        }

        //break out if the player doesn't have a unit with that id
        if (m == null) {
            server.campaign.CampaignMain.cm.toUser("Target player doesn't have a unit with ID# " + unitID + ".",
                  Username,
                  true);
            return;
        }

        //tell the user about the scrap
        server.campaign.CampaignMain.cm.toUser("You forced " +
                                                     targetName +
                                                     " to donate his " +
                                                     m.getModelName() +
                                                     " (ID#" +
                                                     m.getId() +
                                                     ").", Username, true);
        server.campaign.CampaignMain.cm.toUser(Username +
                                                     " forced you to donate your " +
                                                     m.getModelName() +
                                                     " (ID#" +
                                                     m.getId() +
                                                     ")", targetName, true);
        //server.MWLogger.modLog(Username + " forced + " + targetName + " to donate his " + m.getModelName() + " (ID#" + m.getId() + ").");
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " forced " + targetName + " to donate his " + m.getModelName() + " (ID#" + m.getId() + ").");

        //and then do it ...
        target.removeUnit(unitID, true);
        target.getMyHouse().addUnit(m, true);
    }
}//end AdminDonateCommand
