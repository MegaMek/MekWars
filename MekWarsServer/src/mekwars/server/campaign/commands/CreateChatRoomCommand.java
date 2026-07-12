/*
 * MekWars - Copyright (C) 2009
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

package mekwars.server.campaign.commands;

import mekwars.server.campaign.CampaignMain;
import server.campaign.util.ChatRoom;

public class CreateChatRoomCommand implements Command {

    int accessLevel = 0;
    String syntax = "ChatRoomName#Private";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        // check if they player already owns a ChatRoom 1 per player.
        for (ChatRoom room : CampaignMain.campaignMain.getChatRoomList()) {
            if (room.isOwner(Username)) {
                CampaignMain.campaignMain.toUser("AM:You already own a chat room " +
                                                       room.getRoomName() +
                                                       " and cannot own another.", Username, true);
                return;
            }
        }

        try {
            String chatRoomName = command.nextToken();
            boolean isPrivate = Boolean.parseBoolean(command.nextToken());
            ChatRoom chatroom = new ChatRoom(chatRoomName, Username, isPrivate);
            CampaignMain.campaignMain.addChatRoom(chatRoomName, chatroom);
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("AM:Invalid Syntax: " + syntax, Username, true);
            return;
        }
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }
}// end DefendCommand
