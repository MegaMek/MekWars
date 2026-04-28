/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - Jason Tighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package mekwars.server.campaign.commands.mod;


/**
 * Moving the Ignore command from MWServ into the normal command structure.
 * <p>
 * Syntax  /c Ignore#Player
 */
public class IgnoreCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }

        String user = command.nextToken();
        server.MWClientInfo client = server.campaign.CampaignMain.cm.getServer().getUser(user);

        //Offline users may only be de-listed.
        if (client.getName().equals("Nobody")) {
            server.campaign.CampaignMain.cm.getServer().getIgnoreList().remove(user);
            server.campaign.CampaignMain.cm.getServer().getFactionLeaderIgnoreList().remove(client.getName());
            //MWLogger.modLog(Username + " unmuted " + client.getName());
            server.campaign.CampaignMain.cm.toUser("AM:You set " +
                                                         user +
                                                         " to be ignored to: false. He/She is currently not in the channel.",
                  Username);
            return;
        }

        //standard mute/unmute
        if (server.campaign.CampaignMain.cm.getServer().getIgnoreList().indexOf(client.getName()) == -1) {
            server.campaign.CampaignMain.cm.getServer().getIgnoreList().add(client.getName());
            //MWLogger.modLog(Username + " muted " + client.getName());
            server.campaign.CampaignMain.cm.getServer().sendChat("AM:" + Username + " muted " + client.getName());
        } else {
            server.campaign.CampaignMain.cm.getServer().getIgnoreList().remove(client.getName());
            server.campaign.CampaignMain.cm.getServer().getFactionLeaderIgnoreList().remove(client.getName());
            server.campaign.CampaignMain.cm.getServer().sendChat("AM:" + Username + " unmuted " + client.getName());
            //MWLogger.modLog(Username + " unmuted " + client.getName());
        }

    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}
