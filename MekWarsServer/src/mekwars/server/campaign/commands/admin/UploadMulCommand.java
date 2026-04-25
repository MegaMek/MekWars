/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - jtighe (torren@users.sourceforge.net)
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


public class UploadMulCommand implements server.campaign.commands.Command {

    /*
     * This command allows an Admin to upload a single build table
     * from a directory on their local machine.  The directory structure
     * on the local machine must match that on the server - i.e, ./buildtables/rare
     * ./buildtables/reward and ./buildtables/standard.  The replacement build
     * table is put into place and a backup of the original build table is created.
     */

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "FileName#Line#Line#Line...";

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
        String fileName = "./data/armies/" + command.nextToken();

        try {
            java.io.File newMul = new java.io.File(fileName);
            if (!newMul.exists()) {
                newMul.createNewFile();
            }
            java.io.FileOutputStream out = new java.io.FileOutputStream(fileName);
            java.io.PrintStream p = new java.io.PrintStream(out);
            while (command.hasMoreTokens()) {
                p.println(command.nextToken());
            }
            p.close();
            out.close();

            server.campaign.CampaignMain.cm.toUser(newMul.getPath() + " Saved", Username, true);
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("File Not found", Username, true);
            return;
        }

    }
}
