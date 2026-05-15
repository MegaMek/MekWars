/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - Jason Tighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package mekwars.server.campaign.commands;

import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;
import server.util.MWPasswd;


/**
 * Moving the Register command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c Register#Name,Password
 */
public class RegisterCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        /*
         * Never check access level for register, but DO check
         * to ensure that a player is enrolled in the campaign.
         */
        if (CampaignMain.campaignMain.getPlayer(Username) == null) {
            CampaignMain.campaignMain.toUser(
                  "<font color=\"navy\"><br>---<br>You must have a campaign account in order to register a nickname. [<a href=\"MEKWARS/c enroll\">Click to get started</a>]<br>---<br></font>",
                  Username,
                  true);
            return;
        }

        try {
            java.util.StringTokenizer str = new java.util.StringTokenizer(command.nextToken(), ",");
            String regname = "";
            String pw = "";
            server.campaign.SPlayer player = null;

            try {
                regname = str.nextToken().trim().toLowerCase();
                pw = str.nextToken();
            } catch (Exception ex) {
                MWLogger.errLog("Failure to register: " + regname);
                return;
            }


            //Check to see if the Username is already registered
            boolean regged = false;
            try {
                //MWPasswd.getRecord(regname, null);
                player = CampaignMain.campaignMain.getPlayer(regname);
                if (player.getPassword() != null && player.getPassword().access >= 2) {regged = true;}
            } catch (Exception ex) {
                //Username already registered, ignore error.
                //MWLogger.errLog(ex);
                regged = true;
            }

            if (regged && !CampaignMain.campaignMain.getServer().isAdmin(Username)) {
                CampaignMain.campaignMain.toUser("AM:Nickname \"" + regname + "\" is already registered!",
                      Username);
                //MWLogger.modLog(Username + " tried to register the nickname \"" + regname + "\", which was already registered.");
                CampaignMain.campaignMain.doSendModMail("NOTE",
                      Username + " tried to register the nickname \"" + regname + "\", which was already registered.");
                return;
            }

            //check passwd length
            if (pw.length() < 3 && pw.length() > 11) {
                CampaignMain.campaignMain.toUser("AM:Passwords must be between 4 and 10 characters!", Username);
                return;
            }

            //change userlevel
            int level = -1;
            if (CampaignMain.campaignMain.getServer().isAdmin(Username)) {
                MWPasswd.writeRecord(regname, server.MWChatServer.auth.IAuthenticator.ADMIN, pw);
                level = server.MWChatServer.auth.IAuthenticator.ADMIN;
            } else {
                MWPasswd.writeRecord(regname, server.MWChatServer.auth.IAuthenticator.REGISTERED, pw);
                level = server.MWChatServer.auth.IAuthenticator.REGISTERED;
            }

            //send the userlevel change to all players
            CampaignMain.campaignMain.getServer().getClient(regname).setAccessLevel(level);
            CampaignMain.campaignMain.getServer().getUser(regname).setLevel(level);
            CampaignMain.campaignMain.getServer().sendRemoveUserToAll(regname, false);
            CampaignMain.campaignMain.getServer().sendNewUserToAll(regname, false);

            if (player != null) {
                CampaignMain.campaignMain.doSendToAllOnlinePlayers("PI|DA|" +
                                                                         CampaignMain.campaignMain.getPlayerUpdateString(
                                                                               player), false);
            }

            //acknowledge registration
            CampaignMain.campaignMain.toUser("AM:\"" + regname + "\" successfully registered.", Username);
            MWLogger.modLog("New nickname registered: " + regname);
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  "New nickname registered: " + regname + " by: " + Username);

        } catch (Exception e) {
            MWLogger.errLog(e);
            MWLogger.errLog("^ Not supposed to happen! ^");
            MWLogger.errLog(e);
            MWLogger.errLog("Not supposed to happen");
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
