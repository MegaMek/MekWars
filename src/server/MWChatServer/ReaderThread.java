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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import common.util.MWLogger;
import server.MWChatServer.commands.ICommands;

/**
 * Constantly reads from the BufferedReader.
 * Notifies the MWChatServerLocal via the incomingMessage() method
 */
public class ReaderThread extends Thread {
    protected IConnectionListener _connectionListener;
    protected BufferedReader _in;
    protected InputStream _inputStream;
    protected boolean _keepGoing = true;
    protected ConnectionHandler _connectionHandler;

    public ReaderThread(ConnectionHandler handler, IConnectionListener listener, InputStream in) {
        super("ReaderThread "+ handler._client.getUserId());
        _connectionHandler = handler;
        _connectionListener = listener;
        _inputStream = in;
        try {
			_in = new BufferedReader(new InputStreamReader(in, "UTF8"));
        } catch (Exception e) {
        	MWLogger.errLog(e);
        }
    }
    
    public void normalRun() {
        while ( _keepGoing ){
            try {
                String newLine;
                while ((newLine = _in.readLine()) != null ) {
                    	_connectionHandler.setLastReceived(System.currentTimeMillis());
        				if (!newLine.startsWith(ICommands.PONG)) {	
                            _connectionListener.incomingMessage(newLine);
        				}
                }
                
                //Socket is closed let it die.
                if ( _in.readLine() == null )
                    pleaseStop();
            }
    		catch (SocketException se) {
                pleaseStop();
                //MWLogger.errLog(_connectionHandler._client._userId+" Disconnected. Socket exception.");
                //MWLogger.errLog(se);
    		}//Socket Read timed out keep going.
            catch ( SocketTimeoutException ste ){
            }
            catch (Exception ex) {
    			// including but not limited to IOException
    			// -- in particular if the message handler croaks we want to know how/why
                pleaseStop();//potential fix for MMNET crashing issue? @urgru 4.08.06
                MWLogger.errLog(ex); 
            }
        }
    }
    
    @Override
	public synchronized void run() {
        normalRun();
        _connectionHandler.shutdown(true);
    }
    
    void pleaseStop() {
        _keepGoing = false;
    }
}
