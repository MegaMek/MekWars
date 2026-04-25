package mekwars.server.campaign.operations.resolvers;

public class Team {
    public java.util.Vector<server.campaign.SPlayer> players;
    public int teamID;

    public Team(int id, java.util.Vector<server.campaign.SPlayer> p) {
        teamID = id;
        players = p;
    }

    public Team() {
        players = new java.util.Vector<server.campaign.SPlayer>();
    }

    public java.util.Vector<server.campaign.SPlayer> getPlayers() {
        return players;
    }

    int getID() {
        return teamID;
    }

    boolean playerPlaysFor(String name) {
        for (server.campaign.SPlayer p : players) {
            if (p.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
