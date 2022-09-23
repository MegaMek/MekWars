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
package server.MWChatServer.commands;

/**
 * This class describes the protocol between the server and the client.
 * Contained in here are a whole bunch of constants that represent
 * commands going over the wire.  A command consists of string arguments
 * separated by DELIMITER.  The recipient of a command determines what
 * the command is by looking at the first argument.  The first argument
 * will be a string from this class (at least for the core set of commands).
 * <p>
 * There are two helper classes for creating valid commands (one server,
 * one client).
 * <p>
 * <b>Note:</b> Consult the protocol documentation instead of this
 * class.  It's messy in here.
 *
 * @see com.lyrisoft.chat.server.local.CommandMakerLocal
 * @see com.lyrisoft.chat.server.remote.CommandMakerRemote
 */
public interface ICommands {

    /**
     * The delimiter.  A tab character.
     */
	public static final String DELIMITER = "\t";

	/** if you understand this you are a 1.1-compliant client.
	 *  Following DEFLATED + DELIMITER is the number of bytes in the undeflated text.  This will be
	 *  a maximum of 29999, so you don't have to buffer more than that.
     *  com.carnageblender.chat.net gives an example implemenation. */
	public static final String DEFLATED = "/deflated";

    //////////////////////////////////////////////////////////////////////////
    // client - to - server
    //////////////////////////////////////////////////////////////////////////
    /**
     * Request server statitics<p>
     *
     * args: none<br>
     * acknowledgement: none
     */
    public static final String STATS = "/stats";

    /**
     * Sign on to server.<p>
     *
     * args: username, (password)<br>
     * acknowledgement: SIGNON_ACK
     * @see #SIGNON_ACK
     */
    public static final String SIGNON = "/signon";

    /**
     * Quit, logout, or exit from the server.<p>
     *
     * args: none<br>
     * acknowledgement: the server will hang up
     */
    public static final String SIGNOFF = "/quit";

    /**
     * Request some info about a user.<p>
     *
     * args: userId<br>
     * acknowledgement: plain text
     */
    public static final String WHOIS = "/whois";

    /**
     * Request help in general or help about a specific command.<p>
     *
     * args: a specific command (optional)
     * acknowledgement: plain text
     */
    public static final String HELP = "/help";

    /**
     * Version<p>
     *
     * Tell the server about what version of the client this is.
     * This can be a freeform string.  The first space-delmited token
     * should be the protocol version implemented:
     * 1.0 = basic; 1.1 = basic + deflate.
     * The default implementation sends "NFC Classic.
     * Java version <vendor> - <version> on <os>".
     *
     * It is suggested that writers of new clients send this 
     * command once, after authentication
     * takes place.<p>
     *
     * args: the version string<br>
     * acknowledgement: none
     */
    public static final String VERSION = "/version";

    /**
     * Kick<p>
     *
     * Kick somebody out of a room.<p>
     *
     * args: room, userid, (message)<br>
     * acknowledgement: a confirmation message
     */
    public static final String KICK = "/kick";

    /**
     * Invite<p>
     *
     * Invite somebody to a room.<p>
     *
     * args: room, userid<br>
     * acknowledgement: a confirmation message
     *
     */
    public static final String INVITE = "/invite";
    
    /**
     * UnInvite<p>
     *
     * UnInvite somebody from a room.<p>
     *
     * args: room, userid<br>
     * acknowledgement: a confirmation message
     *
     */
    public static final String UNINVITE = "/uninvite";


	/**
	 * IgnoreList<p>
	 *
	 * List ignored users
	 *
	 * args: none
	 * acknowledgement: list of users
	 */
	public static final String IGNORE_LIST = "/ignorelist";    

	/**
	 * Mail<p>
	 *
	 * Queue mail for delivery to user
	 *
	 * args: user, message body
	 * acknowledgement: MAIL_ACK
	 */
	public static final String MAIL = "/mail";    

