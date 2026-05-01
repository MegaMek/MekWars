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

import mekwars.common.util.MWLogger;

/**
 * Write the messages in the queue to the socket's output stream
 */
public class WriterThread extends Thread {
    private final Vector<String> outgoingMessages;
    private final PrintStream _out;
    private boolean keepGoing = true;

    WriterThread(PrintStream out) {
        super("ConnectionHandler$WriterThread");
        _out = out;
        outgoingMessages = new Vector<>(1, 1);
    }

    @Override
    public synchronized void run() {
        try {
            while (keepGoing) {
                flushOutputQueue();
                // wait until there are more messages
                wait(1000);
            }
            MWLogger.errLog("WriterThread: stopping gracefully.");
        } catch (InterruptedException e) {
            MWLogger.errLog("ConnectionHandlerLocal$WriterThread.run(): Interrupted!");
        }
    }

    void flushOutputQueue() {
        while (!outgoingMessages.isEmpty()) {
            String message = outgoingMessages.elementAt(0);
            outgoingMessages.removeElementAt(0);
            mekwars.common.campaign.clientutils.protocol.ConnectionHandlerLocal.DEBUG(STR."> \{message}");
            _out.println(message);
        }
        _out.flush();
    }


    synchronized void queueMessage(String s) {
        outgoingMessages.addElement(s);
        // notify the writer thread that there is at least one new message
        notify();
    }

    void pleaseStop() {
        keepGoing = false;
    }

}
