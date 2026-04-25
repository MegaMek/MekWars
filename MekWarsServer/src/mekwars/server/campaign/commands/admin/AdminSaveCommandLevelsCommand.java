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

import common.util.MWLogger;


public class AdminSaveCommandLevelsCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

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

        java.util.TreeMap<String, server.campaign.commands.Command> commandTable = new java.util.TreeMap<String, server.campaign.commands.Command>(
              server.campaign.CampaignMain.cm.getServerCommands());
        java.io.PrintStream p = null;
        try {

            java.io.File fp = new java.io.File("./data/commands");
            if (!fp.exists()) {fp.mkdir();}

            java.io.FileOutputStream out = new java.io.FileOutputStream("./data/commands/commands.dat");
            p = new java.io.PrintStream(out);

            String commandName = "";
            for (java.util.Iterator<String> i = commandTable.keySet().iterator();
                  i.hasNext();
                  commandName = (String) i.next()) {
                server.campaign.commands.Command commandMethod = server.campaign.CampaignMain.cm.getServerCommands()
                                                                       .get(commandName);
                if (commandName == null || commandMethod == null) {continue;}
                p.println(commandName.toUpperCase() + "#" + commandMethod.getExecutionLevel());
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Unable to save command levels");
        } finally {
            p.close();
        }
        server.campaign.CampaignMain.cm.toUser("AM:Command levels saved!", Username, true);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " has saved the command levels to file.");

    }
}
