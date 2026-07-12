/*
 * MekWars - Copyright (C) 2007
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - JTighe (Torren@users.sourceforge.net)
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

public class SetAutoReorderCommand implements Command {

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

        if (!CampaignMain.campaignMain.getBooleanConfig("UsePartsRepair")) {return;}

        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);

        try {
            p.setAutoReorder(Boolean.parseBoolean(command.nextToken()));
            CampaignMain.campaignMain.toUser("PL|ROP|" + p.getAutoReorder(), Username, false);
        }//end try
        catch (Exception ex) {
            CampaignMain.campaignMain.toUser(
                  "AM:SetAutoAutoReorder command failed. Check your input. It should be something like this: /c setAutoReorder#True/False",
                  Username);
            return;
        }//end catch

        CampaignMain.campaignMain.toUser("AM:Auto Reorder set.", Username, true);

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}//end SetAutoEjectCommand class

