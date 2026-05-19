/*
 * Copyright (C) 2004 MekWars
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

package mekwars.server.dataProvider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import megamek.logging.MMLogger;
import mekwars.common.CampaignData;
import mekwars.common.persistence.BinWriter;

/**
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class CommandTaskThread extends Thread {
    private final static MMLogger LOGGER = MMLogger.create(CommandTaskThread.class);

    private Socket client;
    private CampaignData data;

    /**
     * Create a new thread to handle an incoming call
     */
    public CommandTaskThread(Socket client, CampaignData data) {
        super("Command Task Thread");
        try {
            this.client = client;
            this.data = data;
        } catch (Exception ex) {
            LOGGER.error(ex, "Error created a CommandTaskThread: {}", ex.getLocalizedMessage());
        }
    }

    /**
     * @see Runnable#run()
     */
    @Override
    public void run() {
        // timestamp is bufferedReader this format
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        LOGGER.info("DataProvider call accepted from {}", client.getInetAddress());
        BinWriter out = null;
        BufferedReader bufferedReader;
        String cmdStr;
        String timeStr;

        try {
            bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            while ((cmdStr = bufferedReader.readLine()) != null) {
                //read the command name
                try {
                    timeStr = bufferedReader.readLine();
                } catch (Exception e) {
                    bufferedReader.close();
                    LOGGER.error(e, "Error getting data provider command or timestamp from client.");
                    return;
                }//end command name try/catch

                if (out == null) {
                    try {
                        out = new BinWriter(new PrintWriter(client.getOutputStream()));
                    } catch (Exception e) {
                        bufferedReader.close();
                        LOGGER.error(e, "Error bufferedReader data provider while creating output stream.");
                        return;
                    }
                }//end output stream if

                //get the actual command class
                Class<?> cmdClass;
                ServerCommand cmd;
                try {
                    cmdClass = Class.forName(STR."server.dataProvider.commands.\{cmdStr}");
                    cmd = (ServerCommand) cmdClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    bufferedReader.close();
                    out.close();
                    LOGGER.error(e, "Error creating DataProvider command: {}", cmdStr);
                    return;
                }//end command class try/catch

                //writing timestamp
                out.println(simpleDateFormat.format(new Date()), "lastTimestamp");

                //execute command
                try {
                    cmd.execute(timeStr.isEmpty() ? null : simpleDateFormat.parse(timeStr), out, data);
                } catch (Exception e) {
                    bufferedReader.close();
                    out.close();
                    LOGGER.error(e, "Error executing DataProvider command: {}", cmdStr);
                    return;
                }//end execute try/catch
                out.flush();
            }//end While
            try {
                LOGGER.info("Closing DataProvider call from {}", client.getInetAddress());
                bufferedReader.close();

                if (out != null) {
                    out.close();
                }

                client.close();
                client = null;
            } catch (SocketException se) {
                //no reason to report closed sockets.
                client = null;
            } catch (Exception e) {
                LOGGER.error(e, "Not a socket exception: {}", e.getLocalizedMessage());
            }//end client.close() try
        } catch (java.net.SocketException ignored) {
        } catch (java.net.SocketTimeoutException ste) {
            try {
                if (out != null) {
                    out.close();
                }
                client.close();
                LOGGER.info("TimeOut DataProvider call from {}", client.getInetAddress());
            } catch (Exception ignored) {

            }
            client = null;
        } catch (Exception ex) {
            LOGGER.error(ex);
        }//end first try
    }//end run()
}
