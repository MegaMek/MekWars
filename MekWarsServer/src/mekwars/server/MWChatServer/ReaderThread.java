/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - Torren (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */


/*
 * Derived from NFCChat, a GPL chat client/server.
 * Original code can be found @ http://nfcchat.sourceforge.net
 * Our thanks to the original authors.
 */
/**
 *
 * @author Torren (Jason Tighe) 11.5.05
 *
 */

package mekwars.server.MWChatServer;
import server.MWChatServer.commands.ICommands;
import megamek.logging.MMLogger;

/**
 * Constantly reads from the BufferedReader. Notifies the MWChatServerLocal via the incomingMessage() method
 */
public class ReaderThread extends Thread {
    private static final MMLogger LOGGER = MMLogger.create(ReaderThread.class);
    protected IConnectionListener _connectionListener;
    protected java.io.BufferedReader _in;
    protected java.io.InputStream _inputStream;
    protected boolean _keepGoing = true;
    protected ConnectionHandler _connectionHandler;

    public ReaderThread(ConnectionHandler handler, IConnectionListener listener, java.io.InputStream in) {
        super("ReaderThread " + handler._client.getUserId());
        _connectionHandler = handler;
        _connectionListener = listener;
        _inputStream = in;
        try {
            _in = new java.io.BufferedReader(new java.io.InputStreamReader(in, "UTF8"));
        } catch (Exception e) {
            LOGGER.error(e, "");
        }
    }

    @Override
    public synchronized void run() {
        normalRun();
        _connectionHandler.shutdown(true);
    }

    public void normalRun() {
        while (_keepGoing) {
            try {
                String newLine;
                while ((newLine = _in.readLine()) != null) {
                    _connectionHandler.setLastReceived(System.currentTimeMillis());
                    if (!newLine.startsWith(ICommands.PONG)) {
                        _connectionListener.incomingMessage(newLine);
                    }
                }

                //Socket is closed let it die.
                if (_in.readLine() == null) {pleaseStop();}
            } catch (java.net.SocketException se) {
                pleaseStop();
                //MWLogger.errLog(_connectionHandler._client._userId+" Disconnected. Socket exception.");
                //MWLogger.errLog(se);
            }//Socket Read timed out keep going.
            catch (java.net.SocketTimeoutException ste) {
            } catch (Exception ex) {
                // including but not limited to IOException
                // -- in particular if the message handler croaks we want to know how/why
                pleaseStop();//potential fix for MMNET crashing issue? @urgru 4.08.06
                LOGGER.error(ex, "");
            }
        }
    }

    void pleaseStop() {
        _keepGoing = false;
    }
}
