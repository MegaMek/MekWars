/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - nmorris (urgru@users.sourceforge.net)
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

package mekwars.server.campaign.commands.admin;

import common.util.MWLogger;


public class RemoveTraitCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#Trait Name#CONFIRM";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }

        //Syntax AddTrait Faction#TraitName#SkillList($)


        String faction = "common";
        String traitName = "none";
        String confirmString = "";

        try {
            faction = command.nextToken();
            traitName = command.nextToken();
            confirmString = command.nextToken();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

        if (!confirmString.equals("CONFIRM")) {return;}

        java.util.Vector<String> traits = server.campaign.CampaignMain.cm.getFactionTraits(faction.toLowerCase());

        for (int pos = 0; pos < traits.size(); pos++) {
            java.util.StringTokenizer traitToken = new java.util.StringTokenizer(traits.elementAt(pos), "*");
            if (traitName.equalsIgnoreCase(traitToken.nextToken())) {
                traits.removeElementAt(pos);
                server.campaign.CampaignMain.cm.toUser("Trait " + traitName + " has been removed.", Username, true);
                server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                      Username + " has removed trait " + traitName + ".");
                server.campaign.CampaignMain.cm.saveFactionTraits(faction, traits);
                return;
            }
        }
    }

}
