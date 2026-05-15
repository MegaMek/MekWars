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


import mekwars.server.campaign.CampaignMain;

public class CreateArmyCommand implements Command {

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

        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
        int maxlances = CampaignMain.campaignMain.getIntegerConfig("MaxLancesPerPlayer");

        if (p.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE) {
            CampaignMain.campaignMain.toUser("AM:You may not create new armies while on active duty.",
                  Username,
                  true);
            return;
        }

        if (p.getArmies().size() >= maxlances) {
            CampaignMain.campaignMain.toUser("AM:You have reached the max number of allowable armies (" +
                                                   maxlances +
                                                   ").", Username, true);
            return;
        }

        /*
         * Determine the lowest free army id by searching upwards until
         * no army with a matching ID is found in the player's list. This
         * involves hideous nested loops, but works well.
         */
        p.getArmies().trimToSize();
        int i = 0;
        boolean free = false;
        while (!free) {
            free = true;
            for (int j = 0; j < p.getArmies().size(); j++) {
                if (p.getArmies().elementAt(j).getID() == i) {
                    free = false;
                    i++;
                }
            }
        }

        //make the new army, and set misc. data
        server.campaign.SArmy newArmy = new server.campaign.SArmy(i, p.getName());

        //check for all standard illegal name chars
        if (command.hasMoreElements()) {

            String name = (String) command.nextElement();
            boolean illegalName = false;

            if (name.length() > 50) {name = name.substring(0, 50);}

            if (name.indexOf("%") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (% forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf("~") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (~ forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf("$") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name ($ forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf("|") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (| forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf("!") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (! forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf("*") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (* forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf("#") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (# forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf(">") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (> forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf("<") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (< forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf("@") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (@ forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf("&") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (& forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf("^") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (^ forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf("+") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (+ forbidden).", Username, true);
                illegalName = true;
            } else if (name.indexOf("=") != -1) {
                CampaignMain.campaignMain.toUser("AM:Illegal army name (= forbidden).", Username, true);
                illegalName = true;
            }

            if (!illegalName) {newArmy.setName(name);}
        }

        //player is making a new army. set the default limiters.
        newArmy.setUpperLimiter(CampaignMain.campaignMain.getIntegerConfig("DefaultUpperLimit"));
        newArmy.setLowerLimiter(CampaignMain.campaignMain.getIntegerConfig("DefaultLowerLimit"));

        //add the army to the player's list
        if (p.getArmies().size() < newArmy.getID()) {p.getArmies().add(newArmy);} else {
            p.getArmies().add(newArmy.getID(), newArmy);
        }


        //send relevant data to client
        CampaignMain.campaignMain.toUser("PL|SAD|" + p.getArmy(i).toString(true, "%"), Username, false);
        CampaignMain.campaignMain.toUser("AM:Created a new Army (#" + p.getArmy(i).getID() + ").",
              Username,
              true);

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}//end CreateArmyCommand
