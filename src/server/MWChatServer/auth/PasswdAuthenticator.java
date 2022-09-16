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

import java.io.IOException;

import common.util.MWLogger;
import server.MWChatServer.MWChatClient;
import server.MWChatServer.MWChatServer;
import server.util.MWPasswd;
import server.util.MWPasswdRecord;

/**
 * Authenitcator that reads from a password file.<p>
 *
 * The file is made up of colon-delimited fields:
 * <userId>:<access_level>:<password>
 * <p>
 * If a user if found in the password file, his password is checked.
 * If a user is not found in the password file, the access level IAuthenticator.USER
 * is returned.
 */
public class PasswdAuthenticator extends NullAuthenticator {
	
	public PasswdAuthenticator(MWChatServer server, boolean allowGuests, boolean storeGuests) {
		super(server, allowGuests, storeGuests);
	}
			
    @Override
	public Auth authenticate(MWChatClient client, String password) throws Exception {
    	String userId = client.getUserId();
        try {
            MWPasswdRecord record = MWPasswd.getRecord(userId, password);
            
            if (record == null) {
            	MWLogger.debugLog("record is null for: "+userId);
            	if (_allowGuests) {
            		Auth auth = super.authenticate(client, password);
            		if (_storeGuests) {
		            	MWPasswd.writeRecord(auth.getUserId(), IAuthenticator.GUEST, password);
            		}
    	            return auth;
            	} 
            	//else
            	throw new Exception(userId);
            }
            
            //else
            return new Auth(userId, record.access);
            
        }
        catch (IOException e) {
            MWLogger.errLog(e);
            throw new Exception(userId);
        }
    }
    
	/** checks all stored users besides just those currently logged on */
    @Override
	public String getUserId(String target) {
    	return MWPasswd.getUserId(target);
    }
    
}
