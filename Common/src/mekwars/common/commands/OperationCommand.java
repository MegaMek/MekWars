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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.operations.DefaultOperation;
import mekwars.common.campaign.operations.Operation;
import mekwars.common.gui.dialogs.opviewer.OperationViewerDialog;
import mekwars.common.util.MMNetXStream;

/**
 *
 * @author Spork
 *       <p>
 *       The OperationCommand class handles the Operation Viewer
 *
 */
public class OperationCommand extends Command {
    private final static MMLogger LOGGER = MMLogger.create(OperationCommand.class);

    public OperationCommand(IClient client) {
        super(client);
    }

    @Override
    public void execute(String input) {
        OperationViewerDialog operationViewerDialog;
        StringTokenizer stringTokenizer = decode(input);
        String cmd = stringTokenizer.nextToken().trim();

        switch (cmd.toLowerCase()) {
            case "add":
                String name = stringTokenizer.nextToken().trim();
                MMNetXStream xml = new MMNetXStream();
                Properties properties = (Properties) xml.fromXML(stringTokenizer.nextToken());
                Operation operation = new Operation(name, new DefaultOperation(), properties);
                String folder = "./data/operations/xml";
                String fileName = String.format("%s.xml", name);
                operation.writeToXmlFile(folder, fileName);
                break;
            case "view":
                operationViewerDialog = new OperationViewerDialog(client.getMainFrame(), client);
                new Thread(operationViewerDialog).start();
                break;
            case "md5":
                Hashtable<String, String> serverMd5s = new Hashtable<>();
                StringTokenizer tokenizer = new StringTokenizer(stringTokenizer.nextToken(), "#");

                while (tokenizer.hasMoreTokens()) {
                    String opName = tokenizer.nextToken();
                    String opMd5 = tokenizer.nextToken();
                    serverMd5s.put(opName, opMd5);
                }

                // Do we have a local md5 file?
                File md5File = new File("./data/operations/opsmd5.txt");
                if (!md5File.exists()) {
                    // No, we do not.
                    // Write it out. Since we don't know if we're synced up
                    // locally, pull *all* operations
                    FileWriter fileWriter;

                    try {
                        fileWriter = new FileWriter(md5File);

                        for (String key : serverMd5s.keySet()) {
                            fileWriter.write(String.format("%s#%s\n", key, serverMd5s.get(key)));
                        }

                        fileWriter.close();
                    } catch (IOException e) {
                        LOGGER.error(e, "Unable to write opsmd5.txt");
                    }

                    // Delete all local op xmls
                    File dir = new File("./data/operations/xml");
                    if (dir.exists()) {
                        String[] fileList = dir.list();

                        if (fileList != null) {
                            for (String string : fileList) {
                                if (string.endsWith(".xml")) {
                                    File file = new File(String.format("%s/%s", dir, string));
                                    file.delete();
                                }
                            }
                        }
                    }

                    client.sendChat(String.format("%sgetops getall", IClient.CAMPAIGN_PREFIX));
                    return;
                } else {
                    // We *do* have the file.  Check the contents.
                    Vector<String> opsToGet = new Vector<>();
                    Vector<String> opsToTest = new Vector<>();
                    try (FileInputStream fileInputStream = new FileInputStream(md5File)) {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
                        while (bufferedReader.ready()) {
                            String line = bufferedReader.readLine();
                            String[] arr = line.split("#");
                            String opName = arr[0];
                            String opMd5 = arr[1];

                            if (!opMd5.equalsIgnoreCase(serverMd5s.get(opName))) {
                                opsToGet.add(opName);
                            } else {
                                opsToTest.add(opName);
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.error(e, "Unable to read opsmd5.txt");
                    }

                    // If opsToGet is populated, then our MD5 file is wrong.
                    // Write out a new one
                    if (!opsToGet.isEmpty()) {
                        FileWriter fileWriter;

                        try {
                            fileWriter = new FileWriter(md5File);

                            for (String key : serverMd5s.keySet()) {
                                fileWriter.write(String.format("""
%s#%s
""", key, serverMd5s.get(key)));
                            }

                            fileWriter.close();
                        } catch (IOException e) {
                            LOGGER.error(e, "Unable to write opsmd5.txt");
                        }
                    }

                    // First, check that we don't have extraneous old xml files
                    File dir = new File("./data/operations/xml");
                    if (dir.exists()) {
                        String[] fileList = dir.list();

                        if (fileList != null) {
                            for (String string : fileList) {
                                if (string.endsWith(".xml")) {
                                    if (!opsToTest.contains(string.replace(".xml", ""))) {
                                        File file = new File(String.format("%s/%s", dir, string));
                                        file.delete();
                                    }
                                }
                            }
                        }
                    }

                    // Now, check that files that should exist do
                    if (!dir.exists()) {
                        // The xml dir doesn't exist.  Obviously we need to get everything
                        client.sendChat(String.format("%sgetops getall", IClient.CAMPAIGN_PREFIX));
                        return;
                    } else {
                        for (String opName : opsToTest) {
                            File file = new File(String.format("%s/%s.xml", dir, opName));

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
                        client.sendChat(String.format("%sgetops getsome#%s", IClient.CAMPAIGN_PREFIX, sb.toString()));
                    } else {
                        // Our ops are good
                        operationViewerDialog = new OperationViewerDialog(client.getMainFrame(), client);
                        new Thread(operationViewerDialog).start();
                    }
                }
                break;
            default:
                LOGGER.debug("Default case reached in OperationCommand command");
                break;
        }

    }

    /**
     *
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     *
     */
    @Override
    public void parseArguments(String s) {

    }
}
