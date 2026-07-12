/*
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
package mekwars.common.threads;

import java.io.PrintStream;
import java.util.Vector;

import megamek.logging.MMLogger;

/**
 * Background thread that owns the outgoing side of a socket connection: callers enqueue outbound
 * protocol message strings via {@link #queueMessage(String)}, and this thread drains the queue and
 * writes each message as a line to the socket's {@link PrintStream}.
 * <p>
 * This is the write half of the client/server connection; the corresponding read half is
 * {@link ReaderThread}. Producers (e.g. connection handler code building protocol commands) never
 * write to the socket directly — they only ever call {@link #queueMessage(String)}, keeping all
 * actual socket writes on this single thread.
 */
public class WriterThread extends Thread {
    private final static MMLogger LOGGER = MMLogger.create(WriterThread.class);

    /** FIFO queue of not-yet-sent message strings; access is synchronized via this thread's monitor. */
    private final Vector<String> outgoingMessages;
    /** The socket's output stream that messages are ultimately written (and flushed) to. */
    private final PrintStream _out;
    /** Checked each loop iteration in {@link #run()}; cleared by {@link #pleaseStop()} to end the thread. */
    private boolean keepGoing = true;

    /**
     * @param out the print stream wrapping the socket's output stream that queued messages will be
     *            written to
     */
    public WriterThread(PrintStream out) {
        super("ConnectionHandler$WriterThread");
        _out = out;
        outgoingMessages = new Vector<>(1, 1);
    }

    /**
     * Main write loop: flushes any currently queued messages to the socket, then waits (via
     * {@link Object#wait(long)} on this thread's own monitor, since the method is
     * {@code synchronized}) for up to 1 second before checking again. {@link #queueMessage(String)}
     * calls {@code notify()} to wake this loop early as soon as a new message is enqueued, so the
     * 1-second wait is just a safety-net poll interval rather than the only trigger. Runs until
     * {@link #pleaseStop()} clears {@link #keepGoing}; an {@link InterruptedException} during the
     * wait is logged and ends the thread (it is not otherwise retried).
     */
    @Override
    public synchronized void run() {
        try {
            while (keepGoing) {
                flushOutputQueue();
                // wait until there are more messages
                wait(1000);
            }
            LOGGER.debug("WriterThread: stopping gracefully.");
        } catch (InterruptedException e) {
            LOGGER.error(e, "ConnectionHandlerLocal$WriterThread.run(): Interrupted!");
        }
    }

    /**
     * Dequeues and writes every currently pending message (in FIFO order) to {@link #_out}, then
     * flushes the stream once at the end. Not synchronized itself — it is only ever invoked from
     * within the {@code synchronized} {@link #run()} method, so removals from {@link #outgoingMessages}
     * are safe with respect to {@link #queueMessage(String)} (which is separately synchronized).
     */
    public void flushOutputQueue() {
        while (!outgoingMessages.isEmpty()) {
            String message = outgoingMessages.elementAt(0);
            outgoingMessages.removeElementAt(0);
            _out.println(message);
        }

        _out.flush();
    }


    /**
     * Appends a message to the outgoing queue and wakes up the writer loop (via {@code notify()})
     * so it doesn't have to wait out the rest of its poll interval before sending it.
     *
     * @param s the raw protocol message line to send
     */
    synchronized public void queueMessage(String s) {
        outgoingMessages.addElement(s);
        // notify the writer thread that there is at least one new message
        notify();
    }

    /**
     * Signals the write loop in {@link #run()} to exit after its current wait/flush cycle. Any
     * messages queued after this call but before the thread actually exits may not be sent.
     */
    public void pleaseStop() {
        keepGoing = false;
    }
}