	/**
	 * Mail Queued<p>
	 *
	 * ack for Mail
	 *
	 * args: user
	 */
	public static final String MAIL_ACK = "/ack_mail";    

	/**
	 * Ignore<p>
	 *
	 * Mute Messages of another User.<p>
	 *
	 * args: userid of user to ignore, (message)<br>
	 * acknowledgement: a general message
	 */
	public static final String IGNORE = "/ignore";    

    /**
     * Unignore<p>
     *
     * Unmute Messages of another User.<p>
     *
     * args: userid of user to remember, (message)<br>
     * acknowledgement: a general message
     */
    public static final String UNIGNORE = "/unignore";    

    //////////////////////////////////////////////////////////////////////////
    // bidirectional
    //////////////////////////////////////////////////////////////////////////
    /**
     * "say" something to everybody in a room.<p>
     *
     * <i>When sent from the client...</i><br>
     * args: room, message<br>
     * acknowledgement: a SAY_TO_ROOM message from the server<p>
     *
     * <i>When sent from the server...</i><br>
     * args: user_who_is_talking, room, message
     */
    public static final String SAY_TO_ROOM = "/sayroom";

    /**
     * "say" something "privately" to just one user.<p>
     *
     * <i>When sent from the client...</i><br>
     * args: recipient, message<br>
     * acknowledgement: none<p>
     *
     * <i>When sent from the server...</i><br>
     * args: sender,  message
     */
    public static final String SAY_TO_USER = "/msg";

    /**
     * "emote" something<p>
     *
     * <i>When sent from the client...</i><br>
     * args: room, emote<br>
     *
     * <i>When sent from the server...</i><br>
     * args: sender, room, message
     *
     * @see #ROOM_MSG
     */
    public static final String EMOTE_TO_ROOM = "/me";

    /**
     * "emote" something, privately, to only one user<p>
     *
     * <i>When sent from the client...</i><br>
     * args: recipient, emote<br>
     * acknowledgement: none<p>
     *
     * <i>When sent from the server...</i><br>
     * args: sender,  message
     */
    public static final String EMOTE_TO_USER = "/mesg";

    /**
     * Join a room.<p>
     *
     * <i>When sent from the client...</i><br>
     * args: room<br>
     * acknowledgement: JOIN_ROOM_ACK<p>
     *
     * <i>When sent from the server...</i><br>
     * Denotes that a new user just joined a room, and will only
     * be sent to clients who are currently "in" that room.<br>
     * args: user, room
     *
     * @see #JOIN_ROOM_ACK
     */
    public static final String JOIN_ROOM = "/join";

    /**
     * Part, or leave, a room.<p>
     *
     * <i>When sent from the client...</i><br>
     * args: room<br>
     * acknowledgement: PART_ROOM_ACK<p>
     *
     * <i>When sent from the server...</i><br>
     * Denotes that a user just left a room, and will only be sent
     * to clients who are currently "in" that room.<br>
     * args: user, room
     *
     * @see #PART_ROOM_ACK
     */
    public static final String PART_ROOM = "/leave";

    /**
     * Request (or send) a list of the users in a particular room.<p>
     *
     * <i>When sent from the client...</i><br>
     * args: room<br>
     * acknowledgement: GET_USERS_IN_ROOM<p>
     *
     * <i>When sent from the server...</i><br>
     * A response to a client request.<br>
     * args: room, room, ........
     *
     */
    public static final String GET_USERS_IN_ROOM = "/who";

    /**
     * Request (or send) a list of the users currently on the server.<p>
     *
     * <i>When sent from the client...</i><br>
     * args: none<br>
     * acknowledgement: GET_USERS_ON_SERVER<p>
     *
     * <i>When sent from the server...</i><br>
     * A response to a client request.<br>
     * args: user, user, ........
     *
     */
    public static final String GET_USERS_ON_SERVER = "/users";

