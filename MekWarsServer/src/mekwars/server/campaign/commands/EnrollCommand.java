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

package mekwars.server.campaign.commands;


import common.House;
import common.util.MWLogger;

public class EnrollCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        /*
         * Never check access level for Enroll.
         */

        //don't let stock names enroll
        if (Username.startsWith("Nobody")) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Nobodies are not allowed to enroll. If you're signing on for the " +
                        "first time and were labelled a Nobody, you're probably using a name someone else " +
                        "has already registered. If you've registered this name previously, you're either not " +
                        "sending a password or sending an incorrect password. Try changing/removing your password in " +
                        "the configuration menu and connecting again.",
                  Username,
                  true);
            return;
        }

        if (Username.equalsIgnoreCase("DRAW")) {
            server.campaign.CampaignMain.cm.toUser("AM:The name DRAW is reserved for system use. Try another name.",
                  Username,
                  true);
            return;

        }

        if (Username.startsWith("[Dedicated]")) {
            server.campaign.CampaignMain.cm.toUser("AM:Dedicated hosts may not enroll in the campaign.",
                  Username,
                  true);
            return;
        }

        /*
         * Do not allow players to enroll with campaign faction  names. This would cause
         * problems with the Market and with various admin commands (scrap, transfer, etc).
         */
        for (House currFaction : server.campaign.CampaignMain.cm.getData().getAllHouses()) {
            if (Username.equalsIgnoreCase(currFaction.getName())) {
                server.campaign.CampaignMain.cm.toUser(
                      "AM:You may not enroll in the campaign using the name of an existing faction.",
                      Username,
                      true);
                return;
            }
        }

        //reserve "SERVER" as a PM and BM name
        if (Username.trim().equalsIgnoreCase("SERVER")) {
            server.campaign.CampaignMain.cm.toUser("AM:The name SERVER is reserved for system use. Try another name.",
                  Username,
                  true);
            return;
        }

        /*
         * block special charachters used in to/from string,
         * or which may be used in the future, or are reserved
         * for special players.
         */
        if (Username.indexOf("~") > 0 ||
                  Username.indexOf("$") > -1 ||
                  Username.indexOf("#") > -1 ||
                  Username.indexOf("^") > -1 ||
                  Username.indexOf("@") > -1 ||
                  Username.indexOf("*") > -1 ||
                  Username.indexOf("&") > -1 ||
                  Username.indexOf("%") > -1 ||
                  Username.indexOf(".") > -1 ||
                  Username.indexOf(",") > -1 ||
                  Username.indexOf("!") > -1 ||
                  Username.indexOf("<") > -1 ||
                  Username.indexOf(">") > -1 ||
                  Username.indexOf("+") > -1 ||
                  Username.indexOf("=") > -1 ||
                  Username.indexOf("|") > -1) {
            server.campaign.CampaignMain.cm.toUser("AM:Your name contains one or more illegal charachters. These are "
                                                         +
                                                         " reserved for system use or high-level players (mods, admins). Remove any of "
                                                         +
                                                         " the following and try enrolling again: ~ @ # $ % ^ + = & < > * . , ! |",
                  Username,
                  true);
            return;
        }

        //make sure the player isn't alread enrolled
        if (server.campaign.CampaignMain.cm.getHouseForPlayer(Username) != null) {
            server.campaign.CampaignMain.cm.toUser("AM:You are already enrolled in the campaign. Nice try though.",
                  Username,
                  true);
            return;
        }

        //this shouldn't ever happen, but still needs to be checked ...
        server.campaign.SHouse h = server.campaign.CampaignMain.cm.getHouseFromPartialString(
              server.campaign.CampaignMain.cm.getConfig("NewbieHouseName"), null);
        if (h == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Training faction is null. Contact an admin immediately.",
                  Username,
                  true);
            return;
        }

        //cast the house into a newbie house
        server.campaign.NewbieHouse nh = null;
        if (h instanceof server.campaign.NewbieHouse) {nh = (server.campaign.NewbieHouse) h;}

        if (nh == null) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Named training faction is not a NewbieHouse. Contact an admin immediately.",
                  Username,
                  true);
            return;
        }

        /*
         * break outs passed. enroll the player in a newbie faction,
         * give him some units, give him some money, and send him a
         * pleasant welcome msg.
         */
        server.campaign.SPlayer newPlayer = new server.campaign.SPlayer();
        newPlayer.setName(Username);
        newPlayer.setMyHouse(nh);

        String unitInfo = nh.getNewSOLUnits(newPlayer, null);
        newPlayer.addMoney(server.campaign.CampaignMain.cm.getIntegerConfig("PlayerBaseMoney"));
        newPlayer.addReward(server.campaign.CampaignMain.cm.getIntegerConfig("PlayerBaseRP"));  //@Salient adding option to give new player RP
        newPlayer.addInfluence(server.campaign.CampaignMain.cm.getIntegerConfig("PlayerBaseFlu")); //@Salient adding option to give new player Flu

        String result = new String("AM:<font color=\"navy\">WELCOME TO MEKWARS!</font>"
                                         +
                                         "<br><br>You've been assigned to " +
                                         nh.getNameAsLink() +
                                         ", "
                                         +
                                         "a training faction. Take some time here to learn about the server rules, "
                                         +
                                         "the unique qualities of the factions that you may join, and the software "
                                         +
                                         "in general. If you're looking for helpful hints, many servers include "
                                         +
                                         "a short <b>New Player Guide</b>. Most have active forums. All have veteran "
                                         +
                                         "players, moderators and admins willing to help new folks learn the ropes.<br><br>"
                                         +
                                         "You've been assigned a starting force: " +
                                         unitInfo +
                                         ".<br><br>Have fun!"
                                         +
                                         "</font><br>");
        server.campaign.CampaignMain.cm.toUser(result, Username, true);

        if (server.campaign.CampaignMain.cm.getServer().getUserLevel(Username) <
                  server.MWChatServer.auth.IAuthenticator.REGISTERED) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:<font color=\"navy\"><br>---<br>NOTE: Your account will not be password protected until you [<a href=\"MWREG\">register</a>] your nickname.<br>---<br></font>",
                  Username,
                  true);
        }

        server.campaign.CampaignMain.cm.doLoginPlayer(Username);

        //tell the mods and add to the IP log
        java.net.InetAddress ip = server.campaign.CampaignMain.cm.getServer().getIP(Username);
        //MWLogger.modLog(Username + " enrolled in the campaign (IP: " + ip + ").");
        MWLogger.ipLog("ENROLL: " + Username + " IP: " + ip);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " enrolled in the campaign (IP: " + ip + ").");

    }//end process()
}//end EnrollCommand
