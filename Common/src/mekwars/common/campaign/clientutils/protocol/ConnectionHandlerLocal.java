/*
 * Copyright (C) 2000 Lyrisoft Solutions, Inc. - Used by permission.
 * Copyright (C) 2005 Torren (torren@users.sourceforge.net)
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
import java.io.PrintStream;
import java.net.Socket;

import megamek.logging.MMLogger;
import mekwars.common.threads.ReaderThread;
import mekwars.common.threads.WriterThread;

/**
 * The keeper of the Socket on the client side. Using deprecated JDK1.0.2 I/O methods on purpose, because this may be
 * running in a crappy browser.
 * <p>
 * This method spawns two threads: One for reading and one for writing.  When new messages are read, they are passed to
 * the ChatServerLocal via its incomingMessage () method
 * <p>
 * This is the concrete, real-socket {@link IConnectionHandler} implementation used by {@link CConnector} on the
 * client side of the MekWars protocol; it owns the {@link Socket} plus a background {@link ReaderThread} (which
 * blocks reading lines and forwards them to the registered {@link IConnectionListener}) and a background
 * {@link WriterThread} (which drains a queue of outgoing messages onto the socket's output stream).
 *
 */

public class ConnectionHandlerLocal implements IConnectionHandler {
    private final static MMLogger LOGGER = MMLogger.create(ConnectionHandlerLocal.class);

    /** The underlying TCP socket connecting this client to the campaign server. */
    private final Socket _socket;
    /** Output stream wrapper used to write outgoing data to the socket. */
    protected PrintStream _out;
    /** The listener notified of incoming messages and connection loss; set via {@link #setListener}. */
    protected IConnectionListener _listener;
    /** Background thread that blocks reading lines from the socket and forwards them to {@link #_listener}. */
    protected ReaderThread _reader;
    /** Background thread that drains queued outgoing messages and writes them to {@link #_out}. */
    protected WriterThread _writer;

    /**
     * Construct the ConnectionHandler and spawn the reader and writer threads. Note: the writer thread is started
     * immediately here, but the reader thread is not started until {@link #setListener(IConnectionListener)} is
     * called (it needs a listener to forward incoming messages to before it can usefully run).
     *
     * @param socket the already-connected socket to the campaign server
     * @throws IOException if the socket's output stream cannot be obtained
     */
    public ConnectionHandlerLocal(Socket socket) throws IOException {
        _socket = socket;
        _out = new PrintStream(socket.getOutputStream());
        _reader = new ReaderThread(this, _socket);
        _writer = new WriterThread(_out);
        _writer.start();
    }

    /**
     * Queue an outgoing message. Handed off to {@link #_writer}'s internal queue; actual transmission happens
     * asynchronously on the writer thread.
     */
    public void queueMessage(String message) {
        _writer.queueMessage(message);
    }

    /**
     * Writes a message directly to the socket's output stream and flushes immediately, bypassing the writer
     * thread's queue entirely.
     */
    public void sendImmediately(String message) {
        _out.println(message);
        _out.flush();
    }

    /**
     * Try to stop the threads gracefully, close the socket, then call connectionLost() on the ChatServerLocal. This
     * method is typically called by the ReaderThread when it has detected the connection died.
     *
     * @param notify whether to notify {@link #_listener} (via {@link IConnectionListener#socketClosed()}) that the
     *               connection was closed; pass false when the listener itself already knows (e.g. it initiated the
     *               shutdown) to avoid a redundant/re-entrant notification
     */
    public void shutdown(boolean notify) {
        _reader.pleaseStop();
        _writer.pleaseStop();
        _writer.flushOutputQueue();

        try {_socket.close();} catch (IOException ex) {
            LOGGER.error(ex, "Error while shutting down: {}", ex.getMessage());
        }

        if (notify) {
            _listener.socketClosed();
        }
    }

    /**
     * Registers the listener to receive incoming-message/socket-closed notifications, wires it into the reader
     * thread, and starts the reader thread running.
     */
    public void setListener(IConnectionListener listener) {
        _listener = listener;
        _reader.setListener(listener);
        _reader.start();
    }
}

