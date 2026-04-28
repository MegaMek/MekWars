/*
 * MekWars - Copyright (C) 2004
 *
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

/**
 * @author jtighe This Command is used bye server admins to change config items on the fly while the server is
 *       still running.
 *
 */
package mekwars.server.campaign.commands.admin;

import server.campaign.util.ChristmasHandler;
import server.campaign.util.scheduler.MWScheduler;

/**
 * Allows the administrator to change configs.  Used by Server Config dialog.
 * <p>
 * Starting at v2016.10.26, added some catches for specific config changes to restart services that would otherwise
 * require a server restart
 *
 * @version 2016.10.26
 */
public class AdminChangeServerConfigCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "config#arg";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        //get config var and new setting
        String config = command.nextToken();
        String arg = command.nextToken();

        //make setting change
        server.campaign.CampaignMain.cm.getConfig().setProperty(config, arg);

        // Check for Schedule changes here
        if (config.equalsIgnoreCase("Christmas_StartDate")) {
            ChristmasHandler.getInstance().reschedule();
        } else if (config.equalsIgnoreCase("Christmas_EndDate")) {
            ChristmasHandler.getInstance().reschedule();
        } else if (config.equalsIgnoreCase("Scheduler_FactionSave")) {

        } else if (config.equalsIgnoreCase("Scheduler_PlayerActivity_flu")) {
            MWScheduler.getInstance().rescheduleAllActivePlayers();
        } else if (config.equalsIgnoreCase("Scheduler_PlayerActivity_comps")) {
            MWScheduler.getInstance().rescheduleAllActivePlayers();
        } else if (config.equalsIgnoreCase("Christmas_Units_Method_OneOfEach")) {
            if (arg.equalsIgnoreCase("true")) {
                ChristmasHandler.getInstance().setUnitMethod(ChristmasHandler.UNIT_METHOD_ONEOFEACH);
            }
        } else if (config.equalsIgnoreCase("Christmas_Units_Method_XOfEach")) {
            if (arg.equalsIgnoreCase("true")) {
                ChristmasHandler.getInstance().setUnitMethod(ChristmasHandler.UNIT_METHOD_XOFEACH);
            }
        } else if (config.equalsIgnoreCase("Christmas_Units_Method_XTotal")) {
            if (arg.equalsIgnoreCase("true")) {
                ChristmasHandler.getInstance().setUnitMethod(ChristmasHandler.UNIT_METHOD_XTOTAL);
            }
        } else if (config.equalsIgnoreCase("Christmas_Units_X")) {
            ChristmasHandler.getInstance().setNumberOfUnits(Integer.parseInt(arg));
        } else if (config.equalsIgnoreCase("Celebrate_Christmas")) {
            ChristmasHandler.getInstance().setCelebrateChristmas(Boolean.parseBoolean(arg));
        } else if (config.equalsIgnoreCase("TrackerResetUUID")) {
            server.campaign.CampaignMain.cm.getConfig()
                  .setProperty("TrackerUUID", java.util.UUID.randomUUID().toString());
            server.campaign.CampaignMain.cm.getConfig().setProperty("TrackerResetUUID", "false");
        }

        //NOTE:
        //NO MODMAIL for setting changes. Server Config GUI would spam too much.

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
