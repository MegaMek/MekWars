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

import server.MWChatServer.commands.ICommands;

/**
 * Used to construct messages that will be sent over the socket to the client
 */
public class CommandMakerRemote implements ICommands {
    
    public static final String constructSignonAck(String myname) {
        StringBuilder sb = new StringBuilder();
        sb.append(SIGNON_ACK);
        sb.append(DELIMITER);
        sb.append(myname);
        return sb.toString();
    }

    public static final String constructJoinRoomAck(String room) {
        StringBuilder sb = new StringBuilder();
        sb.append(JOIN_ROOM_ACK);
        sb.append(DELIMITER);
        sb.append(room);
        return sb.toString();
    }

    public static final String constructErrorMessage(String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(ERROR);
        sb.append(DELIMITER);
        sb.append(msg);
        return sb.toString();
    }

    public static final String constructGeneralMessage(String msg) {
        return msg;
    }

    public static final String constructRoomMessage(String from, String room, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(SAY_TO_ROOM);
        sb.append(DELIMITER);
        sb.append(from);
        sb.append(DELIMITER);
        sb.append(room);
        sb.append(DELIMITER);
        sb.append(msg);
        return sb.toString();
    }

    public static final String constructPrivateMessage(String from, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(SAY_TO_USER);
        sb.append(DELIMITER);
        sb.append(from);
        sb.append(DELIMITER);
        sb.append(msg);
        return sb.toString();
    }

    public static final String constructSignoffAckMessage() {
        return SIGNOFF;
    }

    public static final String constructRoomUserListMessage(String room, String[] names) {
        StringBuilder sb = new StringBuilder();
        sb.append(GET_USERS_IN_ROOM);
        sb.append(DELIMITER);
        sb.append(room);
        sb.append(DELIMITER);
        for (int i=0; i < names.length; i++) {
            sb.append(names[i]);
            sb.append(DELIMITER);
        }
        return sb.toString();
    }

    public static final String constructGlobalUserListMessage(String[] names) {
        StringBuilder sb = new StringBuilder();
        sb.append(GET_USERS_ON_SERVER);
        sb.append(DELIMITER);
        for (int i=0; i < names.length; i++) {
            sb.append(names[i]);
            sb.append(DELIMITER);
        }
        return sb.toString();
    }


    public static final String constructUserJoinedRoomMessage(String user, String room) {
        StringBuilder sb = new StringBuilder();
        sb.append(ROOM_USER_DIFF);
        sb.append(DELIMITER);
        sb.append(room);
        sb.append(DELIMITER);
        sb.append("+");
        sb.append(DELIMITER);
        sb.append(user);
        return sb.toString();
    }

    public static final String constructUserPartedRoomMessage(String user, String room, 
                                                              boolean signOff) 
    {
        StringBuilder sb = new StringBuilder();
        sb.append(ROOM_USER_DIFF);
        sb.append(DELIMITER);
        sb.append(room);
        sb.append(DELIMITER);
        sb.append("-");
        sb.append(DELIMITER);
        sb.append(user);
        return sb.toString();
    }

    public static final String constructPartRoomAck(String room) {
        StringBuilder sb = new StringBuilder();
        sb.append(PART_ROOM_ACK);
        sb.append(DELIMITER);
        sb.append(room);
        return sb.toString();
    }

    public static final String constructPrivateEmoteMessage(String from, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(EMOTE_TO_USER);
        sb.append(DELIMITER);
        sb.append(from);
        sb.append(DELIMITER);
        sb.append(msg);
        return sb.toString();
    }

    public static final String constructRoomListMessage(String[] rooms) {
		StringBuilder sb = new StringBuilder();
		sb.append(GET_ROOMS);
		sb.append(DELIMITER);
		for (int i=0; i < rooms.length; i++) {
			sb.append(rooms[i]);
			sb.append(DELIMITER);
		}
		return sb.toString();
    }

    public static final String constructPing(String user, String arg) {
        StringBuilder sb = new StringBuilder();
        sb.append(PING);
        sb.append(DELIMITER);
        sb.append(user);
        sb.append(DELIMITER);
        sb.append(arg);
        return sb.toString();
    }

    public static final String constructPong(String user, String arg) {
        StringBuilder sb = new StringBuilder();
        sb.append(PONG); 
        sb.append(DELIMITER);
        sb.append(user);
        sb.append(DELIMITER);
        sb.append(arg);
        return sb.toString();
    }

