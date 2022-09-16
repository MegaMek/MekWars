/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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
 * ClientTest.java
 *
 * Created on June 30, 2002, 5:26 PM
 */

package server;

import java.net.InetAddress;
import java.rmi.AccessException;
import java.util.Iterator;

import common.util.MWLogger;
import common.util.StringUtils;
import server.MWChatServer.MWChatClient;
import server.MWChatServer.MWChatServer;
import server.MWChatServer.auth.Auth;
import server.MWChatServer.auth.IAuthenticator;
import server.MWChatServer.commands.ICommands;


public class ServerWrapper extends MWChatServer{
	
	MWServ myServer;
	
	public static ServerWrapper createServer(MWServ server) throws Exception {
		return new ServerWrapper(server);
	}
	
	public ServerWrapper(MWServ server) throws Exception {
		super(server.getConfigParam("SERVERIP"),Integer.parseInt(server.getConfigParam("SERVERPORT")));
		this.myServer = server;
	}
	
	public void start() {
		MWLogger.mainLog("Starting");
		this.acceptConnections();
	}
	
	public MWServ getMWServ() {
		return this.myServer;
	}
	
	public void processCommand(String username, String command) {
		if (username == null) return; //happens when access is denied
		try {
			this.myServer.clientRecieve(command, username);
		} catch (Exception e) {
			MWLogger.errLog(e);
		}
	}
	
	public void sendServerMessage(String msg, String name) {
		MWChatClient client = this.getClient(MWChatServer.clientKey(name));
		if (client != null) {
			try {
				client.sendRaw("/comm" + ICommands.DELIMITER + common.comm.TransportCodec.encode(msg));
			} catch (Exception e) {
				MWLogger.errLog(e);
			}
		}
	}
	
	//this is a hack...
	//there should be comm objects
	public void broadcastComm(String command) {
		MWLogger.debugLog("Sending Broadcast Message: " + command);
		synchronized (_users) {
			for (Iterator<MWChatClient> i = _users.values().iterator(); i.hasNext(); ) {
				MWChatClient cc = i.next();
				this.sendServerMessage(command, MWChatServer.clientKey(cc));
			}
		}
	}
	
	public InetAddress getIP(String username) {
		
		try {
			MWChatClient c = this.getClient(username);
			if (c == null) {
				MWLogger.mainLog("WARNING: Tried to get the IP from " + username + ", who is not here.");
				
				/*
				 * We don't want to log out player who we can't find - logout uses getIP
				 * itself, which causes an infinite loop and kills the server. If there
				 * was a valid reason to be doing this, we can find some alternative, or
				 * simply store the IP in SPlayer @ connection time as a string and use it
				 * instead of fetching it from the client thread.
				 * 
				 * We know that this code was causing then nobodies after a /c check, so getting
				 * rid o fit may be good anyway ... but be wary of problems with offline players
				 * for the next few releases.
				 * 
				 * @urgru 2.18.06
				 */
				//this.myServer.clientLogout(username);
				
				try {
					return InetAddress.getLocalHost();
				} catch (Exception ex) {
					MWLogger.errLog(ex);
					return null;
				}
			}
			return InetAddress.getByName(c.getHost());
		}
		
		catch (Exception e) {
			MWLogger.errLog(e);
			try {
				return InetAddress.getLocalHost();
			} catch (Exception ex) {
				return null;
			}
		}
	}
	
	/** Sign on to the server
	 * @param client the MWChatClient
	 * @param password
	 * @exception AccessException is the login failed
	 *
	 * This needs to be coupled a little more (or less) with the signon command object
	 */
	@Override
	public boolean signOn(MWChatClient client, String password) throws Exception{
		
		MWLogger.infoLog(client.getUserId() + " is attempting a signon: ");
		String userId = client.getUserId();
		validateUserId(userId);
		
		Auth auth = null;
		auth = _authenticator.authenticate(client, password);
		
		client.setUserId(auth.getUserId());
		synchronized (_users) {
			if (userExists(clientKey(client))) {
				if (auth.getAccess() >= IAuthenticator.REGISTERED || (auth.getAccess() < IAuthenticator.REGISTERED && client.getUserId().startsWith("[Dedicated]"))) {
					//kill the old instance
					signOff(client.getServer().getClient(clientKey(client)));
				} else {
					//this should trigger the assignment of a nobody
					throw new Exception(ACCESS_DENIED);
				}
			}
			int access = auth.getAccess();
			client.setAccessLevel(access);
			_users.put(clientKey(client), client);
			MWLogger.infoLog(client.getUserId() + " is authenticated.  Access = " + access + (client.getTunneling() ? " (tunneling)" : ""));
			_cumulativeLogins++;
		}
		
		client.ackSignon(auth.getUserId());		
		return this.myServer.clientLogin(client.getUserId());
	}
	
	@Override
	public void signOff(MWChatClient client) {
		super.signOff(client);
		try {
			this.myServer.clientLogout(client.getUserId());
		} catch (Exception e) {
			MWLogger.errLog(e);
		}
	}
	
	/** Determines if a user id is valid.  Ensures that the name is made up of
	 * alphanumerical characters and contains no spaces.
	 */
	@Override
	protected void validateUserId(String user) throws Exception {
		if (getName().toLowerCase().equals(user.toLowerCase())) 
			throw new Exception(user+" is a reserved name");
		
			if ( StringUtils.hasBadChars(user).trim().length() > 0) {
				throw new Exception(INVALID_CHARACTER);
			}
		
	}
}