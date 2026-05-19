/*
 * Copyright (C) 2005 - Torren (torren@users.sourceforge.net)
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

/*
 * Derived from NFCChat, a GPL chat client/server.
 * Original code can be found @ http://nfcchat.sourceforge.net
 * Our thanks to the original authors.
 */

package mekwars.common.campaign.clientutils.protocol;

import java.io.IOException;
import java.net.Socket;
import java.util.Base64;

import megamek.logging.MMLogger;
import mekwars.common.gui.SplashWindow;

public class CConnector implements IConnectionListener {
    final private static MMLogger LOGGER = MMLogger.create(CConnector.class);

    protected IClient client;

    protected String _host = "";
    protected int _port = -1;
    protected boolean _connected = false;
    protected IConnectionHandler _connectionHandler;
    private SplashWindow splash;

    public CConnector(IClient client) {
        this.client = client;
    }

    public CConnector(IClient client, String host, int port) {
        this.client = client;
        _host = host;
        _port = port;
    }

    public static String encode(String data) {
        return encode(data.getBytes());
    }

    public static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    public boolean isConnected() {
        return _connected;
    }

    /**
     * This method is called by ConnectionHandlerLocal when a new message comes in from the server.
     */
    public void incomingMessage(String message) {
        client.processIncoming(message);
    }

    /**
     * This method is called by ConnectionHandlerLocal when the connection to the server is lost. connectionLost() is
     * called on the client to inform it that the connection is lost.
     */
    public void socketClosed() {
        _connected = false;
        client.connectionLost();
    }

    /**
     * Construct and queue an outgoing message.
     */
    public void send(String message) {
        if (!message.contains("CH%7c%2fc+sendclientdata%23")
                  && !message.contains("CH%7c%2fc+sendtomisc%23")
                  && !message.contains("/pong")) {
            LOGGER.info("SENT: {}", message);
        }

        _connectionHandler.queueMessage(message);
    }

    /**
     * Make a socket connection to the server (if we're not already connected). Once connected, create a
     * ConnectionHandlerLocal that will handle I/O.
     *
     * @see ConnectionHandlerLocal
     */

    public void connect(String host, int port) {
        _host = host;
        _port = port;
        connect();
    }

    public void connect() {
        if (_connected) {
            LOGGER.info("Already connected...");
            return;
        }

        if (_host.isEmpty() || _port == -1) {
            LOGGER.info("no host or port set...");
            return;
        }

        LOGGER.info("Opening socket connection to {}: {}", _host, _port);
        Socket socket;

        try {
            socket = new Socket(_host, _port);
            LOGGER.info("CConnector: connected to {}:{}", _host, _port);
            socket.setTcpNoDelay(true);
            _connectionHandler = new ConnectionHandlerLocal(socket);
            _connectionHandler.setListener(this);
            _connected = true;
            client.connectionEstablished();
        } catch (IOException ex) {
            LOGGER.error(ex, "Failed to Connect: {}", ex.getMessage());

            if (splash != null) {
                splash.setStatus(splash.STATUS_CONNECT_FAILED);
            }
        }
    }

    public void closeConnection() {
        _connectionHandler.shutdown(true);
    }

    public void setSplashWindow(SplashWindow s) {
        splash = s;
    }
}
