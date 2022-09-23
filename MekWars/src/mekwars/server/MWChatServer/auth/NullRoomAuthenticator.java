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
import server.MWChatServer.RoomServer;

/**
 * Interface for objects that will handle authentication.  The server will delegate signOn
 * calls to us.
 */
public class NullRoomAuthenticator implements IRoomAuthenticator {
	public boolean isCreateAllowed(MWChatClient client, String room, String password) {
		return true;
	}
    
    public int getAccessLevel(MWChatClient client, RoomServer room) {
    	return client.getAccessLevel();
    }
    
    public MWChatClient getNextOp(RoomServer room) {
    	return room.getOldestClient();
    }

}
