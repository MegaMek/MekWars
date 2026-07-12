package mekwars.server.campaign.operations.resolvers;

import mekwars.common.UnitFactory;
import mekwars.server.campaign.SPlanet;
import mekwars.server.campaign.operations.ShortOperation;

public class NewShortResolver {
    private int gameID;
    private ShortOpPlayers players;
    private SPlanet planet;
    private ShortOperation op;
    private UnitFactory factory;
    private String opName;

    private boolean canTakeLand;
    private boolean canTakeUnits;
    private boolean canTakeComponents;
    private boolean affectsELO;

    public NewShortResolver(int gameId, SPlanet sPlanet, ShortOperation shortOperation, ShortOpPlayers shortOpPlayers) {
        this.gameID = gameId;
        planet = sPlanet;
        op = shortOperation;
        players = shortOpPlayers;
        opName = op.getName();
    }

    private int calculateLandExchange() {
        return 0;
    }

    private int calculateUnitExchange() {
        return 0;
    }

    private int calculateComponentExchange() {
        return 0;
    }
}
