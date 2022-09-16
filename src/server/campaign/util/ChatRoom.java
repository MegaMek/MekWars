/*
 * MekWars - Copyright (C) 2009
 * 
 * original author: jtighe (torren@users.sourceforge.net)
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

package server.campaign.util;

import java.util.Vector;

public class ChatRoom {

    private Vector<String> roomList;
    private boolean isPrivate = false;
    private String chatRoomName;
    private String chatRoomOwnerName;

    public ChatRoom(String name, String owner, boolean isPrivate) {
        roomList = new Vector<String>(10);
        roomList.add(owner.toLowerCase());
        chatRoomOwnerName = owner.toLowerCase();
        this.isPrivate = isPrivate;
        chatRoomName = name;
    }

    public String getRoomName() {
        return chatRoomName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isOwner(String player) {
        return chatRoomOwnerName.equalsIgnoreCase(player);
    }

    public String getOwnerName() {
        return chatRoomOwnerName;
    }

    public String getRoomList() {
        StringBuffer sb = new StringBuffer("<html>");

        for (String user : roomList) {
            sb.append(user);
            sb.append("<br>");
        }

        sb.append("</html>");

        return sb.toString();
    }

    public Vector<String> getRoomVector() {
        return roomList;

    }

    public void addPlayer(String player) {
        if (!roomList.contains(player.toLowerCase())) {
            roomList.add(player.toLowerCase());
        }
    }

    public void removePlayer(String player) {
        roomList.remove(player.toLowerCase());
    }
}