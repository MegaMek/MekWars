package mekwars.server.campaign.operations.resolvers;

import java.util.HashMap;
import java.util.Vector;

import megamek.logging.MMLogger;
import mekwars.server.campaign.SPlayer;

public class ShortOpPlayers {
    private final static MMLogger LOGGER = MMLogger.create(ShortOpPlayers.class);
    private final HashMap<Integer, Team> teams;

    public ShortOpPlayers() {
        teams = new java.util.HashMap<>();
    }

    public void addTeam(int id, Vector<SPlayer> players) {
        Team t = new Team(id, players);
        teams.put(id, t);
    }

    /**
     * Temporary method for troubleshooting
     */
    public void reportTeams() {
        for (int id : teams.keySet()) {
            LOGGER.debug(String.format("SOP Reporting: TeamID %s", id));
            Team t = getTeam(id);
            for (SPlayer p : t.getPlayers()) {
                LOGGER.debug("==> {}", p.getName());
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
