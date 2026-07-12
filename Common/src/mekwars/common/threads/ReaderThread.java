/*
 * Copyright (C) 2000 Lyrisoft Solutions, Inc. - Used by permission.
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

package mekwars.common.threads;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.Inflater;

import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.clientutils.protocol.IConnectionHandler;
import mekwars.common.campaign.clientutils.protocol.IConnectionListener;

/**
 * Background thread that continuously reads line-oriented protocol messages from a socket's
 * {@link InputStream} and hands each one off to an {@link IConnectionListener} (typically the
 * client-side or server-side connection handler that interprets the MekWars text protocol).
 * <p>
 * This is the read half of the client/server connection; the corresponding write half is
 * {@link WriterThread}. Both threads are normally owned by an {@link IConnectionHandler}, which is
 * notified (via {@link IConnectionHandler#shutdown(boolean)}) if the read loop dies unexpectedly.
 * <p>
 * Messages are read one line at a time (delimited by a raw {@code '\n'} byte, not a full line
 * terminator abstraction) and decoded as UTF-8. Messages that start with the {@link IClient#DEFLATED}
 * marker are treated specially: they carry a zlib/deflate-compressed payload (used to cut down
 * bandwidth for large messages) which is decompressed via {@link #inflate(String)} and delivered to
 * the listener as one or more decompressed lines instead of the raw compressed line itself.
 */
public class ReaderThread extends Thread {
    private final static MMLogger LOGGER = MMLogger.create(ReaderThread.class);

    private static final int NL = 10; // "\n" in ASCII and UTF8
    /** Handler notified with {@code shutdown(true)} if the read loop terminates due to an I/O error. */
    private final IConnectionHandler _connectionHandler;
    /** Reused zlib inflater for decompressing {@link IClient#DEFLATED}-prefixed messages. */
    private final Inflater inflater = new Inflater();
    /** Scratch buffer used by {@link #readLine()} to accumulate bytes of the current line; reused across reads. */
    private final byte[] rlBuffer = new byte[256 * 256];
    /** Flag checked each loop iteration in {@link #run()}; set to false by {@link #pleaseStop()} to end the thread gracefully. */
    private boolean keepGoing = true;

    //private Checksum checksum = new CRC32();
    private InputStream _sis;
    /** Recipient of every decoded (and, where applicable, decompressed) incoming message line. */
    private IConnectionListener _listener;

    /**
     * Creates the reader thread and immediately obtains the socket's input stream (any failure to
     * do so is logged, leaving {@link #_sis} {@code null} and {@link #run()} likely to throw a
     * {@link NullPointerException} on first read — the constructor does not fail fast).
     *
     * @param handler connection handler to notify if the read loop exits due to an I/O error
     * @param s       the connected socket to read from
     */
    public ReaderThread(IConnectionHandler handler, Socket s) {
        super("ConnectionHandler$ReaderThread");

        try {
            _sis = s.getInputStream();
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to get input stream. {}", ex.getLocalizedMessage());
        }

        _connectionHandler = handler;
    }

    /**
     * Sets (or replaces) the listener that receives incoming messages. Until this is called, lines
     * read from the socket are simply logged at debug level and discarded (see {@link #run()}).
     *
     * @param listener callback to receive each incoming protocol message line
     */
    public void setListener(IConnectionListener listener) {
        _listener = listener;
    }

    /**
     * Main read loop: repeatedly blocks in {@link #readLine()} until a full line is available,
     * then either decompresses it (if it is a {@link IClient#DEFLATED}-prefixed message) and
     * forwards the decompressed lines to the listener, or forwards the raw line directly.
     * Continues until {@link #keepGoing} is cleared via {@link #pleaseStop()} or an
     * {@link IOException} occurs (e.g. the peer closed the socket). On an unexpected IOException
     * while still supposed to be running, this calls {@link #pleaseStop()}, logs the error, and
     * tells the connection handler to {@link IConnectionHandler#shutdown(boolean) shutdown(true)}.
     * If the socket was closed deliberately via {@link #pleaseStop()} first, the IOException (if
     * any) from the in-flight read is swallowed without notifying the handler.
     */
    @Override
    public void run() {
        try {
            String newLine;
            while (keepGoing) {

                newLine = readLine();
                if (_listener != null) {

                    if (newLine.startsWith(IClient.DEFLATED)) {
                        String[] args = decompose(newLine);
                        if (args.length > 0) { // can be 0 if server is having problems
                            try {
                                inflate(newLine);
                            } catch (Exception ex) {
                                LOGGER.error(ex, "Unable to inflate: {}", ex.getLocalizedMessage());
                            }

                            continue;
                        }
                    }

                    //else
                    _listener.incomingMessage(newLine);

                } else {
                    LOGGER.debug(String.format("Null listener: %s", newLine));
                }
            }

            LOGGER.debug("ReaderThread: stopping gracefully.");
        } catch (IOException e) {
            if (keepGoing) {
                pleaseStop();
                LOGGER.error(e, "ReaderThread Error");
                _connectionHandler.shutdown(true);
            }
        }
    }

