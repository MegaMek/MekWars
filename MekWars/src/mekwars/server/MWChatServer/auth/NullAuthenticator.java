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

package server.MWChatServer.auth;

import server.MWChatServer.MWChatClient;
import server.MWChatServer.MWChatServer;

/**
 * Authenitcator that authenticates everybody
 * also serves as superclass for "real" authenticators
 */
public class NullAuthenticator implements IAuthenticator {
	protected MWChatServer _server;
	/** Boolean indicating whether users not in the database are allowed access. */
	protected boolean _allowGuests;
	/** Boolean indicating whether guests should be stored in the database. */
	protected boolean _storeGuests;
	
	public NullAuthenticator(MWChatServer server, boolean allowGuests, boolean storeGuests) {
		_server = server;
		_allowGuests = allowGuests;
		_storeGuests = storeGuests;
	}

	/** handles username conflicts by appending integers until it finds an unused one */	
    public Auth authenticate(MWChatClient client, String password) throws Exception{
    	// allow/store Guests ignored
    	String newUserId = client.getUserId();
    	/*int i = 1;
    	while (getUserId(newUserId) != null) {
    		newUserId = newUserId + i;
    		i++;
    	}*/
        return new Auth(newUserId, GUEST);
    }
    
	public String getUserId(String target) {
		MWChatClient c = _server.getClient(target);
    	return c == null ? target : c.getUserId();
    }
    
}
