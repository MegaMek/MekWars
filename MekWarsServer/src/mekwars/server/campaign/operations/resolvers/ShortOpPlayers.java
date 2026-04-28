package mekwars.server.campaign.operations.resolvers;

import common.util.MWLogger;

public class ShortOpPlayers {
    private java.util.HashMap<Integer, mekwars.server.campaign.operations.resolvers.Team> teams;

    public ShortOpPlayers() {
        teams = new java.util.HashMap<Integer, mekwars.server.campaign.operations.resolvers.Team>();
    }

    public void addTeam(int id, java.util.Vector<server.campaign.SPlayer> p) {
        Team t = new Team(id, p);
        teams.put(Integer.valueOf(id), t);
    }

    /**
     * Temporary method for troubleshooting
     */
    public void reportTeams() {
        for (int id : teams.keySet()) {
            MWLogger.testLog("SOP Reporting: TeamID " + id);
            Team t = getTeam(id);
            for (server.campaign.SPlayer p : t.getPlayers()) {
                MWLogger.testLog("==> " + p.getName());
            }
        }
    }

    /**
     * @param id
     *
     * @return Team if the id exists, null if not
     *
     * @author Spork
     */
    public Team getTeam(int id) {
        return teams.get(id);
    }

    /**
     * @param pName - the name of the player to find
     *
     * @return int teamID.  If the player is not found, the method returns -1
     *
     * @author Spork
     */
    int getTeamByPlayer(String pName) {
        for (Team t : teams.values()) {
            if (t.playerPlaysFor(pName)) {
                return t.getID();
            }
        }
        return -1;
    }
}
