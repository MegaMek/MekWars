package mekwars.common;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import mekwars.common.util.MWLogger;


public class GameWrapper implements GameInterface {

    private final Game game;

    public GameWrapper(Game game) {
        this.game = game;
    }

    public Enumeration<Entity> getDevastatedEntities() {
        return game.getDevastatedEntities();
    }

    public Enumeration<Entity> getGraveyardEntities() {
        return game.getGraveyardEntities();
    }

    public Iterator<Entity> getEntities() {
        return game.getEntitiesVector().iterator();
    }

    public Enumeration<Entity> getRetreatedEntities() {
        return game.getRetreatedEntities();
    }

    public List<String> getWinners() {
        ArrayList<String> result = new ArrayList<>();

        //TODO: Winners sometimes coming up empty. Let's see why

        List<Player> playersList = game.getPlayersList();

        MWLogger.errLog("  :: game.getPlayers(): " + playersList.toString());
        MWLogger.errLog("  :: VictoryTeam: " + game.getVictoryTeam());

        for (Player player : playersList) {
            MWLogger.errLog("  :: ==> Player: " + player.getName().trim() + " :: Team: " + player.getTeam());

            if (player.getTeam() == game.getVictoryTeam()) {
                result.add(player.getName().trim());
            }
        }
        return result;
    }

    public boolean hasWinner() {
        return game.getVictoryTeam() != Player.TEAM_NONE;
    }


}
