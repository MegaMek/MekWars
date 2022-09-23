package server.campaign.operations.resolvers;

import java.util.Vector;

import server.campaign.SPlayer;

public class Team {
	public Vector<SPlayer> players;
	public int teamID;

	public Team(int id, Vector<SPlayer> p) {
		teamID = id;
		players = p;
	}
	
	public Team() {
		players = new Vector<SPlayer>();
	}
	
	public Vector<SPlayer> getPlayers() {
		return players;
	}
	
	int getID() {
		return teamID;
	}
	
	boolean playerPlaysFor(String name) {
		for (SPlayer p : players) {
			if (p.getName().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}
}