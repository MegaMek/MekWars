/*
 * MekWars - Copyright (C) 2008
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

import server.util.discord.DiscordMessageHandler;

public class CodeTestCommand implements Command {

    final int ACTION_START = 0;
    final int ACTION_STOP = 1;
    int accessLevel = 0;
    String syntax = "start or stop";

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


        int action;

        if (!command.hasMoreTokens()) {
            server.campaign.CampaignMain.cm.toUser("AM: invalid syntax: try /codetest start or /codetest stop",
                  Username,
                  false);
            return;
        }

        String s = command.nextToken();

        if (s.equalsIgnoreCase("start")) {
            action = ACTION_START;
        } else if (s.equalsIgnoreCase("stop")) {
            action = ACTION_STOP;
        } else {
            server.campaign.CampaignMain.cm.toUser("AM: invalid syntax: try /codetest start or /codetest stop",
                  Username,
                  false);
            return;
        }

        switch (action) {
            case ACTION_START:

                DiscordMessageHandler handler = new DiscordMessageHandler();
                handler.post("This is a test of the Comstar broadcast system");

                break;
            case ACTION_STOP:
                //			try {
                //				CampaignMain.cm.getScheduler().unscheduleJob(new TriggerKey("trigger1", "group1"));
                //				Scheduler sc = CampaignMain.cm.getScheduler();
                //				for (String group : sc.getJobGroupNames()) {
                //					for(JobKey jobKey : sc.getJobKeys(jobGroupEquals(group))) {
                //						CampaignMain.cm.doSendModMail("SERVER", "Found Job Identified by: " + jobKey);
                //					}
                //				}
                //
                //			} catch (SchedulerException e) {
                //				MWLogger.errLog(e);
                //			}
                break;
        }
    }

    public int getExecutionLevel() {return 200;}

    public void setExecutionLevel(int i) {}

    public String getSyntax() {return syntax;}
}