    /**
     * Request (or send) a list of the users currently on the server.<p>
     *
     * <i>When sent from the client...</i><br>
     * args: none<br>
     * acknowledgement: GET_ROOMS<p>
     *
     * <i>When sent from the server...</i><br>
     * A response to a client request.<br>
     * args: room, number of users, room, number of users, ........
     *
     */
    public static final String GET_ROOMS = "/rooms";

    /**
     * Ping<p>
     *
     * <i>When sent from the client...</i><br>
     * args: current time (milliseconds)<br>
     * acknowledgement: PONG<p>
     *
     * <i>When sent from the server...</i><br>
     * Notify a user that he is being pinged<br>
     * args: current time (milliseconds)<br>
     * acknowledgement: PONG
     *
     * @see #PONG
     */
    public static final String PING = "/ping";

    /**
     * Pong<p>
     *
     * <i>When sent from the client...</i><br>
     * Response to a ping<br>
     * args: current time (milliseconds)<br>
     * acknowledgement: none<p>
     *
     * <i>When sent from the server...</i><br>
     * The final reply to a PING<br>
     * args: current time (milliseconds)<br>
     * acknowledgement: none
     *
     * @see #PING
     */
    public static final String PONG = "/pong";

    /**
     * Kill<p>
     *
     * <i>When sent from the client...</i><br>
     * Request to kill somebody<br>
     * args: user to kill<br>
     * acknowledgment: ACK_KILL<p>
     *
     * <i>When sent from the server...</i><br>
     * Tells you that you have been killed.<br>
     * args: the user who killed you, a message<p>
     */
    public static final String KILL = "/kill";


    //////////////////////////////////////////////////////////////////////////
    // server - to - client
    //////////////////////////////////////////////////////////////////////////
    /**
     * Indicates an error message that should be displayed by the client<p>
     *
     * args: the message
     */
    public static final String ERROR = "/err";

    /**
     * Acknowledge that a room was successfully joined.<p>
     *
     * args: the room<br>
     * in response to: JOIN_ROOM
     *
     * @see #JOIN_ROOM
     */
    public static final String JOIN_ROOM_ACK = "/ack_join";

    /**
     * Acknowledge that a room was successfully parted.<p>
     *
     * args: the room<br>
     * in response to: PART_ROOM
     *
     * @see #PART_ROOM
     */
    public static final String PART_ROOM_ACK = "/ack_part";

    /**
     * Acknowledge that thes that a signon was successful<p>
     *
     * args: none<br>
     * in response to: SIGNON<br>
     *
     * @see #SIGNON
     */
    public static final String SIGNON_ACK = "/ack_signon";

    /**
     * Indicates that a message should be displayed on the client as
     * coming from a particular room<p>
     *
     * args: room, the message<br>
     * in response to: EMOTE, any time the server feels like sending
     * a message that logically belongs in a room.
     *
     * @see #EMOTE_TO_ROOM
     */
    public static final String ROOM_MSG = "/room_msg";

    public static final String NO_INVITE = "/noinvite";
    public static final String ROOM_ACCESS_DENIED = "/roomaccessdenied";
    public static final String ALREADY_SIGNED_ON = "/alreadyon";
    public static final String INVALID_CHARACTER = "/badchars";

    public static final String NO_SUCH_USER = "/nosuchuser";
    public static final String ACCESS_DENIED = "/denied";
    public static final String ACK_KILL = "/ack_kill";

    public static final String USER_DIFF = "/user";

    public static final String ROOM_DIFF = "/room";
    
    // -------------------------------------------------------------------
    public static final String HYPERLINK = "/link";

    public static final String SHUTDOWN = "/shutdown";


    // -------------------------------------------------------------------
    public static final String REMOTE_CLIENT_MESSAGE = "/rclient";


    public static final String ROOM_USER_DIFF = "/roomuser";


    public static final String OP = "/op";
}


