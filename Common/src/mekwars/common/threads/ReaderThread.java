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

import mekwars.common.campaign.clientutils.protocol.ConnectionHandlerLocal;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.clientutils.protocol.IConnectionHandler;
import mekwars.common.campaign.clientutils.protocol.IConnectionListener;
import mekwars.common.util.MWLogger;

/**
 * Constantly read from the socket's input stream
 */
public class ReaderThread extends Thread {
    private static final int NL = 10; // "\n" in ASCII and UTF8
    private final IConnectionHandler _connectionHandler;
    private final Inflater inflater = new Inflater();
    private final byte[] rlBuffer = new byte[256 * 256];
    private boolean keepGoing = true;

    //private Checksum checksum = new CRC32();
    private InputStream _sis;
    private IConnectionListener _listener;

    public ReaderThread(IConnectionHandler handler, Socket s) {
        super("ConnectionHandler$ReaderThread");
        //_in = in;
        try {
            _sis = s.getInputStream();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
        _connectionHandler = handler;
    }

    public void setListener(IConnectionListener listener) {
        _listener = listener;
    }

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
                                MWLogger.errLog(ex);
                            }
                            continue;
                        }
                    }

                    //else
                    ConnectionHandlerLocal.DEBUG(STR."< \{newLine}");
                    _listener.incomingMessage(newLine);

                } else {
                    MWLogger.errLog(STR."Null listener: \{newLine}");
                }
            }
            MWLogger.errLog("ReaderThread: stopping gracefully.");

        } catch (IOException e) {
            if (keepGoing) {
                pleaseStop();
                MWLogger.errLog("ReaderThread Error");
                MWLogger.errLog(e);
                _connectionHandler.shutdown(true);
            }
        }
    }

    private String readLine() throws IOException {
        try {
            int n = 0;
            int i;
            while ((i = _sis.read()) != NL) {
                rlBuffer[n++] = (byte) i;
            }
            // rlBuffer[n++] = (byte) NL;
            byte[] a = new byte[n];
            System.arraycopy(rlBuffer, 0, a, 0, n);
            return new String(a, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IOException();
        }
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

    private void inflate(String command) throws Exception {
        String[] args = decompose(command);
        int size = Integer.parseInt(args[1]);
        int fullSize = 29999;

        //just in case
        if (args.length > 2) {fullSize = Integer.parseInt(args[2]);}

        byte[] compressedBytes = new byte[size];
        byte[] rawBytes = new byte[fullSize];
        // use an Inflater instead of InflaterInputStream so we don't
        // have to worry about the internal IIS buffers screwing our stream
        // position.
        int totalRead = 0;
        while (totalRead < size) {
            totalRead += _sis.read(compressedBytes, totalRead, size - totalRead);
            ConnectionHandlerLocal.DEBUG(STR."< Read \{totalRead} of \{size}");
        }

        inflater.reset();
        inflater.setInput(compressedBytes, 0, size);
        int textLength = inflater.inflate(rawBytes);

        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rawBytes, 0, textLength),
              StandardCharsets.UTF_8));
        while ((command = br.readLine()) != null) {
            ConnectionHandlerLocal.DEBUG(STR."< inflated: \{command}");
            _listener.incomingMessage(command);
        }
    }

    public void pleaseStop() {
        keepGoing = false;
    }
}
