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

//Syntax setoperation#optype#opname#data
public class SetOperationCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Op Type[Short/Long/Special]#Op Name";

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

        String opType;
        String opName;

        try {
            opType = command.nextToken();
            opName = command.nextToken();
        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("Syntax setoperation#optype#opname", Username, true);
            return;
        }

        java.io.File opFile = new java.io.File("./data/operations/" + opType + "/" + opName + ".txt");

        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(opFile);
            java.io.PrintStream ps = new java.io.PrintStream(fos);
            while (command.hasMoreTokens()) {
                ps.println(command.nextToken().replaceAll("\\(pound\\)", "#"));
            }
            ps.close();
            fos.close();

        } catch (Exception ex) {
            server.campaign.CampaignMain.cm.toUser("Unable to write to " + opFile.getName(), Username, true);
            return;
        }

        server.campaign.CampaignMain.cm.doSendModMail("NOTE", Username + " has updated " + opFile.getName());
        // Delete md5 file so clients will refresh properly
        java.io.File md5File = new java.io.File("./data/operations/opsmd5.txt");
        if (md5File.exists()) {
            md5File.delete();
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}//end RetrieveShortOperation
