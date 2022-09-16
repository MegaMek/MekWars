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

import java.util.ArrayList;
import java.util.HashMap;

import server.MWChatServer.auth.IAuthenticator;
import server.MWChatServer.commands.ICommands;

/**
 * RoomServer.  This represents a room in the system, and is sort of a little server of
 * its own.
 */
public class RoomServer implements ICommands {
    protected String _roomName;
    protected ArrayList<MWChatClient> _users;
    protected HashMap<String,MWChatClient> _ops;
    protected TimedUserList _kickedUsers;

    protected MWChatServer _server;
    private String _password;
//    private HashSet _invitations = new HashSet();

    public RoomServer(String name, MWChatServer server) {
        _server = server;
        _roomName = name;
        _users = new ArrayList<MWChatClient>();
        _ops = new HashMap<String,MWChatClient>();
        _kickedUsers = new TimedUserList(_server.getKickBanSeconds());
    }

    public RoomServer(String name, String password, MWChatServer server) {
    	this(name, server);
        _password = password;
    }
    
    public MWChatClient getOldestClient() {
    	synchronized(_users) {
	    	return (_users.size() > 0) ? (MWChatClient)_users.get(0) : null;
    	}
    }
    
    /**
     * Get the name of this room
     */
    public String getName() {
        return _roomName;
    }

    /**
     * Tells whether or not this room is empty
     * @return true if emtpy, false otherwise
     */
    public boolean isEmpty() {
        return _users.size() == 0;
    }

    /**
     * Send a general message to all the clients in this room
     * @param message the general message
     */
    public void broadcast(String message) {
        synchronized (_users) {
            for (MWChatClient rcpt : _users) {
                rcpt.generalRoomMessage(_roomName, message);
            }
        }
    }

    /**
     * Say something to everyone in the room
     * @param sender the person doing the talking
     * @param message the message text
     */
    public void say(MWChatClient sender, String message) {
        String from = sender.getUserId();
        say(from, message);
    }

    public void say(String from, String message) {
        synchronized (_users) {
            for (MWChatClient rcpt : _users) {
                rcpt.messageFromUser(from, _roomName, message);
            }
        }
    }

    protected void notifyJoin(String userId) {
        synchronized (_users) {
            // notify the other people
            for (MWChatClient rcpt :_users) {
                rcpt.userJoinedRoom(userId, _roomName);
            }
        }
    }

    public void remoteJoin(String username, String password) {
        notifyJoin(username);
    }

    protected void notifyPart(String userId, boolean isSignoff) {
        synchronized (_users) {
            // inform the other users here that someone signed off
            for (MWChatClient rcpt : _users) {
                rcpt.userPartedRoom(userId, _roomName, isSignoff);
            }
        }
    }

    public void remotePart(String username, boolean isSignoff) {
        notifyPart(username, isSignoff);
    }
    
    /**
     * Add a user to this room.  All other room members are notified
     * @param client the client that is joining
     * @see MWChatClient#userJoinedRoom
     */
    public void join(MWChatClient client, String password)  throws Exception{

        int level = _server.getRoomAccessLevel(client, this);
        if (level == IAuthenticator.NONE
        	|| _kickedUsers.contains(client.getKey())
        	|| (_password != null && !_password.equals(password))) {
            throw new Exception("It No Workie");
        }

		synchronized (_users) {
	        if (!_users.contains(client)) {
                notifyJoin(client.getUserId());

                // add the user to the hashtable
                _users.add(client);
                
                client.ackJoinRoom(_roomName);
			}
        }
        
/*        String msg = _server.getWelcomeMsg(client, _roomName);
        if (msg != null) {
        	broadcast(msg);
        }*/

		if (level >= IAuthenticator.MODERATOR) {
			op(client);
		} else {
			if (_ops.size() == 0) {
				MWChatClient nextOp = _server.getRoomNextOp(this);
				if (nextOp != null) {
					op(nextOp);
				}
			}
		}
    }

    public void op(MWChatClient client) {
    	op(null, client);
    }

    public void deop(MWChatClient client) {
    	deop(null, client);
    }

    public void op(MWChatClient op, MWChatClient newOp) {
        synchronized (_ops) {
            if (op == null || _ops.keySet().contains(MWChatServer.clientKey(op))) {
                if (!_ops.keySet().contains(MWChatServer.clientKey(newOp))) {
                    _ops.put(MWChatServer.clientKey(newOp), newOp);
					String actor = (op == null) ? "Server": op.getUserId();
                    broadcast(Translator.getMessage("op.add", actor, newOp.getUserId()));
                }
            } else {
                op.generalError(Translator.getMessage("op.denied"));
            }
        }
    }

    public void deop(MWChatClient op, MWChatClient newOp) {
        synchronized (_ops) {
            if (op == null || _ops.keySet().contains(MWChatServer.clientKey(op))) {
                if (_ops.keySet().contains(MWChatServer.clientKey(newOp))) {
                    _ops.remove(MWChatServer.clientKey(newOp));
                    newOp.generalMessage(Translator.getMessage("op.remove.confirm", _roomName));
//					String actor = (op == null) ? "Server" : op.getUserId();
					// we don't broadcast deop; it's (almost?) never interesting
                }
                if (_ops.size() == 0) {
					MWChatClient nextOp = _server.getRoomNextOp(this);
					if (nextOp != null) {
						op(nextOp);
					}
                }
            } else {
                op.generalError(Translator.getMessage("op.denied"));
            }
        }
    }

    /**
     * Remove a user from this room.  All other room members are notified
     * @param client the client that is leaving
     * @param isSignoff true if the client is signing off; false if the client is leaving only
     *                  this room.
     * @see MWChatClient#userPartedRoom
     */
    public void part(MWChatClient client, boolean isSignoff) {
    	String userId = client.getUserId();
        synchronized (_users) {
            if (_users.remove(client)) {
            	// we only send ack if not signing off.
            	if (_server.getClient(userId) != null) {
                    client.ackPartRoom(_roomName);
                }

                notifyPart(userId, isSignoff);
            }
        }
        deop(client);
    }

    /**
     * Get the number of users in this room
     *
    public int getUserCount() {
        return _users.size();
    }
*/
    /**
     * Get a string array containing the names of all the users in this room
     
    public String[] getUsers() {
		String[] names = new String[_users.size()];
        synchronized (_users) {
        	int j = 0;
            for (MWChatClient rcpt :_users) {
                String name = rcpt.getUserId();
                names[j++] = name;
            }
        }
        return names;
    }
*/
}



