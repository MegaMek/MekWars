/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Copuright (C) 2014 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


package mekwars.common.commands;

import java.util.StringTokenizer;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.operations.DefaultOperation;
import mekwars.common.campaign.operations.Operation;
import mekwars.common.gui.dialogs.opviewer.OperationViewerDialog;
import mekwars.common.util.MMNetXStream;
import mekwars.common.util.MWLogger;

/**
 *
 * @author Spork
 *       <p>
 *       The OperationCommand class handles the Operation Viewer
 *
 */
public class OperationCommand extends Command {

    public OperationCommand(IClient client) {
        super(client);
    }

    @Override
    public void execute(String input) {
        OperationViewerDialog ojd;
        StringTokenizer stringTokenizer = decode(input);
        String cmd = stringTokenizer.nextToken().trim();

        switch (cmd.toLowerCase()) {
            case "add":
                String name = stringTokenizer.nextToken().trim();
                MMNetXStream xml = new MMNetXStream();
                java.util.Properties properties = (java.util.Properties) xml.fromXML(stringTokenizer.nextToken());
                Operation operation = new Operation(name, new DefaultOperation(), properties);
                String folder = "./data/operations/xml";
                String fileName = STR."\{name}.xml";
                operation.writeToXmlFile(folder, fileName);
                break;
            case "view":
                ojd = new OperationViewerDialog(client.getMainFrame(), client);
                new Thread(ojd).start();
                break;
            case "md5":
                java.util.Hashtable<String, String> serverMd5s = new java.util.Hashtable<>();
                java.util.StringTokenizer stk = new java.util.StringTokenizer(stringTokenizer.nextToken(), "#");
                while (stk.hasMoreTokens()) {
                    String opName = stk.nextToken();
                    String opMd5 = stk.nextToken();
                    serverMd5s.put(opName, opMd5);
                }
                // Do we have a local md5 file?
                java.io.File md5File = new java.io.File("./data/operations/opsmd5.txt");
                if (!md5File.exists()) {
                    // No, we do not.
                    // Write it out. Since we don't know if we're synced up
                    // locally, pull *all* operations

                    java.io.FileWriter fw;

                    try {
                        fw = new java.io.FileWriter(md5File);
                        for (String key : serverMd5s.keySet()) {
                            fw.write(STR."\{key}#\{serverMd5s.get(key)}\n");
                        }
                        fw.close();
                    } catch (java.io.IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    // Delete all local op xmls
                    java.io.File dir = new java.io.File("./data/operations/xml");
                    if (dir.exists()) {
                        String[] fileList = dir.list();
                        if (fileList != null) {
                            for (String s : fileList) {
                                if (s.endsWith(".xml")) {
                                    java.io.File file = new java.io.File(STR."\{dir}/\{s}");
                                    file.delete();
                                }
                            }
                        }
                    }
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}getops getall");
                    return;
                } else {
                    // We *do* have the file.  Check the contents.
                    java.util.Vector<String> opsToGet = new java.util.Vector<>();
                    java.util.Vector<String> opsToTest = new java.util.Vector<>();
                    try (java.io.FileInputStream in = new java.io.FileInputStream(md5File)) {
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                        while (br.ready()) {
                            String line = br.readLine();
                            String[] arr = line.split("#");
                            String opName = arr[0];
                            String opMd5 = arr[1];

                            if (!opMd5.equalsIgnoreCase(serverMd5s.get(opName))) {
                                opsToGet.add(opName);
                            } else {
                                opsToTest.add(opName);
                            }
                        }
                    } catch (java.io.IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    // If opsToGet is populated, then our MD5 file is wrong.
                    // Write out a new one
                    if (!opsToGet.isEmpty()) {
                        java.io.FileWriter fw;
                        try {
                            fw = new java.io.FileWriter(md5File);

                            for (String key : serverMd5s.keySet()) {
                                fw.write(STR."""
\{key}#\{serverMd5s.get(key)}
""");
                            }

                            fw.close();
                        } catch (java.io.IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                    // First, check that we don't have extraneous old xml files
                    java.io.File dir = new java.io.File("./data/operations/xml");
                    if (dir.exists()) {
                        String[] fileList = dir.list();
                        if (fileList != null) {
                            for (String s : fileList) {
                                if (s.endsWith(".xml")) {
                                    if (!opsToTest.contains(s.replace(".xml", ""))) {
                                        java.io.File file = new java.io.File(STR."\{dir}/\{s}");
                                        file.delete();
                                    }
                                }
                            }
                        }
                    }

                    // Now, check that files that should exist do
                    if (!dir.exists()) {
                        // The xml dir doesn't exist.  Obviously we need to get everything
                        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}getops getall");
                        return;
                    } else {
                        for (String opName : opsToTest) {
                            java.io.File file = new java.io.File(STR."\{dir}/\{opName}.xml");
                            if (!file.exists()) {
                                opsToGet.add(opName);
                            }
                        }
                    }

                    // Now, do we *really* want to check the MD5?  At this point,
                    // if it's wrong, it's because the player hand-edited the
                    // thing, or a SO updated files not through the GUI and
                    // neglected to delete the md5 file.  The latter, we can't do
                    // anything about, and this would not fix anyway.  The former,
                    // meh, I can't account for player stupidity.

                    // In other words, maybe later.  Not now.

                    if (!opsToGet.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        boolean first = true;
                        for (String opName : opsToGet) {
                            if (first) {
                                sb.append(opName);
                                first = false;
                            } else {
                                sb.append("#").append(opName);
                            }
                        }

                        // Now, request these
                        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}getops getsome#\{sb.toString()}");
                    } else {
                        // Our ops are good
                        ojd = new OperationViewerDialog(client.getMainFrame(), client);
                        new Thread(ojd).start();
                    }
                }
                break;
            default:
                MWLogger.errLog("Default case reached in OperationCommand command");
                break;
        }

    }

    /**
     * @param s
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * @param s
     */
    @Override
    public void parseArguments(String s) {

    }
}
