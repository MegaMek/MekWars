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

package server.MWChatServer;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.zip.Deflater;

import common.util.MWLogger;
import server.MWChatServer.commands.ICommands;
import server.campaign.CampaignMain;

/**
 * Constantly reads from the BufferedReader. Notifies the MWChatServerLocal via
 * the incomingMessage() method
 */
public class WriterThread extends Thread {
    protected static final int MAX_DEFLATED_SIZE = 29999;

    protected Socket _socket;
    protected PrintWriter _out;
    protected Deflater _deflater = new Deflater();
    protected byte[] _deflatedBytes = new byte[WriterThread.MAX_DEFLATED_SIZE];
    protected boolean _keepGoing = true;
    protected LinkedList<String> _messages = new LinkedList<String>();
    protected String _host;

    public WriterThread(Socket socket, PrintWriter out, String host) {
        super("WriterThread " + host);
        _socket = socket;
        _out = out;
        _host = host;
    }

    public void queueMessage(String message) {
        synchronized (_messages) {
            _messages.add(message);
        }
    }

    @Override
    public synchronized void run() {
        while (_keepGoing) {
            if (_keepGoing) {
                try {

                    if ((_socket == null) || _socket.isClosed() || !_socket.isConnected() || _socket.isInputShutdown() || _socket.isOutputShutdown()) {
                        pleaseStop();
                        return;
                    }
                    long start = System.currentTimeMillis();
                    flush();
                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed < 20) {
                        this.wait(20 - elapsed);
                    }
                } catch (InterruptedException e) {
                    MWLogger.errLog(e);
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            }
        }
    }

    /**
     * Called by dispatcher. Sends queued messages to a downstream client. Small
     * messages are sent uncompressed, but large items are GZIP'ed before
     * transmission.
     */
    public void flush() {

        // no need to synchronize the size() call, we don't care
        // if we get the wrong answer once in a while :P
        if (_messages.size() == 0) {
            return;
        }

        /*
         * Lock the message queue, check size of the contents and determine
         * whether the contents should be sent raw or be compressed.
         */
        synchronized (_messages) {

            StringBuilder sb = new StringBuilder();
            while (!_messages.isEmpty()) {

                // add to buffer, and break messages with newlines
                sb.append(_messages.remove());
                sb.append("\n");

                if (CampaignMain.cm.getBooleanConfig("SendSingleCommandAtATime")) {
                    break;
                }

                /*
                 * if the buffer exceeds 9000 chars, compress and send
                 * immedaitely. one server was crashing as soon as an outbound
                 * buffer huit 14k chats. This prevents the buffer overload, but
                 * costs bandwidth :-(
                 */
                if (sb.length() >= 9000) {
                    deflateAndSend(sb.toString());
                    sb.setLength(0);
                }
            }

            String s = sb.toString();

            // If the message is brief (under 200 chars), send uncompressed,
            // then return.
            if (s.length() < 200) {
                try {
                    // MWLogger.warnLog("Client: " +
                    // _client.getUserId() + " /" + _client.getHost() +
                    // " Size: " + s.length() + " Message: " + s);
                    MWLogger.debugLog("Sending data to " + _host + ":Size:" + s.length());
                    _out.print(s);
                    _out.flush();
                } catch (Exception ex) {
                    MWLogger.errLog("Socket error; shutting down client at " + _host);
                    MWLogger.errLog(ex);
                    pleaseStop();
                    try {
                        _socket.close();
                    } catch (Exception se) {
                        MWLogger.errLog(se);
                    }
                }
                return;
            }

            // 9000 chars > message > 200 chars. Send compressed.
            deflateAndSend(s);
        }
    }

    /**
     * Deflate a string, then send it to the downstream client.
     * 
     * @param s
     *            - String to deflate
     */
    private void deflateAndSend(String s) {

        try {
            byte[] rawBytes = s.getBytes("UTF8");
            _deflatedBytes = new byte[s.length()];
            _deflater.reset();
            _deflater.setInput(rawBytes);
            _deflater.finish();

            int n = _deflater.deflate(_deflatedBytes);// should always be
                                                      // nonzero since we called
                                                      // finish()

            // MWLogger.warnLog("Deflating Message for " +
            // _client.getUserId()+ "/" + _client.getHost() + " from " +
            // s.length() + " to " + n + " Message: " + s);

            /*
             * @NFC comment
             * 
             * we need to include the number of bytes so client can easily read
             * the deflated section into a byte array and decompress. (At first
             * I wanted to just create an InflaterInputStream over the socket on
             * the client side, but it turns out that it's @$%^ _impossible_ to
             * get a truly unbuffered reader and the IIS and normal stream
             * reader cannot coexist no matter how hard you try.)
             */

            /*
             * Old NFCChat println command, collapsed into deflate b/c this is
             * the only place it was used. Combine a prefix indicating deflation
             * with a delimiter and the byte array, then print to the
             * PrintWriter.
             */
            String o = ICommands.DEFLATED + ICommands.DELIMITER + n + ICommands.DELIMITER + s.length() + "\n";
            _out.print(o);

            /*
             * End of NFC prinln, resumption of deflateAndSend.
             */
            MWLogger.debugLog("Sending deflated data to " + _host + ":Size:" + s.length() + ":Deflated Size:" + Integer.toString(n));
            _out.flush();
            _socket.getOutputStream().write(_deflatedBytes, 0, n);
            _socket.getOutputStream().flush();

        } catch (Exception e) {
            MWLogger.errLog("Socket error; shutting down client");
            MWLogger.errLog(e);
            pleaseStop();
            try {
                _socket.close();
            } catch (Exception se) {
                MWLogger.errLog(se);
            }
            // Commenting out for now. letting the socket get closed in the
            // readerthread code. --Torren
            // shutdown(true);
        }

    }

    void pleaseStop() {
        _keepGoing = false;
    }

}
