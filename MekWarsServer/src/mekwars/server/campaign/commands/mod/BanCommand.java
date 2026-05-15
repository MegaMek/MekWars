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

package mekwars.server.campaign.commands.mod;


import mekwars.server.campaign.CampaignMain;

/**
 * Moving the Ban command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c Ban
 */
public class BanCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Type Ban with No Arguments for Syntax";

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

        try {

            long minute = 60000;
            long hour = minute * 60;
            long day = hour * 24;
            long week = day * 7;
            long month = day * 30;
            long year = day * 365;
            long howlong = year;
            String timeName = "minutes";

            String toKill = command.nextToken().trim();
            String banTime = "";
            if (command.hasMoreTokens()) {
                banTime = command.nextToken();
                if (banTime.equalsIgnoreCase("perm")) {
                    howlong = Long.MAX_VALUE / 1000;
                    timeName = "permanently";
                } else {
                    String banValue = banTime.substring(banTime.length() - 1);
                    if (banValue.trim().equalsIgnoreCase("h")) {
                        howlong = Long.parseLong(banTime.substring(0, banTime.length() - 1)) * hour;
                        if (howlong / hour == 1) {timeName = "for 1 hour";} else {
                            timeName = "for " + Long.toString(howlong / hour) + " hours";
                        }
                    } else if (banValue.trim().equalsIgnoreCase("d")) {
                        howlong = Long.parseLong(banTime.substring(0, banTime.length() - 1)) * day;
                        if (howlong / day == 1) {timeName = "for 1 day";} else {
                            timeName = "for " + Long.toString(howlong / day) + " days";
                        }
                    } else if (banValue.trim().equalsIgnoreCase("w")) {
                        howlong = Long.parseLong(banTime.substring(0, banTime.length() - 1)) * week;
                        if (howlong / week == 1) {timeName = "for 1 week";} else {
                            timeName = "for " + Long.toString(howlong / week) + " weeks";
                        }

                    } else if (banValue.trim().equalsIgnoreCase("m")) {
                        howlong = Long.parseLong(banTime.substring(0, banTime.length() - 1)) * month;
                        if (howlong / month == 1) {timeName = "for 1 month";} else {
                            timeName = "for " + Long.toString(howlong / month) + " months";
                        }
                    } else if (banValue.trim().equalsIgnoreCase("y")) {
                        howlong = Long.parseLong(banTime.substring(0, banTime.length() - 1)) * year;
                        if (howlong / year == 1) {timeName = "for 1 year";} else {
                            timeName = "for " + Long.toString(howlong / year) + " years";
                        }
                    } else {
                        howlong = Long.parseLong(banTime) * minute;
                        if (howlong / minute == 1) {timeName = "for 1 minute";} else {
                            timeName = "for " + Long.toString(howlong / minute) + " minutes";
                        }
                    }

                }
            } else {
                howlong = year;
                timeName = "year";
            }

            if (CampaignMain.campaignMain.getServer().isAdmin(toKill) && !Username.startsWith("[Dedicated]")) {
                CampaignMain.campaignMain.toUser("AM:You may not ban an admin.", Username);
                return;
            }

            //howlong = Long.parseLong(str.nextToken());
            java.net.InetAddress banip = CampaignMain.campaignMain.getServer().getIP(toKill);
            if (banip.equals(java.net.InetAddress.getLocalHost())) {banip = null;}

            long until = System.currentTimeMillis() + howlong;
            CampaignMain.campaignMain.toUser("AM:You were banned by " + Username + " " + timeName + ".", toKill);
            CampaignMain.campaignMain.toUser("PL|GBB|Banned!", toKill, false);

            try {
                Thread.sleep(125);
            } catch (Exception ex) {}

            if (banip != null) {CampaignMain.campaignMain.getServer().getBanIps().put(banip, until);}
            CampaignMain.campaignMain.getServer()
                  .getBanAccounts()
                  .put(toKill.toLowerCase(), Long.toString(until));

            //CampaignMain.cm.getServer().ISPlog.put(CampaignMain.cm.getServer().myCommunicator.getMmClient(toKill).getClientVersion(),until);
            //retreiveISPS(until,toKill);
            CampaignMain.campaignMain.getServer().bansUpdate();
            //MWLogger.modLog(Username + " banned " + toKill + " " +timeName+".");
            CampaignMain.campaignMain.getServer()
                  .sendChat("AM:" + Username + " banned " + toKill + " " + timeName + ".");

            CampaignMain.campaignMain.getOpsManager().doDisconnectCheckOnPlayer(toKill);
            CampaignMain.campaignMain.getServer().getCampaign().doLogoutPlayer(toKill);

            if (CampaignMain.campaignMain.getServer().getClient(toKill) != null) {
                CampaignMain.campaignMain.getServer().killClient(toKill, Username);
            }
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("AM:Incorrect Syntax: Syntax is as follows<br>/ban playername#time" +
                                                   "<br>Please note the time argument can be left off and will default to 1 year" +
                                                   "<br>also note that the time argument can be made in different ways" +
                                                   "<br>/ban playername#10 (player is banned for 10 mins)" +
                                                   "<br>/ban playername#10h (player is banned for 10 hours)" +
                                                   "<br>/ban playername#10d (player is banned for 10 days)" +
                                                   "<br>/ban playername#10w (player is banned for 10 weeks)" +
                                                   "<br>/ban playername#10m (player is banned for 10 months)" +
                                                   "<br>/ban playername#10y (player is banned for 10 years)" +
                                                   "<br>/ban playername#perm (player is banned for a very long time)",
                  Username);
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
