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

/**
 * Client-side network gateway between a MekWars {@link IClient} and the campaign server. Owns the TCP socket
 * lifecycle (via an {@link IConnectionHandler}, normally a {@link ConnectionHandlerLocal}) and translates between
 * the low-level {@link IConnectionListener} callbacks it receives and the higher-level {@link IClient} methods
 * ({@link IClient#processIncoming(String)}, {@link IClient#connectionLost()}, {@link IClient#connectionEstablished()}).
 * Also provides the static Base64 encode/decode helpers used to obscure protocol payloads on the wire.
 */
public class CConnector implements IConnectionListener {
    final private static MMLogger LOGGER = MMLogger.create(CConnector.class);

    /** The client this connector delivers incoming messages to and reports connection status changes on. */
    protected IClient client;

    /** Hostname/address of the campaign server to connect to. Empty until set via a constructor or {@link #connect(String, int)}. */
    protected String _host = "";
    /** TCP port of the campaign server to connect to. -1 until set via a constructor or {@link #connect(String, int)}. */
    protected int _port = -1;
    /** Whether a connection to the server is currently established. */
    protected boolean _connected = false;
    /** The low-level handler performing actual socket reads/writes once connected; null until {@link #connect()} succeeds. */
    protected IConnectionHandler _connectionHandler;
    /** Optional splash/loading window to notify of connection failures, or null if none is showing. */
    private SplashWindow splash;

    /**
     * Creates a connector with no host/port set yet. Call {@link #connect(String, int)} later to specify where to
     * connect.
     *
     * @param client the client to deliver incoming data and connection events to
     */
    public CConnector(IClient client) {
        this.client = client;
    }

    /**
     * Creates a connector pre-configured with a host and port.
     *
     * @param client the client to deliver incoming data and connection events to
     * @param host   the campaign server hostname/address
     * @param port   the campaign server TCP port
     */
    public CConnector(IClient client, String host, int port) {
        this.client = client;
        _host = host;
        _port = port;
    }

    /**
     * Base64-encodes a string's UTF default-charset bytes for wire transmission.
     *
     * @param data the string to encode
     * @return the Base64-encoded representation
     */
    public static String encode(String data) {
        return encode(data.getBytes());
    }

    /**
     * Base64-encodes raw bytes for wire transmission.
     *
     * @param data the bytes to encode
     * @return the Base64-encoded representation
     */
    public static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decodes a Base64-encoded protocol payload back to raw bytes.
     *
     * @param data the Base64 string to decode
     * @return the decoded bytes
     */
    public static byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    /**
     * @return true if a socket connection to the server is currently established.
     */
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
     * Construct and queue an outgoing message. Logs the message at INFO level, unless it is a "sendclientdata",
     * "sendtomisc", or "/pong" message — those are excluded from logging, presumably because they fire frequently
     * enough (or carry large enough payloads) to flood the log.
     *
     * @param message the fully-formed protocol message to send (already delimited/encoded as needed)
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

    /**
     * Sets the target host/port and then delegates to {@link #connect()} to open the connection.
     *
     * @param host the campaign server hostname/address
     * @param port the campaign server TCP port
     */
    public void connect(String host, int port) {
        _host = host;
        _port = port;
        connect();
    }

    /**
     * Opens a socket connection to the previously configured {@link #_host}/{@link #_port}, if not already
     * connected. Does nothing (just logs) if already connected, or if host/port haven't been set. On success,
     * disables Nagle's algorithm on the socket, creates a {@link ConnectionHandlerLocal} to own I/O, registers this
     * connector as its listener, marks the connection established, and notifies {@link IClient#connectionEstablished()}.
     * On failure, logs the error and, if a {@link SplashWindow} has been registered via
     * {@link #setSplashWindow(SplashWindow)}, updates it to show a connection-failed status.
     */
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

    /**
     * Closes the current connection by shutting down the connection handler and notifying the listener (this
     * connector, which will in turn call {@link IClient#connectionLost()} via {@link #socketClosed()}).
     */
    public void closeConnection() {
        _connectionHandler.shutdown(true);
    }

    /**
     * Registers a splash/loading window to be notified (its status updated) if a subsequent {@link #connect()}
     * attempt fails.
     *
     * @param s the splash window to notify, or null to stop notifying one
     */
    public void setSplashWindow(SplashWindow s) {
        splash = s;
    }
}
