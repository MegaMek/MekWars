	/*
     * MekWars - Copyright (C) 2008
     *
     * Original author - Bob Eldred (billypinhead@users.sourceforge.net)
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


    package mekwars.server.campaign.commands;

    import common.util.MWLogger;
    import mekwars.server.campaign.CampaignMain;


    public class RequestBuildTableCommand implements Command {

        /*
         * This command allows an Admin to upload a single build table
         * from a directory on their local machine.  The directory structure
         * on the local machine must match that on the server - i.e, ./buildtables/rare
         * ./buildtables/reward and ./buildtables/standard.  The replacement build
         * table is put into place and a backup of the original build table is created.
         */

        int accessLevel = 2;
        String syntax = "list";

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
            String subcommand = command.nextToken();
            if (subcommand.equalsIgnoreCase("list")) {
                StringBuilder toReturn = new StringBuilder();
                String folderDelimiter = "?";
                String fileDelimiter = "*";
                String[] folderList = { "standard" };
                boolean viewer = false;

                if (command.hasMoreTokens()) {
                    viewer = Boolean.parseBoolean(command.nextToken());
                }

                for (int i = 0; i < folderList.length; i++) {
                    java.io.File currF = new java.io.File("./data/buildtables/" + folderList[i]);
                    toReturn.append(folderList[i]);
                    toReturn.append(folderDelimiter);
                    if (!currF.exists() || !currF.isDirectory()) {continue;}
                    java.io.File fileNames[] = currF.listFiles();

                    for (java.io.File currFile : fileNames) {
                        if (currFile.getName().endsWith("txt")) {
                            toReturn.append(currFile.getName());
                            toReturn.append(fileDelimiter);
                        }
                    }
                    toReturn.append(folderDelimiter);
                }
                CampaignMain.campaignMain.toUser("BT|PLS|" + toReturn.toString() + "|" + viewer, Username, false);

                return;
            } else if (subcommand.equalsIgnoreCase("get")) {
                StringBuilder toReturn = new StringBuilder();
                String folder = "";
                String table = "";
                long time = 0;

                if (command.hasMoreTokens()) {folder = command.nextToken();}
                if (command.hasMoreTokens()) {table = command.nextToken();}

                if (command.hasMoreTokens()) {
                    time = Long.parseLong(command.nextToken());
                }

                if (folder.length() == 0 || table.length() == 0) {
                    CampaignMain.campaignMain.toUser("Bad Build Table Request: " +
                                                           (folder.length() == 0 ?
                                                                  "Empty folder name" :
                                                                  "Empty file name"), Username, true);
                    return;
                }
                java.io.File file = new java.io.File("./data/buildtables/" + folder + "/" + table);
                if (!file.exists()) {
                    CampaignMain.campaignMain.toUser("Bad Build Table Request: " +
                                                           folder +
                                                           "/" +
                                                           table +
                                                           " does not exist.", Username, true);
                    return;
                }

                if (time >= file.lastModified()) {
                    return;
                }

                // The request is good, so send it.
                try {
                    java.io.FileInputStream in = new java.io.FileInputStream(file);
                    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                    try {
                        while (br.ready()) {
                            toReturn.append("|" + br.readLine());
                        }
                        br.close();
                        in.close();
                    } catch (java.io.IOException ex) {

                    }
                } catch (java.io.FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    MWLogger.errLog(e);
                }
                CampaignMain.campaignMain.toUser("BT|BT|" + folder + "|" + table + toReturn.toString(),
                      Username,
                      false);

            } else if (subcommand.equalsIgnoreCase("view")) {
                CampaignMain.campaignMain.toUser("BT|VS|DONE#DONE", Username, false);
            }
        }

        public int getExecutionLevel() {return accessLevel;}

        public void setExecutionLevel(int i) {accessLevel = i;}

        public String getSyntax() {return syntax;}
    }
