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

/**
 * Interface for objects that will handle authentication.  The server will delegate signOn
 * calls to us.
 */
public interface IAuthenticator {
	
	//global & per-room
    public static final int NONE = 0;
    public static final int GUEST = 1; // same as user
	public static final int REGISTERED = 2; // don't confuse with moderator
	public static final int MODERATOR = 100;
    public static final int ADMIN = 200;
	public static final int SERVER = 1000;
	
	/** must implement constructor taking a ChatServer and two boolean arguments -- see NullAuthenticator */
	
	/**
	 * Check the authentication of a user.
	 * @param userId the user id
	 * @param password the password
	 * @return an Auth object that represents the userId and access level.
	 */
	public Auth authenticate(MWChatClient client, String password) throws Exception;
	
	/** return userId for target (which may have wrong capitalization) */
	public String getUserId(String target);
}
