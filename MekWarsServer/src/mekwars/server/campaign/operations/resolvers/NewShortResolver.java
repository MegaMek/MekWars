package mekwars.server.campaign.operations.resolvers;

import common.UnitFactory;

public class NewShortResolver {
    int gameID;
    ShortOpPlayers players;
    server.campaign.SPlanet planet;
    server.campaign.operations.ShortOperation op;
    UnitFactory factory;
    String opName;

    boolean canTakeLand;
    boolean canTakeUnits;
    boolean canTakeComponents;
    boolean affectsELO;

    private int calculateLandExchange() {
        return 0;
    }

    private int calculateUnitExchange() {
        return 0;
    }

    private int calculateComponentExchange() {
        return 0;
    }

    public NewShortResolver(int gameId, server.campaign.SPlanet p, server.campaign.operations.ShortOperation o,
          ShortOpPlayers sop) {
        this.gameID = gameId;
        planet = p;
        op = o;
        players = sop;
        opName = op.getName();


    }
}
