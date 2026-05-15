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

public class DeleteAccountCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Name#ScrapUnits[true/false]";

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

        //vars
        server.campaign.SPlayer p = null;
        boolean scrapUnits;

        try {
            p = CampaignMain.campaignMain.getPlayer(command.nextToken());
            scrapUnits = Boolean.parseBoolean(command.nextToken());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("Improper command. Try: /c deleteaccount#Name#ScrapUnits",
                  Username,
                  true);
            return;
        }

        if (p == null) {
            CampaignMain.campaignMain.toUser("Couldn't find a player with that name.", Username, true);
            return;
        }

        if (p.getDutyStatus() > server.campaign.SPlayer.STATUS_RESERVE) {
            CampaignMain.campaignMain.toUser("Fighting and active players may not be deleted.", Username, true);
            return;
        }

        //non-null player, delete the account
        p.getMyHouse().removePlayer(p, !scrapUnits);

        //delete the pfile
        java.io.File fp = new java.io.File("./campaign/players/" + p.getName().toLowerCase() + ".dat");
        if (fp.exists()) {fp.delete();}

        CampaignMain.campaignMain.toUser("You deleted " + p.getName() + "'s account.", Username, true);
        CampaignMain.campaignMain.toUser(Username + " deleted your account.", p.getName(), true);
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " deleted " + p.getName() + "'s account.");
        //server.MWLogger.modLog(Username + " deleted " + p.getName() + "'s account.");
        CampaignMain.campaignMain.doLogoutPlayer(p.getName(), false);  //Baruk Khazad! 20151110
        if (CampaignMain.campaignMain.getServer()
                  .getClient(server.MWChatServer.MWChatServer.clientKey(p.getName())) != null) {
            CampaignMain.campaignMain.getServer().killClient(p.getName(), Username);
        }

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}
