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

package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

public class AddOmniVariantModCommand implements Command {

    int accessLevel = IAuthenticator.ADMIN;
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

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
                return;
            }
        }

        // Syntax AddOmniVariantMod Variant Name#mod Money|Mod components|Mod
        // flu

        String variant = command.nextToken();
        String mods = command.nextToken();
        CampaignMain.cm.getOmniVariantMods().put(variant, mods);

        StringTokenizer modlist = new StringTokenizer(mods, "$");
        String money = modlist.nextToken();
        String comp = modlist.nextToken();
        String flu = modlist.nextToken();

        CampaignMain.cm.toUser("AM:Variant " + variant + " has been given the following repod mods " + CampaignMain.cm.moneyOrFluMessage(true, true, Integer.parseInt(money)) + " " + comp + " components " + CampaignMain.cm.moneyOrFluMessage(false, true, Integer.parseInt(flu)) + ".", Username, true);
        CampaignMain.cm.doSendModMail("NOTE", Username + " has given variant " + variant + " the following repod mods " + CampaignMain.cm.moneyOrFluMessage(true, true, Integer.parseInt(money)) + " " + comp + " components " + CampaignMain.cm.moneyOrFluMessage(false, true, Integer.parseInt(flu)) + ".");
    }

}