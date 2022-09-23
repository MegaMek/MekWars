package server.campaign.operations.resolvers;

import java.util.HashMap;
import java.util.Vector;

import common.CampaignData;
import common.util.MWLogger;
import server.campaign.SPlayer;

public class ShortOpPlayers {
	private HashMap<Integer, Team> teams;
	
	public void addTeam(int id, Vector<SPlayer> p) {
		Team t = new Team(id, p);
		teams.put(Integer.valueOf(id), t);
	}
	
	/**
	 * @author Spork
	 * @param id
	 * @return Team if the id exists, null if not
	 */
	public Team getTeam(int id) {
		return teams.get(id);
	}
	
	/**
	 * Temporary method for troubleshooting
	 */
	public void reportTeams() {
		for( int id : teams.keySet()) {
			MWLogger.testLog("SOP Reporting: TeamID " + id);
			Team t = getTeam(id);
			for(SPlayer p : t.getPlayers()) {
				MWLogger.testLog("==> " + p.getName());
			}
		}
	}
	
	/**
	 * @author Spork
	 * 
	 * @param pName - the name of the player to find
	 * @return int teamID.  If the player is not found, the method returns -1
	 */
	int getTeamByPlayer(String pName) {
		for(Team t : teams.values()) {
			if(t.playerPlaysFor(pName)) {
				return t.getID();
			}
		}
		return -1;
	}
	
	public ShortOpPlayers() {
		teams = new HashMap<Integer, Team>();
	}
}
