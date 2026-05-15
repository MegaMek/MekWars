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

import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;
import server.util.MWPasswd;

public class AdminPasswordCommand implements server.campaign.commands.Command {

    // conforming methods
    int accessLevel = server.MWChatServer.auth.IAuthenticator.REGISTERED;
    String syntax = "Admin Password Commands:<br>" +
                          " /c adminpassword#save - save the password file<br>" +
                          " /c adminpassword#remove#NAME - remove NAME's password<br>" +
                          " /c adminpassword#level#NAME#LEVEL - set a player's userlevel.<br>" +
                          " EXAMPLE: /c adminpassword#remove#urgru<br>" +
                          " EXAMPLE: /c adminpassword#level#urgru#200";

    public String getSyntax() {
        return syntax;
    }

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

        if (!command.hasMoreTokens()) {
            CampaignMain.campaignMain.toUser("Admin Password Commands:<br>" +
                                                   " /c adminpassword#save - save the password file<br>" +
                                                   " /c adminpassword#remove#NAME - remove NAME's password<br>" +
                                                   " /c adminpassword#level#NAME#LEVEL - set a player's userlevel.<br>" +
                                                   " EXAMPLE: /c adminpassword#remove#urgru<br>" +
                                                   " EXAMPLE: /c adminpassword#level#urgru#200", Username, true);
            return;
        }

        String action = command.nextToken();
        if (action.equalsIgnoreCase("save")) {
            try {
                MWPasswd.save();
                CampaignMain.campaignMain.toUser("Password-file saved!", Username, true);
                CampaignMain.campaignMain.doSendModMail("NOTE", Username + " has saved the password-file.");
            } catch (Exception ex) {
                CampaignMain.campaignMain.toUser("Problems saving password file!", Username, true);
            }
        } else if (action.equalsIgnoreCase("remove")) {
            String target = command.nextToken();
            MWPasswd.removeRecord(target);
            try {
                MWPasswd.save();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
            CampaignMain.campaignMain.toUser("Password for " + target + " removed!", Username, true);
            CampaignMain.campaignMain.doSendModMail("NOTE", Username + " has removed " + target + "'s password");
        } else if (action.equalsIgnoreCase("level")) {

            String target = command.nextToken();
            int level = Integer.parseInt(command.nextToken());
            server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(target);

            try {
                MWPasswd.getRecord(target).setAccess(level);
            } catch (Exception ex) {
                CampaignMain.campaignMain.toUser(target +
                                                       " is not registered. Have them register, then try again.",
                      Username);
                return;
            }

            try {
                CampaignMain.campaignMain.getServer().getClient(target).setAccessLevel(level);
                CampaignMain.campaignMain.getServer().getUser(target).setLevel(level);
                CampaignMain.campaignMain.getServer().sendRemoveUserToAll(target, false);
                CampaignMain.campaignMain.getServer().sendNewUserToAll(target, false);
                MWPasswd.writeRecord(p.getPassword(), target);

                if (p != null) {
                    CampaignMain.campaignMain.doSendToAllOnlinePlayers("PI|DA|" +
                                                                             CampaignMain.campaignMain.getPlayerUpdateString(
                                                                                   p), false);
                }

            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }

            CampaignMain.campaignMain.toUser("Level for " + target + " set to " + level + "!", Username, true);
            CampaignMain.campaignMain.doSendModMail("NOTE",
                  Username + " has set " + target + "'s level to " + level);
        }
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}
