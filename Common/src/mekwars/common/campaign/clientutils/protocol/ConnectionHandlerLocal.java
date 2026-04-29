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

import mekwars.common.util.MWLogger;

/**
 * The keeper of the Socket on the client side. Using deprecated JDK1.0.2 I/O methods on purpose, because this may be
 * running in a crappy browser.
 * <p>
 * This method spawns two threads: One for reading and one for writing.  When new messages are read, they are passed to
 * the ChatServerLocal via its incomingMessage () method
 *
 * @see ChatServerLocal#incomingMessage
 */

public class ConnectionHandlerLocal implements IConnectionHandler {
    static final boolean DEBUG = false;
    private final Socket _socket;
    protected PrintStream _out;
    protected IConnectionListener _listener;
    protected ReaderThread _reader;
    protected WriterThread _writer;

    /**
     * Construct the ConnectionHandler and spawn the reader and writer threads.
     */
    public ConnectionHandlerLocal(Socket socket) throws IOException {
        _socket = socket;
        _out = new PrintStream(socket.getOutputStream());
        _reader = new ReaderThread(this, _socket);
        _writer = new WriterThread(_out);
        _writer.start();
    }

    public static void DEBUG(String s) {
        if (DEBUG) {
            MWLogger.errLog(s);
        }
    }

    /**
     * Queue an outgoing message
     */
    public void queueMessage(String message) {
        _writer.queueMessage(message);
    }

    public void sendImmediately(String message) {
        _out.println(message);
        _out.flush();
    }

    /**
     * Try to stop the threads gracefully, close the socket, then call connectionLost() on the ChatServerLocal. This
     * method is typically called by the ReaderThread when it has detected the the connection died.
     */
    public void shutdown(boolean notify) {
        _reader.pleaseStop();
        _writer.pleaseStop();
        _writer.flushOutputQueue();

        try {_socket.close();} catch (IOException e) {
            MWLogger.errLog("Error closing socket.");
            MWLogger.errLog(e);
        }

        if (notify) {
            _listener.socketClosed();
        }
    }

    public void setListener(IConnectionListener listener) {
        _listener = listener;
        _reader.setListener(listener);
        _reader.start();
    }
}

