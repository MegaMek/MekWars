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

import java.net.Socket;

import common.util.MWLogger;
import server.MWChatServer.auth.IAuthenticator;
import server.MWChatServer.commands.ICommands;

/**
 * This is the representation of a Client, on the server side. All the
 * IChatClient interface methods are implemented by constructing a message (with
 * the help of CommandMakerRemote), and queuing it up to be sent to the client.
 * 
 * @see CommandMakerRemote
 */
public class MWChatClient implements IConnectionListener, ICommands {

	protected AbstractConnectionHandler _connectionHandler;
	protected MWChatServer _server;

	protected String _userId;

	private int _accessLevel = IAuthenticator.NONE;
	private long _connectionTime;

	private String _host;
	private String _clientVersion;
	private String _key = "";

	private boolean _tunneling = false;

	public MWChatClient(MWChatServer server, Socket s) throws java.io.IOException {
		_server = server;
		_connectionTime = System.currentTimeMillis();
		_host = s.getInetAddress().getHostAddress();

		// constructing our CH in three steps like this
		// looks ugly, but we do it so all FlashMWChatClient
		// has to override is createConnectionHandler.
		_connectionHandler = createConnectionHandler(s);
		((ConnectionHandler) _connectionHandler).init();
	}

	public AbstractConnectionHandler createConnectionHandler(Socket s) throws java.io.IOException {
		return new ConnectionHandler(s, this);
	}

	public boolean getTunneling() {
		return _tunneling;
	}

	/**
	 * Get the time (in milliseconds) when the user logged in
	 */
	public long getConnectionTime() {
		return _connectionTime;
	}

	/**
	 * Get the access level for this user
	 * 
	 * @see IAuthenticator
	 */
	public int getAccessLevel() {
		return _accessLevel;
	}

	/**
	 * Get the version of the client
	 * 
	 * @return the version of the client
	 */
	public String getClientVersion() {
		return _clientVersion;
	}

	/**
	 * Set the version of the client
	 * 
	 * @param version
	 *            the version
	 */
	public void setClientVersion(String version) {
		_clientVersion = version;
		//_connectionHandler.setDeflateable(version.startsWith("1.1"));
	}

	/**
	 * Get the host the user is connected from
	 */
	public String getHost() {
		return _host;
	}

	/**
	 * Set the access level for this user
	 */
	public void setAccessLevel(int level) {
		_accessLevel = level;
	}

	/**
	 * Get the user's id
	 */
	public String getUserId() {
		return _userId;
	}

	public static String getKey(String userId) {
		return userId.toLowerCase();
	}

	/**
	 * Set the user's id
	 */
	public void setUserId(String userId) {
	    
       if ( userId == null || userId.equalsIgnoreCase("null") ) {
           try{
            throw new NullPointerException();
           }catch (Exception ex){
               MWLogger.errLog("Null user in setUserId report the following error to Torren");
               MWLogger.errLog(ex);
           }
        }
		// it's possible for this to be called multiple times
		// w/ same userId thanks to the way MWChatServer & auth work.
		if (_userId == null || !_userId.equals(userId)) {
			_userId = userId;
			_key = userId == null ? null : MWChatClient.getKey(userId);
			/*
			 * List savedIgnored =
			 * _server.getIgnoreStore().getIgnoredByUser(_userId); _ignored =
			 * new HashMap(savedIgnored.size()); for (Iterator i =
			 * savedIgnored.iterator(); i.hasNext(); ) { String s =
			 * (String)i.next(); _ignored.put(s.toLowerCase(), s); }
			 */
		}
	}

	/**
	 * Do not call this method!
	 * 
	 * The proper way to kill a connection is with MWChatServer.SignOff(). Only
	 * the MWChatServer should call this.
	 */
	public void die() {
		_connectionHandler.shutdown(false);
	}

	public MWChatServer getServer() {
		return _server;
	}

	/**
	 * from the IConnectionListener interface delegated to
	 * CommandProcessorRemote.process()
	 * 
	 * @see CommandProcessorRemote#process
	 */
	public void incomingMessage(String msg) {
		// deal with message
		// MWLogger.infoLog("incoming mesage: "+msg);
		CommandProcessorRemote.process(msg, this);
	}

