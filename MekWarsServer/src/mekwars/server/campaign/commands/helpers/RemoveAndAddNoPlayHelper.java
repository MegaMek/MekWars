/*
 * MekWars - Copyright (C) 2005
 *
 * Original Author - Nathan Morris (urgru)
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
 * A simple helper which takes in data for 2 commands, and fires their process() methods. Used to provide html/link
 * which removes one no-play entry and adds another simultaneousnly (previously had to present the users with 2 links,
 * one for each command operation).
 *
 * @urgru
 */

package mekwars.server.campaign.commands.helpers;

//imports

public class RemoveAndAddNoPlayHelper implements server.campaign.commands.Command {

    //process the command.
    int accessLevel = 0;

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    String syntax = "";

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

        String nameToRemove = "remove#" + command.nextToken() + "#CONFIRM";
        String nameToAdd = "add#" + command.nextToken() + "#CONFIRM";

        java.util.StringTokenizer removeTokenizer = new java.util.StringTokenizer(nameToRemove, "#");
        java.util.StringTokenizer addTokenizer = new java.util.StringTokenizer(nameToAdd, "#");

        //do the remove
        server.campaign.commands.NoPlayCommand noplayCommand = new server.campaign.commands.NoPlayCommand();
        noplayCommand.process(removeTokenizer, Username);

        //then the add
        noplayCommand.process(addTokenizer, Username);

    }//end process()

}//end HireAndRequestNewHelper
