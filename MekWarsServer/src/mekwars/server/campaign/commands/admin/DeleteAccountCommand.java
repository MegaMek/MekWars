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

public class DeleteAccountCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Name#ScrapUnits[true/false]";

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

        //vars
        server.campaign.SPlayer p = null;
        boolean scrapUnits;

        try {
            p = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());
            scrapUnits = Boolean.parseBoolean(command.nextToken());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("Improper command. Try: /c deleteaccount#Name#ScrapUnits",
                  Username,
                  true);
            return;
        }

        if (p == null) {
            server.campaign.CampaignMain.cm.toUser("Couldn't find a player with that name.", Username, true);
            return;
        }

        if (p.getDutyStatus() > server.campaign.SPlayer.STATUS_RESERVE) {
            server.campaign.CampaignMain.cm.toUser("Fighting and active players may not be deleted.", Username, true);
            return;
        }

        //non-null player, delete the account
        p.getMyHouse().removePlayer(p, !scrapUnits);

        //delete the pfile
        java.io.File fp = new java.io.File("./campaign/players/" + p.getName().toLowerCase() + ".dat");
        if (fp.exists()) {fp.delete();}

        server.campaign.CampaignMain.cm.toUser("You deleted " + p.getName() + "'s account.", Username, true);
        server.campaign.CampaignMain.cm.toUser(Username + " deleted your account.", p.getName(), true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " deleted " + p.getName() + "'s account.");
        //server.MWLogger.modLog(Username + " deleted " + p.getName() + "'s account.");
        server.campaign.CampaignMain.cm.doLogoutPlayer(p.getName(), false);  //Baruk Khazad! 20151110
        if (server.campaign.CampaignMain.cm.getServer()
                  .getClient(server.MWChatServer.MWChatServer.clientKey(p.getName())) != null) {
            server.campaign.CampaignMain.cm.getServer().killClient(p.getName(), Username);
        }

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}
