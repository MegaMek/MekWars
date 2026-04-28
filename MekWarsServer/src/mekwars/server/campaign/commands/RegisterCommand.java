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
        if (server.campaign.CampaignMain.cm.getPlayer(Username) == null) {
            server.campaign.CampaignMain.cm.toUser(
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
                player = server.campaign.CampaignMain.cm.getPlayer(regname);
                if (player.getPassword() != null && player.getPassword().access >= 2) {regged = true;}
            } catch (Exception ex) {
                //Username already registered, ignore error.
                //MWLogger.errLog(ex);
                regged = true;
            }

            if (regged && !server.campaign.CampaignMain.cm.getServer().isAdmin(Username)) {
                server.campaign.CampaignMain.cm.toUser("AM:Nickname \"" + regname + "\" is already registered!",
                      Username);
                //MWLogger.modLog(Username + " tried to register the nickname \"" + regname + "\", which was already registered.");
                server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                      Username + " tried to register the nickname \"" + regname + "\", which was already registered.");
                return;
            }

            //check passwd length
            if (pw.length() < 3 && pw.length() > 11) {
                server.campaign.CampaignMain.cm.toUser("AM:Passwords must be between 4 and 10 characters!", Username);
                return;
            }

            //change userlevel
            int level = -1;
            if (server.campaign.CampaignMain.cm.getServer().isAdmin(Username)) {
                MWPasswd.writeRecord(regname, server.MWChatServer.auth.IAuthenticator.ADMIN, pw);
                level = server.MWChatServer.auth.IAuthenticator.ADMIN;
            } else {
                MWPasswd.writeRecord(regname, server.MWChatServer.auth.IAuthenticator.REGISTERED, pw);
                level = server.MWChatServer.auth.IAuthenticator.REGISTERED;
            }

            //send the userlevel change to all players
            server.campaign.CampaignMain.cm.getServer().getClient(regname).setAccessLevel(level);
            server.campaign.CampaignMain.cm.getServer().getUser(regname).setLevel(level);
            server.campaign.CampaignMain.cm.getServer().sendRemoveUserToAll(regname, false);
            server.campaign.CampaignMain.cm.getServer().sendNewUserToAll(regname, false);

            if (player != null) {
                server.campaign.CampaignMain.cm.doSendToAllOnlinePlayers("PI|DA|" +
                                                                               server.campaign.CampaignMain.cm.getPlayerUpdateString(
                                                                                     player), false);
            }

            //acknowledge registration
            server.campaign.CampaignMain.cm.toUser("AM:\"" + regname + "\" successfully registered.", Username);
            MWLogger.modLog("New nickname registered: " + regname);
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
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
