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

package mekwars.server.campaign.commands;

import server.campaign.votes.Vote;


public class RemoveVoteCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

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

        server.campaign.SPlayer castingPlayer = server.campaign.CampaignMain.cm.getPlayer(Username);
        String recipientName = "";//blank string

        try {
            recipientName = new String(command.nextToken()).toString();
        }//end try
        catch (NumberFormatException ex) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:RemoveVote command failed. Check your input. It should be something like this: /c removevote#name",
                  Username,
                  true);
            return;
        }//end catch

        //break out if a player is trying to vote for himself
        if (Username.equals(recipientName)) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not vote for youself.", Username, true);
            return;
        }

        //break out if voting isnt enabled on the server
        boolean canVote = Boolean.parseBoolean(server.campaign.CampaignMain.cm.getConfig("VotingEnabled"));
        if (!canVote) {
            server.campaign.CampaignMain.cm.toUser("AM:Voting is disabled on this server.", Username, true);
            return;
        }

        //get all votes cast by the player issuing the command
        java.util.Vector<Vote> castersVotes = server.campaign.CampaignMain.cm.getVoteManager()
                                                    .getAllVotesBy(castingPlayer);

        //break out if the player has no outstanding votes to remove
        if (castersVotes.isEmpty()) {
            server.campaign.CampaignMain.cm.toUser("AM:You have not cast any votes. Removal is impossible.",
                  Username,
                  true);
            return;
        }

        //get the SPlayer who is receiving for the next couple of checks
        server.campaign.SPlayer recipientPlayer = server.campaign.CampaignMain.cm.getPlayer(recipientName);

        //break out if the recieving player isnt known
        if (recipientPlayer == null) {
            server.campaign.CampaignMain.cm.toUser("AM:You can't remove a vote for a player who doesn't exist.",
                  Username,
                  true);
            return;
        }

        //break out if player has no votes cast for recipient
        java.util.Enumeration<Vote> e = castersVotes.elements();
        boolean hasVoteForRecipient = false;
        Vote v = null;
        while (e.hasMoreElements() && !hasVoteForRecipient) {
            v = e.nextElement();
            if (v.getRecipient().equals(recipientName)) {
                hasVoteForRecipient = true;
            }
        }//end while(more elements)

        /*
         * The last vote drawn from the enumeration has the proper recipient, if
         * hasVoteForRecipient is true, because the loop ends before a replacement
         * element is drawn. If true, attempt to remove the vote. If false, break the
         * bad news to the player.
         */

        if (!hasVoteForRecipient) {
            server.campaign.CampaignMain.cm.toUser("AM:You have not voted for this player.", Username, true);
            return;
        }

        //else if
        boolean voteRemoved = server.campaign.CampaignMain.cm.getVoteManager().removeVote(v);
        if (!voteRemoved) {
            server.campaign.CampaignMain.cm.toUser("AM:There was an error removing the vote. Please contact your " +
                                                         "server admin or file a bug report.", Username, true);
            return;
        }

        //else
        server.campaign.CampaignMain.cm.toUser("AM:Your vote for " + recipientName + " has been removed.",
              Username,
              true);
    }
}
