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
package mekwars.server.MWChatServer;

class TimedUserList {
    // we wrap instead of subclassing b/c we're only going to
    // provide a small subset of the Collection interface
    java.util.ArrayList<mekwars.server.MWChatServer.TimedUserList.UserRecord> _users = new java.util.ArrayList<mekwars.server.MWChatServer.TimedUserList.UserRecord>(
          2);
    int _seconds = 60 * 60 * 24;

    public TimedUserList(int seconds) {
        _seconds = seconds;
    }

    public void add(String userId) {
        synchronized (_users) {
            _users.add(new mekwars.server.MWChatServer.TimedUserList.UserRecord(userId));
        }
    }

    /**
     * determines if userId is currently in list. Also prunes outdated UserRecords.
     */
    public boolean contains(String userId) {
        long now = System.currentTimeMillis();
        synchronized (_users) {
            for (java.util.Iterator<mekwars.server.MWChatServer.TimedUserList.UserRecord> i = _users.iterator();
                  i.hasNext(); ) {
                mekwars.server.MWChatServer.TimedUserList.UserRecord u = i.next();
                if (now > u.whenAdded + _seconds * 1000) {
                    i.remove();
                } else if (u.userId.equalsIgnoreCase(userId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * records when users were kicked.
     * <p>
     * store uid instead of ChatClient in case users logs off and on again.
     */
    private static class UserRecord {
        public String userId;
        public long whenAdded;

        public UserRecord(String userId) {
            this.userId = userId;
            whenAdded = System.currentTimeMillis();
        }
    }
}
