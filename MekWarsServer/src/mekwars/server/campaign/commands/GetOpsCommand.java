/*
 * MekWars - Copyright (C) 2014
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
 *
 */
package mekwars.server.campaign.commands;

import common.campaign.operations.Operation;
import common.util.MWLogger;

public class GetOpsCommand implements Command {

    int accessLevel = 2;
    String syntax = "/getops [getall, md5, getsome#list]";

    @Override
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

        if (command.hasMoreTokens()) {
            String cmd = command.nextToken();
            switch (cmd.toLowerCase()) {
                case "getall":
                    for (Operation o : server.campaign.CampaignMain.cm.getOpsManager().getOperations().values()) {
                        server.campaign.CampaignMain.cm.toUser("OP|add|" + o.getName() + "|" + o.getXmlString(),
                              Username,
                              false);
                    }
                    server.campaign.CampaignMain.cm.toUser("OP|view", Username, false);
                    break;

                case "getsome":
                    java.util.Vector<Operation> opsToSend = new java.util.Vector<Operation>();
                    while (command.hasMoreTokens()) {
                        opsToSend.add(server.campaign.CampaignMain.cm.getOpsManager()
                                            .getOperation(command.nextToken()));
                    }
                    for (Operation o : opsToSend) {
                        server.campaign.CampaignMain.cm.toUser("OP|add|" + o.getName() + "|" + o.getXmlString(),
                              Username,
                              false);
                    }
                    server.campaign.CampaignMain.cm.toUser("OP|view", Username, false);
                    break;

                case "md5":
                    java.io.File md5File = new java.io.File("./data/operations/opsmd5.txt");
                    if (!md5File.exists()) {
                        // Calculate MD5s and write the file
                        java.io.FileWriter fw = null;
                        try {
                            fw = new java.io.FileWriter(md5File);
                            for (Operation o : server.campaign.CampaignMain.cm.getOpsManager()
                                                     .getOperations()
                                                     .values()) {
                                java.security.MessageDigest md = null;
                                try {
                                    md = java.security.MessageDigest.getInstance("MD5");
                                } catch (java.security.NoSuchAlgorithmException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                byte[] array = md.digest(o.getXmlString().getBytes("UTF-8"));
                                StringBuffer sb = new StringBuffer();
                                for (int i = 0; i < array.length; ++i) {
                                    sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
                                }
                                fw.write(o.getName() + "#" + sb.toString() + "\n");
                            }
                        } catch (java.io.IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } finally {
                            try {
                                fw.close();
                            } catch (java.io.IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                    // Now, send it to them
                    StringBuffer toReturn = new StringBuffer();
                    try {
                        java.io.FileInputStream in = new java.io.FileInputStream(md5File);
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                        try {
                            while (br.ready()) {
                                toReturn.append(br.readLine() + "#");
                            }
                            br.close();
                            in.close();
                        } catch (java.io.IOException ex) {

                        }
                    } catch (java.io.FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        MWLogger.errLog(e);
                    }
                    server.campaign.CampaignMain.cm.toUser("OP|md5|" + toReturn.toString(), Username, false);

                    break;

                default:
                    server.campaign.CampaignMain.cm.toUser("AM: invalid syntax, use: " + getSyntax(), Username, true);
                    break;
            }
        } else {
            server.campaign.CampaignMain.cm.toUser("AM: invalid syntax, use: " + getSyntax(), Username, true);
        }
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {return syntax;}

}
