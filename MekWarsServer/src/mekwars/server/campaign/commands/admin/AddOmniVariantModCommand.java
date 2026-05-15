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

import mekwars.server.campaign.CampaignMain;

public class AddOmniVariantModCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Variant Name#mod Money$Mod components$Mod flu";

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

        // Syntax AddOmniVariantMod Variant Name#mod Money|Mod components|Mod
        // flu

        String variant = command.nextToken();
        String mods = command.nextToken();
        CampaignMain.campaignMain.getOmniVariantMods().put(variant, mods);

        java.util.StringTokenizer modlist = new java.util.StringTokenizer(mods, "$");
        String money = modlist.nextToken();
        String comp = modlist.nextToken();
        String flu = modlist.nextToken();

        CampaignMain.campaignMain.toUser("AM:Variant " +
                                               variant +
                                               " has been given the following repod mods " +
                                               CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                     true,
                                                     Integer.parseInt(money)) +
                                               " " +
                                               comp +
                                               " components " +
                                               CampaignMain.campaignMain.moneyOrFluMessage(false,
                                                     true,
                                                     Integer.parseInt(flu)) +
                                               ".", Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username +
                    " has given variant " +
                    variant +
                    " the following repod mods " +
                    CampaignMain.campaignMain.moneyOrFluMessage(true, true, Integer.parseInt(money)) +
                    " " +
                    comp +
                    " components " +
                    CampaignMain.campaignMain.moneyOrFluMessage(false, true, Integer.parseInt(flu)) +
                    ".");
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

}