    /**
     * Blocks reading single bytes from the socket's input stream into {@link #rlBuffer} until a
     * {@code '\n'} ({@link #NL}) byte is encountered, then returns everything read before it
     * (excluding the newline) decoded as UTF-8.
     * <p>
     * Note: {@link #rlBuffer} is sized at 256*256 (64KB) and there is no bounds check on the
     * accumulating index {@code n} — a line longer than that (e.g. a malformed or malicious
     * message without a timely newline) would overflow the buffer with an
     * {@link ArrayIndexOutOfBoundsException}.
     *
     * @return the next newline-delimited line from the stream, decoded as UTF-8
     * @throws IOException if the underlying socket read fails or the stream is closed
     */
    private String readLine() throws IOException {
        int n = 0;
        int i;

        while ((i = _sis.read()) != NL) {
            rlBuffer[n++] = (byte) i;
        }

        byte[] a = new byte[n];
        System.arraycopy(rlBuffer, 0, a, 0, n);
        return new String(a, StandardCharsets.UTF_8);
    }

    /**
     * Decompose a raw message into an array of String, splitting on the DELIMITER defined in ICommands.
     */

    private String[] decompose(String input) {
        StringTokenizer st = new StringTokenizer(input, IClient.DELIMITER);
        Vector<String> v = new Vector<>(5, 1);

        while (st.hasMoreTokens()) {
            v.addElement(st.nextToken());
        }

        String[] args = new String[v.size()];
        v.copyInto(args);
        return args;
    }

    /**
     * Handles a {@link IClient#DEFLATED}-prefixed message: parses the compressed payload size (and
     * optional uncompressed size hint) out of the message header, reads exactly that many
     * compressed bytes directly from the socket (blocking/looping until all are received), inflates
     * them with zlib, and delivers each resulting decompressed line to {@link #_listener} as if it
     * had been received normally.
     * <p>
     * An {@link megamek.common.util.Inflater} instance is reused (via {@link #inflater}, reset each
     * call) rather than wrapping the stream in an {@code InflaterInputStream}, specifically so the
     * inflate step doesn't consume/buffer extra bytes from {@link #_sis} beyond the exact compressed
     * payload, which would desync the stream position used by subsequent {@link #readLine()} calls.
     * <p>
     * The uncompressed buffer defaults to 29999 bytes if the message doesn't specify a larger size;
     * if the actual decompressed content exceeds that (or the declared size), data would be
     * truncated or an exception thrown by {@link Inflater#inflate(byte[])}.
     *
     * @param command the raw {@link IClient#DEFLATED}-prefixed line (used only to extract the size
     *                arguments; the compressed bytes themselves are read separately from the stream)
     * @throws Exception if the header cannot be parsed or inflation fails
     */
    private void inflate(String command) throws Exception {
        String[] args = decompose(command);
        int size = Integer.parseInt(args[1]);
        int fullSize = 29999;

        //just in case
        if (args.length > 2) {
            fullSize = Integer.parseInt(args[2]);
        }

        byte[] compressedBytes = new byte[size];
        byte[] rawBytes = new byte[fullSize];

        // use an Inflater instead of InflaterInputStream so we don't have to worry about the internal IIS buffers
        // screwing our stream position.
        int totalRead = 0;

        while (totalRead < size) {
            totalRead += _sis.read(compressedBytes, totalRead, size - totalRead);
            LOGGER.debug(String.format("< Read %s of %s", totalRead, size));
        }

        inflater.reset();
        inflater.setInput(compressedBytes, 0, size);
        int textLength = inflater.inflate(rawBytes);

        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rawBytes, 0, textLength),
              StandardCharsets.UTF_8));
        while ((command = br.readLine()) != null) {
            LOGGER.debug(String.format("< inflated: %s", command));
            _listener.incomingMessage(command);
        }
    }

    /**
     * Signals the read loop in {@link #run()} to stop after its current blocking read completes (or
     * immediately if it's between reads). Does not interrupt an in-progress blocking socket read —
     * the thread only actually exits once {@link #readLine()} returns or throws, so closing the
     * underlying socket separately is typically required to unblock a thread stuck reading.
     */
    public void pleaseStop() {
        keepGoing = false;
    }
}
