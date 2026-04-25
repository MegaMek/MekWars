/*
 * MekWars - Copyright (C) 2004
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
 * A simple helper which takes in data for 2 commands, and fires their process() methods. Used to provide html/links
 * which hire and maintain units simultaneousnly (previously had to present the users with 2 links, one for each command
 * operation).
 *
 * @urgru
 */

package mekwars.server.campaign.commands.helpers;

//imports

public class HireAndMaintainHelper implements server.campaign.commands.Command {

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

        //get things from the command.
        String numtohire = command.nextToken();
        String numtoset = command.nextToken();

        server.campaign.commands.HireTechsCommand hireCommand = new server.campaign.commands.HireTechsCommand();
        hireCommand.process(new java.util.StringTokenizer(numtohire), Username);

        server.campaign.commands.SetMaintainedCommand setMaintainedCommand = new server.campaign.commands.SetMaintainedCommand();
        setMaintainedCommand.process(new java.util.StringTokenizer(numtoset), Username);
    }//end process()

}//end HireAndMaintainHelper()