	/**
	 * from the IConnectionListener interface call signOff on the server
	 * 
	 * @see MWChatServer#signOff
	 */
	public void socketClosed() {
		// tell the server that the user is gone
		_server.signOff(this);
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void ackSignon(String myName) {
		if (_connectionHandler == null) {
			System.err.println("conn handler is null");
		}
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructSignonAck(myName));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void connectionLost() {
		// no implementation. if the connection is lost, we can't send a message
		// back
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void ackJoinRoom(String room) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructJoinRoomAck(room));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void ackPartRoom(String room) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructPartRoomAck(room));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void messageFromUser(String user, String room, String msg) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructRoomMessage(user, room, msg));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void roomList(String[] roomList) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructRoomListMessage(roomList));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void globalUserList(String[] users) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructGlobalUserListMessage(users));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void roomUserList(String room, String[] users) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructRoomUserListMessage(room, users));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void userJoinedRoom(String user, String room) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructUserJoinedRoomMessage(user, room));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void userPartedRoom(String user, String room, boolean signOff) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructUserPartedRoomMessage(user, room, signOff));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void generalError(String message) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructErrorMessage(message));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void generalMessage(String message) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructGeneralMessage(message));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void generalRoomMessage(String room, String message) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructGeneralRoomMessage(room, message));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void ping(String user, String arg) {
		_connectionHandler.queuePriorityMessage(CommandMakerRemote
				.constructPing(user, arg));
	}

	/**
	 * Construct and queue a message that will be sent back to the client
	 */
	public void pong(String user, String arg) {
		_connectionHandler.queueMessage(CommandMakerRemote.constructPong(user,
				arg));
	}

	/**
	 * Create a RoomJoinError message and send it to the client.
	 * 
	 * @param error
	 *            the error.
	 * @param room
	 *            the room.
	 */
	public void roomJoinError(String error, String room) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructRoomJoinError(error, room));
	}

	/**
	 * Create a SignOnError message and send it to the client.
	 * 
	 * @param error
	 *            the error.
	 * @param user
	 *            the user.
	 */
	public void signOnError(String error, String user) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructSignOnError(error, user));
	}

	/**
	 * Create an Error message and send it to the client.
	 * 
	 * @param type
	 *            the error type.
	 * @param arg
	 *            anything you'd want to add.
	 */
	public void error(String type, String arg) {
		_connectionHandler.queueMessage(CommandMakerRemote.constructError(type,
				arg));
	}

	/**
	 * Create a killed message and send it to the client.
	 * 
	 * @param killer
	 *            the user that killed this MWChatClient.
	 * @param msg
	 */
	public void killed(String killer, String msg) {
		_connectionHandler.queuePriorityMessage(CommandMakerRemote
				.constructKilled(killer, msg));
	}

	/**
	 * Create an ackkill message and send it to the client. This confirms that
	 * this MWChatClient made a kill.
	 * 
	 * @param victim
	 *            the victim.
	 */
	public void ackKill(String victim) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructAckKill(victim));
	}

	/**
	 * Send a raw String over the socket.
	 */
	public void sendRaw(String s) {
		_connectionHandler.queueMessage(s);
	}

	/**
	 * Notify this user that another user has signed on.
	 * 
	 * @param userId
	 *            the user that signed on.
	 */
	public void userSignOn(String userId) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructUserSignOn(userId));
	}

	/**
	 * Notify this user that another user has signed off.
	 * 
	 * @param userId
	 *            the user that signed off.
	 */
	public void userSignOff(String userId) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructUserSignOff(userId));
	}

	/**
	 * Notify this user that a room was created.
	 * 
	 * @param room
	 *            the room that was created.
	 */
	public void roomCreated(String room) {
		_connectionHandler.queueMessage(CommandMakerRemote
				.constructRoomCreated(room));
	}

	public String getKey() {
		return _key;
	}

	public void ackMail(String toUser) {
		_connectionHandler.queueMessage(CommandMakerRemote.constructAckMail(toUser));
	}

}
