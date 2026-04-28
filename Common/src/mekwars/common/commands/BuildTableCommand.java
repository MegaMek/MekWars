	/*
     * MekWars - Copyright (C) 2007
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


    package mekwars.common.commands;

    import java.io.File;
    import java.util.StringTokenizer;

    import mekwars.common.campaign.clientutils.protocol.IClient;
    import mekwars.common.gui.CCommPanel;
    import mekwars.common.util.MWLogger;

    /**
     * @author Spork
     *       <p>
     *       Handles Build Table up/downloading for admins/mods
     *
     */
    public class BuildTableCommand extends Command {

        /**
         *
         */
        public BuildTableCommand(IClient client) {
            super(client);
        }

        /**
         * @see Command#execute(String)
         */
        @Override
        public void execute(String input) {
            StringTokenizer st = decode(input);

            String cmd = st.nextToken();
            boolean viewer = false;
            if (cmd.equalsIgnoreCase("LS")) {
                StringTokenizer folderT = new StringTokenizer(st.nextToken(), "?");

                if (st.hasMoreTokens()) {
                    viewer = Boolean.parseBoolean(st.nextToken());
                }

                while (folderT.hasMoreTokens()) {
                    //Token 1 is the folder
                    String dName = folderT.nextToken();
                    MWLogger.infoLog(dName);
                    // Token 2 is the names of the lists
                    if (folderT.hasMoreTokens()) {
                        StringTokenizer listT = new StringTokenizer(folderT.nextToken(), "*");
                        while (listT.hasMoreTokens()) {
                            String fileName = listT.nextToken();
                            long time = 0;
                            File file = new File("./data/buildtables/" + dName + "/" + fileName);

                            if (file.exists()) {
                                time = file.lastModified();
                            }

                            client.sendChat(
                                  IClient.CAMPAIGN_PREFIX +
                                        "AdminRequestBuildTable get#" +
                                        dName +
                                        "#" +
                                        fileName +
                                        "#" +
                                        time);
                        }
                    }

                }

                if (viewer) {
                    client.sendChat(IClient.CAMPAIGN_PREFIX + "AdminRequestBuildTable view");
                }

            }
            if (cmd.equalsIgnoreCase("PLS")) {
                StringTokenizer folderT = new StringTokenizer(st.nextToken(), "?");

                if (st.hasMoreTokens()) {
                    viewer = Boolean.parseBoolean(st.nextToken());
                }

                while (folderT.hasMoreTokens()) {
                    //Token 1 is the folder
                    String dName = folderT.nextToken();
                    MWLogger.infoLog(dName);
                    // Token 2 is the names of the lists
                    if (folderT.hasMoreTokens()) {
                        StringTokenizer listT = new StringTokenizer(folderT.nextToken(), "*");
                        while (listT.hasMoreTokens()) {
                            String fileName = listT.nextToken();
                            long time = 0;
                            File file = new File("./data/buildtables/" + dName + "/" + fileName);

                            if (file.exists()) {
                                time = file.lastModified();
                            }
                            client.sendChat(
                                  IClient.CAMPAIGN_PREFIX +
                                        "RequestBuildTable get#" +
                                        dName +
                                        "#" +
                                        fileName +
                                        "#" +
                                        time);
                        }
                    }

                }
                if (viewer) {
                    client.sendChat(IClient.CAMPAIGN_PREFIX + "RequestBuildTable view");
                }
            } else if (cmd.equalsIgnoreCase("BuildTableCommand")) {
                String folder = st.nextToken();
                String table = st.nextToken();
                boolean isMod = client.isMod();
                java.io.File file = new java.io.File("./data/buildtables");
                if (!file.exists()) {file.mkdir();}
                file = new java.io.File("./data/buildtables/" + folder);
                if (!file.exists()) {file.mkdir();}
                file = new java.io.File("./data/buildtables/" + folder + "/" + table);
                try {
                    file.createNewFile();
                } catch (java.io.IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                java.io.FileOutputStream out;
                try {
                    out = new java.io.FileOutputStream(file);
                    java.io.PrintStream p = new java.io.PrintStream(out);
                    while (st.hasMoreTokens()) {p.println(st.nextToken());}
                    if (isMod) {
                        client.addToChat("Received build table " + folder + "/" + table, CCommPanel.CHANNEL_MISC);
                    }
                    p.close();
                    try {
                        out.close();
                    } catch (java.io.IOException e) {
                        // TODO Auto-generated catch block
                        MWLogger.errLog(e);
                    }
                } catch (java.io.FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    MWLogger.errLog(e);
                }
            } else if (cmd.equalsIgnoreCase("VS")) {
                client.setWaiting(false);
            }
        }

        /**
         * @param s
         */
        @Override
        public void parseReplyArgs(String s) {

        }

        /**
         * @param mwClient
         */
        @Override
        public void setClient(IClient mwClient) {

        }

        /**
         * @param s
         */
        @Override
        public void parseArguments(String s) {

        }
    }
