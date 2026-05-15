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

package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;

/**
 * A command to create a unit
 * <p>
 * This command allows an admin to create a unit, which is then dropped into his hangar
 *
 * @version 2016.10.26
 */
public class CreateUnitCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "filename#flavortext#gunnery#pilot#weightclass#skill1,skill2,skill3[Random]";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
        String filename;
        String FlavorText;
        String gunnery;
        String piloting;
        String skillTokens = null;

        try {
            filename = command.nextToken();
            FlavorText = command.nextToken();
            gunnery = command.nextToken();
            piloting = command.nextToken();
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser(syntax, Username);
            return;
        }

        int weight = server.campaign.SUnit.LIGHT;

        if (command.hasMoreElements()) {weight = Integer.parseInt(command.nextToken());}

        if (command.hasMoreTokens()) {
            skillTokens = command.nextToken();
        }
        //cm.setPilot(pilot);
        server.campaign.SUnit cm = server.campaign.SUnit.create(filename,
              FlavorText,
              Integer.parseInt(gunnery),
              Integer.parseInt(piloting),
              weight,
              skillTokens);
        p.addUnit(cm, true);
        CampaignMain.campaignMain.toUser("Unit created: " +
                                               filename +
                                               " " +
                                               FlavorText +
                                               " " +
                                               gunnery +
                                               " " +
                                               piloting +
                                               " " +
                                               cm.getPilot().getSkillString(true) +
                                               ". ID #" +
                                               cm.getId(), Username, true);
        //server.MWLogger.modLog(Username + " created a unit: " + filename + " " + FlavorText + " " + gunnery + " " + piloting+" "+pilot.getSkillString(true));
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username +
                    " created a unit: " +
                    filename +
                    " " +
                    FlavorText +
                    " " +
                    gunnery +
                    " " +
                    piloting +
                    " " +
                    cm.getPilot().getSkillString(true));
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
