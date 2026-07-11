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
import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;


public class RemoveTraitCommand implements server.campaign.commands.Command {
    private static final MMLogger LOGGER = MMLogger.create(RemoveTraitCommand.class);

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Faction Name#Trait Name#CONFIRM";

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

        //Syntax AddTrait Faction#TraitName#SkillList($)


        String faction = "common";
        String traitName = "none";
        String confirmString = "";

        try {
            faction = command.nextToken();
            traitName = command.nextToken();
            confirmString = command.nextToken();
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }

        if (!confirmString.equals("CONFIRM")) {return;}

        java.util.Vector<String> traits = CampaignMain.campaignMain.getFactionTraits(faction.toLowerCase());

        for (int pos = 0; pos < traits.size(); pos++) {
            java.util.StringTokenizer traitToken = new java.util.StringTokenizer(traits.elementAt(pos), "*");
            if (traitName.equalsIgnoreCase(traitToken.nextToken())) {
                traits.removeElementAt(pos);
                CampaignMain.campaignMain.toUser("Trait " + traitName + " has been removed.", Username, true);
                CampaignMain.campaignMain.doSendModMail("NOTE",
                      Username + " has removed trait " + traitName + ".");
                CampaignMain.campaignMain.saveFactionTraits(faction, traits);
                return;
            }
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}
