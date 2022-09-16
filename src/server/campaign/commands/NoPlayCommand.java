/*
 * MekWars - Copyright (C) 2005 
 * 
 * original author - nmorris (urgru@users.sourceforge.net)
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

package server.campaign.commands;

import java.io.File;
import java.util.StringTokenizer;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.util.ExclusionList;

public class NoPlayCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
                return;
            }
        }

        // player who issued the command
        SPlayer p = CampaignMain.cm.getPlayer(Username);
        if (p == null) {
            return;
        }

        if (p.getDutyStatus() >= SPlayer.STATUS_ACTIVE) {
            CampaignMain.cm.toUser("AM:You may not use this command while active.", Username, true);
            return;
        }

        String mode = "";
        String excludeName = "";
        try {
            mode = command.nextToken().toLowerCase();
            excludeName = command.nextToken();
        } catch (Exception e) {
            CampaignMain.cm.toUser("AM:Improper format. Try: /c noplay#add#name or /c noplay#remove#name.", Username, true);
            return;
        }

        boolean commandConfirmed = false;
        if (command.hasMoreElements()) {
            try {
                commandConfirmed = command.nextToken().equals("CONFIRM");
            } catch (Exception e) {
                CampaignMain.cm.toUser("AM:Improper format. Try: /c noplay#add#name#CONFIRM or /c noplay#remove#name#CONFIRM", Username, true);
                return;
            }
        }

        // load the exlusion list
        ExclusionList exList = p.getExclusionList();
        if (exList == null) {
            CampaignMain.cm.toUser("AM:ERROR. Your no-play list was null. Report this to an admin.", Username, true);
            return;
        }

        // load the target players current exclude status
        int exclusionStatus = exList.checkExclude(excludeName);

        // load relevant config variables
        int maxSize = CampaignMain.cm.getIntegerConfig("NoPlayListSize");
        int removeRPCost = CampaignMain.cm.getIntegerConfig("NoPlayRPCost");
        int removeFluCost = CampaignMain.cm.getIntegerConfig("NoPlayInfluenceCost");
        int removeMUCost = CampaignMain.cm.getIntegerConfig("NoPlayMUCost");
        boolean adminListCountsForCap = Boolean.parseBoolean(CampaignMain.cm.getConfig("NoPlaysFromAdminsCountForMax"));

        // check player's values
        boolean hasEnoughRP = false;
        boolean hasEnoughInfluence = false;
        boolean hasEnoughMU = false;
        if (p.getReward() >= removeRPCost) {
            hasEnoughRP = true;
        }
        if (p.getMoney() >= removeMUCost) {
            hasEnoughMU = true;
        }
        if (p.getInfluence() >= removeFluCost) {
            hasEnoughInfluence = true;
        }

        // COST BLOCK
        String costBlock = "";
        String shortCost = "";

        // see what kind of grammer we need
        int totalVarsWithCost = 0;
        if (removeRPCost > 0) {
            totalVarsWithCost++;
        }
        if (removeFluCost > 0) {
            totalVarsWithCost++;
        }
        if (removeMUCost > 0) {
            totalVarsWithCost++;
        }

        boolean canAffordRemove = false;
        if (hasEnoughRP && hasEnoughInfluence && hasEnoughMU) {
            costBlock += " will ";
            canAffordRemove = true;
        } else {// can't afford
            costBlock += " would ";
        }

        costBlock += " cost ";

        if (removeRPCost > 0) {
            shortCost += removeRPCost + " " + CampaignMain.cm.getConfig("RPShortName");

            if (totalVarsWithCost == 3) {
                shortCost += ", ";
            } else if (totalVarsWithCost == 2) {
                shortCost += " and ";
            }
        }
        if (removeMUCost > 0) {
            shortCost += CampaignMain.cm.moneyOrFluMessage(true, true, removeMUCost) + "";

            if (totalVarsWithCost == 3) {
                shortCost += " and ";
            } else if ((totalVarsWithCost == 2) && (removeRPCost <= 0)) {
                shortCost += " and ";
            }
        }
        if (removeFluCost > 0) {
            shortCost += CampaignMain.cm.moneyOrFluMessage(false, true, removeFluCost) + "";
        }

        costBlock = costBlock + shortCost + ".";

        // ADD BLOCK
        if (mode.equals("add")) {

            // check to make sure the player who will be excluded exists
            boolean playerExists = false;
            playerExists = new File("./campaign/players/" + excludeName.toLowerCase() + ".dat").exists();

            if (!playerExists) {
                CampaignMain.cm.toUser(excludeName + " does not have a player file. cannot add to your no-play list.", Username, true);
                return;
            }

            // check to see is the given player is already excluded
            if (exclusionStatus != ExclusionList.NO_EXCLUSION) {
                CampaignMain.cm.toUser(excludeName + " is already on your no-play list.", Username, true);
                return;
            }

            // check to make sure the player isn't no-play'ing himself
            if (p.getName().toLowerCase().equals(excludeName)) {
                CampaignMain.cm.toUser("AM:You can't put your own name on your no-play list. Jackass.", Username, true);
                return;
            }

            // check the current list size [adminList only]
            if (adminListCountsForCap && (exList.getAdminExcludes().size() >= maxSize)) {
                CampaignMain.cm.toUser("AM:Moderators/Admins have filled your no-play list. You may not add" + " any players on your own at this time.", Username, true);
                return;
            }

            // check the current list size [all lists]
            int currentSize = exList.getPlayerExcludes().size();
            if (adminListCountsForCap) {
                currentSize += exList.getAdminExcludes().size();
            }
            if ((currentSize >= maxSize) && (exList.getPlayerExcludes().size() > 0)) {

                if (canAffordRemove) {
                    String toUser = "AM:You must remove a player from your no-play list in order to add " + excludeName + ". Removing " + costBlock + "<br>";

                    // loop through and make links to remove
                    for (String currName : exList.getPlayerExcludes()) {
                        toUser += "<a href=\"MEKWARS/c removeandaddnoplay#" + currName + "#" + excludeName + "\">Click here to remove " + currName + " and add " + excludeName + "</a>.<br>";
                    }
                    CampaignMain.cm.toUser(toUser, Username, true);
                    return;
                }

                // else, its too expensive
                String toUser = "AM:You must remove a player from your no-play list before adding " + excludeName + "; however, you cannot " + "afford a removal at this time. Removing costs " + shortCost + ".<br>";
                CampaignMain.cm.toUser(toUser, Username, true);
                return;
            }

            // player can be excluded, and there is room, so check confirmation
            if (commandConfirmed) {
                try {
                    exList.addExclude(false, excludeName);
                    CampaignMain.cm.toUser(excludeName + " was added to your no-play list.", Username, true);
                    CampaignMain.cm.toUser("PL|PEU|" + p.getExclusionList().playerExcludeToString("$"), Username, false);
                    p.setSave();
                    return;
                } catch (Exception e) {
                    CampaignMain.cm.toUser("AM:Error while adding " + excludeName + " to your no-play list. Report this to an admin.", Username, true);
                    return;
                }
            }

            // else, request confirmation
            String toUser = "AM:Are you sure you want to add " + excludeName + " to your no-play list? <br><a " + "href=\"MEKWARS/c noplay#add#" + excludeName + "#CONFIRM\">Click here to confirm the addition " + "of " + excludeName + " to your no-play list</a>.<br>";
            CampaignMain.cm.toUser(toUser, Username, true);
            return;

        }// end if(mode == add)

        else if (mode.equals("remove")) {

            if (exclusionStatus == ExclusionList.NO_EXCLUSION) {
                CampaignMain.cm.toUser(excludeName + " is not on your no-play list.", Username, true);
                return;
            }

            if (exclusionStatus == ExclusionList.ADMIN_EXCLUDED) {
                CampaignMain.cm.toUser("AM:You cannot remove " + excludeName + " from your no-play list. He/she " + "was added to the list by a Mod or Admin.", Username, true);
                return;
            }

            // check costs
            if (!canAffordRemove) {
                String toUser = "AM:You cannot afford to remove " + excludeName + " from your no-play list. Removal " + costBlock;
                CampaignMain.cm.toUser(toUser, Username, true);
                return;
            }

            // name is there, and was added by the player, so remove it.
            if (commandConfirmed) {
                try {
                    exList.removeExclude(false, excludeName);
                    CampaignMain.cm.toUser(excludeName + "was removed from your no-play list (-" + shortCost + ").", Username, true);
                    p.addMoney(-removeMUCost);
                    p.addReward(-removeRPCost);
                    p.addInfluence(-removeFluCost);
                    CampaignMain.cm.toUser("PL|PEU|" + p.getExclusionList().playerExcludeToString("$"), Username, false);
                    p.setSave();
                    return;
                } catch (Exception e) {
                    CampaignMain.cm.toUser("AM:Error while removing " + excludeName + " from your no-play list. Report this to an admin.", Username, true);
                    return;
                }
            }

            // else, request confirmation
            String toUser = "AM:Are you sure you want to remove " + excludeName + " from your no-play list? Removal " + costBlock + "<br><a " + "href=\"MEKWARS/c noplay#remove#" + excludeName + "#CONFIRM\">Click here to confirm the removal</a>.<br>";
            CampaignMain.cm.toUser(toUser, Username, true);
            return;

        }// end (if mode == remove)

        else {// mode is gibberish. alert the user.
            CampaignMain.cm.toUser("AM:Improper format. Try: /c noplay#add#name or /c noplay#remove#name.", Username, true);
            return;
        }

    }
}