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
public interface IRoomAuthenticator {
	/** default constructor will always be invoked by ChatServer */

	/** Call on create. */
	// we need to include password here so we can reject
	// attempts to create w/ password a room that's supposed
	// to be open to all.
	public boolean isCreateAllowed(MWChatClient client, String room, String password);
    
    /** Call on join.  returns one of the values defined by IAuthenticator. */
    // clints whose AccessLevel is MODERATOR will be added to ops list.
    public int getAccessLevel(MWChatClient client, RoomServer room);
    
    /** Call on create and whenever last op leaves.  Returns null for nobody. */
	// allows implemenation of "user longest in room gets to be op."
    public MWChatClient getNextOp(RoomServer room);
}
