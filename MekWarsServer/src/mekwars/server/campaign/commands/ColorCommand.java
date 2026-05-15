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

package mekwars.server.campaign.commands;

import mekwars.server.campaign.CampaignMain;

/**
 * Moving the Color command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c Color#Color Schema
 */
public class ColorCommand implements Command {

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

        String text = "";

        try {
            text = command.nextToken();
        } catch (Exception ex) {}

        if (text.trim().length() < 1) {
            CampaignMain.campaignMain.toUser("AM:You need to choose a color. /color blue", Username, true);
        } else {
            //Hex numbers need a # in from of them. This is parsed out by the tokenizer
            try {
                Integer.parseInt(text);
                text = "#" + text;
            } catch (Exception ex) {}
            CampaignMain.campaignMain.getServer().getUser(Username).setColor(text);
            CampaignMain.campaignMain.getServer()
                  .broadcastRaw("UC|" +
                                      Username +
                                      "|" +
                                      CampaignMain.campaignMain.getServer().getUser(Username).getColor());
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
