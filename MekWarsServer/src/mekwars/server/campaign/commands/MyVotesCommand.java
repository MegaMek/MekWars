/*
 * Copyright (C) 2004 Nathan Morris (urgru@verizon.net)
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

import java.util.Enumeration;
import java.util.Vector;

import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SPlayer;
import mekwars.server.campaign.votes.Vote;
import mekwars.server.campaign.votes.VoteManager;

public class MyVotesCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser(STR."AM:Insufficient access level for command. Level: \{userLevel}. Required: \{accessLevel}.",
                      Username,
                      true);
                return;
            }
        }

        //break out if voting isnt enabled on the server
        boolean canVote = Boolean.parseBoolean(CampaignMain.campaignMain.getConfig("VotingEnabled"));
        if (!canVote) {
            CampaignMain.campaignMain.toUser("AM:Voting is disabled on this server.", Username, true);
            return;
        }

        //set up the player and get the vote manager
        SPlayer campaignMainPlayer = CampaignMain.campaignMain.getPlayer(Username);
        VoteManager voteManager = CampaignMain.campaignMain.getVoteManager();

        //get all of campaignMainPlayer's positive votes
        Vector<Vote> posVotes = voteManager.getAllVotesBy(campaignMainPlayer, Vote.POSITIVE_VOTE);

        //and all negative votes by the same
        Vector<Vote> negVotes = voteManager.getAllVotesBy(campaignMainPlayer, Vote.NEGATIVE_VOTE);

        //set up the string to feed the player
        StringBuilder toPlayer = new StringBuilder("Votes Cast: <br>-----------<br>");

        //do the positives
        if (posVotes.isEmpty()) {
            toPlayer.append("- No positive votes cast.");
        } else {
            toPlayer.append(STR."Positive Votes Cast (\{posVotes.size()}total):<br>");
            Enumeration<Vote> elements = posVotes.elements();
            while (elements.hasMoreElements()) {
                Vote currVote = elements.nextElement();
                toPlayer.append(currVote.getRecipient()).append("<br>");
            }//end while(more elements)
            toPlayer.append("<br>");//and extra break to better split the two lists
        }//end else (has cast positive votes)


        //do the negatives
        if (negVotes.isEmpty()) {
            toPlayer.append("- No negative votes cast.");
        } else {
            toPlayer.append("Negative Votes Cast (").append(negVotes.size()).append("total):<br>");
            Enumeration<Vote> elements = negVotes.elements();
            while (elements.hasMoreElements()) {
                Vote currVote = elements.nextElement();
                toPlayer.append("<br>").append(currVote.getRecipient());
            }//end while(more elements)
        }//end else (has cast negative votes)

        //check for abstentions
        Vector<Vote> absVotes = voteManager.getAllVotesBy(campaignMainPlayer, Vote.ABSTAIN_VOTE);
        int numAbs = absVotes.size();
        if (numAbs > 0) {
            toPlayer.append("<br><br>NOTE: You have cast ").append(numAbs).append("abstaining vote");
            if (numAbs > 1) {
                toPlayer.append("s");
            }

            toPlayer.append(". Votes to abstain have no effect. You should probably remove");
            if (numAbs > 1) {
                toPlayer.append(" it.");
            } else {
                toPlayer.append(" them.");
            }
        }//end if(has obtaining votes)

        //give vote total info
        int votesCast = CampaignMain.campaignMain.getVoteManager().getAllVotesBy(campaignMainPlayer).size();
        int votesAllowed = campaignMainPlayer.getNumberOfVotesAllowed();
        if (votesAllowed == votesCast) {
            toPlayer.append("<br><br>NOTE: You have cast all of your votes (")
                  .append(votesCast)
                  .append("/")
                  .append(votesAllowed)
                  .append(").");
        } else {
            toPlayer.append("<br><br>Votes Totals: ")
                  .append(votesCast)
                  .append(" votes cast. ")
                  .append(votesAllowed)
                  .append(" votes allowed ")
                  .append(votesCast)
                  .append("/")
                  .append(votesAllowed)
                  .append(").<br>");
        }

        //now, give this info to the player
        CampaignMain.campaignMain.toUser(toPlayer.toString(), Username, true);
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
