/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Nathan Morris (urgru@verizon.net)
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

package server.campaign.commands;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.votes.Vote;
import server.campaign.votes.VoteManager;



public class MyVotesCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		//break out if voting isnt enabled on the server
		boolean canVote = Boolean.parseBoolean(CampaignMain.cm.getConfig("VotingEnabled"));
		if (!canVote) {
			CampaignMain.cm.toUser("AM:Voting is disabled on this server.",Username,true);
			return;
		}
		
		//set up the player and get the vote manager
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		VoteManager vm = CampaignMain.cm.getVoteManager();
		
		//get all of p's positive votes
		Vector<Vote> posVotes = vm.getAllVotesBy(p, Vote.POSITIVE_VOTE);
		
		//and all negative votes by the same
		Vector<Vote> negVotes = vm.getAllVotesBy(p, Vote.NEGATIVE_VOTE);
		
		//set up the string to feed the player
		String toPlayer = "Votes Cast: <br>-----------<br>";
		
		//do the positives
		if (posVotes.size() == 0) {
			toPlayer += "- No positive votes cast.";
		}
		else {
			toPlayer += "Positive Votes Cast (" + posVotes.size() + "total):<br>";
			Enumeration<Vote> e = posVotes.elements();
			while (e.hasMoreElements()) {
				Vote currVote = e.nextElement();
				toPlayer += currVote.getRecipient() + "<br>";
			}//end while(more elements)
			toPlayer += "<br>";//and extra break to better split the two lists
		}//end else (has cast positive votes)
		
		
		//do the negatives
		if (negVotes.size() == 0) {
			toPlayer += "- No negative votes cast.";
		}
		else {
			toPlayer += "Negative Votes Cast (" + negVotes.size() + "total):<br>";
			Enumeration<Vote> e = negVotes.elements();
			while (e.hasMoreElements()) {
				Vote currVote = e.nextElement();
				toPlayer += "<br>" + currVote.getRecipient();
			}//end while(more elements)
		}//end else (has cast negative votes)
		
		//check for abstentations
		Vector<Vote> absVotes = vm.getAllVotesBy(p, Vote.ABSTAIN_VOTE);
		int numAbs = absVotes.size();
		if (numAbs > 0) {
			toPlayer += "<br><br>NOTE: You have cast " + numAbs + "abstaining vote";
			if (numAbs > 1)
				toPlayer += "s";
			toPlayer += ". Votes to abstain have no effect. You should probably remove";
			if (numAbs > 1)
				toPlayer += " it.";
			else
				toPlayer += " them.";
		}//end if(has abtaining votes)
		
		//give vote total info
		int votesCast = CampaignMain.cm.getVoteManager().getAllVotesBy(p).size();
    	int votesAllowed = p.getNumberOfVotesAllowed();
    	if (votesAllowed == votesCast) {
    		toPlayer += "<br><br>NOTE: You have cast all of your votes (" + votesCast + "/" + votesAllowed + ").";
    	}
    	else {
    		toPlayer += "<br><br>Votes Totals: " + votesCast + " votes cast. " + votesAllowed + " votes allowed " + votesCast + "/" + votesAllowed + ").<br>";
    	}
	
    	//now, give this info to the player
    	CampaignMain.cm.toUser(toPlayer,Username,true);
		
		return;
	}
}