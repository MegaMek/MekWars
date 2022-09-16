/*
 * Copyright (c) 2000 Lyrisoft Solutions, Inc.
 * Used by permission
 */
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
package common.campaign.clientutils.protocol;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.Inflater;

import common.util.MWLogger;

/**
 * Constantly read from the socket's input stream
 */
public class ReaderThread extends Thread {
    private boolean keepGoing = true;

    //private BufferedReader _in;

    private InputStream _sis;

    private IConnectionListener _listener;

    private IConnectionHandler _connectionHandler;

    private byte[] compressedBytes = null;

    private byte[] rawBytes = null;

    private Inflater inflater = new Inflater();

    //private Checksum checksum = new CRC32();

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

    private byte[] rlBuffer = new byte[256 * 256];

    private static final int NL = 10; // "\n" in ASCII and UTF8

    private String readLine() throws IOException{
        try {
            int n = 0;
            int i;
            while ((i = _sis.read()) != NL) {
                rlBuffer[n++] = (byte) i;
            }
            // rlBuffer[n++] = (byte) NL;
            byte[] a = new byte[n];
            System.arraycopy(rlBuffer, 0, a, 0, n);
            return new String(a, "UTF8");
        } catch (Exception e) {
            throw new IOException();
        }
    }

    /**
     * Decompose a raw message into an array of String, splitting on the
     * DELIMITER defined in ICommands.
     */

    private String[] decompose(String input) {
        StringTokenizer st = new StringTokenizer(input, IClient.DELIMITER);
        Vector<String> v = new Vector<String>(5,1);
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
        if ( args.length > 2 )
            fullSize = Integer.parseInt(args[2]);

        compressedBytes = new byte[size];
        rawBytes = new byte[fullSize];
        // use an Inflater instead of InflaterInputStream so we don't
        // have to worry about the internal IIS buffers screwing our stream
        // position.
        int totalRead = 0;
        while (totalRead < size) {
            totalRead += _sis.read(compressedBytes, totalRead, size - totalRead);
            ConnectionHandlerLocal.DEBUG("< Read " + totalRead + " of " + size);
        }

        // debugging output
        /*
         * checksum.reset(); checksum.update(compressedBytes, 0, size);
         * MWLogger.infoLog("\t...Checksum of /deflated is " +
         * checksum.getValue()); BufferedReader cbr = new BufferedReader(new
         * InputStreamReader(new ByteArrayInputStream(compressedBytes, 0,
         * size))); // ... more debugging StringBuilder sb2 = new StringBuilder();
         * for (int i = 0; i < Math.min(size, 10); i++) {
         * sb2.append(compressedBytes[i]); sb2.append(" "); }
         * //MWLogger.infoLog("\tfirst bytes are " +
         * sb2.toString()); // ... still more String s;
         * //MWLogger.infoLog("Probably useless /deflate
         * payload follows:"); while ((s = cbr.readLine()) != null) {
         * MWLogger.infoLog("\t" + s); }
         */
        inflater.reset();
        inflater.setInput(compressedBytes, 0, size);
        int textLength = inflater.inflate(rawBytes);

        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rawBytes, 0, textLength), "UTF8"));
        while ((command = br.readLine()) != null) {
            ConnectionHandlerLocal.DEBUG("< inflated: " + command);
            _listener.incomingMessage(command);
        }
    }

    @Override
	public void run() {
        try {
            String newLine;
            while (keepGoing) {
                
            	newLine = readLine();
            	if (newLine == null) {
                    pleaseStop();
                    continue;
                }
                
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
            		ConnectionHandlerLocal.DEBUG("< " + newLine);
                    _listener.incomingMessage(newLine);
                    
                } else {
                    MWLogger.errLog("Null listener: " + newLine);
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

    public void pleaseStop() {
        keepGoing = false;
    }
}