    public static final String constructGeneralRoomMessage(String room, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(ROOM_MSG);
        sb.append(DELIMITER);
        sb.append(room);
        sb.append(DELIMITER);
        sb.append(msg);
        return sb.toString();
    }
    /**
     * Method constructRoomJoinError.
     * @param s
     * @param room
     * @return String
     */
    public static String constructRoomJoinError(String s, String room) {
        StringBuilder sb = new StringBuilder(s);
        sb.append(DELIMITER);
        sb.append(room);
        return sb.toString();
    }

    /**
     * Method constructSignOnError.
     * @param s
     * @param user
     * @return String
     */
    public static String constructSignOnError(String s, String user) {
        StringBuilder sb = new StringBuilder(s);
        sb.append(DELIMITER);
        sb.append(user);
        return sb.toString();
    }

    /**
     * Method constructError.
     * @param type
     * @param arg
     * @return String
     */
    public static String constructError(String type, String arg) {
        StringBuilder sb = new StringBuilder(type);
        if (arg != null) {
            sb.append(DELIMITER);
            sb.append(arg);
        }
        return sb.toString();
    }

	/**
	 * Method constructKilled.
	 * @param killer
	 * @param msg
	 * @return String
	 */
	public static String constructKilled(String killer, String msg) {
		StringBuilder sb = new StringBuilder(KILL);
		sb.append(DELIMITER);
		sb.append(killer);
		sb.append(DELIMITER);
		sb.append(msg);
		return sb.toString();
	}

	/**
	 * Method constructIgnoreList.
	 * @return String
	 */
	public static String constructIgnoreListMessage(String[] ignorees) {
		StringBuilder sb = new StringBuilder();
		sb.append(IGNORE_LIST);
		sb.append(DELIMITER);
		for (int i=0; i < ignorees.length; i++) {
			sb.append(ignorees[i]);
			sb.append(DELIMITER);
		}
		return sb.toString();
	}

    /**
     * Method constructAckKill.
     * @param victim
     * @return String
     */
    public static String constructAckKill(String victim) {
        StringBuilder sb = new StringBuilder(ACK_KILL);
        sb.append(DELIMITER);
        sb.append(victim);
        return sb.toString();
    }

    /**
     * Method constructEmote.
     * @param user
     * @param room
     * @param message
     * @return String
     */
    public static String constructEmote(String user, String room, String message) {
        StringBuilder sb = new StringBuilder(EMOTE_TO_ROOM);
        sb.append(DELIMITER);
        sb.append(user);
        sb.append(DELIMITER);
        sb.append(room);
        sb.append(DELIMITER);
        sb.append(message);
        return sb.toString();
    }

    /**
     * Method constructUserSignOn.
     * @param userId
     * @return String
     */
    public static String constructUserSignOn(String userId) {
        return constructUserDiff(userId, "+");
    }

    /**
     * Method constructUserSignOff.
     * @param userId
     * @return String
     */
    public static String constructUserSignOff(String userId) {
        return constructUserDiff(userId, "-");
    }
    
    private static String constructUserDiff(String userId, String diff) {
        StringBuilder sb = new StringBuilder(USER_DIFF);
        sb.append(DELIMITER);
        sb.append(diff);
        sb.append(DELIMITER);
        sb.append(userId);
        return sb.toString();
    }

    /**
     * Method constructRoomCreated.
     * @param room
     * @return String
     */
    public static String constructRoomCreated(String room) {
        return constructRoomDiff(room, "+");
    }

    /**
     * Method constructRoomDestroyed.
     * @param room
     * @return String
     */
    public static String constructRoomDestroyed(String room) {
        return constructRoomDiff(room, "-");
    }

    private static String constructRoomDiff(String room, String diff) {
		StringBuilder sb = new StringBuilder(ROOM_DIFF);
		sb.append(DELIMITER);
		sb.append(diff);
		sb.append(DELIMITER);
		sb.append(room);
		return sb.toString();
    }

	public static String constructAckMail(String toUser) {
		StringBuilder sb = new StringBuilder(MAIL_ACK);
		sb.append(DELIMITER);
		sb.append(toUser);
		return sb.toString();
	}
}
