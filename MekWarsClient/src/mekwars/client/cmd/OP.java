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

package mekwars.client.cmd;

import common.campaign.operations.DefaultOperation;
import common.campaign.operations.Operation;
import common.util.MMNetXStream;
import common.util.MWLogger;

/**
 *
 * @author Spork
 *       <p>
 *       The OP class handles the Operation Viewer
 *
 */
public class OP extends Command {

    public OP(client.MWClient client) {
        super(client);
    }

    @Override
    public void execute(String input) {
        client.gui.dialog.opviewer.OperationViewerDialog ojd = null;
        java.util.StringTokenizer st = decode(input);
        String cmd = st.nextToken().trim();

        switch (cmd.toLowerCase()) {
            case "add":
                String name = st.nextToken().trim();
                MMNetXStream xml = new MMNetXStream();
                java.util.Properties p = (java.util.Properties) xml.fromXML(st.nextToken());
                Operation o = new Operation(name, new DefaultOperation(), p);
                String folder = "./data/operations/xml";
                String fileName = name + ".xml";
                o.writeToXmlFile(folder, fileName);
                break;
            case "view":
                ojd = new client.gui.dialog.opviewer.OperationViewerDialog(mwclient.getMainFrame(), mwclient);
                new Thread(ojd).run();
                break;
            case "md5":
                java.util.Hashtable<String, String> serverMd5s = new java.util.Hashtable<String, String>();
                java.util.StringTokenizer stk = new java.util.StringTokenizer(st.nextToken(), "#");
                while (stk.hasMoreTokens()) {
                    String opName = stk.nextToken();
                    String opMd5 = stk.nextToken();
                    serverMd5s.put(opName, opMd5);
                }
                // Do we have a local md5 file?
                java.io.File md5File = new java.io.File("./data/operations/opsmd5.txt");
                if (!md5File.exists()) {
                    // No, we do not.
                    // Write it out. Since we don't know if we're synched up
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
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "getops getall");
                    return;
                } else {
                    // We *do* have the file.  Check the contents.
                    java.util.Vector<String> opsToGet = new java.util.Vector<String>();
                    java.util.Vector<String> opsToTest = new java.util.Vector<String>();
                    java.io.FileInputStream in = null;
                    try {
                        in = new java.io.FileInputStream(md5File);
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                        try {
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
                    } catch (java.io.FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (java.io.IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                    // If opsToGet is populated, then our MD5 file is wrong.
                    // Write out a new one
                    if (opsToGet.size() > 0) {
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
                        mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "getops getall");
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

                    if (opsToGet.size() > 0) {
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
                        mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "getops getsome#" + sb.toString());
                    } else {
                        // Our ops are good
                        ojd = new client.gui.dialog.opviewer.OperationViewerDialog(mwclient.getMainFrame(), mwclient);
                        new Thread(ojd).run();
                    }
                }
                break;
            default:
                MWLogger.errLog("Default case reached in OP command");
                break;
        }

    }

}
