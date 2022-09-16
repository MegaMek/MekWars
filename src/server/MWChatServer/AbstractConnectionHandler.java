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

import java.net.InetAddress;

/**
 * The keeper of the Socket on the server side.  Spawns a thread for reading from the
 * socket.
 *
 * As each line is read from the socket, the server is notified, via the IConnectionListener
 * interface
 *
 * Outgoing messages are passed to the Dispatcher who queues them up 
 */
public abstract class AbstractConnectionHandler {
	
	//alas, we'll have to synchronize manually b/c of how flushDeflated() works
	//protected LinkedList<String> _messages = new LinkedList<String>();
	protected MWChatClient _client;
	protected long _lastReceived = System.currentTimeMillis(); // corresponds to timeInMillis

    public void setListener(MWChatClient listener) {
        _client = listener;
    }

    /**
     * Queue a message headed outbound
     */
	public abstract void queueMessage(String message);
	
	// for PING and anything else that shouldn't be batched by deflate    
	public abstract void queuePriorityMessage(String message);
    
    public void setLastReceived(long ms) {
    	_lastReceived = ms;
    }
    
	public void shutdown(boolean notify) {

		if (notify) {
			_client.socketClosed();
			//_client.getServer().signOff(_client);
		}
	}
	
	public InetAddress getInetAddress() {
		throw new UnsupportedOperationException();
	}
	
}
