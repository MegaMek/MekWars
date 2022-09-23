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

/**
 * Represents a line in the passwd file.  All fields are public
 */
public class PasswdRecord {
    public String userId;
    public String passwd;
    public int access;

    /**
     * Constructor for convenience
     */
    public PasswdRecord(String userId, int access, String cryptedPasswd) {
        this.userId = userId;
        this.access = access;
        this.passwd = cryptedPasswd;
    }
}