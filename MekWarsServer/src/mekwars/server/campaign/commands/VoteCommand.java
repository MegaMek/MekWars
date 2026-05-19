/*
 * Copyright (C) 2004 - nmorris (urgru@users.sourceforge.net)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekwars.server.campaign.commands;

import java.util.StringTokenizer;

import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SPlayer;
import mekwars.server.campaign.votes.Vote;
import mekwars.server.campaign.votes.VoteManager;

public class VoteCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser(STR."AM:Insufficient access level for command. Level: \{userLevel}. Required: \{accessLevel}.",
                      Username,
                      true);
                return;
            }
        }

        SPlayer castingPlayer = CampaignMain.campaignMain.getPlayer(Username);

        String recipientName = "";//blank string
        int type = Vote.ABSTAIN_VOTE;//null vote type

        try {
            recipientName = command.nextToken();
            type = Integer.parseInt(command.nextToken());
        }//end try
        catch (NumberFormatException ex) {
            CampaignMain.campaignMain.toUser(
                  "AM:Vote command failed. Check your input. It should be something like this: /c vote#Name#2",
                  Username,
                  true);
            return;
        }//end catch

        //break out if voting isnt enabled on the server
        boolean canVote = Boolean.parseBoolean(CampaignMain.campaignMain.getConfig("VotingEnabled"));
        if (!canVote) {
            CampaignMain.campaignMain.toUser("AM:Voting is disabled on this server.", Username, true);
            return;
        }

        //break out if a player is trying to vote for himself
        if (castingPlayer.getName().equals(recipientName)) {
            CampaignMain.campaignMain.toUser("AM:You may not vote for yourself.", Username, true);
            return;
        }

        //break out on unknown vote type
        if (type < Vote.ABSTAIN_VOTE || type > Vote.NEGATIVE_VOTE) {
            CampaignMain.campaignMain.toUser(STR."AM:You tried to use an illegal vote type. Types are:<br> Positive: \{Vote.POSITIVE_VOTE}<br> Negative: \{Vote.NEGATIVE_VOTE}<br> Abstain: \{Vote.ABSTAIN_VOTE}",
                  Username,
                  true);
            return;
        }

        //break out if the player doesnt have enough votes to cast again
        int votesCast = CampaignMain.campaignMain.getVoteManager().getAllVotesBy(castingPlayer).size();
        if (votesCast > castingPlayer.getNumberOfVotesAllowed()) {
            CampaignMain.campaignMain.toUser("AM:You have no votes left to cast. Retract one of your current" +
                                                   "votes and try again.", Username, true);
            return;
        }

        //get the SPlayer who is receiving for the next couple of checks
        SPlayer recipientPlayer = CampaignMain.campaignMain.getPlayer(recipientName);

        //break out if the receiving player isnt known
        if (recipientPlayer == null) {
            CampaignMain.campaignMain.toUser("AM:The player you tried to vote for doesn't exist.",
                  Username,
                  true);
            return;
        }

        //break out if receiving player isnt in the same faction as the caster
        if (!castingPlayer.getMyHouse().equals(recipientPlayer.getMyHouse())) {
            CampaignMain.campaignMain.toUser("AM:You may only vote for players in your own faction.",
                  Username,
                  true);
            return;
        }

        //Now create the vote.
        Vote toCast = new Vote(type, castingPlayer.getName(), recipientName);

        //Grab the VoteManager to run a duplication check.
        //Make sure a matching vote isn't already being stored.
        VoteManager voteManager = CampaignMain.campaignMain.getVoteManager();

        //break out if the caster has already voted for this player
        if (voteManager.checkForDuplicate(toCast)) {
            CampaignMain.campaignMain.toUser("AM:You have already cast a vote for this player.", Username, true);
            return;
        }

        //breaks all passed. try to add the vote to the VoteManager's collection
        boolean voteAdded = voteManager.addVote(toCast);

        if (!voteAdded) {
            CampaignMain.campaignMain.toUser(
                  "AM:Your vote was not counted. This is a catchall error -- please contact your server " +
                        "admin and ask them to investigate, or file a bug report.",
                  Username,
                  true);
            return;
        }

        //vote was added properly
        CampaignMain.campaignMain.toUser(STR."AM:You have cast a vote for \{recipientName}", Username, true);
        return;

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
}
