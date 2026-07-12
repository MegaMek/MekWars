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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import megamek.logging.MMLogger;
import mekwars.common.CampaignData;

/**
 * Starts a server which provides diffs to data files for the clients.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class Server extends Thread {
    private final static MMLogger LOGGER = MMLogger.create(Server.class);

    private final CampaignData data;
    private final int dataPort;
    private final String IpAddress;

    public Server(CampaignData data, int dataPort, String IpAddress) {
        super("Server");
        this.data = data;
        this.dataPort = dataPort;
        this.IpAddress = IpAddress;
    }

    /**
     * Starts the server. It blocks forever, so make sure to run this in an extra thread.
     *
     */
    public void run() {
        LOGGER.info("DataProvider: startup...");

        //open and bind a socket and wait for incoming calls
        //If bindip is "-1", we want to bind to all available interfaces.
        ServerSocket server;

        try {
            if (IpAddress.equals("-1")) {
                server = new ServerSocket(dataPort, 0, null);
            } else {
                server = new ServerSocket(dataPort, 0, InetAddress.getByName(IpAddress));
            }

        } catch (IOException e) {
            LOGGER.error(e, "Shutting down during initial server creation because: {}", e.getLocalizedMessage());
            return;
        }

        LOGGER.info(String.format("DataProvider: server created at port %s. Address %s. Waiting for calls...", dataPort, IpAddress));

        //listen for new data requests until an error occurs, or forever.
        while (true) {
            try {
                Socket client = server.accept();
                new CommandTaskThread(client, data).start();
            } catch (OutOfMemoryError OOM) {
                LOGGER.error(OOM, "Out of Memory while opening data provider socket:");

                /*
                 * Ok, so too many socket connections, let's try a reset
                 * --Torren.
                 */
                try {
                    server.close();
                    server = null;
                    System.gc();
                    if (IpAddress.equals("-1")) {
                        server = new ServerSocket(dataPort, 0, null);
                    } else {
                        server = new ServerSocket(dataPort, 0, InetAddress.getByName(IpAddress));
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex, "Shutting down on OOM Server connections because: {}", ex.getLocalizedMessage());
                    return;
                }
            } catch (IOException e) {
                LOGGER.error(e, "DataProvider IO Exception:");
            } catch (Exception ex) {
                LOGGER.error(ex, "Unknown Exception: {}", ex.getLocalizedMessage());
            }
        }

    }//end run()

}//end Server.java
