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

public class AddOmniVariantModCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Variant Name#mod Money$Mod components$Mod flu";

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

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

        // Syntax AddOmniVariantMod Variant Name#mod Money|Mod components|Mod
        // flu

        String variant = command.nextToken();
        String mods = command.nextToken();
        server.campaign.CampaignMain.cm.getOmniVariantMods().put(variant, mods);

        java.util.StringTokenizer modlist = new java.util.StringTokenizer(mods, "$");
        String money = modlist.nextToken();
        String comp = modlist.nextToken();
        String flu = modlist.nextToken();

        server.campaign.CampaignMain.cm.toUser("AM:Variant " +
                                                     variant +
                                                     " has been given the following repod mods " +
                                                     server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                           true,
                                                           Integer.parseInt(money)) +
                                                     " " +
                                                     comp +
                                                     " components " +
                                                     server.campaign.CampaignMain.cm.moneyOrFluMessage(false,
                                                           true,
                                                           Integer.parseInt(flu)) +
                                                     ".", Username, true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username +
                    " has given variant " +
                    variant +
                    " the following repod mods " +
                    server.campaign.CampaignMain.cm.moneyOrFluMessage(true, true, Integer.parseInt(money)) +
                    " " +
                    comp +
                    " components " +
                    server.campaign.CampaignMain.cm.moneyOrFluMessage(false, true, Integer.parseInt(flu)) +
                    ".");
    }

}
