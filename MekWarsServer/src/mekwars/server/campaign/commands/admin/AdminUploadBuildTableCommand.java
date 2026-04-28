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


public class AdminUploadBuildTableCommand implements server.campaign.commands.Command {

    /*
     * This command allows an Admin to upload a single build table
     * from a directory on their local machine.  The directory structure
     * on the local machine must match that on the server - i.e, ./buildtables/rare
     * ./buildtables/reward and ./buildtables/standard.  The replacement build
     * table is put into place and a backup of the original build table is created.
     */

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "[rare/reward/standard]/Build Table File Name";

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
        String fileName = "./data/buildtables/" + command.nextToken();

        if (!command.hasMoreElements()) {
            server.campaign.CampaignMain.cm.toUser(
                  "No file data found. This may be due to the fact that the command line was used intead of the GUI.",
                  Username,
                  true);
            return;
        }
        try {
            java.io.File newTable = new java.io.File(fileName);
            if (newTable.exists()) {
                // Back it up
                String newFileName = fileName + ".bak";
                java.io.File backupFile = new java.io.File(newFileName);
                if (backupFile.exists()) {backupFile.delete();}
                newTable.renameTo(backupFile);
                newTable.delete();
            }
            java.io.FileOutputStream out = new java.io.FileOutputStream(fileName);
            java.io.PrintStream p = new java.io.PrintStream(out);
            while (command.hasMoreTokens()) {
                p.println(command.nextToken());
            }
            p.close();
            out.close();

            server.campaign.CampaignMain.cm.toUser(fileName + " Saved", Username, true);
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("File Not found", Username, true);
            return;
        }

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
