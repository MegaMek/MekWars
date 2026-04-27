/*
 * MekWars - Copyright (C) 2014
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
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

package mekwars.client.commands;

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
                String fileName = name + ".xml";
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
                    java.io.FileWriter fw = null;
                    try {
                        fw = new java.io.FileWriter(md5File);
                    } catch (java.io.IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    for (String key : serverMd5s.keySet()) {
                        try {
                            fw.write(key + "#" + serverMd5s.get(key) + "\n");
                        } catch (java.io.IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    try {
                        fw.close();
                    } catch (java.io.IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // Delete all local op xmls
                    java.io.File dir = new java.io.File("./data/operations/xml");
                    if (dir.exists()) {
                        String[] fileList = dir.list();
                        for (String s : fileList) {
                            if (s.endsWith(".xml")) {
                                java.io.File file = new java.io.File(dir + "/" + s);
                                file.delete();
                            }
                        }
                    }
                    client.sendChat(client.MWClient.CAMPAIGN_PREFIX + "getops getall");
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
                        java.io.FileWriter fw = null;
                        try {
                            fw = new java.io.FileWriter(md5File);
                        } catch (java.io.IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }

                        for (String key : serverMd5s.keySet()) {
                            try {
                                fw.write(key + "#" + serverMd5s.get(key) + "\n");
                            } catch (java.io.IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }

                        try {
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
                        for (String s : fileList) {
                            if (s.endsWith(".xml")) {
                                if (!opsToTest.contains(s.replace(".xml", ""))) {
                                    java.io.File file = new java.io.File(dir + "/" + s);
                                    file.delete();
                                }
                            }
                        }
                    }

                    // Now, check that files that should exist do
                    if (!dir.exists()) {
                        // The xml dir doesn't exist.  Obviously we need to get everything
                        client.sendChat(client.MWClient.CAMPAIGN_PREFIX + "getops getall");
                        return;
                    } else {
                        for (String opName : opsToTest) {
                            java.io.File file = new java.io.File(dir + "/" + opName + ".xml");
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
                                sb.append("#" + opName);
                            }
                        }
                        // Now, request these
                        //JOptionPane.showMessageDialog(null, opsToGet);
                        client.sendChat(client.MWClient.CAMPAIGN_PREFIX + "getops getsome#" + sb.toString());
                    } else {
                        // Our ops are good
                        ojd = new client.gui.dialog.opviewer.OperationViewerDialog(client.getMainFrame(), client);
                        new Thread(ojd).run();
                    }
                }
                break;
            default:
                MWLogger.errLog("Default case reached in OperationCommand command");
                break;
        }

    }

}
