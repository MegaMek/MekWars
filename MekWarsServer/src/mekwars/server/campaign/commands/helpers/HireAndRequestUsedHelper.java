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
 * A simple helper which takes in data for 2 commands, and fires their process() methods. Used to provide html/links
 * which hire and buy used units simultaneousnly (previously had to present the users with 2 links, one for each command
 * operation).
 *
 * @urgru
 */

package mekwars.server.campaign.commands.helpers;

//imports

import mekwars.server.campaign.CampaignMain;

public class HireAndRequestUsedHelper implements server.campaign.commands.Command {

    int accessLevel = 0;
    String syntax = "";

    public String getSyntax() {return syntax;}

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

        //find out if your are using advanced repair if so buy bays instead of hiring techs.
        boolean useBays = CampaignMain.campaignMain.isUsingAdvanceRepair();

        //get number of techs to hire from the command
        String numtohire = command.nextToken();

        /*
         * Assemble a new StringTokenize which contains only the
         * Tokens needed by RequestDonatedCommand. Its safe to assume that
         * the Request would have failed initially if any of the
         * inputs were wrong, so just pass them along without checking.
         */
        String requestText = "";
        while (command.hasMoreTokens()) {requestText += command.nextToken() + "#";}

        java.util.StringTokenizer requestTokenizer = new java.util.StringTokenizer(requestText, "#");

        if (useBays) {
            //purchase bays
            server.campaign.commands.BuyBaysCommand buyCommand = new server.campaign.commands.BuyBaysCommand();
            buyCommand.process(new java.util.StringTokenizer(numtohire), Username);
        } else {
            //hire the techs
            server.campaign.commands.HireTechsCommand hireCommand = new server.campaign.commands.HireTechsCommand();
            hireCommand.process(new java.util.StringTokenizer(numtohire), Username);
        }

        //fire the request
        server.campaign.commands.RequestDonatedCommand requestCommand = new server.campaign.commands.RequestDonatedCommand();
        requestCommand.process(requestTokenizer, Username);

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}//end HireAndRequestNewHelper
